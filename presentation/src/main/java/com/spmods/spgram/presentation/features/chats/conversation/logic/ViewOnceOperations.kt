package com.spmods.spgram.presentation.features.chats.conversation.logic

import android.util.Log
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import com.spmods.spgram.domain.models.MessageContent
import com.spmods.spgram.domain.models.MessageModel
import com.spmods.spgram.presentation.features.chats.conversation.DefaultChatComponent

/**
 * Handles tapping a view-once (self-destructing) Photo or Video message.
 *
 * Flow:
 * 1. Call openMessageContent() — TDLib reveals the real fileId and kicks off download.
 * 2. Wait (up to 30s) for _state to contain a non-null path for this message.
 * 3. Open the viewer once the path arrives.
 *
 * Voice: handled entirely in VoiceMessageBubble (UI layer owns VoicePlaybackController).
 * VideoNote: inline player recomposes automatically once path arrives — nothing to do here.
 */
internal fun DefaultChatComponent.handleOpenViewOnce(message: MessageModel) {
    scope.launch {
        try {
            // TDLib requires the message to be marked as read before OpenMessageContent
            // will trigger self-destruction on incoming view-once messages.
            repositoryMessage.markAsRead(chatId, message.id)
            repositoryMessage.openMessageContent(chatId, message.id)
        } catch (e: Throwable) {
            Log.e("ViewOnce", "openMessageContent failed: msgId=${message.id}", e)
        }

        when (message.content) {
            is MessageContent.Photo -> {
                // Wait for _state to have a non-null path (download completes after TDLib reveals fileId)
                val path = withTimeoutOrNull(30_000) {
                    _state
                        .filter { state ->
                            val content = state.messages
                                .find { it.id == message.id }
                                ?.content as? MessageContent.Photo
                            !content?.path.isNullOrBlank()
                        }
                        .first()
                        .messages
                        .find { it.id == message.id }
                        .let { (it?.content as? MessageContent.Photo)?.path }
                }
                if (path != null) {
                    val caption = (_state.value.messages.find { it.id == message.id }
                        ?.content as? MessageContent.Photo)?.caption
                    onOpenImages(
                        images = listOf(path),
                        captions = listOf(caption?.takeIf { it.isNotBlank() }),
                        startIndex = 0,
                        messageId = message.id,
                        messageIds = listOf(message.id)
                    )
                } else {
                    Log.e("ViewOnce", "Photo path never arrived within timeout msgId=${message.id}")
                }
            }
            is MessageContent.Video -> {
                val path = withTimeoutOrNull(30_000) {
                    _state
                        .filter { state ->
                            val content = state.messages
                                .find { it.id == message.id }
                                ?.content as? MessageContent.Video
                            !content?.path.isNullOrBlank()
                        }
                        .first()
                        .messages
                        .find { it.id == message.id }
                        .let { (it?.content as? MessageContent.Video)?.path }
                }
                if (path != null) {
                    val caption = (_state.value.messages.find { it.id == message.id }
                        ?.content as? MessageContent.Video)?.caption
                    onOpenVideo(path = path, messageId = message.id, caption = caption)
                } else {
                    Log.e("ViewOnce", "Video path never arrived within timeout msgId=${message.id}")
                }
            }
            else -> Unit
        }
    }
}
