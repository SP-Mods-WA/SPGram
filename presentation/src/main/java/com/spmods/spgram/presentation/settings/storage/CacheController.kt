package com.spmods.spgram.presentation.settings.storage

import android.content.Context
import coil3.imageLoader
import com.spmods.spgram.presentation.core.media.ExoPlayerCache
import java.io.File

class CacheController(val context: Context, val exoPlayerCache: ExoPlayerCache) {
    private val tdLibCacheRelativePath = "tdlib/files"

    fun clearExo() {
        exoPlayerCache.clearCache(context)
    }

    fun clearImageLoader() {
        context.imageLoader.memoryCache?.clear()
        context.imageLoader.diskCache?.clear()
    }

    fun clearAllCache() {
        clearExo()
        clearImageLoader()
        clearTdLibCache()
    }

    fun getCacheDir(): File? {
        return context.cacheDir
    }

    private fun clearTdLibCache() {
        File(context.cacheDir, tdLibCacheRelativePath).deleteRecursively()
        context.externalCacheDir?.let { externalCacheDir ->
            File(externalCacheDir, tdLibCacheRelativePath).deleteRecursively()
        }
    }
}