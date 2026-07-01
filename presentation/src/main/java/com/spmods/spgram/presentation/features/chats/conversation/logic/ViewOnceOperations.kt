package com.spmods.spgram.presentation.features.chats.conversation.logic

import android.util.Log
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.spmods.spgram.domain.models.MessageContent
import com.spmods.spgram.domain.models.MessageModel
import com.spmods.spgram.presentation.features.chats.conversation.DefaultChatComponent

/**
 * Handles tapping a view-once (self-destructing) message.
 *
 * View-once files are NOT auto-downloaded. The user must tap the message
 * to trigger download and open. We read the LATEST message from _state
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

        // Only mark Photo/Video as "opened" once we actually have a path to show —
        // otherwise the view-once overlay (flame icon / download progress / "tap to
        // view" label) disappears the instant the user taps, before the file has even
        // downloaded, and nothing is left on screen to auto-open once it's ready.
        // Voice/VideoNote open inline via their own players, so it's safe to flip
        // those immediately.
        val passedPhoto = message.content as? MessageContent.Photo
        val passedVideo = message.content as? MessageContent.Video
        val currentPhotoPath = (latest.content as? MessageContent.Photo)?.path ?: passedPhoto?.path
        val currentVideoPath = (latest.content as? MessageContent.Video)?.path ?: passedVideo?.path

        val hasDisplayablePath = when (latest.content) {
            is MessageContent.Photo -> currentPhotoPath != null
            is MessageContent.Video -> currentVideoPath != null
            is MessageContent.Voice, is MessageContent.VideoNote -> true
            else -> false
        }

        // IMPORTANT: only tell TDLib the content was "opened" once we're actually
        // about to display it (path is ready). TDLib/the server treats this as the
        // signal that self-destructing media has been viewed and may begin expiring
        // the underlying file reference shortly after. Calling this BEFORE the photo
        // has finished downloading — as this used to do — can cause the in-flight
        // download to fail/hang on slow connections, leaving the user stuck with a
        // permanently un-openable "tap to view" bubble.
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
                            is MessageContent.Photo -> msg.copy(content = content.copy(isViewOnceOpened = true))
                            is MessageContent.Video -> msg.copy(content = content.copy(isViewOnceOpened = true))
                            is MessageContent.Voice -> msg.copy(content = content.copy(isViewOnceOpened = true))
                            is MessageContent.VideoNote -> msg.copy(content = content.copy(isViewOnceOpened = true))
                            else -> msg
                        }
                    }
                )
            }
        }

        when (val content = latest.content) {
            is MessageContent.Photo -> {
                val path = currentPhotoPath
                if (path != null) {
                    onOpenImages(
                        images = listOf(path),
                        captions = listOf(content.caption.takeIf { it.isNotBlank() }),
                        startIndex = 0,
                        messageId = latest.id,
                        messageIds = listOf(latest.id)
                    )
                } else {
                    // Not downloaded yet: kick off the download and leave the overlay
                    // in place (openMessageContent is NOT sent yet — see above). The
                    // auto-open effect in PhotoMessageBubble watches for content.path
                    // to arrive and will call this function again automatically —
                    // no second tap required.
                    Log.d("ViewOnce", "Photo not downloaded yet — triggering download")
                    onDownloadFile(content.fileId)
                }
            }
            is MessageContent.Video -> {
                val path = currentVideoPath
                if (path != null) {
                    onOpenVideo(path = path, messageId = latest.id, caption = content.caption)
                } else {
                    Log.w("ViewOnce", "Video tapped but path still null — download in progress")
                    onDownloadFile(content.fileId)
                }
            }
            // Voice: VoiceMessageBubble calls togglePlayPause directly after onOpenViewOnce.
            // VideoNote: inline player recomposes automatically once path arrives.
            else -> Unit
        }
    }
}
