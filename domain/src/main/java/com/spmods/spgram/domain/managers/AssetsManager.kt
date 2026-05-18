package com.spmods.spgram.domain.managers

import java.io.File
import java.io.InputStream

interface AssetsManager {
    fun getAssets(path: String): InputStream
    fun getFilesDir(): File
    fun getCacheDir(): File
    fun getExternalCacheDir(): File?
    fun getDatabasePath(name: String): File
    fun clearSharedPreferences(name: String)
    fun exitProcess(status: Int)
}
