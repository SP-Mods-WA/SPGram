package com.spmods.spgram.domain.repository

import kotlinx.coroutines.flow.Flow
import com.spmods.spgram.domain.models.GifModel

interface GifRepository {
    fun getGifFile(gif: GifModel): Flow<String?>
    fun getGifThumbnailFile(fileId: Long): Flow<String?>
    suspend fun getSavedGifs(): List<GifModel>
    suspend fun addSavedGif(path: String)
    suspend fun searchGifs(query: String): List<GifModel>
}