package com.spmods.spgram.presentation.features.download

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spmods.spgram.core.date.toDate
import com.spmods.spgram.domain.models.DownloadedFileModel
import com.spmods.spgram.domain.models.DownloadsFilter
import com.spmods.spgram.domain.models.FileDownloadEvent
import com.spmods.spgram.domain.models.MessageContent
import com.spmods.spgram.domain.repository.FileRepository
import com.spmods.spgram.presentation.core.ui.AvatarForChat
import com.spmods.spgram.presentation.core.util.DateFormatManager
import com.spmods.spgram.presentation.core.util.IDownloadUtils
import com.spmods.spgram.presentation.core.util.toShortRelativeDate
import com.spmods.spgram.presentation.features.chats.conversation.ui.message.formatDuration
import com.spmods.spgram.presentation.features.chats.conversation.ui.message.formatFileSize
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.util.Calendar
import java.util.Date

private data class DownloadTabSpec(val filter: DownloadsFilter, val label: String, val icon: ImageVector)

private val downloadTabs = listOf(
    DownloadTabSpec(DownloadsFilter.ALL, "All", Icons.Rounded.Download),
    DownloadTabSpec(DownloadsFilter.PHOTOS, "Photos", Icons.Rounded.Image),
    DownloadTabSpec(DownloadsFilter.VIDEOS, "Videos", Icons.Rounded.Movie),
    DownloadTabSpec(DownloadsFilter.FILES, "Files", Icons.AutoMirrored.Rounded.InsertDriveFile),
    DownloadTabSpec(DownloadsFilter.MUSIC, "Music", Icons.Rounded.Audiotrack),
    DownloadTabSpec(DownloadsFilter.VOICE, "Voice", Icons.Rounded.Mic),
)

/** Normalized payload used to patch a DownloadedFileModel entry when a fileDownloadFlow event arrives. */
private data class FileDownloadPatch(
    val fileId: Int,
    val progress: Float,
    val isDownloading: Boolean,
    val completedPath: String?
)

/** The local file path for whichever downloadable MessageContent subtype this is, if any. */
private fun MessageContent.localPathOrNull(): String? = when (this) {
    is MessageContent.Photo -> path
    is MessageContent.Video -> path
    is MessageContent.VideoNote -> path
    is MessageContent.Gif -> path
    is MessageContent.Document -> path
    is MessageContent.Audio -> path
    is MessageContent.Voice -> path
    else -> null
}

/** Patches the live download progress/active-state onto whichever MessageContent subtype this is. */
private fun MessageContent.withDownloadProgress(progress: Float, isDownloading: Boolean): MessageContent =
    when (this) {
        is MessageContent.Photo -> copy(downloadProgress = progress, isDownloading = isDownloading)
        is MessageContent.Video -> copy(downloadProgress = progress, isDownloading = isDownloading)
        is MessageContent.VideoNote -> copy(downloadProgress = progress, isDownloading = isDownloading)
        is MessageContent.Gif -> copy(downloadProgress = progress, isDownloading = isDownloading)
        is MessageContent.Document -> copy(downloadProgress = progress, isDownloading = isDownloading)
        is MessageContent.Audio -> copy(downloadProgress = progress, isDownloading = isDownloading)
        is MessageContent.Voice -> copy(downloadProgress = progress, isDownloading = isDownloading)
        else -> this
    }

/**
 * Download tab — shows files that have been downloaded (or are downloading)
 * from any chat, grouped into All / Photos / Videos / Files / Music / Voice,
 * mirroring Telegram's own Downloads manager (backed by TDLib's real
 * SearchFileDownloads API). Items are grouped under relative date headers
 * (Today / Yesterday / This Week / Earlier) so the list reads as a timeline
 * rather than a flat dump of files.
 *
 * Pause/resume/cancel act on the real TDLib download (the same global state
 * the in-chat bubble shows), opening a completed file hands off to the
 * system viewer, and "View in chat" jumps straight to the source message.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadContent(
    onNavigateToChat: (chatId: Long, messageId: Long) -> Unit = { _, _ -> }
) {
    val fileRepository: FileRepository = koinInject()
    val downloadUtils: IDownloadUtils = koinInject()
    val timeFormat = koinInject<DateFormatManager>().getHourMinuteFormat()
    val coroutineScope = rememberCoroutineScope()

    var selectedTabIndex by remember { mutableStateOf(0) }
    val itemsByFilter = remember { mutableMapOf<DownloadsFilter, List<DownloadedFileModel>>() }
    var currentItems by remember { mutableStateOf<List<DownloadedFileModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val selectedFilter = downloadTabs[selectedTabIndex].filter

    // Keep a stable reference to the latest currentItems/itemsByFilter for the
    // long-lived flow collector below, without restarting it on every list change.
    val currentItemsState = rememberUpdatedState(currentItems)

    LaunchedEffect(selectedFilter) {
        val cached = itemsByFilter[selectedFilter]
        if (cached != null) {
            currentItems = cached
            isLoading = false
        } else {
            isLoading = true
        }
        val result = fileRepository.searchFileDownloads(filter = selectedFilter, limit = 100)
        itemsByFilter[selectedFilter] = result.files
        currentItems = result.files
        isLoading = false
    }

    // Live-sync progress/pause/completion across chat <-> downloads page: both
    // surfaces observe the same underlying TDLib file state via this shared flow.
    LaunchedEffect(Unit) {
        fileRepository.fileDownloadFlow.collect { event ->
            val patch = when (event) {
                is FileDownloadEvent.Progress -> FileDownloadPatch(event.fileId, event.progress, event.progress < 1f, null)
                is FileDownloadEvent.Completed -> FileDownloadPatch(event.fileId, 1f, false, event.path)
            }
            val items = currentItemsState.value
            if (items.none { it.fileId == patch.fileId }) return@collect

            val updated = items.map { entry ->
                if (entry.fileId != patch.fileId) return@map entry
                val patchedContent = entry.message.content.withDownloadProgress(patch.progress, patch.isDownloading)
                entry.copy(
                    message = entry.message.copy(content = patchedContent),
                    isPaused = if (patch.isDownloading) false else entry.isPaused,
                    completeDate = if (patch.completedPath != null) (System.currentTimeMillis() / 1000).toInt() else entry.completeDate
                )
            }
            currentItems = updated
            itemsByFilter[selectedFilter] = updated
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Downloads",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                modifier = Modifier.statusBarsPadding()
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
        ) {
            DownloadTabRow(
                selectedTabIndex = selectedTabIndex,
                onTabSelected = { selectedTabIndex = it }
            )

            AnimatedContent(
                targetState = Triple(isLoading, currentItems.isEmpty(), selectedFilter),
                transitionSpec = {
                    fadeIn(tween(180, easing = LinearOutSlowInEasing)) togetherWith
                        fadeOut(tween(120))
                },
                label = "DownloadListState"
            ) { (loading, empty, filter) ->
                when {
                    loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    empty -> {
                        DownloadsEmptyState(filter = filter)
                    }
                    else -> {
                        val grouped = remember(currentItems) { groupDownloadsByDate(currentItems) }
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 80.dp, top = 4.dp)
                        ) {
                            grouped.forEach { (sectionLabel, entries) ->
                                item(key = "header_$sectionLabel") {
                                    DownloadSectionHeader(sectionLabel)
                                }
                                items(entries, key = { it.fileId }) { entry ->
                                    DownloadedFileRow(
                                        entry = entry,
                                        timeFormat = timeFormat,
                                        onOpen = {
                                            val path = entry.message.content.localPathOrNull()
                                            if (!path.isNullOrBlank()) {
                                                downloadUtils.openFile(path)
                                            }
                                        },
                                        onViewInChat = {
                                            onNavigateToChat(entry.message.chatId, entry.message.id)
                                        },
                                        onPauseToggle = {
                                            val newPaused = !entry.isPaused
                                            val updated = currentItems.map {
                                                if (it.fileId == entry.fileId) it.copy(isPaused = newPaused) else it
                                            }
                                            currentItems = updated
                                            itemsByFilter[selectedFilter] = updated
                                            coroutineScope.launch {
                                                fileRepository.toggleDownloadIsPaused(entry.fileId, newPaused)
                                            }
                                        },
                                        onCancel = {
                                            val updated = currentItems.filterNot { it.fileId == entry.fileId }
                                            currentItems = updated
                                            itemsByFilter[selectedFilter] = updated
                                            coroutineScope.launch {
                                                fileRepository.cancelDownloadFile(entry.fileId)
                                            }
                                        },
                                        onRemove = {
                                            val updated = currentItems.filterNot { it.fileId == entry.fileId }
                                            currentItems = updated
                                            itemsByFilter[selectedFilter] = updated
                                            coroutineScope.launch {
                                                fileRepository.removeFileFromDownloads(entry.fileId)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadTabRow(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
    ) {
        PrimaryScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            edgePadding = 4.dp,
            divider = {},
            indicator = {
                Box(
                    Modifier
                        .tabIndicatorOffset(selectedTabIndex)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        ) {
            downloadTabs.forEachIndexed { index, spec ->
                val selected = selectedTabIndex == index
                Tab(
                    selected = selected,
                    onClick = { onTabSelected(index) },
                    modifier = Modifier
                        .height(40.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    selectedContentColor = MaterialTheme.colorScheme.onPrimary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = spec.icon,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = spec.label,
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun DownloadsEmptyState(filter: DownloadsFilter) {
    val spec = downloadTabs.first { it.filter == filter }
    val message = when (filter) {
        DownloadsFilter.ALL -> "Files you download from any chat will show up here"
        DownloadsFilter.PHOTOS -> "Photos you download will show up here"
        DownloadsFilter.VIDEOS -> "Videos you download will show up here"
        DownloadsFilter.FILES -> "Documents you download will show up here"
        DownloadsFilter.MUSIC -> "Music you download will show up here"
        DownloadsFilter.VOICE -> "Voice messages you download will show up here"
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = spec.icon,
                    contentDescription = null,
                    modifier = Modifier.size(30.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
            Text(
                text = "No ${spec.label.lowercase()} yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun DownloadSectionHeader(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 6.dp)
    )
}

/** Visual identity (icon + tint) for a piece of downloaded content. */
private data class FileTypeStyle(
    val icon: ImageVector,
    val tint: Color,
    val container: Color
)

@Composable
private fun fileTypeStyle(content: MessageContent): FileTypeStyle = when (content) {
    is MessageContent.Photo -> FileTypeStyle(
        icon = Icons.Rounded.Image,
        tint = Color(0xFF2EA6FF),
        container = Color(0xFF2EA6FF).copy(alpha = 0.14f)
    )
    is MessageContent.Video -> FileTypeStyle(
        icon = Icons.Rounded.Movie,
        tint = Color(0xFFFF5C7A),
        container = Color(0xFFFF5C7A).copy(alpha = 0.14f)
    )
    is MessageContent.VideoNote -> FileTypeStyle(
        icon = Icons.Rounded.Movie,
        tint = Color(0xFFFF5C7A),
        container = Color(0xFFFF5C7A).copy(alpha = 0.14f)
    )
    is MessageContent.Gif -> FileTypeStyle(
        icon = Icons.Rounded.Movie,
        tint = Color(0xFFB266FF),
        container = Color(0xFFB266FF).copy(alpha = 0.14f)
    )
    is MessageContent.Document -> FileTypeStyle(
        icon = Icons.AutoMirrored.Rounded.InsertDriveFile,
        tint = Color(0xFF4C8DFF),
        container = Color(0xFF4C8DFF).copy(alpha = 0.14f)
    )
    is MessageContent.Audio -> FileTypeStyle(
        icon = Icons.Rounded.Audiotrack,
        tint = Color(0xFFFFA640),
        container = Color(0xFFFFA640).copy(alpha = 0.14f)
    )
    is MessageContent.Voice -> FileTypeStyle(
        icon = Icons.Rounded.Mic,
        tint = Color(0xFF35C77E),
        container = Color(0xFF35C77E).copy(alpha = 0.14f)
    )
    else -> FileTypeStyle(
        icon = Icons.AutoMirrored.Rounded.InsertDriveFile,
        tint = MaterialTheme.colorScheme.primary,
        container = MaterialTheme.colorScheme.primaryContainer
    )
}

@Composable
private fun DownloadedFileRow(
    entry: DownloadedFileModel,
    timeFormat: String,
    onOpen: () -> Unit,
    onViewInChat: () -> Unit,
    onPauseToggle: () -> Unit,
    onCancel: () -> Unit,
    onRemove: () -> Unit
) {
    val message = entry.message
    var showMenu by remember { mutableStateOf(false) }

    val (title, subtitle, isDownloading, downloadProgress) = remember(message.content, entry.isPaused, entry.isCompleted) {
        describeContent(message.content)
    }
    val style = fileTypeStyle(message.content)

    val dateText = remember(entry.addDate) {
        entry.addDate.toDate().toShortRelativeDate(timeFormat)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = entry.isCompleted, onClick = onOpen)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(46.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isDownloading && !entry.isPaused) {
                CircularProgressIndicator(
                    progress = { downloadProgress.coerceIn(0f, 1f) },
                    modifier = Modifier.size(46.dp),
                    color = style.tint,
                    trackColor = style.container,
                    strokeWidth = 2.5.dp
                )
            }
            Surface(
                shape = CircleShape,
                color = style.container,
                modifier = Modifier.size(if (isDownloading && !entry.isPaused) 36.dp else 44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = style.icon,
                        contentDescription = null,
                        tint = style.tint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarForChat(
                    path = message.senderAvatar,
                    fallbackPath = message.senderPersonalAvatar,
                    name = message.senderName,
                    size = 16.dp,
                    fontSize = 9
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = buildString {
                        append(message.senderName)
                        append(" • ")
                        append(subtitle)
                        if (isDownloading && !entry.isCompleted) {
                            append(" • ")
                            append("${(downloadProgress * 100).toInt()}%")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDownloading) style.tint else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isDownloading) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        if (!entry.isCompleted) {
            IconButton(onClick = onPauseToggle) {
                Icon(
                    imageVector = if (entry.isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                    contentDescription = if (entry.isPaused) "Resume" else "Pause",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            Text(
                text = dateText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "More options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                if (!entry.isCompleted) {
                    DropdownMenuItem(
                        text = { Text("Cancel download") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Rounded.Cancel, contentDescription = null)
                        },
                        onClick = {
                            showMenu = false
                            onCancel()
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("View in chat") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Rounded.ChatBubbleOutline, contentDescription = null)
                    },
                    onClick = {
                        showMenu = false
                        onViewInChat()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Rounded.Delete, contentDescription = null)
                    },
                    onClick = {
                        showMenu = false
                        onRemove()
                    }
                )
            }
        }
    }
}

/** title, subtitle, isDownloading, downloadProgress (0f..1f) */
private data class ContentDescription(
    val title: String,
    val subtitle: String,
    val isDownloading: Boolean,
    val downloadProgress: Float
)

private fun describeContent(content: MessageContent): ContentDescription =
    when (content) {
        is MessageContent.Photo -> ContentDescription(
            "Photo", "Image", content.isDownloading, content.downloadProgress
        )
        is MessageContent.Video -> ContentDescription(
            "Video", formatDuration(content.duration), content.isDownloading, content.downloadProgress
        )
        is MessageContent.VideoNote -> ContentDescription(
            "Video message", formatDuration(content.duration), content.isDownloading, content.downloadProgress
        )
        is MessageContent.Gif -> ContentDescription(
            "GIF", "Animation", content.isDownloading, content.downloadProgress
        )
        is MessageContent.Document -> ContentDescription(
            content.fileName.ifBlank { "Document" },
            formatFileSize(content.size, content.isDownloading, content.downloadProgress),
            content.isDownloading,
            content.downloadProgress
        )
        is MessageContent.Audio -> ContentDescription(
            content.title.ifBlank { content.fileName.ifBlank { "Audio" } },
            content.performer.ifBlank { formatDuration(content.duration) },
            content.isDownloading,
            content.downloadProgress
        )
        is MessageContent.Voice -> ContentDescription(
            "Voice message", formatDuration(content.duration), content.isDownloading, content.downloadProgress
        )
        else -> ContentDescription("File", "", false, 0f)
    }

/** Groups downloads into Today / Yesterday / This Week / Earlier sections, preserving original order within each. */
private fun groupDownloadsByDate(items: List<DownloadedFileModel>): List<Pair<String, List<DownloadedFileModel>>> {
    val now = Calendar.getInstance()
    val todayStart = (now.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    val yesterdayStart = (todayStart.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
    val weekStart = (todayStart.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -7) }

    fun label(epochSeconds: Int): String {
        val date = Date(epochSeconds.toLong() * 1000)
        return when {
            date.after(todayStart.time) -> "Today"
            date.after(yesterdayStart.time) -> "Yesterday"
            date.after(weekStart.time) -> "This Week"
            else -> "Earlier"
        }
    }

    val order = listOf("Today", "Yesterday", "This Week", "Earlier")
    val buckets = items.groupBy { label(it.addDate.takeIf { d -> d != 0 } ?: 0) }
    return order.mapNotNull { key -> buckets[key]?.let { key to it } }
}
