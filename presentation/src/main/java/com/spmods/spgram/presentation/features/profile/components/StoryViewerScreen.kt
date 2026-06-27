package com.spmods.spgram.presentation.features.profile.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.spmods.spgram.domain.models.StoryContentModel
import com.spmods.spgram.domain.models.StoryModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val STORY_DURATION_MS = 5_000L

/**
 * Full story viewer.
 *
 * @param posterName Display name of the story owner, shown in the header.
 * @param posterAvatarPath Local file path (or url) for the owner's avatar, shown in the header.
 * @param onSendReply Called when the user sends a text reply from the reply bar.
 *                     Receives the story being viewed and the typed text.
 * @param onLikeStory Called when the user taps the like (heart) button.
 *                     Receives the story being viewed. Return/track the "liked" state
 *                     externally if you want it to persist across re-opens; locally the
 *                     button toggles its own state immediately for a responsive feel.
 */
@Composable
fun StoryViewerScreen(
    stories: List<StoryModel>,
    initialIndex: Int,
    posterName: String = "",
    posterAvatarPath: String? = null,
    canDeleteStory: Boolean = false,
    onDelete: (Int) -> Unit = {},
    onSendReply: (StoryModel, String) -> Unit = { _, _ -> },
    onLikeStory: (StoryModel) -> Unit = {},
    onDismiss: () -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(initialIndex.coerceIn(0, stories.lastIndex)) }
    var progress by remember { mutableFloatStateOf(0f) }
    var isPaused by remember { mutableStateOf(false) }
    var videoDurationMs by remember { mutableStateOf<Long?>(null) }
    var replyText by remember { mutableStateOf("") }
    var isReplyFieldFocused by remember { mutableStateOf(false) }
    val likedStoryIds = remember { mutableStateOf(setOf<Int>()) }
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val story = stories.getOrNull(currentIndex) ?: run { onDismiss(); return }
    val isLiked = likedStoryIds.value.contains(story.id)

    // For video stories, drive the progress bar from real elapsed playback time
    // instead of the fixed-duration timer (video length varies per story).
    LaunchedEffect(currentIndex, videoDurationMs) {
        if (story.content !is StoryContentModel.Video) return@LaunchedEffect
        progress = 0f
        val duration = videoDurationMs ?: return@LaunchedEffect
        val startedAt = System.currentTimeMillis()
        var elapsedWhilePaused = 0L
        var pauseStartedAt: Long? = null
        while (progress < 1f) {
            if (isPaused || isReplyFieldFocused) {
                if (pauseStartedAt == null) pauseStartedAt = System.currentTimeMillis()
                delay(50)
            } else {
                pauseStartedAt?.let {
                    elapsedWhilePaused += System.currentTimeMillis() - it
                    pauseStartedAt = null
                }
                val elapsed = System.currentTimeMillis() - startedAt - elapsedWhilePaused
                progress = (elapsed.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                delay(50)
            }
        }
    }

    // Auto-advance timer — pauses while isPaused is true or the reply field has focus,
    // and resumes from the exact progress value where it left off (no restart).
    // For video stories, advancement is driven by onPlaybackEnded instead, since video
    // length varies; this loop is skipped for videos so it doesn't fight with playback.
    LaunchedEffect(currentIndex) {
        if (story.content is StoryContentModel.Video) return@LaunchedEffect
        progress = 0f
        val steps = 100
        val stepDelay = STORY_DURATION_MS / steps
        var step = 0
        while (step < steps) {
            if (isPaused || isReplyFieldFocused) {
                delay(50)
            } else {
                delay(stepDelay)
                step++
                progress = step / steps.toFloat()
            }
        }
        if (currentIndex < stories.lastIndex) {
            currentIndex++
        } else {
            onDismiss()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Story content
        when (val content = story.content) {
            is StoryContentModel.Photo -> {
                SubcomposeAsyncImage(
                    model = content.filePath,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
            is StoryContentModel.Video -> {
                com.spmods.spgram.presentation.core.media.VideoStickerPlayer(
                    path = content.filePath,
                    type = com.spmods.spgram.presentation.core.media.VideoType.Gif,
                    modifier = Modifier.fillMaxSize(),
                    animate = !isPaused && !isReplyFieldFocused,
                    shouldLoop = false,
                    volume = 1f,
                    contentScale = ContentScale.Fit,
                    thumbnailData = content.thumbnailPath.ifEmpty { null },
                    onDurationKnown = { durationMs ->
                        if (durationMs > 0L) videoDurationMs = durationMs
                    },
                    onPlaybackEnded = {
                        if (currentIndex < stories.lastIndex) currentIndex++ else onDismiss()
                    }
                )
            }
            is StoryContentModel.Unsupported -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Story format not supported",
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Tap left/right to navigate + press-and-hold anywhere to pause.
        // A single pointerInput per side handles both gestures. We only treat the
        // gesture as "tap to navigate" if the finger was released quickly; a long
        // press-and-hold (used to pause) should just resume playback on release,
        // not navigate or dismiss the story.
        val tapMaxDurationMs = 250L
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .pointerInput(currentIndex) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            isPaused = true
                            val pressStart = System.currentTimeMillis()
                            val up = waitForUpOrCancellation()
                            isPaused = false
                            val heldMs = System.currentTimeMillis() - pressStart
                            if (up != null && heldMs < tapMaxDurationMs) {
                                if (currentIndex > 0) currentIndex-- else onDismiss()
                            }
                        }
                    }
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .pointerInput(currentIndex) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            isPaused = true
                            val pressStart = System.currentTimeMillis()
                            val up = waitForUpOrCancellation()
                            isPaused = false
                            val heldMs = System.currentTimeMillis() - pressStart
                            if (up != null && heldMs < tapMaxDurationMs) {
                                if (currentIndex < stories.lastIndex) currentIndex++ else onDismiss()
                            }
                        }
                    }
            )
        }

        // Top overlay — progress bars + header (avatar, name, time) + close/delete
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .systemBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Progress indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                stories.forEachIndexed { idx, _ ->
                    val segmentProgress = when {
                        idx < currentIndex -> 1f
                        idx == currentIndex -> progress
                        else -> 0f
                    }
                    LinearProgressIndicator(
                        progress = { segmentProgress },
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp)),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.4f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Header row: avatar + name/time, then delete/close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!posterAvatarPath.isNullOrBlank()) {
                            SubcomposeAsyncImage(
                                model = posterAvatarPath,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = posterName.ifBlank { "Story" },
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1
                        )
                        Text(
                            text = formatStoryRelativeTime(story.date),
                            color = Color.White.copy(alpha = 0.75f),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    }
                }

                if (canDeleteStory) {
                    IconButton(onClick = { onDelete(story.id) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete story",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Caption overlay, sits just above the reply bar
        if (story.caption.isNotBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 76.dp)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = story.caption,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Bottom bar — reply text field + send button + like button
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .imePadding()
                .systemBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = replyText,
                onValueChange = { replyText = it },
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { state -> isReplyFieldFocused = state.isFocused },
                placeholder = {
                    Text(
                        text = "Reply privately...",
                        color = Color.White.copy(alpha = 0.6f)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.White.copy(alpha = 0.6f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.35f),
                    cursorColor = Color.White,
                    focusedContainerColor = Color.White.copy(alpha = 0.08f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.08f)
                )
            )

            val canSend = replyText.isNotBlank()
            AnimatedVisibility(visible = canSend) {
                IconButton(
                    onClick = {
                        val textToSend = replyText.trim()
                        if (textToSend.isNotEmpty()) {
                            onSendReply(story, textToSend)
                            replyText = ""
                            focusManager.clearFocus()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send reply",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            AnimatedVisibility(visible = !canSend) {
                IconButton(
                    onClick = {
                        val nowLiked = !isLiked
                        likedStoryIds.value = if (nowLiked) {
                            likedStoryIds.value + story.id
                        } else {
                            likedStoryIds.value - story.id
                        }
                        if (nowLiked) {
                            coroutineScope.launch { onLikeStory(story) }
                        }
                    }
                ) {
                    AnimatedLikeIcon(isLiked = isLiked)
                }
            }
        }
    }
}

@Composable
private fun AnimatedLikeIcon(isLiked: Boolean) {
    Box(contentAlignment = Alignment.Center) {
        AnimatedVisibility(
            visible = isLiked,
            enter = scaleIn(animationSpec = tween(180)) + fadeIn(),
            exit = scaleOut(animationSpec = tween(180)) + fadeOut()
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Unlike",
                tint = Color(0xFFFF3B5C),
                modifier = Modifier
                    .size(24.dp)
            )
        }
        AnimatedVisibility(
            visible = !isLiked,
            enter = scaleIn(animationSpec = tween(180)) + fadeIn(),
            exit = scaleOut(animationSpec = tween(180)) + fadeOut()
        ) {
            Icon(
                imageVector = Icons.Default.FavoriteBorder,
                contentDescription = "Like",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Converts a TDLib story unix timestamp (seconds) into a short relative label
 * such as "5m", "3h", "2d", matching the look used on the screenshot reference.
 */
private fun formatStoryRelativeTime(unixSeconds: Int): String {
    val nowSeconds = System.currentTimeMillis() / 1000
    val diff = (nowSeconds - unixSeconds).coerceAtLeast(0)
    return when {
        diff < 60 -> "now"
        diff < 3600 -> "${diff / 60}m"
        diff < 86_400 -> "${diff / 3600}h"
        else -> "${diff / 86_400}d"
    }
}

