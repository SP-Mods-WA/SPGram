package com.spmods.spgram.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import com.spmods.spgram.domain.models.ChatFullInfoModel
import com.spmods.spgram.domain.models.UserModel

interface UserRepository {
    val currentUserFlow: StateFlow<UserModel?>
    val anyUserUpdateFlow: Flow<Long>

    suspend fun getMe(): UserModel
    suspend fun getUser(userId: Long): UserModel?
    suspend fun getUserFullInfo(userId: Long): UserModel?
    suspend fun resolveUserChatFullInfo(userId: Long): ChatFullInfoModel?
    fun getUserFlow(userId: Long): Flow<UserModel?>

    fun logOut()

    suspend fun getContacts(): List<UserModel>
    suspend fun searchContacts(query: String): List<UserModel>
    suspend fun addContact(user: UserModel)
    suspend fun removeContact(userId: Long)
    suspend fun setCachedSimCountryIso(iso: String?)
}
