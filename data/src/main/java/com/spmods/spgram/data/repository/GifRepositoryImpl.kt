package com.spmods.spgram.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import com.spmods.spgram.data.datasource.remote.GifRemoteSource
import com.spmods.spgram.data.stickers.StickerFileManager
import com.spmods.spgram.domain.models.GifModel
import com.spmods.spgram.domain.repository.CacheProvider
import com.spmods.spgram.domain.repository.GifRepository

class GifRepositoryImpl(
    private val remote: GifRemoteSource,
    private val cacheProvider: CacheProvider,
    private val stickerFileManager: StickerFileManager
) : GifRepository {

    override fun getGifFile(gif: GifModel): Flow<String?> {
        return if (gif.fileId == 0L) {
            flowOf(null)
        } else {
            stickerFileManager.getGifFile(gif.fileId)
        }
    }

    override fun getGifThumbnailFile(fileId: Long): Flow<String?> {
        return if (fileId == 0L) {
            flowOf(null)
        } else {
            stickerFileManager.getDefaultFile(fileId)
        }
    }

    override suspend fun getSavedGifs(): List<GifModel> {
        return runCatching {
            remote.getSavedGifs().also { remoteGifs ->
                cacheProvider.setSavedGifs(remoteGifs)
            }
        }.getOrElse {
            cacheProvider.savedGifs.value
        }
    }

    override suspend fun addSavedGif(path: String) {
        remote.addSavedGif(path)
        cacheProvider.setSavedGifs(remote.getSavedGifs())
    }

    override suspend fun searchGifs(query: String): List<GifModel> {
        return remote.searchGifs(query)
    }
}