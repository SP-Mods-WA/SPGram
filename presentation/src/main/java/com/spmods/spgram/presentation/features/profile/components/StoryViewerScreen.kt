package com.spmods.spgram.presentation.features.profile.components

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.spmods.spgram.domain.models.FoundStoryViewersModel
import com.spmods.spgram.domain.models.StoryContentModel
import com.spmods.spgram.domain.models.StoryModel
import com.spmods.spgram.domain.models.StoryViewerModel
import com.spmods.spgram.presentation.core.ui.AvatarForChat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.key
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.sp
import android.media.AudioManager
import android.media.SoundPool
import android.media.AudioAttributes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween as coreTween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.spmods.spgram.domain.repository.EmojiRepository
import com.spmods.spgram.presentation.features.stickers.ui.menu.ReactionPickerSheet
import com.spmods.spgram.presentation.features.stickers.ui.view.StickerImage
import org.koin.compose.koinInject
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private const val STORY_DURATION_MS = 5_000L
private const val SWIPE_DOWN_THRESHOLD = 120f

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StoryViewerScreen(
    stories: List<StoryModel>,
    initialIndex: Int,
    posterName: String = "",
    posterAvatarPath: String? = null,
    isOwnStory: Boolean = false,
    canDeleteStory: Boolean = false,
    onDelete: (Int) -> Unit = {},
    onSendReply: (StoryModel, String) -> Unit = { _, _ -> },
    onSetReaction: (StoryModel, String?) -> Unit = { _, _ -> },
    onOpenViewers: (StoryModel) -> Unit = {},
    onStoryViewed: (StoryModel) -> Unit = {},
    onStoryClosed: (StoryModel) -> Unit = {},
    onGetStoryLink: suspend (StoryModel) -> String? = { null },
    onReportStory: suspend (StoryModel) -> Unit = {},
    onToggleStoryNotifications: suspend (Long, Boolean) -> Unit = { _, _ -> },
    onGetViewers: suspend (StoryModel) -> FoundStoryViewersModel? = { null },
    onForwardStory: suspend (StoryModel, Long) -> Unit = { _, _ -> },
    forwardChatList: List<com.spmods.spgram.domain.models.ChatModel> = emptyList(),
    emojiRepository: EmojiRepository = koinInject(),
    onEditStoryPrivacy: suspend (StoryModel, com.spmods.spgram.domain.models.StoryPrivacy) -> Unit = { _, _ -> },
    onDismiss: () -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(initialIndex.coerceIn(0, stories.lastIndex)) }
    var progress by remember { mutableFloatStateOf(0f) }
    var isPaused by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }

    // ── Reaction system state ──────────────────────────────────────────────
    val haptic = LocalHapticFeedback.current
    var showFullReactionPicker by remember { mutableStateOf(false) }
    var availableReactions by remember { mutableStateOf<List<String>>(emptyList()) }
    data class ReactionBurst(val emoji: String, val x: Float, val y: Float, val id: Long)
    val reactionBursts = remember { mutableStateListOf<ReactionBurst>() }
    data class FloatingEmoji(val emoji: String, val x: Float, val y: Float, val id: Long)
    val floatingEmojis = remember { mutableStateListOf<FloatingEmoji>() }
    val reactionScope = rememberCoroutineScope()

    // soundPool/audioManager/triggerReaction defined below after context is available
    var showMenu by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }
    var isReplyFieldFocused by remember { mutableStateOf(false) }
    var showViewersSheet  by remember { mutableStateOf(false) }
    var showPrivacySheet  by remember { mutableStateOf(false) }
    var viewersData by remember(currentIndex) { mutableStateOf<FoundStoryViewersModel?>(null) }
    var isLoadingViewers by remember { mutableStateOf(false) }
    var showReactionBar by remember { mutableStateOf(false) }
    var showForwardSheet by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // ── Sound / haptic helpers (need context) ─────────────────────────────
    val soundPool = remember {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        SoundPool.Builder().setMaxStreams(3).setAudioAttributes(attrs).build()
    }
    val audioManager = remember { context.getSystemService(android.content.Context.AUDIO_SERVICE) as AudioManager }
    DisposableEffect(Unit) { onDispose { soundPool.release() } }

    fun playReactionSound() {
        val vol = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION).toFloat() /
                  audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION).toFloat()
        if (vol > 0f) {
            audioManager.playSoundEffect(AudioManager.FX_KEY_CLICK, vol * 0.7f)
        }
    }

    fun triggerReaction(emoji: String, screenX: Float = 0f, screenY: Float = 0f) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        playReactionSound()
        val id = System.nanoTime()
        floatingEmojis.add(FloatingEmoji(emoji, screenX, screenY, id))
        reactionBursts.add(ReactionBurst(emoji, screenX, screenY, id))
    }

    val story = stories.getOrNull(currentIndex) ?: run { onDismiss(); return }
    val isLiked = story.chosenReactionEmoji != null
    val isVideo = story.content is StoryContentModel.Video
    val showMuteButton = isVideo

    LaunchedEffect(story.posterChatId, story.id) {
        availableReactions = runCatching {
            emojiRepository.getDefaultEmojis().take(42)
        }.getOrElse {
            listOf("❤️","👍","🔥","🥰","👏","😁","🤔","🤯","😱","🤬","😢","🎉","🍓","👎","💩","🙏")
        }
    }

    DisposableEffect(story.id) {
        onStoryViewed(story)
        onDispose { onStoryClosed(story) }
    }

    // ── Progress timer — photos only; videos drive progress via videoProgressMs ─────────────────
    var videoProgressMs  by remember(currentIndex) { androidx.compose.runtime.mutableStateOf(0L) }
    var videoDurationMs  by remember(currentIndex) { androidx.compose.runtime.mutableStateOf(0L) }
    // Last known position — passed as startPositionMs on resume so ExoPlayer seeks back
    var pausedPositionMs by remember(currentIndex) { androidx.compose.runtime.mutableStateOf(0L) }

    LaunchedEffect(currentIndex, story.content) {
        if (story.content is StoryContentModel.Video) return@LaunchedEffect  // video drives itself
        progress = 0f
        val totalMs = STORY_DURATION_MS
        var elapsedMs = 0L
        var lastTickAt = System.currentTimeMillis()
        while (elapsedMs < totalMs) {
            delay(50)
            val now = System.currentTimeMillis()
            val isBlocked = isPaused || isReplyFieldFocused || showMenu || showViewersSheet || showReactionBar || showFullReactionPicker || showPrivacySheet
            if (!isBlocked) {
                val isLoading = (story.content as? StoryContentModel.Photo)?.filePath?.isBlank() == true
                if (!isLoading) {
                    elapsedMs += (now - lastTickAt)
                    progress = (elapsedMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
                }
            }
            // Always update lastTickAt so paused time is not counted on resume
            lastTickAt = now
        }
        if (currentIndex < stories.lastIndex) currentIndex++ else onDismiss()
    }

    // Video: sync progress bar from actual playback position
    // Seed video duration from activePeriodSeconds until onDurationKnown fires
    LaunchedEffect(currentIndex, story.content) {
        if (story.content !is StoryContentModel.Video) return@LaunchedEffect
        if (videoDurationMs <= 0L) {
            // Use 30s default (most story videos) — onDurationKnown will override with real value
            videoDurationMs = 30_000L
        }
    }
    // Video progress: smooth time-based loop, synced to real ExoPlayer position every ~1s
    LaunchedEffect(currentIndex, story.content, videoDurationMs) {
        if (story.content !is StoryContentModel.Video || videoDurationMs <= 0L) return@LaunchedEffect
        // Start elapsed from last known position (supports resume)
        var elapsedMs  = videoProgressMs   // sync start to current position
        var lastSync   = videoProgressMs   // last real position we received
        var lastTick   = System.currentTimeMillis()
        while (true) {
            delay(50)
            val now = System.currentTimeMillis()
            val isBlocked2 = isPaused || isReplyFieldFocused || showMenu || showViewersSheet || showReactionBar || showFullReactionPicker || showPrivacySheet
            if (!isBlocked2) {
                // If ExoPlayer reported a new position, re-sync elapsed to it (smooth, not jump)
                if (videoProgressMs != lastSync) {
                    elapsedMs = videoProgressMs
                    lastSync  = videoProgressMs
                } else {
                    // Interpolate between real syncs
                    elapsedMs += (now - lastTick).coerceAtLeast(0L)
                }
                progress = (elapsedMs.toFloat() / videoDurationMs.toFloat()).coerceIn(0f, 1f)
            }
            lastTick = now
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            // ── Swipe down to close ───────────────────────────────────────
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {},
                    onDragCancel = {}
                ) { _, dragAmount ->
                    if (dragAmount > SWIPE_DOWN_THRESHOLD) onDismiss()
                }
            }
    ) {
        // ── Story content ──────────────────────────────────────────────────
        when (val content = story.content) {
            is StoryContentModel.Photo -> {
                if (content.filePath.isBlank()) {
                    StoryLoadingIndicator()
                } else {
                    SubcomposeAsyncImage(
                        model = content.filePath,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
            is StoryContentModel.Video -> {
                if (content.filePath.isBlank()) {
                    StoryLoadingIndicator()
                } else {
                    // key() forces full recompose on path change — prevents stale ExoPlayer
                    key(content.filePath) {
                        val isVideoBlocked = isPaused || isReplyFieldFocused || showMenu || showReactionBar || showFullReactionPicker || showPrivacySheet
                        com.spmods.spgram.presentation.core.media.VideoStickerPlayer(
                            path             = content.filePath,
                            type             = com.spmods.spgram.presentation.core.media.VideoType.Gif,
                            modifier         = Modifier.fillMaxSize(),
                            animate          = !isVideoBlocked,
                            shouldLoop       = false,
                            volume           = if (isMuted) 0f else 1f,
                            contentScale     = ContentScale.Fit,
                            thumbnailData    = content.thumbnailPath.ifEmpty { null },
                            startPositionMs  = pausedPositionMs,
                            // reportProgress NOT set — we do our own smooth 50ms loop
                            // onProgressUpdate receives real position every ~1s from ExoPlayer
                            reportProgress   = true,
                            onProgressUpdate = { posMs ->
                                // Sync our elapsed counter to real position to prevent drift
                                videoProgressMs = posMs
                                if (!isVideoBlocked) pausedPositionMs = posMs
                            },
                            onDurationKnown  = { durMs ->
                                if (durMs > 0L) videoDurationMs = durMs
                            },
                            onPlaybackEnded  = {
                                pausedPositionMs = 0L
                                if (currentIndex < stories.lastIndex) currentIndex++ else onDismiss()
                            }
                        )
                    }
                }
            }
            is StoryContentModel.Unsupported -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Story format not supported", color = Color.White, textAlign = TextAlign.Center)
                }
            }
        }

        // ── Tap left/right, hold to pause ─────────────────────────────────
        val tapMaxDurationMs = 250L
        Row(modifier = Modifier.fillMaxSize()) {
            // Tap zones disabled when any overlay is open — prevents accidental dismiss
            val anyOverlayOpen = showReactionBar || showMenu || showViewersSheet || showForwardSheet || showFullReactionPicker || showPrivacySheet
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .pointerInput(currentIndex, anyOverlayOpen) {
                        if (anyOverlayOpen) return@pointerInput
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
                    .pointerInput(currentIndex, anyOverlayOpen) {
                        if (anyOverlayOpen) return@pointerInput
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

        // ── Top overlay — progress bars + header ──────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .systemBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Progress bars
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
                        modifier = Modifier.weight(1f).height(2.dp).clip(RoundedCornerShape(1.dp)),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.4f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar + name + time + count
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
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = posterName.ifBlank { "Story" },
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1
                            )
                            if (stories.size > 1) {
                                Text(
                                    text = "${currentIndex + 1}/${stories.size}",
                                    color = Color.White.copy(alpha = 0.75f),
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1
                                )
                            }
                        }
                        Text(
                            text = formatStoryRelativeTime(story.date),
                            color = Color.White.copy(alpha = 0.75f),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    }
                }

                // Right side: mute (video only) + 3-dot menu
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isVideo) {
                        IconButton(onClick = { isMuted = !isMuted }) {
                            Icon(
                                imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = if (isMuted) "Unmute" else "Mute",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }
        }

        // viewer count now shown in own-story bottom bar below

        // ── Bottom overlay — reaction bar floats above, caption + reply bar below ──
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                        )
                    )
                    .imePadding()
                    .systemBarsPadding()
                    .padding(bottom = 8.dp)
            ) {
            // ── Caption + chosen reaction pill row ────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (story.caption.isNotBlank()) {
                    Text(
                        text = story.caption,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                // Chosen reaction pill — shows current reaction, tap to open bar, long-press to clear
                if (!isOwnStory) {
                    val chosenEmoji = story.chosenReactionEmoji
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (chosenEmoji != null) Color(0xFF4C6EF5).copy(alpha = 0.25f)
                                else Color.White.copy(alpha = 0.12f)
                            )
                            .border(
                                1.dp,
                                if (chosenEmoji != null) Color(0xFF4C6EF5).copy(alpha = 0.6f)
                                else Color.White.copy(alpha = 0.2f),
                                RoundedCornerShape(20.dp)
                            )
                            .pointerInput(chosenEmoji) {
                                detectTapGestures(
                                    onTap = { showReactionBar = true },
                                    onLongPress = {
                                        if (chosenEmoji != null) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onSetReaction(story, null)
                                        }
                                    }
                                )
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = chosenEmoji ?: "☺",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (isOwnStory) {
                // ── Own story bottom bar (Telegram-style) ─────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // LEFT: viewers pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0x55000000))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                            .clickable {
                                showViewersSheet = true
                                if (viewersData == null) {
                                    isLoadingViewers = true
                                    coroutineScope.launch {
                                        viewersData = runCatching { onGetViewers(story) }.getOrNull()
                                        isLoadingViewers = false
                                    }
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Visibility, null, tint = Color.White, modifier = Modifier.size(17.dp))
                        Text(
                            text = if (story.viewCount == 0) "No views yet" else story.viewCount.toString(),
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                        )
                        if (story.viewCount > 0) {
                            Text("viewed", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // RIGHT: privacy + delete
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0x55000000))
                                .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                                .clickable { showPrivacySheet = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Lock, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }

                    }
                }

            } else {
                // ── Other's story bottom bar — reply + forward + heart ─────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { state -> isReplyFieldFocused = state.isFocused },
                        placeholder = { Text("Reply privately...", color = Color.White.copy(alpha = 0.55f)) },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White.copy(alpha = 0.5f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            cursorColor = Color.White,
                            focusedContainerColor = Color(0xFF2C2C2E),
                            unfocusedContainerColor = Color(0xFF2C2C2E)
                        )
                    )

                    val canSend = replyText.isNotBlank()

                    AnimatedVisibility(visible = canSend, enter = scaleIn(tween(150)) + fadeIn(), exit = scaleOut(tween(150)) + fadeOut()) {
                        IconButton(onClick = {
                            val text = replyText.trim()
                            if (text.isNotEmpty()) {
                                onSendReply(story, text)
                                replyText = ""
                                focusManager.clearFocus()
                                Toast.makeText(context, "Reply sent", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White, modifier = Modifier.size(26.dp))
                        }
                    }

                    AnimatedVisibility(visible = !canSend && story.canBeForwarded, enter = scaleIn(tween(150)) + fadeIn(), exit = scaleOut(tween(150)) + fadeOut()) {
                        Box(
                            modifier = Modifier.size(48.dp).clip(CircleShape).clickable { showForwardSheet = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Reply, null, tint = Color.White, modifier = Modifier.size(26.dp))
                        }
                    }

                    // Heart / like — tap to like, long press → reaction bar
                    val heartAnimatable = remember { Animatable(1f) }
                    LaunchedEffect(story.chosenReactionEmoji) {
                        if (story.chosenReactionEmoji != null) {
                            heartAnimatable.animateTo(1.45f, spring(dampingRatio = 0.3f, stiffness = 600f))
                            heartAnimatable.animateTo(1.0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 300f))
                        } else {
                            heartAnimatable.animateTo(0.85f, coreTween(80))
                            heartAnimatable.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .pointerInput(isLiked) {
                                detectTapGestures(
                                    onTap = {
                                        val newEmoji = if (isLiked) null else "❤️"
                                        if (newEmoji != null) triggerReaction(newEmoji)
                                        onSetReaction(story, newEmoji)
                                    },
                                    onLongPress = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        showReactionBar = true
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = if (isLiked) Color(0xFFFF3B5C) else Color.White,
                            modifier = Modifier.size(26.dp).graphicsLayer {
                                scaleX = heartAnimatable.value
                                scaleY = heartAnimatable.value
                            }
                        )
                    }
                }
            }
            } // end bottom Column

        } // end bottom Box
    }

    // ── Reaction bar overlay (top-level — never pushes bottom bar) ─────────
    if (showReactionBar) {
        // Full-screen scrim — tap anywhere outside bar to close
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) { detectTapGestures { showReactionBar = false } }
        )
        // Reaction bar anchored bottom-right above reply bar
        Box(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(bottom = 72.dp, end = 12.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            AnimatedVisibility(
                visible = showReactionBar,
                enter = scaleIn(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                    initialScale = 0.4f,
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 1f)
                ) + fadeIn(tween(100)),
                exit = scaleOut(tween(80)) + fadeOut(tween(80))
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color(0xF21C1C1E))
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val quickReactions = listOf("❤️","👍","🔥","🥰","👏","😁","🎉")
                    quickReactions.forEachIndexed { idx, emoji ->
                        val isSelected = story.chosenReactionEmoji == emoji
                        val emojiScale = remember { Animatable(0f) }
                        LaunchedEffect(showReactionBar) {
                            if (showReactionBar) {
                                kotlinx.coroutines.delay(idx * 30L)
                                emojiScale.animateTo(
                                    1f,
                                    spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                                )
                            } else {
                                emojiScale.snapTo(0f)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .graphicsLayer { scaleX = emojiScale.value; scaleY = emojiScale.value }
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) Color.White.copy(alpha = 0.2f)
                                    else Color.Transparent
                                )
                                .clickable {
                                    val newEmoji = if (isSelected) null else emoji
                                    if (newEmoji != null) triggerReaction(newEmoji)
                                    onSetReaction(story, newEmoji)
                                    showReactionBar = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = emoji,
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.graphicsLayer {
                                    scaleX = if (isSelected) 1.2f else 1f
                                    scaleY = if (isSelected) 1.2f else 1f
                                }
                            )
                        }
                    }
                    // "+" → full reaction picker
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF3A3A3C))
                            .clickable {
                                showReactionBar = false
                                showFullReactionPicker = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }

    // ── Forward sheet ──────────────────────────────────────────────────────
    if (showForwardSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var forwardContacts by remember { mutableStateOf<List<com.spmods.spgram.domain.models.ChatModel>>(emptyList()) }
        var forwardSearch by remember { mutableStateOf("") }
        val filteredForward = remember(forwardContacts, forwardSearch) {
            if (forwardSearch.isBlank()) forwardContacts
            else forwardContacts.filter { it.title.contains(forwardSearch, ignoreCase = true) }
        }

        LaunchedEffect(Unit) {
            forwardContacts = forwardChatList
                .filter { it.type == com.spmods.spgram.domain.models.ChatType.PRIVATE || it.type == com.spmods.spgram.domain.models.ChatType.BASIC_GROUP || it.type == com.spmods.spgram.domain.models.ChatType.SUPERGROUP }
                .sortedByDescending { it.order }
        }

        ModalBottomSheet(
            onDismissRequest = { showForwardSheet = false },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                Text(
                    text = "Forward Story",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
                OutlinedTextField(
                    value = forwardSearch,
                    onValueChange = { forwardSearch = it },
                    placeholder = { Text("Search") },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                )
                if (filteredForward.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No chats found.\nForward via link instead:",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                    androidx.compose.material3.TextButton(
                        onClick = {
                            showForwardSheet = false
                            coroutineScope.launch {
                                val link = runCatching { onGetStoryLink(story) }.getOrNull()
                                val shareText = link?.takeIf { it.isNotBlank() } ?: getStoryFilePath(story)
                                withContext(Dispatchers.Main) {
                                    if (!shareText.isNullOrBlank()) {
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Forward Story"))
                                    }
                                }
                            }
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) { Text("Share Link") }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(filteredForward, key = { it.id }) { chat ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showForwardSheet = false
                                        coroutineScope.launch {
                                            runCatching { onForwardStory(story, chat.id) }
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, "Forwarded to ${chat.title}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                    .padding(horizontal = 20.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AvatarForChat(path = chat.avatarPath, name = chat.title, size = 42.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = chat.title, style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Viewers bottom sheet ───────────────────────────────────────────────
    // ── Story action bottom sheet (replaces ugly DropdownMenu) ─────────────
    if (showMenu) {
        ModalBottomSheet(
            onDismissRequest = { showMenu = false },
            containerColor = Color(0xFF1C1C1E),
            contentColor = Color.White,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 8.dp)
                        .size(36.dp, 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.25f))
                )
            }
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                @Composable
                fun MenuItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tint: Color = Color.White, onClick: () -> Unit) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showMenu = false; onClick() }
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
                        Text(label, color = tint, style = MaterialTheme.typography.bodyLarge)
                    }
                }

                if (!isOwnStory) {
                    MenuItem(Icons.Default.NotificationsOff, "Mute story notifications") {
                        coroutineScope.launch {
                            runCatching { onToggleStoryNotifications(story.posterChatId, true) }
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Story notifications muted", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = Color.White.copy(alpha = 0.08f))
                }

                MenuItem(Icons.Default.Save, "Save to gallery") {
                    coroutineScope.launch {
                        val ok = runCatching { saveStoryToGallery(context, story) }.isSuccess
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, if (ok) "Saved" else "Save failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = Color.White.copy(alpha = 0.08f))

                MenuItem(Icons.Default.ContentCopy, "Copy link") {
                    coroutineScope.launch {
                        val link = runCatching { onGetStoryLink(story) }.getOrNull()
                        withContext(Dispatchers.Main) {
                            if (!link.isNullOrBlank()) {
                                clipboardManager.setText(AnnotatedString(link))
                                Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
                            } else Toast.makeText(context, "Link unavailable", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = Color.White.copy(alpha = 0.08f))

                MenuItem(Icons.Default.Share, "Share") {
                    coroutineScope.launch {
                        val link = runCatching { onGetStoryLink(story) }.getOrNull()
                        val shareText = link?.takeIf { it.isNotBlank() } ?: getStoryFilePath(story)
                        withContext(Dispatchers.Main) {
                            if (!shareText.isNullOrBlank()) {
                                context.startActivity(Intent.createChooser(
                                    Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, shareText) },
                                    "Share Story"
                                ))
                            } else Toast.makeText(context, "Nothing to share", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                if (canDeleteStory) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = Color.White.copy(alpha = 0.08f))
                    MenuItem(Icons.Default.Delete, "Delete story", tint = Color(0xFFFF453A)) {
                        onDelete(story.id)
                    }
                }

                if (!isOwnStory) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = Color.White.copy(alpha = 0.08f))
                    MenuItem(Icons.Default.Flag, "Report", tint = Color(0xFFFF453A)) {
                        coroutineScope.launch {
                            runCatching { onReportStory(story) }
                            withContext(Dispatchers.Main) { Toast.makeText(context, "Reported", Toast.LENGTH_SHORT).show() }
                        }
                    }
                }
            }
        }
    }

    // ── Privacy edit sheet (own stories — top-level so ModalBottomSheet renders correctly) ──
    if (isOwnStory && showPrivacySheet) {
        val privacyOptions = listOf(
            Triple("🌍", "Everyone",      com.spmods.spgram.domain.models.StoryPrivacy.Everyone()),
            Triple("👥", "Contacts",      com.spmods.spgram.domain.models.StoryPrivacy.Contacts()),
            Triple("⭐", "Close Friends", com.spmods.spgram.domain.models.StoryPrivacy.CloseFriends),
        )
        ModalBottomSheet(
            onDismissRequest  = { showPrivacySheet = false },
            containerColor    = Color(0xFF1C1C1E),
            contentColor      = Color.White,
            dragHandle = {
                Box(modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    .size(36.dp, 4.dp).clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.25f)))
            }
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 40.dp)) {
                Text(
                    "Who can see this story",
                    color      = Color.White,
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    modifier   = Modifier.padding(vertical = 16.dp)
                )
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.07f))
                ) {
                    privacyOptions.forEachIndexed { idx, (icon, label, privacy) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showPrivacySheet = false
                                    coroutineScope.launch {
                                        runCatching { onEditStoryPrivacy(story, privacy) }
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Privacy updated to $label", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(icon, style = MaterialTheme.typography.titleLarge)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(label, color = Color.White, style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                                val sub = when (idx) {
                                    0    -> "All Telegram users"
                                    1    -> "Your contacts only"
                                    else -> "Your close friends list"
                                }
                                Text(sub, color = Color.White.copy(alpha = 0.45f), style = MaterialTheme.typography.bodySmall)
                            }
                            Icon(Icons.Default.ArrowForwardIos, null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(18.dp))
                        }
                        if (idx < privacyOptions.lastIndex) {
                            HorizontalDivider(modifier = Modifier.padding(start = 58.dp), color = Color.White.copy(alpha = 0.06f))
                        }
                    }
                }
            }
        }
    }

    // ── Floating emoji overlay (rises from center bottom) ──────────────────
    floatingEmojis.toList().forEach { fe ->
        key(fe.id) {
            FloatingEmojiOverlay(
                emoji = fe.emoji,
                startX = fe.x,
                startY = fe.y,
                onFinished = { floatingEmojis.removeAll { it.id == fe.id } }
            )
        }
    }

    // ── Particle burst canvas overlay ────────────────────────────────────────
    reactionBursts.toList().forEach { burst ->
        key(burst.id) {
            ReactionParticleBurst(
                emoji = burst.emoji,
                x = burst.x,
                y = burst.y,
                onFinished = { reactionBursts.removeAll { it.id == burst.id } }
            )
        }
    }

    // ── Full reaction picker sheet ───────────────────────────────────────────
    if (showFullReactionPicker && availableReactions.isNotEmpty()) {
        ReactionPickerSheet(
            availableReactions = availableReactions,
            chosenReactions = story.chosenReactionEmoji?.let { setOf(it) } ?: emptySet(),
            onReaction = { emoji ->
                val newEmoji = if (story.chosenReactionEmoji == emoji) null else emoji
                if (newEmoji != null) triggerReaction(newEmoji)
                onSetReaction(story, newEmoji)
                showFullReactionPicker = false
            },
            onDismiss = { showFullReactionPicker = false }
        )
    }

    if (showViewersSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showViewersSheet = false },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if ((viewersData?.totalCount ?: 0) > 0)
                        "Viewed by ${viewersData?.totalCount}"
                    else "Viewers",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
                when {
                    isLoadingViewers -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(160.dp),
                            contentAlignment = Alignment.Center
                        ) { LoadingIndicator() }
                    }
                    viewersData?.viewers.isNullOrEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No views yet", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                            contentPadding = PaddingValues(bottom = 32.dp)
                        ) {
                            items(viewersData!!.viewers, key = { it.userId }) { viewer ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AvatarForChat(
                                        path = viewer.avatarPath,
                                        name = viewer.name,
                                        size = 42.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = viewer.name, style = MaterialTheme.typography.bodyLarge,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(
                                            text = formatViewedTime(viewer.viewedAtSeconds),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    val emoji = viewer.reactionEmoji
                                    if (emoji != null) {
                                        Text(text = emoji, style = MaterialTheme.typography.titleLarge)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Helpers ────────────────────────────────────────────────────────────────────

private suspend fun saveStoryToGallery(context: Context, story: StoryModel) {
    val filePath = getStoryFilePath(story) ?: return
    val file = File(filePath)
    if (!file.exists()) return
    withContext(Dispatchers.IO) {
        val isVideo = filePath.endsWith(".mp4", ignoreCase = true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val collection = if (isVideo)
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            else
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val values = ContentValues().apply {
                put(if (isVideo) MediaStore.Video.Media.DISPLAY_NAME else MediaStore.Images.Media.DISPLAY_NAME, file.name)
                put(if (isVideo) MediaStore.Video.Media.MIME_TYPE else MediaStore.Images.Media.MIME_TYPE,
                    if (isVideo) "video/mp4" else "image/jpeg")
                put(if (isVideo) MediaStore.Video.Media.RELATIVE_PATH else MediaStore.Images.Media.RELATIVE_PATH,
                    if (isVideo) Environment.DIRECTORY_MOVIES + "/SPGram" else Environment.DIRECTORY_PICTURES + "/SPGram")
                put(if (isVideo) MediaStore.Video.Media.IS_PENDING else MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(collection, values) ?: return@withContext
            context.contentResolver.openOutputStream(uri)?.use { out ->
                FileInputStream(file).use { it.copyTo(out) }
            }
            values.clear()
            values.put(if (isVideo) MediaStore.Video.Media.IS_PENDING else MediaStore.Images.Media.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
        } else {
            val destDir = if (isVideo)
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            else
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val dest = File(destDir, "SPGram_${file.name}")
            file.copyTo(dest, overwrite = true)
            MediaScannerConnection.scanFile(context, arrayOf(dest.absolutePath), null, null)
        }
    }
}

private fun getStoryFilePath(story: StoryModel): String? = when (val c = story.content) {
    is StoryContentModel.Photo -> c.filePath.ifBlank { null }
    is StoryContentModel.Video -> c.filePath.ifBlank { null }
    else -> null
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StoryLoadingIndicator() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        LoadingIndicator(color = Color.White)
    }
}

// ── Floating emoji that rises from tap point ─────────────────────────────────
@Composable
private fun FloatingEmojiOverlay(
    emoji: String,
    startX: Float,
    startY: Float,
    onFinished: () -> Unit
) {
    val offsetY = remember { Animatable(0f) }
    val alpha   = remember { Animatable(1f) }
    val scale   = remember { Animatable(0.4f) }

    LaunchedEffect(Unit) {
        launch {
            scale.animateTo(1.3f, spring(dampingRatio = 0.4f, stiffness = 400f))
            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
        launch {
            offsetY.animateTo(-320f, androidx.compose.animation.core.tween(900, easing = FastOutSlowInEasing))
        }
        launch {
            kotlinx.coroutines.delay(500)
            alpha.animateTo(0f, androidx.compose.animation.core.tween(400))
            onFinished()
        }
    }

    val density = androidx.compose.ui.platform.LocalDensity.current
    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            text = emoji,
            fontSize = 42.sp,
            modifier = Modifier
                .graphicsLayer {
                    translationX = startX - 42.dp.toPx() / 2
                    translationY = startY + offsetY.value - 42.dp.toPx() / 2
                    this.alpha = alpha.value
                    scaleX = scale.value
                    scaleY = scale.value
                }
        )
    }
}

// ── Particle confetti burst at tap point ──────────────────────────────────────
private data class Particle(
    val angle: Float,
    val speed: Float,
    val color: androidx.compose.ui.graphics.Color,
    val radius: Float
)

@Composable
private fun ReactionParticleBurst(
    emoji: String,
    x: Float,
    y: Float,
    onFinished: () -> Unit
) {
    val progress = remember { Animatable(0f) }
    val particles = remember {
        val colors = listOf(
            androidx.compose.ui.graphics.Color(0xFFFF3B5C),
            androidx.compose.ui.graphics.Color(0xFFFF9500),
            androidx.compose.ui.graphics.Color(0xFFFFCC00),
            androidx.compose.ui.graphics.Color(0xFF34C759),
            androidx.compose.ui.graphics.Color(0xFF5AC8FA),
            androidx.compose.ui.graphics.Color(0xFFAF52DE),
        )
        (0 until 16).map {
            Particle(
                angle  = Random.nextFloat() * 360f,
                speed  = 120f + Random.nextFloat() * 180f,
                color  = colors[it % colors.size],
                radius = 4f + Random.nextFloat() * 5f
            )
        }
    }

    LaunchedEffect(Unit) {
        progress.animateTo(1f, androidx.compose.animation.core.tween(700, easing = FastOutSlowInEasing))
        onFinished()
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val t = progress.value
        particles.forEach { p ->
            val rad = Math.toRadians(p.angle.toDouble()).toFloat()
            val dist = p.speed * t
            val px = x + cos(rad) * dist
            val py = y + sin(rad) * dist
            val particleAlpha = (1f - t * t).coerceIn(0f, 1f)
            drawCircle(
                color = p.color.copy(alpha = particleAlpha),
                radius = p.radius * (1f - t * 0.5f),
                center = Offset(px, py)
            )
        }
    }
}

private fun formatStoryRelativeTime(unixSeconds: Int): String {
    val diff = (System.currentTimeMillis() / 1000 - unixSeconds).coerceAtLeast(0)
    return when {
        diff < 60 -> "now"
        diff < 3600 -> "${diff / 60}m"
        diff < 86_400 -> "${diff / 3600}h"
        else -> "${diff / 86_400}d"
    }
}

private fun formatViewedTime(unixSeconds: Int): String =
    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(unixSeconds.toLong() * 1000))
