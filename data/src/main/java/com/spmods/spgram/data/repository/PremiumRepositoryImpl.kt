package com.spmods.spgram.data.repository

import com.spmods.spgram.data.datasource.remote.UserRemoteDataSource
import com.spmods.spgram.data.mapper.user.toApi
import com.spmods.spgram.data.mapper.user.toDomain
import com.spmods.spgram.domain.models.PremiumFeatureType
import com.spmods.spgram.domain.models.PremiumLimitType
import com.spmods.spgram.domain.models.PremiumSource
import com.spmods.spgram.domain.models.PremiumStateModel
import com.spmods.spgram.domain.repository.PremiumRepository

class PremiumRepositoryImpl(
    private val remote: UserRemoteDataSource
) : PremiumRepository {
    override suspend fun getPremiumState(): PremiumStateModel? {
        val state = remote.getPremiumState() ?: return null
        return state.toDomain()
    }

    override suspend fun getPremiumFeatures(source: PremiumSource): List<PremiumFeatureType> {
        val tdSource = source.toApi() ?: return emptyList()
        val result = remote.getPremiumFeatures(tdSource) ?: return emptyList()
        return result.features.map { it.toDomain() }
    }

    override suspend fun getPremiumLimit(limitType: PremiumLimitType): Int {
        val tdType = limitType.toApi() ?: return 0
        return remote.getPremiumLimit(tdType)?.premiumValue ?: 0
    }
}