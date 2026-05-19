package com.spmods.spgram.data.repository

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import org.drinkless.tdlib.TdApi
import com.spmods.spgram.data.datasource.remote.PrivacyRemoteDataSource
import com.spmods.spgram.data.gateway.UpdateDispatcher
import com.spmods.spgram.data.mapper.toApi
import com.spmods.spgram.data.mapper.toDomain
import com.spmods.spgram.domain.models.PrivacyRule
import com.spmods.spgram.domain.repository.PrivacyKey
import com.spmods.spgram.domain.repository.PrivacyRepository

class PrivacyRepositoryImpl(
    private val remote: PrivacyRemoteDataSource,
    private val updates: UpdateDispatcher
) : PrivacyRepository {

    override fun getPrivacyRules(key: PrivacyKey): Flow<List<PrivacyRule>> = callbackFlow {
        val setting = key.toApi()

        trySend(remote.getPrivacyRules(setting).map { it.toDomain() })

        val job = updates.userPrivacySettingRules
            .filter { it.setting.constructor == setting.constructor }
            .onEach { update -> trySend(update.rules.rules.map { it.toDomain() }) }
            .launchIn(this)

        awaitClose { job.cancel() }
    }

    override suspend fun setPrivacyRule(key: PrivacyKey, rules: List<PrivacyRule>) {
        val setting = key.toApi()
        val tdRules = TdApi.UserPrivacySettingRules(rules.map { it.toApi() }.toTypedArray())
        remote.setPrivacyRules(setting, tdRules)
    }

    override suspend fun getBlockedUsers(): List<Long> = remote.getBlockedUsers()

    override suspend fun blockUser(userId: Long) = remote.blockUser(userId)

    override suspend fun unblockUser(userId: Long) = remote.unblockUser(userId)

    override suspend fun deleteAccount(reason: String, password: String) =
        remote.deleteAccount(reason, password)

    override suspend fun getAccountTtl(): Int = remote.getAccountTtl()

    override suspend fun setAccountTtl(days: Int) = remote.setAccountTtl(days)

    override suspend fun getPasswordState(): Boolean = remote.getPasswordState()

    override suspend fun canShowSensitiveContent(): Boolean =
        remote.getOption("can_ignore_sensitive_content_restrictions")

    override suspend fun isShowSensitiveContentEnabled(): Boolean =
        remote.getOption("ignore_sensitive_content_restrictions")

    override suspend fun setShowSensitiveContent(enabled: Boolean) =
        remote.setOption("ignore_sensitive_content_restrictions", enabled)
}