package com.spmods.spgram.presentation.features.profile.components

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image as ImageIcon
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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

/** Maximum duration TDLib accepts for a story video, matching Telegram's own limit. */
private const val MAX_STORY_VIDEO_SECONDS = 60

private data class DurationOption(val label: String, val seconds: Int)

private val durationOptions = listOf(
    DurationOption("6 hours", 6 * 3600),
    DurationOption("12 hours", 12 * 3600),
    DurationOption("24 hours", 24 * 3600),
    DurationOption("48 hours", 48 * 3600)
)

private data class PrivacyOption(val label: String, val value: StoryPrivacy)

private val privacyOptions = listOf(
    PrivacyOption("Everyone", StoryPrivacy.EVERYONE),
    PrivacyOption("Contacts", StoryPrivacy.CONTACTS),
    PrivacyOption("Close Friends", StoryPrivacy.CLOSE_FRIENDS)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryPosterSheet(
    chatId: Long,
    onPosted: (StoryModel) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val storyRepository: StoryRepository = koinInject()

    var caption by remember { mutableStateOf("") }
    var selectedPath by remember { mutableStateOf<String?>(null) }
    var selectedIsVideo by remember { mutableStateOf(false) }
    var videoThumbnail by remember { mutableStateOf<Bitmap?>(null) }
    var isPreparingMedia by remember { mutableStateOf(false) }
    var isPosting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedDuration by remember { mutableStateOf(durationOptions[2]) } // 24h default
    var selectedPrivacy by remember { mutableStateOf(privacyOptions[0]) } // Everyone default

    /**
     * Copies a picked content:// URI into an app-private cache file and returns its
     * absolute path. Querying the legacy "_data" column (the old approach) silently
     * fails on Android 10+ for most providers under scoped storage, which made
     * picking media here unreliable — copying the bytes directly works on every
     * Android version and provider, with no dependency on column availability.
     */
    suspend fun copyUriToCache(uri: Uri, isVideo: Boolean): String? = withContext(Dispatchers.IO) {
        try {
            val extension = if (isVideo) "mp4" else "jpg"
            val file = File(context.cacheDir, "story_${System.nanoTime()}.$extension")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output -> input.copyTo(output) }
            } ?: return@withContext null
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    /** Returns the video's duration in whole seconds, or null if it couldn't be read. */
    suspend fun videoDurationSeconds(path: String): Int? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(path)
            val ms = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            ms?.let { (it / 1000).toInt() }
        } catch (e: Exception) {
            null
        } finally {
            retriever.release()
        }
    }

    suspend fun videoFrameThumbnail(path: String): Bitmap? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(path)
            retriever.getFrameAtTime(0)
        } catch (e: Exception) {
            null
        } finally {
            retriever.release()
        }
    }

    fun handlePickedMedia(uri: Uri, isVideo: Boolean) {
        scope.launch {
            isPreparingMedia = true
            errorMessage = null
            selectedPath = null
            videoThumbnail = null
            val path = copyUriToCache(uri, isVideo)
            if (path == null) {
                errorMessage = "Couldn't read the selected file. Please try again."
                isPreparingMedia = false
                return@launch
            }
            if (isVideo) {
                val duration = videoDurationSeconds(path)
                if (duration != null && duration > MAX_STORY_VIDEO_SECONDS) {
                    errorMessage = "Videos for stories can be at most $MAX_STORY_VIDEO_SECONDS seconds long " +
                        "(this one is ${duration}s). Trim it and try again."
                    File(path).delete()
                    isPreparingMedia = false
                    return@launch
                }
                videoThumbnail = videoFrameThumbnail(path)
            }
            selectedPath = path
            selectedIsVideo = isVideo
            isPreparingMedia = false
        }
    }

    val photoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { handlePickedMedia(it, isVideo = false) } }

    val videoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { handlePickedMedia(it, isVideo = true) } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Post a Story",
                style = MaterialTheme.typography.titleMedium
            )

            // Media picker buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = { photoLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f),
                    enabled = !isPreparingMedia && !isPosting
                ) {
                    Icon(
                        imageVector = ImageIcon,
                        contentDescription = null,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(end = 4.dp)
                    )
                    Text("Photo")
                }
                OutlinedButton(
                    onClick = { videoLauncher.launch("video/*") },
                    modifier = Modifier.weight(1f),
                    enabled = !isPreparingMedia && !isPosting
                ) {
                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = null,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(end = 4.dp)
                    )
                    Text("Video")
                }
            }

            // Media preview — shows what's actually about to be posted, instead of
            // just a text confirmation, so the user can see they picked the right file.
            if (isPreparingMedia) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(9f / 16f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (selectedPath != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(9f / 16f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black)
                ) {
                    if (selectedIsVideo) {
                        videoThumbnail?.let { bmp ->
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxWidth(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(48.dp)
                        )
                    } else {
                        SubcomposeAsyncImage(
                            model = selectedPath,
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            OutlinedTextField(
                value = caption,
                onValueChange = { caption = it },
                label = { Text("Caption (optional)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            // Privacy selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Who can see this story",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    privacyOptions.forEach { option ->
                        SelectableChip(
                            label = option.label,
                            selected = option == selectedPrivacy,
                            onClick = { selectedPrivacy = option }
                        )
                    }
                }
            }

            // Duration selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Visible for",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    durationOptions.forEach { option ->
                        SelectableChip(
                            label = option.label,
                            selected = option == selectedDuration,
                            onClick = { selectedDuration = option }
                        )
                    }
                }
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = {
                    val path = selectedPath ?: return@Button
                    scope.launch {
                        isPosting = true
                        errorMessage = null
                        try {
                            val result = if (selectedIsVideo) {
                                storyRepository.postVideoStory(
                                    chatId = chatId,
                                    videoPath = path,
                                    caption = caption.trim(),
                                    activePeriodSeconds = selectedDuration.seconds,
                                    privacy = selectedPrivacy.value
                                )
                            } else {
                                storyRepository.postPhotoStory(
                                    chatId = chatId,
                                    photoPath = path,
                                    caption = caption.trim(),
                                    activePeriodSeconds = selectedDuration.seconds,
                                    privacy = selectedPrivacy.value
                                )
                            }
                            if (result != null) {
                                onPosted(result)
                            } else {
                                errorMessage = "Failed to post story. Try again."
                            }
                        } catch (e: Exception) {
                            errorMessage = e.message ?: "Unknown error"
                        } finally {
                            isPosting = false
                        }
                    }
                },
                enabled = selectedPath != null && !isPosting && !isPreparingMedia,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isPosting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Post Story")
                }
            }

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isPosting
            ) {
                Text("Cancel")
            }
        }
    }
}

@Composable
private fun SelectableChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .border(
                width = if (selected) 0.dp else 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = contentColor,
            style = MaterialTheme.typography.labelMedium
        )
    }
}
