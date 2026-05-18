package com.spmods.spgram.domain.repository

import kotlinx.coroutines.flow.Flow
import com.spmods.spgram.domain.models.FileDownloadEvent
import com.spmods.spgram.domain.models.FileModel
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
}