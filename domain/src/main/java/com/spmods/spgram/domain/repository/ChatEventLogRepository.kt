package com.spmods.spgram.domain.repository

import com.spmods.spgram.domain.models.ChatEventLogFiltersModel
import com.spmods.spgram.domain.models.ChatEventModel

interface ChatEventLogRepository {
    suspend fun getChatEventLog(
        chatId: Long,
        query: String = "",
        fromEventId: Long = 0,
        limit: Int = 50,
        filters: ChatEventLogFiltersModel = ChatEventLogFiltersModel(),
        userIds: List<Long> = emptyList()
    ): List<ChatEventModel>
}