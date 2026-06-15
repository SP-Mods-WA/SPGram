package com.spmods.spgram.presentation.features.chats.conversation.logic

import android.util.Log
import kotlinx.coroutines.launch
import com.spmods.spgram.domain.models.MessageContent
import com.spmods.spgram.domain.models.MessageModel
import com.spmods.spgram.presentation.features.chats.conversation.DefaultChatComponent

/**
 * Handles tapping a view-once (self-destructing) message.
 *
 * Files are auto-downloaded on arrival. We read the LATEST message from _state
 * so we never use a stale snapshot that has path=null after download completed.
 *
 * Voice: playback is triggered directly from VoiceMessageBubble (UI layer has
 * access to VoicePlaybackController). This function only handles Photo/Video
 * opening + the TDLib openMessageContent notify for all types.
 */
internal fun DefaultChatComponent.handleOpenViewOnce(message: MessageModel) {
    scope.launch {
        // Always use the freshest version from current state
        val latest = _state.value.messages.find { it.id == message.id } ?: message

        try {
            repositoryMessage.openMessageContent(chatId, latest.id)
        } catch (e: Throwable) {
            Log.e("ViewOnce", "openMessageContent failed: msgId=${latest.id}", e)
        }

        when (val content = latest.content) {
            is MessageContent.Photo -> {
                val path = content.path
                if (path != null) {
                    onOpenImages(
                        images = listOf(path),
                        captions = listOf(content.caption.takeIf { it.isNotBlank() }),
                        startIndex = 0,
                        messageId = latest.id,
                        messageIds = listOf(latest.id)
                    )
                } else {
                    Log.w("ViewOnce", "Photo tapped but path still null — download in progress")
                }
            }
            is MessageContent.Video -> {
                val path = content.path
                if (path != null) {
                    onOpenVideo(path = path, messageId = latest.id, caption = content.caption)
                } else {
                    Log.w("ViewOnce", "Video tapped but path still null — download in progress")
                }
            }
            // Voice: VoiceMessageBubble calls togglePlayPause directly after onOpenViewOnce.
            // VideoNote: inline player recomposes automatically once path arrives.
            else -> Unit
        }
    }
}
