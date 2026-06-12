package com.spmods.spgram.presentation.features.chats.conversation.logic

import android.util.Log
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import com.spmods.spgram.domain.models.MessageContent
import com.spmods.spgram.domain.models.MessageModel
import com.spmods.spgram.presentation.features.chats.conversation.DefaultChatComponent

/**
 * Handles tapping a view-once (self-destructing) photo, video, voice, or video note.
 *
 * Flow:
 * 1. Tell TDLib the user opened the view-once content (triggers server-side destruction
 *    countdown — but only takes effect once the file is fully downloaded).
 * 2. Download the file if not yet local (view-once files are never auto-downloaded).
 * 3. Open the viewer as normal once the path arrives via the existing message update flow.
 * 4. Once the file finishes downloading, call openMessageContent again so TDLib actually
 *    schedules/starts the self-destruct now that the content is locally available. Without
 *    this second call, the first call (sent before the file existed locally) is a no-op and
 *    the message never expires / the sender never sees it as "viewed".
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
                        images = listOf(content.path!!),
                        captions = listOf(content.caption.takeIf { it.isNotBlank() }),
                        startIndex = 0,
                        messageId = message.id,
                        messageIds = listOf(message.id)
                    )
                } else if (content.fileId != 0) {
                    // Not downloaded yet — trigger download; UI will open viewer
                    // automatically when content.path becomes non-null via message update
                    repositoryMessage.downloadFile(content.fileId, priority = 32)
                    reopenAfterDownload(message.id)
                }
            }
            is MessageContent.Video -> {
                if (content.path != null) {
                    onOpenVideo(path = content.path, messageId = message.id, caption = content.caption)
                } else if (content.fileId != 0) {
                    repositoryMessage.downloadFile(content.fileId, priority = 32)
                    reopenAfterDownload(message.id)
                }
            }
            is MessageContent.Voice -> {
                // Voice view-once: just trigger download; inline player handles playback
                if (content.path == null && content.fileId != 0) {
                    repositoryMessage.downloadFile(content.fileId, priority = 32)
                    reopenAfterDownload(message.id)
                }
            }
            is MessageContent.VideoNote -> {
                // VideoNote view-once: just trigger download; inline player handles playback
                if (content.path == null && content.fileId != 0) {
                    repositoryMessage.downloadFile(content.fileId, priority = 32)
                    reopenAfterDownload(message.id)
                }
            }
            else -> Unit
        }
    }
}

/**
 * Waits for the message's content to receive a local file path (i.e. download finished),
 * then notifies TDLib again that the content was opened so it can start the
 * self-destruct countdown / mark the message as viewed for the sender.
 */
private fun DefaultChatComponent.reopenAfterDownload(messageId: Long) {
    scope.launch {
        val updated = withTimeoutOrNull(60_000) {
            repositoryMessage.messageEditedFlow
                .filter { it.id == messageId }
                .filter { msg ->
                    when (val c = msg.content) {
                        is MessageContent.Photo -> c.path != null
                        is MessageContent.Video -> c.path != null
                        is MessageContent.Voice -> c.path != null
                        is MessageContent.VideoNote -> c.path != null
                        else -> false
                    }
                }
                .first()
        }

        if (updated != null) {
            try {
                repositoryMessage.openMessageContent(chatId, messageId)
            } catch (e: Throwable) {
                Log.e("ViewOnce", "re-open openMessageContent failed: msgId=$messageId", e)
            }
        }
    }
}
