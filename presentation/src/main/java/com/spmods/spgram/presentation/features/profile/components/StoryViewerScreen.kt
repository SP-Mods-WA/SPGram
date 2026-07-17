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
import androidx.compose.material.icons.filled.Delete
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

private const val STORY_DURATION_MS = 5_000L
private const val SWIPE_DOWN_THRESHOLD = 120f

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
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
    onGetViewers: suspend (StoryModel) -> FoundStoryViewersModel? = { null },
    onForwardStory: suspend (StoryModel, Long) -> Unit = { _, _ -> },
    forwardChatList: List<com.spmods.spgram.domain.models.ChatModel> = emptyList(),
    onDismiss: () -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(initialIndex.coerceIn(0, stories.lastIndex)) }
    var progress by remember { mutableFloatStateOf(0f) }
    var isPaused by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }
    var isReplyFieldFocused by remember { mutableStateOf(false) }
    var showViewersSheet by remember { mutableStateOf(false) }
    var viewersData by remember { mutableStateOf<FoundStoryViewersModel?>(null) }
    var isLoadingViewers by remember { mutableStateOf(false) }
    var showReactionBar by remember { mutableStateOf(false) }
    var showForwardSheet by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val story = stories.getOrNull(currentIndex) ?: run { onDismiss(); return }
    val isLiked = story.chosenReactionEmoji != null
    val isVideo = story.content is StoryContentModel.Video

    DisposableEffect(story.id) {
        onStoryViewed(story)
        onDispose { onStoryClosed(story) }
    }

    // ── Progress timer (photo + video) ────────────────────────────────────
    LaunchedEffect(currentIndex, story.content) {
        progress = 0f
        val totalMs = STORY_DURATION_MS
        var elapsedMs = 0L
        var lastTickAt = System.currentTimeMillis()
        while (elapsedMs < totalMs) {
            delay(50)
            val now = System.currentTimeMillis()
            val isBlocked = isPaused || isReplyFieldFocused || showMenu || showViewersSheet || showReactionBar
            if (!isBlocked) {
                val isLoading = when (val c = story.content) {
                    is StoryContentModel.Photo -> c.filePath.isBlank()
                    is StoryContentModel.Video -> c.filePath.isBlank()
                    else -> false
                }
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
                    com.spmods.spgram.presentation.core.media.VideoStickerPlayer(
                        path = content.filePath,
                        type = com.spmods.spgram.presentation.core.media.VideoType.Gif,
                        modifier = Modifier.fillMaxSize(),
                        animate = !isPaused && !isReplyFieldFocused && !showMenu && !showReactionBar,
                        shouldLoop = false,
                        volume = if (isMuted) 0f else 1f,
                        contentScale = ContentScale.Fit,
                        thumbnailData = content.thumbnailPath.ifEmpty { null },
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

        // ── Tap left/right, hold to pause ─────────────────────────────────
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
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(Color(0xFF2C2C2E))
                        ) {
                            // Do Not Notify About Stories
                            DropdownMenuItem(
                                text = { Text("Do Not Notify About Stories", color = Color.White) },
                                leadingIcon = { Icon(Icons.Default.NotificationsOff, null, tint = Color.White) },
                                onClick = {
                                    showMenu = false
                                    coroutineScope.launch {
                                        runCatching {
                                            onToggleStoryNotifications(story.posterChatId, true)
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, "Story notifications muted", Toast.LENGTH_SHORT).show()
                                            }
                                        }.onFailure {
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, "Failed to mute notifications", Toast.LENGTH_SHORT).show()
                                            }
                                        }
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
                                        val ok = runCatching { saveStoryToGallery(context, story) }.isSuccess
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(
                                                context,
                                                if (ok) "Saved to gallery" else "Save failed",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
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
                                        withContext(Dispatchers.Main) {
                                            if (!link.isNullOrBlank()) {
                                                clipboardManager.setText(AnnotatedString(link))
                                                Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Could not get link", Toast.LENGTH_SHORT).show()
                                            }
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
                                        val shareText = link?.takeIf { it.isNotBlank() } ?: getStoryFilePath(story)
                                        withContext(Dispatchers.Main) {
                                            if (!shareText.isNullOrBlank()) {
                                                val intent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                                }
                                                context.startActivity(Intent.createChooser(intent, "Share Story"))
                                            } else {
                                                Toast.makeText(context, "Nothing to share", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                            )

                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                            // Delete (own stories only)
                            if (canDeleteStory) {
                                DropdownMenuItem(
                                    text = { Text("Delete Story", color = Color(0xFFFF453A)) },
                                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color(0xFFFF453A)) },
                                    onClick = {
                                        showMenu = false
                                        onDelete(story.id)
                                    }
                                )
                                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                            }

                            // Report
                            DropdownMenuItem(
                                text = { Text("Report", color = Color(0xFFFF453A)) },
                                leadingIcon = { Icon(Icons.Default.Flag, null, tint = Color(0xFFFF453A)) },
                                onClick = {
                                    showMenu = false
                                    coroutineScope.launch {
                                        runCatching { onReportStory(story) }
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Story reported", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // ── Caption ────────────────────────────────────────────────────────
        if (story.caption.isNotBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 80.dp)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(text = story.caption, color = Color.White, style = MaterialTheme.typography.bodyMedium)
            }
        }

        // ── View count (own stories) → tap opens viewers sheet ─────────────
        if (canDeleteStory) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 130.dp)
                    .clip(RoundedCornerShape(16.dp))
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
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.Visibility, null, tint = Color.White, modifier = Modifier.size(16.dp))
                Text(text = story.viewCount.toString(), color = Color.White, style = MaterialTheme.typography.labelMedium)
            }
        }

        // ── Bottom overlay — caption + emoji row + reply bar ───────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
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
            // ── Reaction bar (long-press reaction popup) ───────────────────
            if (showReactionBar) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .clip(RoundedCornerShape(32.dp))
                            .background(Color(0xFF1C1C1E))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val quickReactions = listOf("👍","❤️","🍓","👏","👎","🔥","🥰")
                        quickReactions.forEach { emoji ->
                            val isSelected = story.chosenReactionEmoji == emoji
                            Text(
                                text = emoji,
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color.White.copy(alpha = 0.15f) else Color.Transparent)
                                    .padding(6.dp)
                                    .clickable {
                                        onSetReaction(story, if (isSelected) null else emoji)
                                        showReactionBar = false
                                    }
                            )
                        }
                        // Expand button (▼)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF3A3A3C))
                                .clickable { showReactionBar = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("▾", color = Color.White, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }

            // ── Caption + emoji pill row ───────────────────────────────────
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
            }

            Spacer(modifier = Modifier.height(6.dp))

            // ── Reply field + forward + like ──────────────────────────────
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

                // Send button (typing) / Forward button (idle)
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

                AnimatedVisibility(visible = !canSend, enter = scaleIn(tween(150)) + fadeIn(), exit = scaleOut(tween(150)) + fadeOut()) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable { showForwardSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Reply, null, tint = Color.White, modifier = Modifier.size(26.dp))
                    }
                }

                // Heart / like — tap to like, long press → reaction bar
                val heartScale by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = if (isLiked) 1.2f else 1f,
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy
                    ),
                    label = "heartScale"
                )
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .pointerInput(isLiked) {
                            detectTapGestures(
                                onTap = {
                                    onSetReaction(story, if (isLiked) null else "\u2764")
                                },
                                onLongPress = {
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
                        modifier = Modifier.size(26.dp).graphicsLayer { scaleX = heartScale; scaleY = heartScale }
                    )
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
