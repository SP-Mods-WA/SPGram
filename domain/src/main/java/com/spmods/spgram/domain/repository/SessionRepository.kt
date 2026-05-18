package com.spmods.spgram.domain.repository

import com.spmods.spgram.domain.models.SessionModel

interface SessionRepository {
    suspend fun getActiveSessions(): List<SessionModel>
    suspend fun terminateSession(sessionId: Long): Boolean
    suspend fun confirmQrCode(link: String): Boolean
}