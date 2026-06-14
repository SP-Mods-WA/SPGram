package com.spmods.spgram.data.datasource.remote

import com.spmods.spgram.data.infra.FileDownloadQueue
import com.spmods.spgram.data.mapper.toDomain

class MessageFileCoordinator(
    private val fileDownloadQueue: FileDownloadQueue
) : MessageFileApi {

    override fun registerFileForMessage(fileId: Int, chatId: Long, messageId: Long) {
        fileDownloadQueue.registry.register(fileId, chatId, messageId)
    }

    override fun enqueueDownload(
        fileId: Int,
        priority: Int,
        type: TdMessageRemoteDataSource.DownloadType,
        offset: Long,
        limit: Long,
        synchronous: Boolean
    ) {
        fileDownloadQueue.enqueue(
            fileId = fileId,
            priority = priority,
            type = type.toDomain(),
            offset = offset,
            limit = limit,
            synchronous = synchronous
        )
    }

    override fun isFileQueued(fileId: Int): Boolean {
        return fileDownloadQueue.isFileQueued(fileId)
    }

    override fun suppressDownload(fileId: Int) {
        fileDownloadQueue.suppressDownload(fileId)
    }
}
