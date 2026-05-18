package com.spmods.spgram.domain.repository

import com.spmods.spgram.domain.models.PremiumFeatureType
import com.spmods.spgram.domain.models.PremiumLimitType
import com.spmods.spgram.domain.models.PremiumSource
import com.spmods.spgram.domain.models.PremiumStateModel

interface PremiumRepository {
    suspend fun getPremiumState(): PremiumStateModel?
    suspend fun getPremiumFeatures(source: PremiumSource): List<PremiumFeatureType>
    suspend fun getPremiumLimit(limitType: PremiumLimitType): Int
}
