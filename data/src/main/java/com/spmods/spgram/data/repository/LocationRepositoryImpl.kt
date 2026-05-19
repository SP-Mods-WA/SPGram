package com.spmods.spgram.data.repository

import com.spmods.spgram.data.datasource.remote.NominatimRemoteDataSource
import com.spmods.spgram.domain.repository.LocationRepository

class LocationRepositoryImpl(
    private val remote: NominatimRemoteDataSource
) : LocationRepository {
    override suspend fun reverseGeocode(lat: Double, lon: Double) =
        remote.reverseGeocode(lat, lon)
}