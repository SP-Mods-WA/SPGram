package com.spmods.spgram.data.mapper

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import org.drinkless.tdlib.TdApi
import com.spmods.spgram.data.chats.ChatCache
import com.spmods.spgram.data.datasource.remote.MessageFileApi
import com.spmods.spgram.data.datasource.remote.TdMessageRemoteDataSource
import com.spmods.spgram.domain.repository.AppPreferencesProvider

class TdFileHelper(
    private val connectivityManager: ConnectivityManager,
    private val fileApi: MessageFileApi,
    private val appPreferences: AppPreferencesProvider,
    private val cache: ChatCache
) {
    fun isValidPath(path: String?): Boolean {
        return isValidFilePath(path)
    }

    fun getUpdatedFile(file: TdApi.File): TdApi.File {
        return cache.fileCache[file.id] ?: file
    }

    fun resolveLocalFilePath(file: TdApi.File?): String? {
        if (file == null) return null
        // Only return a path if TDLib confirms the download is fully complete.
        // Checking isDownloadingCompleted is the ONLY reliable guard — temp/partial files
        // may have no recognisable extension and File.exists() returns true for them.
        val resolved = cache.fileCache[file.id] ?: file
        if (resolved.local.isDownloadingCompleted && isValidPath(resolved.local.path)) {
            return resolved.local.path
        }
        return null
    }

    fun findBestAvailablePath(mainFile: TdApi.File?, sizes: Array<TdApi.PhotoSize>? = null): String? {
        val updatedMain = mainFile?.let { getUpdatedFile(it) }
        if (updatedMain != null && updatedMain.local.isDownloadingCompleted && isValidPath(updatedMain.local.path)) {
            return updatedMain.local.path
        }

        if (sizes != null) {
            return sizes.sortedByDescending { it.width }
                .map { getUpdatedFile(it.photo) }
                .firstOrNull { it.local.isDownloadingCompleted && isValidPath(it.local.path) }
                ?.local?.path
        }

        return null
    }

    fun resolveCachedPath(fileId: Int, storedPath: String?): String? {
        // Check cache first for the most up-to-date isDownloadingCompleted flag
        val fromCache = fileId.takeIf { it != 0 }
            ?.let { cache.fileCache[it] }
            ?.takeIf { it.local.isDownloadingCompleted && isValidPath(it.local.path) }
            ?.local?.path
        if (fromCache != null) return fromCache

        // Fallback: stored path is only valid if cache confirms completion
        // (storedPath alone is unreliable after a cancelled/partial download)
        return null
    }

    fun registerCachedFile(fileId: Int, chatId: Long, messageId: Long) {
        if (fileId != 0) {
            fileApi.registerFileForMessage(fileId, chatId, messageId)
        }
    }

    fun enqueueDownload(
        fileId: Int,
        priority: Int,
        downloadType: TdMessageRemoteDataSource.DownloadType,
        offset: Int = 0,
        limit: Int = 0,
        synchronous: Boolean = false
    ) {
        fileApi.enqueueDownload(fileId, priority, downloadType, offset.toLong(), limit.toLong(), synchronous)
    }

    fun isFileQueued(fileId: Int): Boolean = fileApi.isFileQueued(fileId)

    fun computeDownloadProgress(file: TdApi.File): Float {
        return if (file.size > 0) {
            file.local.downloadedSize.toFloat() / file.size.toFloat()
        } else {
            0f
        }
    }

    fun computeUploadProgress(file: TdApi.File): Float {
        return if (file.size > 0) {
            file.remote.uploadedSize.toFloat() / file.size.toFloat()
        } else {
            0f
        }
    }

    fun isNetworkAutoDownloadEnabled(): Boolean {
        return when (getCurrentNetworkType()) {
            is TdApi.NetworkTypeWiFi -> appPreferences.autoDownloadWifi.value
            is TdApi.NetworkTypeMobile -> appPreferences.autoDownloadMobile.value
            is TdApi.NetworkTypeMobileRoaming -> appPreferences.autoDownloadRoaming.value
            else -> appPreferences.autoDownloadWifi.value
        }
    }

    private fun getCurrentNetworkType(): TdApi.NetworkType {
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

        return when {
            capabilities == null -> TdApi.NetworkTypeNone()
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> TdApi.NetworkTypeWiFi()
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                if (connectivityManager.isDefaultNetworkActive && !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING)) {
                    TdApi.NetworkTypeMobileRoaming()
                } else {
                    TdApi.NetworkTypeMobile()
                }
            }

            else -> TdApi.NetworkTypeNone()
        }
    }
}
