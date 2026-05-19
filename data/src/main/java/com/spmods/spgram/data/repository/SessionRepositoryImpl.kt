package com.spmods.spgram.data.repository

import com.spmods.spgram.data.datasource.remote.SettingsRemoteDataSource
import com.spmods.spgram.data.mapper.toDomain
import com.spmods.spgram.domain.models.SessionModel
import com.spmods.spgram.domain.repository.SessionRepository

class SessionRepositoryImpl(
    private val remote: SettingsRemoteDataSource
) : SessionRepository {

    override suspend fun getActiveSessions(): List<SessionModel> {
        return remote.getActiveSessions()?.sessions?.map { it.toDomain() } ?: emptyList()
    }

    override suspend fun terminateSession(sessionId: Long): Boolean {
        return remote.terminateSession(sessionId)
    }

    override suspend fun confirmQrCode(link: String): Boolean {
        return remote.confirmQrCode(link)
    }
}