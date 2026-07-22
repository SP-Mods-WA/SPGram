package com.spmods.spgram.presentation.features.profile.components

// ─── Monogram-style Story Composer ───────────────────────────────────────────
// Ported from org.monogram.presentation.features.stories (Monogram project)
// Adapted to SPGram domain models (StoryRepository, StoryModel, StoryPrivacy)
// Architecture: Compose/Preview dual-stage flow, M3 design, multi-media pager
// ─────────────────────────────────────────────────────────────────────────────

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PeopleAlt
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import com.spmods.spgram.domain.models.StoryModel
import com.spmods.spgram.domain.models.StoryPrivacy
import com.spmods.spgram.domain.models.UserModel
import com.spmods.spgram.domain.repository.StoryRepository
import com.spmods.spgram.domain.repository.UserRepository
import com.spmods.spgram.presentation.R
import com.spmods.spgram.presentation.core.ui.Avatar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import java.io.File
import java.io.FileOutputStream

// ─── Constants ────────────────────────────────────────────────────────────────

private const val MAX_STORY_VIDEO_SECONDS = 60
private const val MAX_STORY_CAPTION_CHARS = 2048
private const val STORY_MEDIA_ASPECT_RATIO = 9f / 16f

// ─── Domain types ─────────────────────────────────────────────────────────────

private enum class StoryMediaType { PHOTO, VIDEO }

private data class StoryMediaItem(
    val sourcePath: String,
    val mediaType: StoryMediaType,
    val thumbnail: Bitmap? = null  // only for VIDEO
)

private enum class StoryComposerStage { COMPOSE, PREVIEW }

private enum class StoryPrivacyUi { EVERYONE, CONTACTS, CLOSE_FRIENDS, SELECTED_USERS }

private data class StoryComposerDraft(
    val mediaItems: List<StoryMediaItem> = emptyList(),
    val selectedIndex: Int = 0,
    val caption: String = "",
    val privacy: StoryPrivacyUi = StoryPrivacyUi.EVERYONE,
    val exceptUserIds: List<Long> = emptyList(),
    val selectedUserIds: List<Long> = emptyList(),
    val activePeriodSeconds: Int = 86400,
    val keepOnProfile: Boolean = false,
    val protectContent: Boolean = false
) {
    val isValid: Boolean get() = mediaItems.isNotEmpty()
    val currentMedia: StoryMediaItem? get() = mediaItems.getOrNull(selectedIndex)
}

private val durationOptions = listOf(
    6 * 3600 to "6h",
    12 * 3600 to "12h",
    24 * 3600 to "24h",
    48 * 3600 to "48h"
)

// ─── Audience picker state ─────────────────────────────────────────────────────

private data class AudiencePickerState(
    val isVisible: Boolean = false,
    val isShowTo: Boolean = false,
    val contacts: List<UserModel> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false
)

// ─── Main Composable ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StoryCreatorScreen(
    chatId: Long,
    onPosted: (StoryModel) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val storyRepo: StoryRepository = koinInject()
    val userRepo: UserRepository = koinInject()

    // ── Composer state ──────────────────────────────────────────────────────
    var draft by remember { mutableStateOf(StoryComposerDraft()) }
    var stage by rememberSaveable { mutableStateOf(StoryComposerStage.COMPOSE) }
    var isPosting by remember { mutableStateOf(false) }
    var isPreparing by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // ── Audience picker ─────────────────────────────────────────────────────
    var audiencePicker by remember { mutableStateOf(AudiencePickerState()) }

    // Load contacts when picker opens
    LaunchedEffect(audiencePicker.isVisible) {
        if (audiencePicker.isVisible && audiencePicker.contacts.isEmpty()) {
            audiencePicker = audiencePicker.copy(isLoading = true)
            val contacts = runCatching { userRepo.getContacts() }.getOrDefault(emptyList())
            audiencePicker = audiencePicker.copy(contacts = contacts, isLoading = false)
        }
    }

    // ── Permission state ────────────────────────────────────────────────────
    var hasGalleryAccess by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED)
        )
    }
    val galleryPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results -> hasGalleryAccess = results.values.any { it } }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    // ── Media launchers ─────────────────────────────────────────────────────
    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            isPreparing = true; errorMsg = null
            val items = uris.mapNotNull { uri ->
                val path = copyUriToTemp(context, uri, false) ?: return@mapNotNull null
                StoryMediaItem(path, StoryMediaType.PHOTO)
            }
            if (items.isNotEmpty()) {
                draft = draft.copy(mediaItems = draft.mediaItems + items, selectedIndex = draft.mediaItems.size)
            }
            isPreparing = false
        }
    }
    val videoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            isPreparing = true; errorMsg = null
            val path = copyUriToTemp(context, uri, true) ?: run { isPreparing = false; return@launch }
            val dur = getVideoDuration(path)
            if (dur != null && dur > MAX_STORY_VIDEO_SECONDS) {
                errorMsg = "Video too long (${dur}s). Max $MAX_STORY_VIDEO_SECONDS s."
                File(path).delete(); isPreparing = false; return@launch
            }
            val thumb = getVideoThumbnail(path)
            draft = draft.copy(mediaItems = draft.mediaItems + StoryMediaItem(path, StoryMediaType.VIDEO, thumb))
            isPreparing = false
        }
    }

    // ── Back handling ───────────────────────────────────────────────────────
    BackHandler(enabled = stage == StoryComposerStage.PREVIEW) {
        stage = StoryComposerStage.COMPOSE
    }
    BackHandler(enabled = audiencePicker.isVisible) {
        audiencePicker = audiencePicker.copy(isVisible = false)
    }

    // ── Post logic ──────────────────────────────────────────────────────────
    fun buildPrivacy(): StoryPrivacy = when (draft.privacy) {
        StoryPrivacyUi.EVERYONE       -> StoryPrivacy.Everyone(draft.exceptUserIds)
        StoryPrivacyUi.CONTACTS       -> StoryPrivacy.Contacts(draft.exceptUserIds)
        StoryPrivacyUi.CLOSE_FRIENDS  -> StoryPrivacy.CloseFriends
        StoryPrivacyUi.SELECTED_USERS -> StoryPrivacy.SelectedUsers(draft.selectedUserIds)
    }

    fun doPost() {
        if (!draft.isValid || isPosting) return
        scope.launch {
            isPosting = true; errorMsg = null
            runCatching {
                var lastStory: StoryModel? = null
                for (item in draft.mediaItems) {
                    val result = when (item.mediaType) {
                        StoryMediaType.PHOTO -> storyRepo.postPhotoStory(
                            chatId = chatId,
                            photoPath = item.sourcePath,
                            caption = draft.caption.trim(),
                            activePeriodSeconds = draft.activePeriodSeconds,
                            privacy = buildPrivacy()
                        )
                        StoryMediaType.VIDEO -> storyRepo.postVideoStory(
                            chatId = chatId,
                            videoPath = item.sourcePath,
                            caption = draft.caption.trim(),
                            activePeriodSeconds = draft.activePeriodSeconds,
                            privacy = buildPrivacy()
                        )
                    }
                    if (result != null) lastStory = result
                    else { errorMsg = "Failed to post one or more stories."; break }
                }
                lastStory?.let { onPosted(it) } ?: run { errorMsg = "Failed to post story." }
            }.onFailure { errorMsg = it.message ?: "Unknown error" }
            isPosting = false
        }
    }

    // ── Scaffold ─────────────────────────────────────────────────────────────
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.story_compose_title),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = when (stage) {
                                StoryComposerStage.COMPOSE -> stringResource(R.string.story_caption_supporting)
                                StoryComposerStage.PREVIEW -> stringResource(R.string.story_preview_subtitle)
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (stage == StoryComposerStage.PREVIEW) {
                                stage = StoryComposerStage.COMPOSE
                            } else if (draft.isValid) {
                                stage = StoryComposerStage.PREVIEW
                            } else {
                                photoLauncher.launch("image/*")
                            }
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = when {
                                stage == StoryComposerStage.PREVIEW -> Icons.Rounded.Edit
                                draft.isValid -> Icons.Rounded.PlayArrow
                                else -> Icons.Rounded.PhotoLibrary
                            },
                            contentDescription = null
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            text = when {
                                stage == StoryComposerStage.PREVIEW -> stringResource(R.string.story_back_to_editor)
                                draft.isValid -> stringResource(R.string.story_preview)
                                else -> stringResource(R.string.story_pick_media)
                            },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = ::doPost,
                        enabled = draft.isValid && !isPosting && !isPreparing,
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (isPosting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.size(8.dp))
                        }
                        Text(
                            text = if (isPosting) stringResource(R.string.story_posting) else stringResource(R.string.story_publish_short),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                // Consume all pointer events to prevent click-through
                .pointerInput(Unit) { detectTapGestures { } }
        ) {
            AnimatedContent(
                targetState = stage,
                label = "StoryStage",
                transitionSpec = {
                    (fadeIn() + slideInVertically { it / 12 }) togetherWith
                            (fadeOut() + slideOutVertically { it / 14 })
                }
            ) { currentStage ->
                when (currentStage) {
                    StoryComposerStage.COMPOSE -> {
                        StoryComposeStage(
                            draft = draft,
                            isPreparing = isPreparing,
                            errorMsg = errorMsg,
                            onDraftChange = { draft = it },
                            onPickPhotos = {
                                if (hasGalleryAccess) {
                                    photoLauncher.launch("image/*")
                                } else {
                                    val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
                                    } else {
                                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                                    }
                                    galleryPermissionLauncher.launch(perms)
                                }
                            },
                            onPickVideo = { videoLauncher.launch("video/*") },
                            onOpenCamera = {
                                if (hasCameraPermission) { /* TODO: open camera */ }
                                else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            },
                            onShowAudiencePicker = { isShowTo ->
                                audiencePicker = audiencePicker.copy(isVisible = true, isShowTo = isShowTo)
                            },
                            onPreview = { stage = StoryComposerStage.PREVIEW }
                        )
                    }
                    StoryComposerStage.PREVIEW -> {
                        StoryPreviewStage(
                            draft = draft,
                            onSelectPage = { draft = draft.copy(selectedIndex = it) }
                        )
                    }
                }
            }
        }
    }

    // ── Audience picker bottom sheet ──────────────────────────────────────────
    if (audiencePicker.isVisible) {
        ModalBottomSheet(
            onDismissRequest = { audiencePicker = audiencePicker.copy(isVisible = false) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            StoryAudiencePickerSheet(
                isShowTo = audiencePicker.isShowTo,
                contacts = audiencePicker.contacts,
                searchQuery = audiencePicker.searchQuery,
                isLoading = audiencePicker.isLoading,
                selectedIds = if (audiencePicker.isShowTo) draft.selectedUserIds else draft.exceptUserIds,
                onSearchChange = { audiencePicker = audiencePicker.copy(searchQuery = it) },
                onToggleUser = { userId ->
                    if (audiencePicker.isShowTo) {
                        val newList = if (userId in draft.selectedUserIds)
                            draft.selectedUserIds - userId else draft.selectedUserIds + userId
                        draft = draft.copy(selectedUserIds = newList)
                    } else {
                        val newList = if (userId in draft.exceptUserIds)
                            draft.exceptUserIds - userId else draft.exceptUserIds + userId
                        draft = draft.copy(exceptUserIds = newList)
                    }
                },
                onClear = {
                    if (audiencePicker.isShowTo) draft = draft.copy(selectedUserIds = emptyList())
                    else draft = draft.copy(exceptUserIds = emptyList())
                },
                onDismiss = { audiencePicker = audiencePicker.copy(isVisible = false) }
            )
        }
    }
}

// ─── Compose Stage ────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StoryComposeStage(
    draft: StoryComposerDraft,
    isPreparing: Boolean,
    errorMsg: String?,
    onDraftChange: (StoryComposerDraft) -> Unit,
    onPickPhotos: () -> Unit,
    onPickVideo: () -> Unit,
    onOpenCamera: () -> Unit,
    onShowAudiencePicker: (Boolean) -> Unit,
    onPreview: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {

        // ── Media preview card ─────────────────────────────────────────────
        StorySectionHeader(
            title = stringResource(R.string.story_preview_title),
            subtitle = stringResource(R.string.story_preview_subtitle)
        )
        StoryMediaPreviewCard(
            draft = draft,
            isPreparing = isPreparing,
            onSelectPage = { onDraftChange(draft.copy(selectedIndex = it)) },
            onPickPhotos = onPickPhotos,
            onPreview = onPreview
        )

        // ── Media picker actions ───────────────────────────────────────────
        StorySectionHeader(
            title = stringResource(R.string.story_change_media),
            subtitle = stringResource(R.string.story_select_media_hint)
        )
        StoryMediaActionsCard(
            hasMedia = draft.isValid,
            onPickPhotos = onPickPhotos,
            onPickVideo = onPickVideo,
            onOpenCamera = onOpenCamera,
            onPreview = if (draft.isValid) onPreview else null
        )

        // ── Error banner ───────────────────────────────────────────────────
        errorMsg?.let { msg ->
            Spacer(Modifier.size(8.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Text(
                    text = msg,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        // ── Caption ────────────────────────────────────────────────────────
        StorySectionHeader(
            title = stringResource(R.string.story_details_title),
            subtitle = stringResource(R.string.story_caption_supporting)
        )
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            OutlinedTextField(
                value = draft.caption,
                onValueChange = { if (it.length <= MAX_STORY_CAPTION_CHARS) onDraftChange(draft.copy(caption = it)) },
                placeholder = { Text(stringResource(R.string.story_caption_label), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.fillMaxWidth().padding(4.dp),
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // ── Privacy ────────────────────────────────────────────────────────
        StorySectionHeader(title = stringResource(R.string.story_privacy_label), subtitle = null)
        StoryPrivacyCard(
            draft = draft,
            onDraftChange = onDraftChange,
            onShowAudiencePicker = onShowAudiencePicker
        )

        // ── Duration ───────────────────────────────────────────────────────
        StorySectionHeader(
            title = stringResource(R.string.story_duration_label),
            subtitle = stringResource(R.string.story_duration_supporting)
        )
        StoryDurationCard(
            selected = draft.activePeriodSeconds,
            onSelect = { onDraftChange(draft.copy(activePeriodSeconds = it)) }
        )

        // ── Settings ───────────────────────────────────────────────────────
        StorySectionHeader(
            title = stringResource(R.string.story_settings_title),
            subtitle = stringResource(R.string.story_audience_timing_subtitle)
        )
        StorySettingsCard(
            keepOnProfile = draft.keepOnProfile,
            protectContent = draft.protectContent,
            onKeepOnProfileChange = { onDraftChange(draft.copy(keepOnProfile = it)) },
            onProtectContentChange = { onDraftChange(draft.copy(protectContent = it)) }
        )

        Spacer(Modifier.size(16.dp))
    }
}

// ─── Preview Stage ────────────────────────────────────────────────────────────

@Composable
private fun StoryPreviewStage(
    draft: StoryComposerDraft,
    onSelectPage: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val maxH = 560.dp
            val maxW = maxH * STORY_MEDIA_ASPECT_RATIO
            val previewWidth = if (maxWidth < maxW) maxWidth else maxW

            Surface(
                modifier = Modifier.width(previewWidth).aspectRatio(STORY_MEDIA_ASPECT_RATIO),
                shape = RoundedCornerShape(28.dp),
                color = Color.Black
            ) {
                StoryMediaPagerContent(
                    mediaItems = draft.mediaItems,
                    selectedIndex = draft.selectedIndex,
                    caption = draft.caption,
                    onPageChanged = onSelectPage
                )
            }
        }

        if (draft.mediaItems.size > 1) {
            StoryPagerDotsIndicator(
                count = draft.mediaItems.size,
                selected = draft.selectedIndex
            )
        }

        // Summary info
        StorySectionHeader(
            title = stringResource(R.string.story_settings_title),
            subtitle = stringResource(R.string.story_preview_subtitle)
        )
        StoryPreviewSummaryCard(draft = draft)
    }
}

// ─── Media Preview Card ───────────────────────────────────────────────────────

@Composable
private fun StoryMediaPreviewCard(
    draft: StoryComposerDraft,
    isPreparing: Boolean,
    onSelectPage: (Int) -> Unit,
    onPickPhotos: () -> Unit,
    onPreview: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val maxH = 360.dp
                val maxW = maxH * STORY_MEDIA_ASPECT_RATIO
                val previewWidth = if (maxWidth < maxW) maxWidth else maxW

                if (isPreparing) {
                    Box(
                        modifier = Modifier
                            .width(previewWidth)
                            .aspectRatio(STORY_MEDIA_ASPECT_RATIO)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (!draft.isValid) {
                    Box(
                        modifier = Modifier
                            .width(previewWidth)
                            .aspectRatio(STORY_MEDIA_ASPECT_RATIO)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable(onClick = onPickPhotos),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Rounded.Image,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            Text(
                                stringResource(R.string.story_pick_media),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                stringResource(R.string.story_select_media_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .width(previewWidth)
                            .aspectRatio(STORY_MEDIA_ASPECT_RATIO)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.Black)
                            .clickable(onClick = onPreview)
                    ) {
                        StoryMediaPagerContent(
                            mediaItems = draft.mediaItems,
                            selectedIndex = draft.selectedIndex,
                            caption = draft.caption,
                            onPageChanged = onSelectPage
                        )
                    }
                }
            }

            if (draft.mediaItems.size > 1) {
                StoryPagerDotsIndicator(
                    count = draft.mediaItems.size,
                    selected = draft.selectedIndex
                )
            }
        }
    }
}

// ─── Media Pager Content ──────────────────────────────────────────────────────

@Composable
private fun StoryMediaPagerContent(
    mediaItems: List<StoryMediaItem>,
    selectedIndex: Int,
    caption: String,
    onPageChanged: (Int) -> Unit
) {
    if (mediaItems.isEmpty()) return
    val current = mediaItems.getOrNull(selectedIndex) ?: mediaItems.first()

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        when (current.mediaType) {
            StoryMediaType.VIDEO -> {
                current.thumbnail?.let { bmp ->
                    androidx.compose.foundation.Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Icon(
                    Icons.Rounded.PlayCircle,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.align(Alignment.Center).size(56.dp)
                )
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
                    shape = RoundedCornerShape(999.dp),
                    color = Color.Black.copy(alpha = 0.42f)
                ) {
                    Text(
                        stringResource(R.string.story_media_video),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                }
            }
            StoryMediaType.PHOTO -> {
                AsyncImage(
                    model = current.sourcePath,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // Gradient overlay
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f)))
            )
        )

        // Caption overlay
        if (caption.isNotBlank()) {
            Surface(
                modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.Black.copy(alpha = 0.38f)
            ) {
                Text(
                    text = caption,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Page indicator (top right if multiple)
        if (mediaItems.size > 1) {
            Surface(
                modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
                shape = RoundedCornerShape(999.dp),
                color = Color.Black.copy(alpha = 0.42f)
            ) {
                Text(
                    text = "${selectedIndex + 1}/${mediaItems.size}",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
            }

            // Swipe arrows for multi-page navigation
            if (selectedIndex > 0) {
                Surface(
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp),
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.42f),
                    onClick = { onPageChanged((selectedIndex - 1).coerceAtLeast(0)) }
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.padding(8.dp).size(20.dp)
                    )
                }
            }
            if (selectedIndex < mediaItems.lastIndex) {
                Surface(
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp),
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.42f),
                    onClick = { onPageChanged((selectedIndex + 1).coerceAtMost(mediaItems.lastIndex)) }
                ) {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.padding(8.dp).size(20.dp)
                    )
                }
            }
        }
    }
}

// ─── Pager Dots ───────────────────────────────────────────────────────────────

@Composable
private fun StoryPagerDotsIndicator(count: Int, selected: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) { index ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .size(
                        width = if (index == selected) 18.dp else 6.dp,
                        height = 6.dp
                    )
                    .clip(CircleShape)
                    .background(
                        if (index == selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant
                    )
            )
        }
    }
}

// ─── Media Actions Card ───────────────────────────────────────────────────────

@Composable
private fun StoryMediaActionsCard(
    hasMedia: Boolean,
    onPickPhotos: () -> Unit,
    onPickVideo: () -> Unit,
    onOpenCamera: () -> Unit,
    onPreview: (() -> Unit)?
) {
    data class ActionItem(val icon: ImageVector, val title: String, val onClick: () -> Unit)

    val items = buildList {
        add(ActionItem(Icons.Rounded.PhotoLibrary, if (hasMedia) stringResource(R.string.story_change_media) else stringResource(R.string.story_pick_media), onPickPhotos))
        add(ActionItem(Icons.Rounded.Image, "Add Video", onPickVideo))
        add(ActionItem(Icons.Rounded.CameraAlt, "Camera", onOpenCamera))
        if (hasMedia && onPreview != null) {
            add(ActionItem(Icons.Rounded.PlayArrow, stringResource(R.string.story_preview), onPreview))
        }
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column {
            items.forEachIndexed { idx, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = item.onClick)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                        }
                    }
                    Text(item.title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                }
                if (idx < items.lastIndex) {
                    androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(start = 72.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }
            }
        }
    }
}

// ─── Privacy Card ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StoryPrivacyCard(
    draft: StoryComposerDraft,
    onDraftChange: (StoryComposerDraft) -> Unit,
    onShowAudiencePicker: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 2
                ) {
                    StoryPrivacyUi.entries.forEach { option ->
                        val isSelected = draft.privacy == option
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                                    else MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onDraftChange(draft.copy(privacy = option)) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when (option) {
                                        StoryPrivacyUi.EVERYONE -> Icons.Rounded.Public
                                        StoryPrivacyUi.CONTACTS -> Icons.Rounded.PeopleAlt
                                        StoryPrivacyUi.CLOSE_FRIENDS -> Icons.Rounded.Favorite
                                        StoryPrivacyUi.SELECTED_USERS -> Icons.Rounded.Person
                                    },
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
                                           else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = when (option) {
                                        StoryPrivacyUi.EVERYONE -> stringResource(R.string.story_privacy_everyone)
                                        StoryPrivacyUi.CONTACTS -> stringResource(R.string.story_privacy_contacts)
                                        StoryPrivacyUi.CLOSE_FRIENDS -> stringResource(R.string.story_privacy_close_friends)
                                        StoryPrivacyUi.SELECTED_USERS -> stringResource(R.string.story_privacy_selected_users)
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
                                            else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // Audience filter row
        AnimatedVisibility(
            visible = draft.privacy != StoryPrivacyUi.CLOSE_FRIENDS,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            val isShowTo = draft.privacy == StoryPrivacyUi.SELECTED_USERS
            val selectedCount = if (isShowTo) draft.selectedUserIds.size else draft.exceptUserIds.size
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                onClick = { onShowAudiencePicker(isShowTo) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (isShowTo) Icons.Rounded.PeopleAlt else Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = if (isShowTo) stringResource(R.string.story_privacy_show_to)
                                   else stringResource(R.string.story_privacy_hide_from),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = when {
                                isShowTo && selectedCount == 0 -> stringResource(R.string.story_privacy_show_to_empty)
                                !isShowTo && selectedCount == 0 -> stringResource(R.string.story_privacy_hide_from_empty)
                                else -> "$selectedCount user(s) selected"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ─── Duration Card ────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StoryDurationCard(selected: Int, onSelect: (Int) -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        FlowRow(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 2
        ) {
            durationOptions.forEach { (seconds, label) ->
                val isSelected = selected == seconds
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(seconds) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
                                    else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

// ─── Settings Card ────────────────────────────────────────────────────────────

@Composable
private fun StorySettingsCard(
    keepOnProfile: Boolean,
    protectContent: Boolean,
    onKeepOnProfileChange: (Boolean) -> Unit,
    onProtectContentChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            onClick = { onKeepOnProfileChange(!keepOnProfile) }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.story_keep_on_profile), style = MaterialTheme.typography.bodyLarge)
                    Text(stringResource(R.string.story_keep_on_profile_subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = keepOnProfile, onCheckedChange = onKeepOnProfileChange)
            }
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            onClick = { onProtectContentChange(!protectContent) }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Rounded.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.story_protect_content), style = MaterialTheme.typography.bodyLarge)
                    Text(stringResource(R.string.story_protect_content_subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = protectContent, onCheckedChange = onProtectContentChange)
            }
        }
    }
}

// ─── Preview Summary Card ─────────────────────────────────────────────────────

@Composable
private fun StoryPreviewSummaryCard(draft: StoryComposerDraft) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryRow(Icons.Rounded.Image, "${draft.mediaItems.size} item(s)", draft.mediaItems.joinToString(", ") { it.mediaType.name.lowercase() })
            SummaryRow(
                when (draft.privacy) {
                    StoryPrivacyUi.EVERYONE -> Icons.Rounded.Public
                    StoryPrivacyUi.CONTACTS -> Icons.Rounded.PeopleAlt
                    StoryPrivacyUi.CLOSE_FRIENDS -> Icons.Rounded.Favorite
                    StoryPrivacyUi.SELECTED_USERS -> Icons.Rounded.Person
                },
                stringResource(R.string.story_privacy_label),
                when (draft.privacy) {
                    StoryPrivacyUi.EVERYONE -> stringResource(R.string.story_privacy_everyone)
                    StoryPrivacyUi.CONTACTS -> stringResource(R.string.story_privacy_contacts)
                    StoryPrivacyUi.CLOSE_FRIENDS -> stringResource(R.string.story_privacy_close_friends)
                    StoryPrivacyUi.SELECTED_USERS -> "${draft.selectedUserIds.size} users"
                }
            )
            SummaryRow(
                Icons.Rounded.Schedule,
                stringResource(R.string.story_duration_label),
                durationOptions.find { it.first == draft.activePeriodSeconds }?.second ?: "${draft.activePeriodSeconds / 3600}h"
            )
        }
    }
}

@Composable
private fun SummaryRow(icon: ImageVector, title: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ─── Audience Picker Sheet ────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StoryAudiencePickerSheet(
    isShowTo: Boolean,
    contacts: List<UserModel>,
    searchQuery: String,
    isLoading: Boolean,
    selectedIds: List<Long>,
    onSearchChange: (String) -> Unit,
    onToggleUser: (Long) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    val filtered = remember(contacts, searchQuery) {
        if (searchQuery.isBlank()) contacts
        else contacts.filter { u ->
            val q = searchQuery.lowercase()
            u.firstName.lowercase().contains(q) ||
            u.lastName?.lowercase()?.contains(q) == true ||
            u.username?.lowercase()?.contains(q) == true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (isShowTo) stringResource(R.string.story_audience_picker_show_to_title)
                           else stringResource(R.string.story_audience_picker_hide_from_title),
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = if (isShowTo) stringResource(R.string.story_audience_picker_show_to_subtitle)
                           else stringResource(R.string.story_audience_picker_hide_from_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onDismiss) { Text("Done") }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text(stringResource(R.string.story_audience_picker_search)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            shape = RoundedCornerShape(16.dp)
        )

        if (selectedIds.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${selectedIds.size} selected", style = MaterialTheme.typography.titleSmall)
                TextButton(onClick = onClear) { Text(stringResource(R.string.story_privacy_clear_selection)) }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                selectedIds.forEach { id ->
                    val user = contacts.find { it.id == id }
                    FilterChip(
                        selected = true,
                        onClick = { onToggleUser(id) },
                        label = { Text(user?.firstName ?: id.toString()) },
                        trailingIcon = { Icon(Icons.Rounded.Close, contentDescription = null) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        border = null
                    )
                }
            }
        }

        Box(
            modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 400.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> CircularProgressIndicator()
                filtered.isEmpty() -> Text(
                    if (searchQuery.isBlank()) stringResource(R.string.story_audience_picker_empty)
                    else stringResource(R.string.story_audience_picker_no_results),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                else -> LazyColumn(Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 4.dp)) {
                    items(filtered, key = { it.id }) { user ->
                        val isSelected = user.id in selectedIds
                        ListItem(
                            headlineContent = {
                                Text(
                                    "${user.firstName} ${user.lastName ?: ""}".trim(),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            },
                            supportingContent = {
                                user.username?.takeIf { it.isNotBlank() }?.let {
                                    Text("@$it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            leadingContent = {
                                Avatar(
                                    path = user.avatarPath,
                                    fallbackPath = user.personalAvatarPath,
                                    name = user.firstName,
                                    size = 40.dp
                                )
                            },
                            trailingContent = {
                                Checkbox(checked = isSelected, onCheckedChange = { onToggleUser(user.id) })
                            },
                            modifier = Modifier.clickable { onToggleUser(user.id) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }
        }
    }
}

// ─── Section Header ───────────────────────────────────────────────────────────

@Composable
private fun StorySectionHeader(title: String, subtitle: String?) {
    Column(
        modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (title.isNotBlank()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

private suspend fun copyUriToTemp(context: Context, uri: Uri, isVideo: Boolean): String? =
    withContext(Dispatchers.IO) {
        runCatching {
            val ext = if (isVideo) "mp4" else "jpg"
            val file = File(context.cacheDir, "story_${System.nanoTime()}.$ext")
            context.contentResolver.openInputStream(uri)?.use { i ->
                FileOutputStream(file).use { o -> i.copyTo(o) }
            } ?: return@withContext null
            file.absolutePath
        }.getOrNull()
    }

private suspend fun getVideoDuration(path: String): Int? = withContext(Dispatchers.IO) {
    val r = MediaMetadataRetriever()
    runCatching {
        r.setDataSource(path)
        r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.let { (it / 1000).toInt() }
    }.also { r.release() }.getOrNull()
}

private suspend fun getVideoThumbnail(path: String): Bitmap? = withContext(Dispatchers.IO) {
    val r = MediaMetadataRetriever()
    runCatching { r.setDataSource(path); r.getFrameAtTime(0) }
        .also { r.release() }.getOrNull()
}
