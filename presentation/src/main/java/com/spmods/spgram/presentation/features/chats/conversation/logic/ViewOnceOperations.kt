package com.spmods.spgram.presentation.features.chats.conversation.logic

import android.util.Log
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.spmods.spgram.domain.models.MessageContent
import com.spmods.spgram.domain.models.MessageModel
import com.spmods.spgram.presentation.features.chats.conversation.DefaultChatComponent

internal fun DefaultChatComponent.handleOpenViewOnce(message: MessageModel) {
    scope.launch {
        val latest = _state.value.messages.find { it.id == message.id } ?: message

        when (val content = latest.content) {
            is MessageContent.Photo -> {
                val path = content.path
                if (!path.isNullOrBlank()) {
                    onOpenImages(
                        images = listOf(path),
                        captions = listOf(content.caption.takeIf { it.isNotBlank() }),
                        startIndex = 0,
                        messageId = latest.id,
                        messageIds = listOf(latest.id)
                    )
                    runCatching { repositoryMessage.openMessageContent(chatId, latest.id) }
                    _state.update { state ->
                        state.copy(messages = state.messages.map {
                            if (it.id == latest.id && it.content is MessageContent.Photo) {
                                it.copy(content = (it.content as MessageContent.Photo).copy(isViewOnceOpened = true))
                            } else it
                        })
                    }
                } else {
                    onDownloadFile(content.fileId)
                }
            }
            is MessageContent.Video -> {
                val path = content.path
                if (!path.isNullOrBlank()) {
                    onOpenVideo(path, latest.id, content.caption)
                    runCatching { repositoryMessage.openMessageContent(chatId, latest.id) }
                } else {
                    onDownloadFile(content.fileId)
                }
            }
            else -> {
                try {
                    repositoryMessage.openMessageContent(chatId, latest.id)
                } catch (e: Throwable) {
                    Log.e("ViewOnce", "open failed", e)
                }
            }
        }
    }
}
