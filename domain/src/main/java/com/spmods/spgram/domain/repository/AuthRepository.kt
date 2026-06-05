package com.spmods.spgram.domain.repository

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

sealed class AuthStep {
    object Loading        : AuthStep()
    object WaitParameters : AuthStep()
    object InputPhone     : AuthStep()
    data class InputCode(
        val codeType: String,
        val codeLength: Int,
        val nextType: String? = null,
        val timeout: Int = 0,
        val isEmailCode: Boolean = false,
        val emailPattern: String? = null
    ) : AuthStep()
    object InputPassword : AuthStep()
    // FIX [3]: new user — show name-input screen after code verified
    data class InputRegistration(val termsText: String? = null) : AuthStep()
    object Ready : AuthStep()
}

enum class AuthSubmissionStage {
    PHONE,
    CODE,
    PASSWORD,
    REGISTER           // FIX [3]
}

sealed class AuthUiStatus {
    object Idle : AuthUiStatus()
    data class Submitting(val stage: AuthSubmissionStage) : AuthUiStatus()
    data class SlowNetwork(val stage: AuthSubmissionStage) : AuthUiStatus()
    data class NetworkError(val stage: AuthSubmissionStage) : AuthUiStatus()
}

sealed class AuthError {
    object InvalidCode      : AuthError()
    object InvalidPassword  : AuthError()
    object CodeExpired      : AuthError()
    object NetworkTimeout   : AuthError()
    object Unexpected       : AuthError()
}

const val AUTH_NETWORK_TIMEOUT_ERROR = "__AUTH_NETWORK_TIMEOUT__"

interface AuthRepository {
    val authState:    StateFlow<AuthStep>
    val authUiStatus: StateFlow<AuthUiStatus>
    val errors:       SharedFlow<AuthError>

    fun sendPhone(phone: String)
    fun resendCode()
    fun sendCode(code: String)
    fun sendPassword(password: String)
    fun registerUser(firstName: String, lastName: String)   // FIX [3]
    fun retryLastAction()
    fun reset()
}
