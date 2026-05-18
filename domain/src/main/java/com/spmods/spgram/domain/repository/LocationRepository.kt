package com.spmods.spgram.domain.repository

import com.spmods.spgram.domain.models.webapp.OSMReverseResponse

interface LocationRepository {
    suspend fun reverseGeocode(lat: Double, lon: Double): OSMReverseResponse?
}