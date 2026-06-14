package com.spmods.spgram.presentation.features.chats.conversation.logic

import android.util.Log
import kotlinx.coroutines.launch
import com.spmods.spgram.domain.models.MessageContent
import com.spmods.spgram.domain.models.MessageModel
import com.spmods.spgram.presentation.features.chats.conversation.AutoDownloadSuppression
import com.spmods.spgram.presentation.features.chats.conversation.DefaultChatComponent

/**
 * Handles tapping a view-once (self-destructing) photo, video, voice, or video note.
 *
 * Flow:
 * 1. Tell TDLib the user opened the view-once content (triggers server-side destruction)
 * 2. Download the file if not yet local (view-once files are never auto-downloaded)
 * 3. Open the viewer as normal once the path arrives via the existing message update flow
 */
internal fun DefaultChatComponent.handleOpenViewOnce(message: MessageModel) {
    scope.launch {
        // Mark as opened immediately so that any recomposition triggered by
        // openMessageContent does not re-suppress the file via the mapper.
        val fileId = when (val content = message.content) {
            is MessageContent.Photo -> content.fileId
            is MessageContent.Video -> content.fileId
            is MessageContent.Voice -> content.fileId
            is MessageContent.VideoNote -> content.fileId
            else -> 0
        }
        if (fileId != 0) {
            repositoryMessage.markViewOnceFileOpened(fileId)
        }

        try {
            repositoryMessage.openMessageContent(chatId, message.id)
        } catch (e: Throwable) {
            Log.e("ViewOnce", "openMessageContent failed: msgId=${message.id}", e)
        }

        when (val content = message.content) {
            is MessageContent.Photo -> {
                if (content.path != null) {
                    onOpenImages(
                        images = listOf(content.path!!),
                        captions = listOf(content.caption.takeIf { it.isNotBlank() }),
                        startIndex = 0,
                        messageId = message.id,
                        messageIds = listOf(message.id)
                    )
                } else if (content.fileId != 0) {
                    repositoryMessage.registerFileForMessage(content.fileId, chatId, message.id)
                    repositoryMessage.downloadFile(content.fileId, priority = 32)
                }
            }
            is MessageContent.Video -> {
                if (content.path != null) {
                    onOpenVideo(path = content.path, messageId = message.id, caption = content.caption)
                } else if (content.fileId != 0) {
                    repositoryMessage.registerFileForMessage(content.fileId, chatId, message.id)
                    repositoryMessage.downloadFile(content.fileId, priority = 32)
                }
            }
            is MessageContent.Voice -> {
                Log.d(
                    "ViewOnce",
                    "Voice tapped: msgId=${message.id} fileId=${content.fileId} " +
                        "path=${content.path} isDownloading=${content.isDownloading} " +
                        "isViewOnce=${content.isViewOnce} isViewOnceOpened=${content.isViewOnceOpened}"
                )
                if (content.path == null && content.fileId != 0) {
                    AutoDownloadSuppression.clear(content.fileId)
                    repositoryMessage.registerFileForMessage(content.fileId, chatId, message.id)
                    repositoryMessage.downloadFile(content.fileId, priority = 32)
                }
            }
            is MessageContent.VideoNote -> {
                if (content.path == null && content.fileId != 0) {
                    repositoryMessage.registerFileForMessage(content.fileId, chatId, message.id)
                    repositoryMessage.downloadFile(content.fileId, priority = 32)
                }
            }
            else -> Unit
        }
    }
}
