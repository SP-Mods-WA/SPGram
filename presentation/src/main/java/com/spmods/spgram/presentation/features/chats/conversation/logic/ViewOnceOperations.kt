package com.spmods.spgram.presentation.features.chats.conversation.logic

import android.util.Log
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.spmods.spgram.domain.models.MessageContent
import com.spmods.spgram.domain.models.MessageModel
import com.spmods.spgram.presentation.features.chats.conversation.DefaultChatComponent

/**
 * Handles tapping a view-once (self-destructing) message.
 *
 * Flow:
 *  1. User taps download icon  → onDownloadPhoto starts the download.
 *  2. User taps flame icon     → onOpenViewOnce is called (path is ready).
 *
 * If the flame is tapped but path is somehow still null (race condition /
 * state lag), we kick off the download and then WAIT for the path to arrive
 * via _state before opening the viewer — instead of silently doing nothing.
 */
internal fun DefaultChatComponent.handleOpenViewOnce(message: MessageModel) {
    scope.launch {
        // Prefer latest state, fall back to the passed snapshot.
        fun latestMsg() = _state.value.messages.find { it.id == message.id } ?: message

        val initial = latestMsg()

        // ── Resolve path, waiting for download if needed ────────────────────
        val photoPath: String? = when (val c = initial.content) {
            is MessageContent.Photo -> {
                if (!c.path.isNullOrBlank()) {
                    c.path
                } else {
                    // Path not ready yet. Start download (in case it wasn't triggered)
                    // and wait for _state to deliver a non-null path for this message.
                    Log.d("ViewOnce", "Photo path null — starting download and waiting…")
                    onDownloadFile(c.fileId)
                    try {
                        val ready = _state.first { state ->
                            val msg = state.messages.find { it.id == message.id }
                            val path = (msg?.content as? MessageContent.Photo)?.path
                            !path.isNullOrBlank()
                        }
                        (ready.messages.find { it.id == message.id }
                            ?.content as? MessageContent.Photo)?.path
                    } catch (e: Throwable) {
                        Log.e("ViewOnce", "Timed out waiting for photo path", e)
                        null
                    }
                }
            }
            else -> null
        }

        val videoPath: String? = when (val c = initial.content) {
            is MessageContent.Video -> {
                if (!c.path.isNullOrBlank()) {
                    c.path
                } else {
                    Log.d("ViewOnce", "Video path null — starting download and waiting…")
                    onDownloadFile(c.fileId)
                    try {
                        val ready = _state.first { state ->
                            val msg = state.messages.find { it.id == message.id }
                            val path = (msg?.content as? MessageContent.Video)?.path
                            !path.isNullOrBlank()
                        }
                        (ready.messages.find { it.id == message.id }
                            ?.content as? MessageContent.Video)?.path
                    } catch (e: Throwable) {
                        Log.e("ViewOnce", "Timed out waiting for video path", e)
                        null
                    }
                }
            }
            else -> null
        }

        // ── Get the freshest message snapshot now that path is ready ─────────
        val latest = latestMsg()

        val hasDisplayablePath = when (latest.content) {
            is MessageContent.Photo    -> photoPath != null
            is MessageContent.Video    -> videoPath != null
            is MessageContent.Voice,
            is MessageContent.VideoNote -> true
            else -> false
        }

        // ── Tell TDLib this content was opened (only once path is ready) ────
        // Calling openMessageContent BEFORE the file is fully available can cause
        // TDLib to expire the file reference mid-download, permanently blocking open.
        if (hasDisplayablePath) {
            try {
                repositoryMessage.openMessageContent(chatId, latest.id)
            } catch (e: Throwable) {
                Log.e("ViewOnce", "openMessageContent failed: msgId=${latest.id}", e)
            }

            _state.update { state ->
                state.copy(
                    messages = state.messages.map { msg ->
                        if (msg.id != latest.id) return@map msg
                        when (val content = msg.content) {
                            is MessageContent.Photo     -> msg.copy(content = content.copy(isViewOnceOpened = true))
                            is MessageContent.Video     -> msg.copy(content = content.copy(isViewOnceOpened = true))
                            is MessageContent.Voice     -> msg.copy(content = content.copy(isViewOnceOpened = true))
                            is MessageContent.VideoNote -> msg.copy(content = content.copy(isViewOnceOpened = true))
                            else -> msg
                        }
                    }
                )
            }
        }

        // ── Open the viewer ─────────────────────────────────────────────────
        when (val content = latest.content) {
            is MessageContent.Photo -> {
                if (photoPath != null) {
                    onOpenImages(
                        images   = listOf(photoPath),
                        captions = listOf(content.caption.takeIf { it.isNotBlank() }),
                        startIndex = 0,
                        messageId  = latest.id,
                        messageIds = listOf(latest.id)
                    )
                } else {
                    Log.e("ViewOnce", "Photo path still null after waiting — cannot open viewer")
                }
            }
            is MessageContent.Video -> {
                if (videoPath != null) {
                    onOpenVideo(path = videoPath, messageId = latest.id, caption = content.caption)
                } else {
                    Log.e("ViewOnce", "Video path still null after waiting — cannot open viewer")
                }
            }
            // Voice: VoiceMessageBubble calls togglePlayPause directly.
            // VideoNote: inline player recomposes once path arrives.
            else -> Unit
        }
    }
}
