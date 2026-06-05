package com.spmods.spgram.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import org.drinkless.tdlib.TdApi
import com.spmods.spgram.data.core.coRunCatching
import com.spmods.spgram.data.datasource.remote.AuthRemoteDataSource
import com.spmods.spgram.data.gateway.UpdateDispatcher
import com.spmods.spgram.data.gateway.isUnexpectedAuthStateError
import com.spmods.spgram.data.gateway.toAuthError
import com.spmods.spgram.data.infra.TdLibParametersProvider
import com.spmods.spgram.data.mapper.toDomain
import com.spmods.spgram.domain.repository.AuthError
import com.spmods.spgram.domain.repository.AuthRepository
import com.spmods.spgram.domain.repository.AuthStep
import com.spmods.spgram.domain.repository.AuthSubmissionStage
import com.spmods.spgram.domain.repository.AuthUiStatus

class AuthRepositoryImpl(
    private val parametersProvider: TdLibParametersProvider,
    private val remote: AuthRemoteDataSource,
    private val updates: UpdateDispatcher,
    private val scope: CoroutineScope
) : AuthRepository {
    private data class PendingAuthAction(
        val stage: AuthSubmissionStage,
        val payload: String
    )

    private companion object {
        const val SLOW_NETWORK_TIMEOUT_MS = 12_000L
        const val NETWORK_ERROR_TIMEOUT_MS = 30_000L
    }

    private val _authState = MutableStateFlow<AuthStep>(AuthStep.Loading)
    override val authState = _authState.asStateFlow()

    private val _authUiStatus = MutableStateFlow<AuthUiStatus>(AuthUiStatus.Idle)
    override val authUiStatus = _authUiStatus.asStateFlow()

    private val _errors = MutableSharedFlow<AuthError>(extraBufferCapacity = 1)
    override val errors = _errors.asSharedFlow()

    private val initMutex = Mutex()
    private var activeWatchdog: Job? = null
    private var activeWatchdogId = 0L
    private var pendingAction: PendingAuthAction? = null

    init {
        scope.launch {
            launchAuthAction {
                val state = remote.getAuthorizationState()
                handleUpdate(state)
            }

            updates.authorizationState.collect { update ->
                handleUpdate(update.authorizationState)
            }
        }
    }

    private fun handleUpdate(state: TdApi.AuthorizationState) {
        if (state is TdApi.AuthorizationStateWaitTdlibParameters) {
            sendTdLibParameters()
        }
        val domainState = state.toDomain()
        _authState.update { domainState }
        if (pendingAction != null && isExpectedNextState(pendingAction!!.stage, domainState)) {
            clearPendingAuthState()
            pendingAction = null
        }
    }

    private fun sendTdLibParameters() {
        if (!initMutex.tryLock()) return

        scope.launch {
            try {
                var attempts = 0
                while (true) {
                    val currentState = coRunCatching { remote.getAuthorizationState() }.getOrNull()
                    if (currentState != null && currentState !is TdApi.AuthorizationStateWaitTdlibParameters) {
                        break
                    }

                    val result = coRunCatching { remote.setTdlibParameters(parametersProvider.create()) }
                    if (result.isSuccess) {
                        val nextState = coRunCatching { remote.getAuthorizationState() }.getOrNull()
                        if (nextState != null) {
                            handleUpdate(nextState)
                        }
                        break
                    }

                    val error = result.exceptionOrNull()
                    if (error?.message?.contains("Parameters are already set", ignoreCase = true) == true) {
                        break
                    }

                    attempts++
                    val delayMs = (1000L * attempts).coerceAtMost(10_000L)
                    delay(delayMs)
                }
            } finally {
                initMutex.unlock()
            }
        }
    }

    private fun launchAuthAction(action: suspend () -> Unit) {
        scope.launch {
            coRunCatching { action() }
                .onFailure { emitError(it) }
        }
    }

    override fun sendPhone(phone: String) {
        submitAuthAction(AuthSubmissionStage.PHONE, phone) {
            remote.setPhoneNumber(phone)
        }
    }

    override fun resendCode() {
        launchAuthAction { remote.resendCode() }
    }

    override fun sendCode(code: String) {
        submitAuthAction(AuthSubmissionStage.CODE, code) {
            val isEmail = (_authState.value as? AuthStep.InputCode)?.isEmailCode == true
            if (isEmail) remote.checkEmailCode(code) else remote.setAuthCode(code)
        }
    }

    override fun sendPassword(password: String) {
        submitAuthAction(AuthSubmissionStage.PASSWORD, password) {
            remote.checkPassword(password)
        }
    }

    override fun registerUser(firstName: String, lastName: String) {
        val payload = "$firstName|$lastName"
        submitAuthAction(AuthSubmissionStage.REGISTER, payload) {
            val parts = payload.split("|", limit = 2)
            remote.registerUser(parts.getOrElse(0) { firstName }, parts.getOrElse(1) { "" })
        }
    }

    override fun retryLastAction() {
        when (val action = pendingAction) {
            null -> Unit
            else -> when (action.stage) {
                AuthSubmissionStage.PHONE    -> sendPhone(action.payload)
                AuthSubmissionStage.CODE     -> sendCode(action.payload)
                AuthSubmissionStage.PASSWORD -> sendPassword(action.payload)
                AuthSubmissionStage.REGISTER -> {
                    val parts = action.payload.split("|", limit = 2)
                    registerUser(parts.getOrElse(0) { "" }, parts.getOrElse(1) { "" })
                }
            }
        }
    }

    override fun reset() {
        clearPendingAuthState()
        pendingAction = null
        _authState.update { AuthStep.InputPhone }
    }

    private fun emitError(t: Throwable) {
        clearPendingAuthState()
        _errors.tryEmit(t.toAuthError())
    }

    private fun submitAuthAction(
        stage: AuthSubmissionStage,
        payload: String,
        action: suspend () -> Unit
    ) {
        if (!canSubmitStage(stage)) return

        pendingAction = PendingAuthAction(stage, payload)
        clearPendingAuthState()
        _authUiStatus.value = AuthUiStatus.Submitting(stage)

        scope.launch {
            coRunCatching { action() }
                .onSuccess { startAuthWatchdog(stage) }
                .onFailure { throwable ->
                    if (shouldSuppressStaleAuthError(stage, throwable)) {
                        clearPendingAuthState()
                        pendingAction = null
                    } else {
                        emitError(throwable)
                    }
                }
        }
    }

    private fun startAuthWatchdog(stage: AuthSubmissionStage) {
        val watchdogId = ++activeWatchdogId
        activeWatchdog?.cancel()
        activeWatchdog = scope.launch {
            delay(SLOW_NETWORK_TIMEOUT_MS)
            if (!isWatchdogStillActive(watchdogId, stage)) return@launch
            _authUiStatus.value = AuthUiStatus.SlowNetwork(stage)

            delay(NETWORK_ERROR_TIMEOUT_MS - SLOW_NETWORK_TIMEOUT_MS)
            if (!isWatchdogStillActive(watchdogId, stage)) return@launch
            _authUiStatus.value = AuthUiStatus.NetworkError(stage)
            _errors.tryEmit(AuthError.NetworkTimeout)
        }
    }

    private fun isWatchdogStillActive(
        watchdogId: Long,
        stage: AuthSubmissionStage
    ): Boolean {
        val currentPendingAction = pendingAction ?: return false
        return activeWatchdogId == watchdogId &&
                currentPendingAction.stage == stage &&
                !isExpectedNextState(stage, _authState.value)
    }

    private fun clearPendingAuthState() {
        activeWatchdog?.cancel()
        activeWatchdog = null
        _authUiStatus.value = AuthUiStatus.Idle
    }

    private fun canSubmitStage(stage: AuthSubmissionStage): Boolean {
        val currentPendingAction = pendingAction
        if (currentPendingAction?.stage == stage) return false

        return when (stage) {
            AuthSubmissionStage.PHONE    -> _authState.value is AuthStep.InputPhone
            AuthSubmissionStage.CODE     -> _authState.value is AuthStep.InputCode
            AuthSubmissionStage.PASSWORD -> _authState.value is AuthStep.InputPassword
            AuthSubmissionStage.REGISTER -> _authState.value is AuthStep.InputRegistration
        }
    }

    private fun isExpectedNextState(
        stage: AuthSubmissionStage,
        state: AuthStep
    ): Boolean {
        return when (stage) {
            AuthSubmissionStage.PHONE    -> state !is AuthStep.InputPhone &&
                    state !is AuthStep.Loading &&
                    state !is AuthStep.WaitParameters
            AuthSubmissionStage.CODE     -> state is AuthStep.InputPassword ||
                    state is AuthStep.InputRegistration ||
                    state is AuthStep.Ready
            AuthSubmissionStage.PASSWORD -> state is AuthStep.Ready
            AuthSubmissionStage.REGISTER -> state is AuthStep.Ready
        }
    }

    private fun shouldSuppressStaleAuthError(
        stage: AuthSubmissionStage,
        throwable: Throwable
    ): Boolean {
        val functionNames = when (stage) {
            AuthSubmissionStage.PHONE    -> listOf("setAuthenticationPhoneNumber")
            AuthSubmissionStage.CODE     -> listOf(
                "checkAuthenticationCode",
                "checkAuthenticationEmailCode"
            )
            AuthSubmissionStage.PASSWORD -> listOf("checkAuthenticationPassword")
            AuthSubmissionStage.REGISTER -> listOf("registerUser")
        }

        return isExpectedNextState(stage, _authState.value) &&
                functionNames.any(throwable::isUnexpectedAuthStateError)
    }
}
