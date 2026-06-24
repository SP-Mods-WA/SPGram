package com.spmods.spgram.domain.repository

import kotlinx.coroutines.flow.Flow
import com.spmods.spgram.domain.models.DownloadsFilter
import com.spmods.spgram.domain.models.FileDownloadEvent
import com.spmods.spgram.domain.models.FileModel
import com.spmods.spgram.domain.models.FoundDownloadsModel
import com.spmods.spgram.domain.models.MessageDownloadEvent

interface FileRepository {
    val fileDownloadFlow: Flow<FileDownloadEvent>
    val messageDownloadFlow: Flow<MessageDownloadEvent>

    fun downloadFile(
        fileId: Int,
        priority: Int = 1,
        offset: Long = 0,
        limit: Long = 0,
        synchronous: Boolean = false
    )

    suspend fun cancelDownloadFile(fileId: Int)

    suspend fun getFilePath(fileId: Int): String?

    suspend fun getFileInfo(fileId: Int): FileModel?

    /**
     * Searches the global list of downloaded/downloading files (across all
     * chats), optionally filtered by media type — mirrors Telegram's own
     * Downloads manager.
     */
    suspend fun searchFileDownloads(
        filter: DownloadsFilter,
        onlyActive: Boolean = false,
        onlyCompleted: Boolean = false,
        offset: String = "",
        limit: Int = 50
    ): FoundDownloadsModel

    suspend fun removeFileFromDownloads(fileId: Int, deleteFromCache: Boolean = false)

    suspend fun toggleDownloadIsPaused(fileId: Int, isPaused: Boolean)
}
