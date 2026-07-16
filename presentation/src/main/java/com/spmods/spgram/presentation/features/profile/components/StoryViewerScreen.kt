package com.spmods.spgram.presentation.features.profile.components

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.BellOff
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.spmods.spgram.domain.models.StoryContentModel
import com.spmods.spgram.domain.models.StoryModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

private const val STORY_DURATION_MS = 5_000L

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StoryViewerScreen(
    stories: List<StoryModel>,
    initialIndex: Int,
    posterName: String = "",
    posterAvatarPath: String? = null,
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
    onDismiss: () -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(initialIndex.coerceIn(0, stories.lastIndex)) }
    var progress by remember { mutableFloatStateOf(0f) }
    var isPaused by remember { mutableStateOf(false) }
    var videoDurationMs by remember { mutableStateOf<Long?>(null) }
    var replyText by remember { mutableStateOf("") }
    var isReplyFieldFocused by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val story = stories.getOrNull(currentIndex) ?: run { onDismiss(); return }
    val isLiked = story.chosenReactionEmoji != null

    // Pause when menu is open
    val effectivelyPaused = isPaused || isReplyFieldFocused || showMenu

    DisposableEffect(story.id) {
        onStoryViewed(story)
        onDispose { onStoryClosed(story) }
    }

    // Video progress driver
    LaunchedEffect(currentIndex, videoDurationMs) {
        if (story.content !is StoryContentModel.Video) return@LaunchedEffect
        progress = 0f
        val duration = videoDurationMs ?: return@LaunchedEffect
        val startedAt = System.currentTimeMillis()
        var elapsedWhilePaused = 0L
        var pauseStartedAt: Long? = null
        while (progress < 1f) {
            if (effectivelyPaused) {
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

    // Photo auto-advance timer
    LaunchedEffect(currentIndex, story.content) {
        if (story.content is StoryContentModel.Video) return@LaunchedEffect
        val isLoadingContent = (story.content as? StoryContentModel.Photo)?.filePath.isNullOrBlank()
        progress = 0f
        val steps = 100
        val stepDelay = STORY_DURATION_MS / steps
        var step = 0
        while (step < steps) {
            if (effectivelyPaused || isLoadingContent) {
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
        // ── Story content ──────────────────────────────────────────────────
        when (val content = story.content) {
            is StoryContentModel.Photo -> {
                if (content.filePath.isBlank()) {
                    StoryContentLoadingIndicator()
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
                    StoryContentLoadingIndicator()
                } else {
                    com.spmods.spgram.presentation.core.media.VideoStickerPlayer(
                        path = content.filePath,
                        type = com.spmods.spgram.presentation.core.media.VideoType.Gif,
                        modifier = Modifier.fillMaxSize(),
                        animate = !effectivelyPaused,
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
            }
            is StoryContentModel.Unsupported -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Story format not supported", color = Color.White, textAlign = TextAlign.Center)
                }
            }
        }

        // ── Tap left/right to navigate, hold to pause ──────────────────────
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

        // ── Top overlay — progress + header ───────────────────────────────
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
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = posterName.ifBlank { "Story" },
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1
                            )
                            // "1/3" story count — only show if more than 1 story
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

                // Right side buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (canDeleteStory) {
                        IconButton(onClick = { onDelete(story.id) }) {
                            Icon(Icons.Default.Delete, null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                    }

                    // 3-dot menu
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(Color(0xFF2C2C2E))
                        ) {
                            // Do Not Notify About Stories
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (isMuted) "Notify About Stories" else "Do Not Notify About Stories",
                                        color = Color.White
                                    )
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.BellOff, null, tint = Color.White)
                                },
                                onClick = {
                                    showMenu = false
                                    val newMuted = !isMuted
                                    isMuted = newMuted
                                    coroutineScope.launch {
                                        runCatching { onToggleStoryNotifications(story.posterChatId, newMuted) }
                                    }
                                }
                            )

                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                            // Save to Gallery
                            DropdownMenuItem(
                                text = { Text("Save to Gallery", color = Color.White) },
                                leadingIcon = { Icon(Icons.Default.Save, null, tint = Color.White) },
                                onClick = {
                                    showMenu = false
                                    coroutineScope.launch {
                                        saveStoryToGallery(context, story)
                                    }
                                }
                            )

                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                            // Copy Link
                            DropdownMenuItem(
                                text = { Text("Copy Link", color = Color.White) },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, null, tint = Color.White) },
                                onClick = {
                                    showMenu = false
                                    coroutineScope.launch {
                                        val link = runCatching { onGetStoryLink(story) }.getOrNull()
                                        if (!link.isNullOrBlank()) {
                                            clipboardManager.setText(AnnotatedString(link))
                                        }
                                    }
                                }
                            )

                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                            // Share
                            DropdownMenuItem(
                                text = { Text("Share", color = Color.White) },
                                leadingIcon = { Icon(Icons.Default.Share, null, tint = Color.White) },
                                onClick = {
                                    showMenu = false
                                    coroutineScope.launch {
                                        val link = runCatching { onGetStoryLink(story) }.getOrNull()
                                        val shareText = if (!link.isNullOrBlank()) link else getStoryFilePath(story) ?: ""
                                        if (shareText.isNotBlank()) {
                                            val intent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, shareText)
                                            }
                                            context.startActivity(Intent.createChooser(intent, "Share Story"))
                                        }
                                    }
                                }
                            )

                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                            // Report
                            DropdownMenuItem(
                                text = { Text("Report", color = Color(0xFFFF453A)) },
                                leadingIcon = { Icon(Icons.Default.Flag, null, tint = Color(0xFFFF453A)) },
                                onClick = {
                                    showMenu = false
                                    coroutineScope.launch {
                                        runCatching { onReportStory(story) }
                                    }
                                }
                            )
                        }
                    }

                    // Close button
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }

        // ── Caption overlay ────────────────────────────────────────────────
        if (story.caption.isNotBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 76.dp)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(text = story.caption, color = Color.White, style = MaterialTheme.typography.bodyMedium)
            }
        }

        // ── View count (own stories) ────────────────────────────────────────
        if (canDeleteStory) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 76.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onOpenViewers(story) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.Visibility, null, tint = Color.White, modifier = Modifier.size(16.dp))
                Text(text = story.viewCount.toString(), color = Color.White, style = MaterialTheme.typography.labelMedium)
            }
        }

        // ── Bottom bar — reply + share + like ──────────────────────────────
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
                placeholder = { Text("Reply privately...", color = Color.White.copy(alpha = 0.6f)) },
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

            // Send button (when typing)
            AnimatedVisibility(visible = canSend) {
                IconButton(onClick = {
                    val textToSend = replyText.trim()
                    if (textToSend.isNotEmpty()) {
                        onSendReply(story, textToSend)
                        replyText = ""
                        focusManager.clearFocus()
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }

            // Share button (when not typing)
            AnimatedVisibility(visible = !canSend) {
                IconButton(onClick = {
                    coroutineScope.launch {
                        val link = runCatching { onGetStoryLink(story) }.getOrNull()
                        val shareText = if (!link.isNullOrBlank()) link else getStoryFilePath(story) ?: ""
                        if (shareText.isNotBlank()) {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Story"))
                        }
                    }
                }) {
                    Icon(Icons.Default.Share, null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }

            // Like button
            IconButton(onClick = {
                val nowLiked = !isLiked
                onSetReaction(story, if (nowLiked) "\u2764" else null)
            }) {
                AnimatedLikeIcon(isLiked = isLiked)
            }
        }
    }
}

// ── Save to gallery ────────────────────────────────────────────────────────────
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
                put(if (isVideo) MediaStore.Video.Media.MIME_TYPE else MediaStore.Images.Media.MIME_TYPE, if (isVideo) "video/mp4" else "image/jpeg")
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

private fun getStoryFilePath(story: StoryModel): String? {
    return when (val c = story.content) {
        is StoryContentModel.Photo -> c.filePath.ifBlank { null }
        is StoryContentModel.Video -> c.filePath.ifBlank { null }
        else -> null
    }
}

// ── Supporting composables ─────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StoryContentLoadingIndicator() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        LoadingIndicator(color = Color.White)
    }
}

@Composable
private fun AnimatedLikeIcon(isLiked: Boolean) {
    Box(contentAlignment = Alignment.Center) {
        AnimatedVisibility(visible = isLiked, enter = scaleIn(tween(180)) + fadeIn(), exit = scaleOut(tween(180)) + fadeOut()) {
            Icon(Icons.Default.Favorite, null, tint = Color(0xFFFF3B5C), modifier = Modifier.size(24.dp))
        }
        AnimatedVisibility(visible = !isLiked, enter = scaleIn(tween(180)) + fadeIn(), exit = scaleOut(tween(180)) + fadeOut()) {
            Icon(Icons.Default.FavoriteBorder, null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
    }
}

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
