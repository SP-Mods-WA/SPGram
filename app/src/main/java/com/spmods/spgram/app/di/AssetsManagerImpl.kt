package com.spmods.spgram.app.di

import android.content.Context
import androidx.core.content.edit
import com.spmods.spgram.domain.managers.AssetsManager
import java.io.File
import java.io.InputStream

class AssetsManagerImpl(private val context: Context) : AssetsManager {
    override fun getAssets(path: String): InputStream {
        return context.assets.open(path)
    }

    override fun getFilesDir(): File {
        return context.filesDir
    }

    override fun getCacheDir(): File {
        return context.cacheDir
    }

    override fun getExternalCacheDir(): File? {
        return context.externalCacheDir
    }

    override fun getDatabasePath(name: String): File {
        return context.getDatabasePath(name)
    }

    override fun clearSharedPreferences(name: String) {
        context.getSharedPreferences(name, Context.MODE_PRIVATE).edit { clear() }
    }

    override fun exitProcess(status: Int) {
        exitProcess(status)
    }
}