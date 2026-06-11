package com.spmods.spgram.presentation.features.chats.conversation.logic

import android.util.Log
import kotlinx.coroutines.launch
import com.spmods.spgram.domain.models.MessageContent
import com.spmods.spgram.domain.models.MessageModel
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
        try {
            repositoryMessage.openMessageContent(chatId, message.id)
        } catch (e: Throwable) {
            Log.e("ViewOnce", "openMessageContent failed: msgId=${message.id}", e)
        }

        when (val content = message.content) {
            is MessageContent.Photo -> {
                if (content.path != null) {
                    onOpenImages(
                        images = listOf(content.path),
                        captions = listOf(content.caption as String?),
                        startIndex = 0,
                        messageId = message.id,
                        messageIds = listOf(message.id)
                    )
                } else if (content.fileId != 0) {
                    // Not downloaded yet — trigger download; UI will open viewer
                    // automatically when content.path becomes non-null via message update
                    repositoryMessage.downloadFile(content.fileId, priority = 32)
                }
            }
            is MessageContent.Video -> {
                if (content.path != null) {
                    onOpenVideo(path = content.path, messageId = message.id, caption = content.caption)
                } else if (content.fileId != 0) {
                    repositoryMessage.downloadFile(content.fileId, priority = 32)
                }
            }
            is MessageContent.Voice -> {
                // Voice view-once: just trigger download; inline player handles playback
                if (content.path == null && content.fileId != 0) {
                    repositoryMessage.downloadFile(content.fileId, priority = 32)
                }
            }
            is MessageContent.VideoNote -> {
                // VideoNote view-once: just trigger download; inline player handles playback
                if (content.path == null && content.fileId != 0) {
                    repositoryMessage.downloadFile(content.fileId, priority = 32)
                }
            }
            else -> Unit
        }
    }
}
