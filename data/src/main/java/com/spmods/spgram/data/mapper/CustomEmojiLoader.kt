package com.spmods.spgram.data.mapper

import org.drinkless.tdlib.TdApi
import com.spmods.spgram.data.datasource.remote.MessageFileApi
import com.spmods.spgram.data.datasource.remote.TdMessageRemoteDataSource
import com.spmods.spgram.data.gateway.TelegramGateway
import com.spmods.spgram.data.infra.FileUpdateHandler

internal class CustomEmojiLoader(
    private val gateway: TelegramGateway,
    private val fileApi: MessageFileApi,
    private val fileUpdateHandler: FileUpdateHandler,
    private val fileHelper: TdFileHelper
) {
    fun getPathIfValid(emojiId: Long): String? {
        return fileUpdateHandler.customEmojiPaths[emojiId]
            ?.takeIf { fileHelper.isValidPath(it) }
    }

    suspend fun loadIfNeeded(emojiId: Long, chatId: Long, messageId: Long, autoDownload: Boolean) {
        if (getPathIfValid(emojiId) != null) return

        val result = gateway.execute(TdApi.GetCustomEmojiStickers(longArrayOf(emojiId)))
        if (result is TdApi.Stickers && result.stickers.isNotEmpty()) {
            val fileToUse = result.stickers.first().sticker

            fileUpdateHandler.fileIdToCustomEmojiId[fileToUse.id] = emojiId
            fileApi.registerFileForMessage(fileToUse.id, chatId, messageId)

            if (!fileHelper.isValidPath(fileToUse.local.path)) {
                if (autoDownload) {
                    fileApi.enqueueDownload(
                        fileToUse.id,
                        32,
                        TdMessageRemoteDataSource.DownloadType.DEFAULT,
                        0,
                        0,
                        false
                    )
                }
            } else {
                fileUpdateHandler.customEmojiPaths[emojiId] = fileToUse.local.path
            }
        }
    }
}