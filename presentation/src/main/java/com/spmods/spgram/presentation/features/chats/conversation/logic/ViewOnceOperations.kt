package com.spmods.spgram.presentation.features.chats.conversation.logic

import android.util.Log
import kotlinx.coroutines.launch
import com.spmods.spgram.domain.models.MessageContent
import com.spmods.spgram.domain.models.MessageModel
import com.spmods.spgram.presentation.features.chats.conversation.DefaultChatComponent

/**
 * Handles tapping a view-once (self-destructing) message that is already downloaded.
 *
 * All view-once files are now auto-downloaded by MessageContentMapper as soon as
 * the message arrives, so by the time the user taps we almost always have a local
 * path already. This function just:
 *  1. Notifies TDLib that the user opened the content (triggers server-side destruction).
 *  2. Opens the viewer/player for whichever content type is present.
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
                val path = content.path ?: return@launch
                onOpenImages(
                    images = listOf(path),
                    captions = listOf(content.caption.takeIf { it.isNotBlank() }),
                    startIndex = 0,
                    messageId = message.id,
                    messageIds = listOf(message.id)
                )
            }
            is MessageContent.Video -> {
                val path = content.path ?: return@launch
                onOpenVideo(path = path, messageId = message.id, caption = content.caption)
            }
            // Voice and VideoNote are inline players — they recompose automatically
            // once path is non-null, so no explicit open call is needed here.
            else -> Unit
        }
    }
}
