package com.spmods.spgram.domain.repository

import com.spmods.spgram.domain.models.ChatRevenueStatisticsModel
import com.spmods.spgram.domain.models.ChatStatisticsModel
import com.spmods.spgram.domain.models.StatisticsGraphModel

interface ChatStatisticsRepository {
    suspend fun getChatStatistics(chatId: Long, isDark: Boolean): ChatStatisticsModel?
    suspend fun getChatRevenueStatistics(chatId: Long, isDark: Boolean): ChatRevenueStatisticsModel?
    suspend fun loadStatisticsGraph(chatId: Long, token: String, x: Long): StatisticsGraphModel?
}