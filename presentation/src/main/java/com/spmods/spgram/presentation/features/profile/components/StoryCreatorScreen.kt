package com.spmods.spgram.presentation.features.profile.components

import androidx.compose.ui.graphics.toArgb

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.LaunchedEffect
import com.spmods.spgram.domain.models.UserModel
import com.spmods.spgram.domain.repository.UserRepository
import com.spmods.spgram.presentation.core.ui.Avatar
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FormatAlignCenter
import androidx.compose.material.icons.rounded.FormatAlignLeft
import androidx.compose.material.icons.rounded.FormatAlignRight
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import com.spmods.spgram.domain.models.StoryModel
import com.spmods.spgram.domain.models.StoryPrivacy
import com.spmods.spgram.domain.repository.StoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

// ─── Constants ───────────────────────────────────────────────────────────────

private const val MAX_STORY_VIDEO_SECONDS = 60
private const val MAX_STORY_CAPTION_CHARS = 2048

private enum class StoryType { PHOTO, VIDEO, TEXT }

private data class DurationOption(val label: String, val seconds: Int)
private val durationOptions = listOf(
    DurationOption("6h",  6  * 3600),
    DurationOption("12h", 12 * 3600),
    DurationOption("24h", 24 * 3600),
    DurationOption("48h", 48 * 3600),
)

private enum class PrivacyType { EVERYONE, CONTACTS, CLOSE_FRIENDS, SELECTED_USERS }

private data class PrivacyOption(val label: String, val icon: String, val type: PrivacyType)
private val privacyOptions = listOf(
    PrivacyOption("Everyone",       "🌍", PrivacyType.EVERYONE),
    PrivacyOption("Contacts",       "👥", PrivacyType.CONTACTS),
    PrivacyOption("Close Friends",  "⭐", PrivacyType.CLOSE_FRIENDS),
    PrivacyOption("Selected Users", "👤", PrivacyType.SELECTED_USERS),
)

private val textBgColors = listOf(
    Color.Transparent,
    Color(0xCC000000),
    Color(0xCC1A1A2E),
    Color(0xCC16213E),
    Color(0xCC0F3460),
    Color(0xCC533483),
    Color(0xCC1B4332),
    Color(0xCC7B2D00),
)

private val textColors = listOf(
    Color.White,
    Color.Black,
    Color(0xFFFFD700),
    Color(0xFFFF6B6B),
    Color(0xFF4ECDC4),
    Color(0xFFA8E6CF),
    Color(0xFFFFAA00),
    Color(0xFFE040FB),
)

private val gradientBgs = listOf(
    listOf(Color(0xFF1A1A2E), Color(0xFF16213E)),
    listOf(Color(0xFF0F3460), Color(0xFF533483)),
    listOf(Color(0xFF1B4332), Color(0xFF081C15)),
    listOf(Color(0xFF7B2D00), Color(0xFF3D0000)),
    listOf(Color(0xFF23074D), Color(0xFFCC5333)),
    listOf(Color(0xFF005C97), Color(0xFF363795)),
    listOf(Color(0xFF134E5E), Color(0xFF71B280)),
    listOf(Color(0xFF373B44), Color(0xFF4286F4)),
)

// ─── Main Composable ──────────────────────────────────────────────────────────

@Composable
fun StoryCreatorScreen(
    chatId: Long,
    onPosted: (StoryModel) -> Unit,
    onDismiss: () -> Unit
) {
    val context      = LocalContext.current
    val scope        = rememberCoroutineScope()
    val storyRepo: StoryRepository = koinInject()
    val userRepo: UserRepository   = koinInject()

    // ── media state
    var storyType        by remember { mutableStateOf(StoryType.PHOTO) }
    var selectedPath     by remember { mutableStateOf<String?>(null) }
    var videoThumbnail   by remember { mutableStateOf<Bitmap?>(null) }
    var isPreparing      by remember { mutableStateOf(false) }
    var isPosting        by remember { mutableStateOf(false) }
    var errorMsg         by remember { mutableStateOf<String?>(null) }

    // ── text story state
    var textContent      by remember { mutableStateOf("") }
    var selectedTextColor   by remember { mutableIntStateOf(0) }
    var selectedTextBg      by remember { mutableIntStateOf(1) }
    var selectedGradient    by remember { mutableIntStateOf(0) }
    var textAlign        by remember { mutableStateOf(TextAlign.Center) }
    var showTextEditor   by remember { mutableStateOf(false) }

    // ── caption (photo/video)
    var caption          by remember { mutableStateOf("") }
    val captionRemaining = MAX_STORY_CAPTION_CHARS - caption.length

    // ── privacy & duration
    var selectedPrivacyType  by remember { mutableStateOf(PrivacyType.EVERYONE) }
    var exceptUserIds        by remember { mutableStateOf<List<Long>>(emptyList()) }
    var selectedUserIds      by remember { mutableStateOf<List<Long>>(emptyList()) }
    var selectedDuration     by remember { mutableStateOf(durationOptions[2]) }

    // ── user picker panel
    var showUserPickerPanel  by remember { mutableStateOf(false) }
    var userPickerMode       by remember { mutableStateOf("selected") } // "selected" or "except"
    var userSearchQuery      by remember { mutableStateOf("") }
    var allContacts          by remember { mutableStateOf<List<UserModel>>(emptyList()) }
    var isLoadingContacts    by remember { mutableStateOf(false) }

    // Load contacts when picker opens
    LaunchedEffect(showUserPickerPanel) {
        if (showUserPickerPanel && allContacts.isEmpty()) {
            isLoadingContacts = true
            allContacts = runCatching { userRepo.getContacts() }.getOrDefault(emptyList())
            isLoadingContacts = false
        }
    }

    // Search filter
    val filteredContacts = remember(allContacts, userSearchQuery) {
        if (userSearchQuery.isBlank()) allContacts
        else allContacts.filter { u ->
            val q = userSearchQuery.lowercase()
            u.firstName.lowercase().contains(q) ||
            u.lastName?.lowercase()?.contains(q) == true ||
            u.username?.lowercase()?.contains(q) == true
        }
    }

    fun buildPrivacy(): StoryPrivacy = when (selectedPrivacyType) {
        PrivacyType.EVERYONE       -> StoryPrivacy.Everyone(exceptUserIds)
        PrivacyType.CONTACTS       -> StoryPrivacy.Contacts(exceptUserIds)
        PrivacyType.CLOSE_FRIENDS  -> StoryPrivacy.CloseFriends
        PrivacyType.SELECTED_USERS -> StoryPrivacy.SelectedUsers(selectedUserIds)
    }

    // ── transform (pinch-zoom on preview)
    var scale            by remember { mutableFloatStateOf(1f) }
    var offsetX          by remember { mutableFloatStateOf(0f) }
    var offsetY          by remember { mutableFloatStateOf(0f) }

    // ── panel visibility
    var showPrivacyPanel   by remember { mutableStateOf(false) }
    var showDurationPanel  by remember { mutableStateOf(false) }
    var showTextStylePanel by remember { mutableStateOf(false) }

    BackHandler { onDismiss() }

    // ── helpers
    suspend fun copyUri(uri: Uri, isVideo: Boolean): String? = withContext(Dispatchers.IO) {
        runCatching {
            val ext  = if (isVideo) "mp4" else "jpg"
            val file = File(context.cacheDir, "story_${System.nanoTime()}.$ext")
            context.contentResolver.openInputStream(uri)?.use { i ->
                FileOutputStream(file).use { o -> i.copyTo(o) }
            } ?: return@withContext null
            file.absolutePath
        }.getOrNull()
    }

    suspend fun videoDuration(path: String): Int? = withContext(Dispatchers.IO) {
        val r = MediaMetadataRetriever()
        runCatching {
            r.setDataSource(path)
            r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()?.let { (it / 1000).toInt() }
        }.also { r.release() }.getOrNull()
    }

    suspend fun videoThumb(path: String): Bitmap? = withContext(Dispatchers.IO) {
        val r = MediaMetadataRetriever()
        runCatching { r.setDataSource(path); r.getFrameAtTime(0) }
            .also { r.release() }.getOrNull()
    }

    fun onPickedMedia(uri: Uri, isVideo: Boolean) {
        scope.launch {
            isPreparing   = true
            errorMsg      = null
            selectedPath  = null
            videoThumbnail = null
            scale = 1f; offsetX = 0f; offsetY = 0f

            val path = copyUri(uri, isVideo)
            if (path == null) { errorMsg = "Couldn't read file."; isPreparing = false; return@launch }
            if (isVideo) {
                val dur = videoDuration(path)
                if (dur != null && dur > MAX_STORY_VIDEO_SECONDS) {
                    errorMsg = "Video too long (${dur}s). Max $MAX_STORY_VIDEO_SECONDS s."
                    File(path).delete(); isPreparing = false; return@launch
                }
                videoThumbnail = videoThumb(path)
            }
            selectedPath  = path
            storyType     = if (isVideo) StoryType.VIDEO else StoryType.PHOTO
            isPreparing   = false
        }
    }

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { it?.let { uri -> onPickedMedia(uri, false) } }
    val videoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { it?.let { uri -> onPickedMedia(uri, true)  } }

    val canPost = when (storyType) {
        StoryType.TEXT  -> textContent.isNotBlank()
        else            -> selectedPath != null
    } && !isPosting && !isPreparing

    fun doPost() {
        scope.launch {
            isPosting = true; errorMsg = null
            runCatching {
                val result = when (storyType) {
                    StoryType.VIDEO -> storyRepo.postVideoStory(
                        chatId              = chatId,
                        videoPath           = selectedPath!!,
                        caption             = caption.trim(),
                        activePeriodSeconds = selectedDuration.seconds,
                        privacy             = buildPrivacy()
                    )
                    StoryType.PHOTO -> storyRepo.postPhotoStory(
                        chatId              = chatId,
                        photoPath           = selectedPath!!,
                        caption             = caption.trim(),
                        activePeriodSeconds = selectedDuration.seconds,
                        privacy             = buildPrivacy()
                    )
                    StoryType.TEXT -> {
                        // Render the text canvas to a temp JPEG and post as photo
                        val textBitmap = renderTextStoryBitmap(
                            context      = context,
                            text         = textContent,
                            textColor    = textColors[selectedTextColor],
                            textBgColor  = textBgColors[selectedTextBg],
                            gradColors   = gradientBgs[selectedGradient]
                        )
                        val tmpFile = File(context.cacheDir, "story_text_${System.currentTimeMillis()}.jpg")
                        withContext(Dispatchers.IO) {
                            FileOutputStream(tmpFile).use { textBitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
                        }
                        storyRepo.postPhotoStory(
                            chatId              = chatId,
                            photoPath           = tmpFile.absolutePath,
                            caption             = "",   // text is baked into the image
                            activePeriodSeconds = selectedDuration.seconds,
                            privacy             = buildPrivacy()
                        )
                    }
                }
                if (result != null) onPosted(result) else errorMsg = "Failed to post. Try again."
            }.onFailure { errorMsg = it.message ?: "Unknown error" }
            isPosting = false
        }
    }

    // ─── Root ────────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0F))
            // Consume ALL pointer events — prevents clicks passing through to chat list behind
            .pointerInput(Unit) { detectTapGestures { /* consume */ } }
    ) {

        // ── 1. Preview canvas ─────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.72f)
                .align(Alignment.TopCenter)
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                .background(Color.Black)
        ) {
            when {
                isPreparing -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }

                storyType == StoryType.TEXT -> {
                    // Gradient background
                    val grad = gradientBgs[selectedGradient]
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.verticalGradient(grad))
                            .pointerInput(Unit) { detectTapGestures { showTextEditor = true } },
                        contentAlignment = Alignment.Center
                    ) {
                        if (textContent.isEmpty()) {
                            Text(
                                text = "Tap to add text",
                                color = Color.White.copy(alpha = 0.45f),
                                style = MaterialTheme.typography.titleMedium
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 32.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(textBgColors[selectedTextBg])
                                    .padding(horizontal = 20.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    text      = textContent,
                                    color     = textColors[selectedTextColor],
                                    style     = TextStyle(
                                        fontSize   = 22.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign  = textAlign,
                                        lineHeight = 30.sp,
                                    )
                                )
                            }
                        }
                    }
                }

                selectedPath != null -> {
                    val scaleAnim by animateFloatAsState(scale, tween(200), label = "scale")
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale   = (scale * zoom).coerceIn(0.8f, 4f)
                                    offsetX += pan.x
                                    offsetY += pan.y
                                }
                            }
                    ) {
                        if (storyType == StoryType.VIDEO) {
                            videoThumbnail?.let { bmp ->
                                Image(
                                    bitmap       = bmp.asImageBitmap(),
                                    contentDescription = null,
                                    modifier     = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            scaleX        = scaleAnim
                                            scaleY        = scaleAnim
                                            translationX  = offsetX
                                            translationY  = offsetY
                                        },
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Icon(
                                imageVector      = Icons.Rounded.PlayCircle,
                                contentDescription = null,
                                tint             = Color.White.copy(alpha = 0.85f),
                                modifier         = Modifier
                                    .align(Alignment.Center)
                                    .size(56.dp)
                            )
                        } else {
                            SubcomposeAsyncImage(
                                model            = selectedPath,
                                contentDescription = null,
                                modifier         = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX       = scaleAnim
                                        scaleY       = scaleAnim
                                        translationX = offsetX
                                        translationY = offsetY
                                    },
                                contentScale     = ContentScale.Crop
                            )
                        }
                    }
                }

                else -> {
                    // Empty canvas — show media type selector
                    Column(
                        modifier            = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            MediaPickTile(
                                icon    = Icons.Rounded.Image,
                                label   = "Photo",
                                onClick = { photoLauncher.launch("image/*") }
                            )
                            MediaPickTile(
                                icon    = Icons.Rounded.Videocam,
                                label   = "Video",
                                onClick = { videoLauncher.launch("video/*") }
                            )
                            MediaPickTile(
                                icon    = Icons.Rounded.TextFields,
                                label   = "Text",
                                onClick = { storyType = StoryType.TEXT }
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Choose story type",
                            color = Color.White.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // ── Top bar overlay ─────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = Color.White)
                }

                Text(
                    "New Story",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )

                // Change media button (only if media selected)
                if (selectedPath != null || storyType == StoryType.TEXT) {
                    IconButton(onClick = {
                        selectedPath = null; videoThumbnail = null
                        textContent  = ""; storyType = StoryType.PHOTO
                        scale = 1f; offsetX = 0f; offsetY = 0f
                    }) {
                        Icon(Icons.Rounded.Close, null, tint = Color.White)
                    }
                } else {
                    Spacer(Modifier.size(48.dp))
                }
            }

            // ── Right toolbar (visible when media/text selected) ─────────
            AnimatedVisibility(
                visible = selectedPath != null || storyType == StoryType.TEXT,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp),
                enter = fadeIn() + slideInVertically(),
                exit  = fadeOut() + slideOutVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (storyType == StoryType.TEXT) {
                        ToolbarIconButton(Icons.Rounded.Palette, "Style")  { showTextStylePanel = !showTextStylePanel }
                        ToolbarIconButton(Icons.Rounded.FormatAlignLeft, "Align") {
                            textAlign = when (textAlign) {
                                TextAlign.Left   -> TextAlign.Center
                                TextAlign.Center -> TextAlign.Right
                                else             -> TextAlign.Left
                            }
                        }
                    }
                    if (selectedPath != null) {
                        ToolbarIconButton(Icons.Rounded.Image, "Change photo") { photoLauncher.launch("image/*") }
                        ToolbarIconButton(Icons.Rounded.Videocam, "Change video") { videoLauncher.launch("video/*") }
                    }
                    ToolbarIconButton(Icons.Rounded.Timer, "Duration") { showDurationPanel = !showDurationPanel; showPrivacyPanel = false }
                }
            }
        }

        // ── 2. Bottom controls panel ──────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color(0xFF111118))
                .padding(WindowInsets.navigationBars.asPaddingValues())
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Caption / Text input
            if (storyType != StoryType.TEXT) {
                OutlinedTextField(
                    value         = caption,
                    onValueChange = { caption = it },
                    placeholder   = { Text("Add a caption…", color = Color.White.copy(alpha = 0.4f)) },
                    modifier      = Modifier.fillMaxWidth(),
                    maxLines      = 2,
                    textStyle     = TextStyle(color = Color.White, fontSize = 14.sp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = Color.White.copy(alpha = 0.5f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        cursorColor          = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Privacy row
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Audience", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
                // ── Privacy — vertical radio-style list ──────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.06f)),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    privacyOptions.forEachIndexed { idx, opt ->
                        val selected = opt.type == selectedPrivacyType
                        val isLast   = idx == privacyOptions.lastIndex

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(
                                    when (idx) {
                                        0               -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                                        privacyOptions.lastIndex -> RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                                        else            -> RoundedCornerShape(0.dp)
                                    }
                                )
                                .clickable {
                                    selectedPrivacyType = opt.type
                                    if (opt.type == PrivacyType.SELECTED_USERS) {
                                        userPickerMode = "selected"; showUserPickerPanel = true
                                    } else {
                                        showUserPickerPanel = false
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(opt.icon, style = MaterialTheme.typography.titleMedium)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    opt.label,
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                )
                                val subtitle = when (opt.type) {
                                    PrivacyType.EVERYONE       -> if (exceptUserIds.isNotEmpty()) "Except ${exceptUserIds.size} users" else "All Telegram users"
                                    PrivacyType.CONTACTS       -> if (exceptUserIds.isNotEmpty()) "Except ${exceptUserIds.size} contacts" else "Your contacts only"
                                    PrivacyType.CLOSE_FRIENDS  -> "Your close friends list"
                                    PrivacyType.SELECTED_USERS -> if (selectedUserIds.isEmpty()) "Tap to choose users" else "${selectedUserIds.size} users selected"
                                }
                                Text(
                                    subtitle,
                                    color = Color.White.copy(alpha = 0.45f),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            // Radio indicator
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(if (selected) Color(0xFF4C6EF5) else Color.Transparent)
                                    .then(
                                        if (!selected) Modifier.then(
                                            Modifier.background(Color.White.copy(alpha = 0.15f))
                                        ) else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                    )
                                }
                            }
                        }

                        if (!isLast) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 52.dp),
                                color = Color.White.copy(alpha = 0.06f)
                            )
                        }
                    }
                }

                // ── "Except…" / "Choose users…" action row ───────────────────
                AnimatedVisibility(
                    visible = selectedPrivacyType == PrivacyType.EVERYONE || selectedPrivacyType == PrivacyType.CONTACTS
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF4C6EF5).copy(alpha = 0.12f))
                            .clickable { userPickerMode = "except"; showUserPickerPanel = true }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Except…", color = Color(0xFF4C6EF5), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        if (exceptUserIds.isNotEmpty()) {
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF4C6EF5)).padding(horizontal = 8.dp, vertical = 2.dp)
                            ) { Text("${exceptUserIds.size}", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
                        }
                    }
                }

                AnimatedVisibility(visible = selectedPrivacyType == PrivacyType.SELECTED_USERS) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF4C6EF5).copy(alpha = 0.12f))
                            .clickable { userPickerMode = "selected"; showUserPickerPanel = true }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (selectedUserIds.isEmpty()) "Tap to choose users…" else "${selectedUserIds.size} users selected",
                            color = Color(0xFF4C6EF5), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Duration row
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("Visible for", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    durationOptions.forEach { opt ->
                        val selected = opt == selectedDuration
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (selected) Color(0xFF4C6EF5) else Color.White.copy(alpha = 0.08f))
                                .clickable { selectedDuration = opt }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(opt.label, color = Color.White, style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                        }
                    }
                }
            }

            // Error
            if (errorMsg != null) {
                Text(errorMsg!!, color = Color(0xFFFF6B6B), style = MaterialTheme.typography.bodySmall)
            }

            // Post button
            Button(
                onClick  = ::doPost,
                enabled  = canPost,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(16.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4C6EF5),
                    disabledContainerColor = Color.White.copy(alpha = 0.1f)
                )
            ) {
                if (isPosting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Text("Post Story", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.White)
                }
            }
        }

        // ── 3. Text style panel (slides up from bottom of canvas) ─────────
        AnimatedVisibility(
            visible  = showTextStylePanel && storyType == StoryType.TEXT,
            modifier = Modifier.align(Alignment.Center).fillMaxWidth(),
            enter    = slideInVertically { it },
            exit     = slideOutVertically { it }
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color    = Color(0xFF1A1A2A),
                shape    = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Text Color", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        textColors.forEachIndexed { idx, color ->
                            ColorDot(color = color, selected = idx == selectedTextColor, onClick = { selectedTextColor = idx })
                        }
                    }
                    Text("Text Background", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        textBgColors.forEachIndexed { idx, color ->
                            val display = if (color == Color.Transparent) Color.White.copy(alpha = 0.15f) else color
                            ColorDot(
                                color    = display,
                                selected = idx == selectedTextBg,
                                onClick  = { selectedTextBg = idx },
                                bordered = idx == 0
                            )
                        }
                    }
                    Text("Gradient Background", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        gradientBgs.forEachIndexed { idx, colors ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Brush.verticalGradient(colors))
                                    .then(if (idx == selectedGradient) Modifier.border(2.dp, Color.White, CircleShape) else Modifier)
                                    .clickable { selectedGradient = idx }
                            )
                        }
                    }
                }
            }
        }

        // ── 4. Inline text editor overlay ─────────────────────────────────
        AnimatedVisibility(
            visible  = showTextEditor && storyType == StoryType.TEXT,
            modifier = Modifier.fillMaxSize(),
            enter    = fadeIn(),
            exit     = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .pointerInput(Unit) { detectTapGestures { showTextEditor = false } },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp)
                        .pointerInput(Unit) { detectTapGestures { /* consume */ } },
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    BasicTextField(
                        value         = textContent,
                        onValueChange = { textContent = it },
                        textStyle     = TextStyle(
                            color      = textColors[selectedTextColor],
                            fontSize   = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign  = textAlign,
                        ),
                        modifier      = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(textBgColors[selectedTextBg])
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        decorationBox = { inner ->
                            if (textContent.isEmpty()) {
                                Text("Type your story…", color = Color.White.copy(alpha = 0.4f), style = MaterialTheme.typography.bodyLarge)
                            }
                            inner()
                        }
                    )
                    Button(
                        onClick  = { showTextEditor = false },
                        colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF4C6EF5)),
                        shape    = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Check, null, tint = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text("Done", color = Color.White)
                    }
                }
            }
        }

        // ── 5. User picker panel ──────────────────────────────────────────
        AnimatedVisibility(
            visible  = showUserPickerPanel,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.55f)
                .align(Alignment.BottomCenter),
            enter    = slideInVertically { it },
            exit     = slideOutVertically { it }
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color    = Color(0xFF16161F),
                shape    = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(
                            if (userPickerMode == "selected") "Select Users" else "Exclude Users",
                            color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .clickable {
                                        if (userPickerMode == "selected") selectedUserIds = emptyList()
                                        else exceptUserIds = emptyList()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) { Text("Clear", color = Color(0xFFFF6B6B), style = MaterialTheme.typography.labelSmall) }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF4C6EF5))
                                    .clickable { showUserPickerPanel = false }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) { Text("Done", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold) }
                        }
                    }
                    OutlinedTextField(
                        value = userSearchQuery, onValueChange = { userSearchQuery = it },
                        placeholder = { Text("Search contacts…", color = Color.White.copy(alpha = 0.4f)) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = Color(0xFF4C6EF5),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            cursorColor          = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    // Contact list
                    when {
                        isLoadingContacts -> {
                            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Color(0xFF4C6EF5))
                            }
                        }
                        filteredContacts.isEmpty() -> {
                            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                                Text(
                                    if (userSearchQuery.isBlank()) "No contacts found" else "No results for \"$userSearchQuery\"",
                                    color = Color.White.copy(alpha = 0.35f),
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                items(filteredContacts, key = { it.id }) { user ->
                                    val isChecked = if (userPickerMode == "selected")
                                        user.id in selectedUserIds
                                    else
                                        user.id in exceptUserIds

                                    val displayName = buildString {
                                        append(user.firstName)
                                        if (!user.lastName.isNullOrBlank()) append(" ${user.lastName}")
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isChecked) Color(0xFF4C6EF5).copy(alpha = 0.15f)
                                                else Color.Transparent
                                            )
                                            .clickable {
                                                if (userPickerMode == "selected") {
                                                    selectedUserIds = if (isChecked)
                                                        selectedUserIds - user.id
                                                    else
                                                        selectedUserIds + user.id
                                                } else {
                                                    exceptUserIds = if (isChecked)
                                                        exceptUserIds - user.id
                                                    else
                                                        exceptUserIds + user.id
                                                }
                                            }
                                            .padding(horizontal = 8.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Avatar(
                                            path         = user.avatarPath,
                                            fallbackPath = user.personalAvatarPath,
                                            name         = user.firstName.ifBlank { "?" },
                                            size         = 40.dp
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                displayName,
                                                color      = Color.White,
                                                style      = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            if (!user.username.isNullOrBlank()) {
                                                Text(
                                                    "@${user.username}",
                                                    color = Color.White.copy(alpha = 0.5f),
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }
                                        if (isChecked) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF4C6EF5)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Rounded.Check, null,
                                                    tint     = Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    val count = if (userPickerMode == "selected") selectedUserIds.size else exceptUserIds.size
                    if (count > 0) {
                        Text(
                            "$count user(s) ${if (userPickerMode == "selected") "selected" else "excluded"}",
                            color = Color(0xFF4C6EF5), style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

// ─── Helper Composables ────────────────────────────────────────────────────────

@Composable
private fun MediaPickTile(
    icon    : androidx.compose.ui.graphics.vector.ImageVector,
    label   : String,
    onClick : () -> Unit
) {
    Column(
        modifier            = Modifier
            .size(90.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(6.dp))
        Text(label, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ToolbarIconButton(
    icon        : androidx.compose.ui.graphics.vector.ImageVector,
    description : String,
    onClick     : () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, description, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun ColorDot(
    color   : Color,
    selected: Boolean,
    onClick : () -> Unit,
    bordered: Boolean = false
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(color)
            .then(if (bordered) Modifier.border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape) else Modifier)
            .then(if (selected) Modifier.border(2.dp, Color.White, CircleShape) else Modifier)
            .clickable(onClick = onClick)
    )
}

/**
 * Renders a text story canvas to a Bitmap (1080x1920) matching what the user sees on screen.
 * Used to convert TEXT-type stories to an image before posting via TDLib.
 */
private fun renderTextStoryBitmap(
    context    : android.content.Context,
    text       : String,
    textColor  : Color,
    textBgColor: Color,
    gradColors : List<Color>
): android.graphics.Bitmap {
    val width  = 1080
    val height = 1920
    val bmp    = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)

    // Draw gradient background
    val shader = android.graphics.LinearGradient(
        0f, 0f, 0f, height.toFloat(),
        intArrayOf(gradColors.first().toArgb(), gradColors.last().toArgb()),
        null,
        android.graphics.Shader.TileMode.CLAMP
    )
    val bgPaint = android.graphics.Paint().apply { this.shader = shader }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

    // Draw text
    val textSizePx = width * 0.07f
    val textPaint  = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color    = textColor.toArgb()
        this.textSize = textSizePx
        textAlign = android.graphics.Paint.Align.CENTER
        typeface  = android.graphics.Typeface.DEFAULT_BOLD
    }
    val bgPad = textSizePx * 0.3f
    val lineH = textPaint.fontMetrics.let { it.descent - it.ascent } + bgPad
    val lines = text.split("\n")
    val totalH = lines.size * lineH
    var y = (height - totalH) / 2f - textPaint.fontMetrics.ascent

    val bgPaintText = android.graphics.Paint().apply { color = textBgColor.toArgb() }
    for (line in lines) {
        val tw = textPaint.measureText(line)
        if (textBgColor != Color.Transparent) {
            canvas.drawRoundRect(
                width / 2f - tw / 2f - bgPad, y + textPaint.fontMetrics.ascent - bgPad,
                width / 2f + tw / 2f + bgPad, y + textPaint.fontMetrics.descent + bgPad,
                bgPad, bgPad, bgPaintText
            )
        }
        canvas.drawText(line, width / 2f, y, textPaint)
        y += lineH
    }
    return bmp
}
