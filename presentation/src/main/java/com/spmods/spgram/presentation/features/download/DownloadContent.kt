package com.spmods.spgram.presentation.features.download

import androidx.compose.foundation.background
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spmods.spgram.core.date.toDate
import com.spmods.spgram.domain.models.DownloadedFileModel
import com.spmods.spgram.domain.models.DownloadsFilter
import com.spmods.spgram.domain.models.MessageContent
import com.spmods.spgram.domain.repository.FileRepository
import com.spmods.spgram.presentation.core.ui.AvatarForChat
import com.spmods.spgram.presentation.core.util.DateFormatManager
import com.spmods.spgram.presentation.core.util.toShortRelativeDate
import com.spmods.spgram.presentation.features.chats.conversation.ui.message.formatDuration
import com.spmods.spgram.presentation.features.chats.conversation.ui.message.formatFileSize
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private data class DownloadTabSpec(val filter: DownloadsFilter, val label: String)

private val downloadTabs = listOf(
    DownloadTabSpec(DownloadsFilter.ALL, "All"),
    DownloadTabSpec(DownloadsFilter.PHOTOS, "Photos"),
    DownloadTabSpec(DownloadsFilter.VIDEOS, "Videos"),
    DownloadTabSpec(DownloadsFilter.FILES, "Files"),
    DownloadTabSpec(DownloadsFilter.MUSIC, "Music"),
    DownloadTabSpec(DownloadsFilter.VOICE, "Voice"),
)

/**
 * Download tab — shows files that have been downloaded (or are downloading)
 * from any chat, grouped into All / Photos / Videos / Files / Music / Voice,
 * mirroring Telegram's own Downloads manager (backed by TDLib's real
 * SearchFileDownloads API).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadContent() {
    val fileRepository: FileRepository = koinInject()
    val timeFormat = koinInject<DateFormatManager>().getHourMinuteFormat()
    val coroutineScope = rememberCoroutineScope()

    var selectedTabIndex by remember { mutableStateOf(0) }
    val itemsByFilter = remember { mutableMapOf<DownloadsFilter, List<DownloadedFileModel>>() }
    var currentItems by remember { mutableStateOf<List<DownloadedFileModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val selectedFilter = downloadTabs[selectedTabIndex].filter

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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
            ) {
                PrimaryScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    edgePadding = 4.dp,
                    divider = {},
                    indicator = {
                        Box(
                            Modifier
                                .tabIndicatorOffset(selectedTabIndex)
                                .fillMaxSize()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                ) {
                    downloadTabs.forEachIndexed { index, spec ->
                        val selected = selectedTabIndex == index
                        Tab(
                            selected = selected,
                            onClick = { selectedTabIndex = index },
                            modifier = Modifier
                                .height(44.dp)
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(24.dp)),
                            selectedContentColor = MaterialTheme.colorScheme.onPrimary,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            text = {
                                Text(
                                    text = spec.label,
                                    fontSize = 14.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }
            }

            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                currentItems.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Text(
                                text = "No downloaded files yet",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(currentItems, key = { it.fileId }) { entry ->
                            DownloadedFileRow(
                                entry = entry,
                                timeFormat = timeFormat,
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

@Composable
private fun DownloadedFileRow(
    entry: DownloadedFileModel,
    timeFormat: String,
    onPauseToggle: () -> Unit,
    onRemove: () -> Unit
) {
    val message = entry.message
    var showMenu by remember { mutableStateOf(false) }

    val (icon, title, subtitle) = remember(message.content, entry.isPaused, entry.isCompleted) {
        describeContent(message.content)
    }

    val dateText = remember(entry.addDate) {
        entry.addDate.toDate().toShortRelativeDate(timeFormat)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp)
                )
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
                    text = "${message.senderName} • $subtitle",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Remove from Downloads") },
                        onClick = {
                            showMenu = false
                            onRemove()
                        }
                    )
                }
            }
        }
    }
}

private fun describeContent(content: MessageContent): Triple<androidx.compose.ui.graphics.vector.ImageVector, String, String> =
    when (content) {
        is MessageContent.Photo -> Triple(Icons.Rounded.Image, "Photo", "Image")
        is MessageContent.Video -> Triple(Icons.Rounded.Movie, "Video", formatDuration(content.duration))
        is MessageContent.VideoNote -> Triple(Icons.Rounded.Movie, "Video message", formatDuration(content.duration))
        is MessageContent.Gif -> Triple(Icons.Rounded.Movie, "GIF", "Animation")
        is MessageContent.Document -> Triple(
            Icons.AutoMirrored.Rounded.InsertDriveFile,
            content.fileName.ifBlank { "Document" },
            formatFileSize(content.size, isDownloading = false, downloadProgress = 0f)
        )
        is MessageContent.Audio -> Triple(
            Icons.Rounded.Audiotrack,
            content.title.ifBlank { content.fileName.ifBlank { "Audio" } },
            content.performer.ifBlank { formatDuration(content.duration) }
        )
        is MessageContent.Voice -> Triple(Icons.Rounded.Mic, "Voice message", formatDuration(content.duration))
        else -> Triple(Icons.AutoMirrored.Rounded.InsertDriveFile, "File", "")
    }
