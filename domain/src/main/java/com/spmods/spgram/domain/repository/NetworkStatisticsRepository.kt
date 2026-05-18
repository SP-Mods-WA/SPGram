package com.spmods.spgram.domain.repository

import com.spmods.spgram.domain.models.NetworkUsageModel

interface NetworkStatisticsRepository {
    suspend fun getNetworkUsage(): NetworkUsageModel?
    suspend fun getNetworkStatisticsEnabled(): Boolean
    suspend fun setNetworkStatisticsEnabled(enabled: Boolean)
    suspend fun resetNetworkStatistics(): Boolean
}