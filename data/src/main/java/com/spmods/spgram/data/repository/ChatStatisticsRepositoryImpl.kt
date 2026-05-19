package com.spmods.spgram.data.repository

import com.spmods.spgram.data.datasource.remote.UserRemoteDataSource
import com.spmods.spgram.data.mapper.user.toDomain
import com.spmods.spgram.domain.models.ChatRevenueStatisticsModel
import com.spmods.spgram.domain.models.ChatStatisticsModel
import com.spmods.spgram.domain.models.StatisticsGraphModel
import com.spmods.spgram.domain.repository.ChatStatisticsRepository

class ChatStatisticsRepositoryImpl(
    private val remote: UserRemoteDataSource
) : ChatStatisticsRepository {

    override suspend fun getChatStatistics(chatId: Long, isDark: Boolean): ChatStatisticsModel? {
        val stats = remote.getChatStatistics(chatId, isDark) ?: return null
        return stats.toDomain()
    }

    override suspend fun getChatRevenueStatistics(
        chatId: Long,
        isDark: Boolean
    ): ChatRevenueStatisticsModel? {
        val stats = remote.getChatRevenueStatistics(chatId, isDark) ?: return null
        return stats.toDomain()
    }

    override suspend fun loadStatisticsGraph(
        chatId: Long,
        token: String,
        x: Long
    ): StatisticsGraphModel? {
        val graph = remote.getStatisticsGraph(chatId, token, x) ?: return null
        return graph.toDomain()
    }
}