package com.spmods.spgram.domain.repository

import kotlinx.coroutines.flow.Flow
import com.spmods.spgram.domain.models.WallpaperModel

interface WallpaperRepository {
    fun getWallpapers(): Flow<List<WallpaperModel>>
    suspend fun downloadWallpaper(fileId: Int)
    suspend fun setDefaultWallpaper(
        wallpaper: WallpaperModel,
        isBlurred: Boolean,
        isMoving: Boolean
    ): WallpaperModel?

    suspend fun uploadWallpaper(
        filePath: String,
        isBlurred: Boolean,
        isMoving: Boolean
    ): WallpaperModel?
}
