package com.spmods.spgram.presentation.features.chats.conversation.logic

import android.util.Log
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.spmods.spgram.domain.models.MessageContent
import com.spmods.spgram.domain.models.MessageModel
import com.spmods.spgram.presentation.features.chats.conversation.DefaultChatComponent

internal fun DefaultChatComponent.handleOpenViewOnce(message: MessageModel) {
    scope.launch {
        // ✅ store.stateFlow has the real messages — _state does NOT.
        // _state is a separate MutableStateFlow for UI-only things (overlays, scroll, etc.)
        // and its messages list is always emptyList().
        fun latestMsg(): MessageModel =
            state.value.messages.find { it.id == message.id } ?: message

        val current = latestMsg()

        // ── If photo not downloaded yet, kick off download and wait for path ─
        val photoPath: String? = when (val c = current.content) {
            is MessageContent.Photo -> {
                if (!c.path.isNullOrBlank()) {
                    c.path
                } else {
                    // Start download
                    onDownloadFile(c.fileId)
                    // Wait for store.stateFlow to emit a non-null path
                    runCatching {
                        state.first { s ->
                            val msg = s.messages.find { it.id == message.id }
                            !(msg?.content as? MessageContent.Photo)?.path.isNullOrBlank()
                        }.messages
                            .find { it.id == message.id }
                            ?.let { (it.content as? MessageContent.Photo)?.path }
                    }.getOrNull()
                }
            }
            else -> null
        }

        val videoPath: String? = when (val c = current.content) {
            is MessageContent.Video -> {
                if (!c.path.isNullOrBlank()) {
                    c.path
                } else {
                    onDownloadFile(c.fileId)
                    runCatching {
                        state.first { s ->
                            val msg = s.messages.find { it.id == message.id }
                            !(msg?.content as? MessageContent.Video)?.path.isNullOrBlank()
                        }.messages
                            .find { it.id == message.id }
                            ?.let { (it.content as? MessageContent.Video)?.path }
                    }.getOrNull()
                }
            }
            else -> null
        }

        // Fresh snapshot after waiting
        val latest = latestMsg()

        // ── Notify TDLib the message was opened ──────────────────────────────
        val canOpen = when (latest.content) {
            is MessageContent.Photo     -> photoPath != null
            is MessageContent.Video     -> videoPath != null
            is MessageContent.Voice,
            is MessageContent.VideoNote -> true
            else                        -> false
        }

        if (!canOpen) {
            Log.e("ViewOnce", "Cannot open — path still null after waiting. msgId=${message.id}")
            return@launch
        }

        runCatching {
            repositoryMessage.openMessageContent(chatId, latest.id)
        }.onFailure {
            Log.e("ViewOnce", "openMessageContent failed", it)
        }

        // ── Open the viewer ──────────────────────────────────────────────────
        when (val content = latest.content) {
            is MessageContent.Photo -> onOpenImages(
                images     = listOf(photoPath!!),
                captions   = listOf(content.caption.takeIf { it.isNotBlank() }),
                startIndex = 0,
                messageId  = latest.id,
                messageIds = listOf(latest.id)
            )
            is MessageContent.Video -> onOpenVideo(
                path      = videoPath,
                messageId = latest.id,
                caption   = content.caption
            )
            else -> Unit
        }
    }
}
