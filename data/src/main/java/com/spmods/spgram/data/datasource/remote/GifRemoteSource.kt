package com.spmods.spgram.data.datasource.remote

import com.spmods.spgram.domain.models.GifModel

interface GifRemoteSource {
    suspend fun getSavedGifs(): List<GifModel>
    suspend fun addSavedGif(path: String)
    suspend fun searchGifs(query: String): List<GifModel>
}