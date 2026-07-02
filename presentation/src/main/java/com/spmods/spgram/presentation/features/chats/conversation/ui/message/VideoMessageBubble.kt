package com.spmods.spgram.presentation.features.chats.conversation.ui.message

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.DpSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.rounded.Stream
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.spmods.spgram.presentation.ui.theme.LocalDarkTheme
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.spmods.spgram.domain.models.ForwardInfo
import com.spmods.spgram.domain.models.MessageContent
import com.spmods.spgram.domain.models.MessageModel
import com.spmods.spgram.presentation.R
import com.spmods.spgram.presentation.core.media.VideoStickerPlayer
import com.spmods.spgram.presentation.core.media.VideoType
import com.spmods.spgram.presentation.core.util.IDownloadUtils
import com.spmods.spgram.presentation.core.util.namespacedCacheKey
import com.spmods.spgram.presentation.features.chats.conversation.AutoDownloadSuppression

private class VideoBubbleLayoutTracker {
    var videoPosition: Offset = Offset.Zero
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VideoMessageBubble(
    content: MessageContent.Video,
    msg: MessageModel,
    isOutgoing: Boolean,
    isSameSenderAbove: Boolean,
    isSameSenderBelow: Boolean,
    fontSize: Float,
    letterSpacing: Float,
    autoDownloadMobile: Boolean,
    autoDownloadWifi: Boolean,
    autoDownloadRoaming: Boolean,
    autoplayVideos: Boolean,
    onVideoClick: (MessageModel) -> Unit,
    onOpenViewOnce: (MessageModel) -> Unit = {},
    modifier: Modifier = Modifier,
    onCancelDownload: (Int) -> Unit = {},
    onLongClick: (Offset) -> Unit,
    onReplyClick: (MessageModel) -> Unit = {},
    onReactionClick: (String) -> Unit = {},
    showMetadata: Boolean = true,
    showReactions: Boolean = true,
    toProfile: (Long) -> Unit = {},
    onForwardOriginClick: (ForwardInfo) -> Unit = {},
    downloadUtils: IDownloadUtils,
    isAnyViewerOpen: Boolean = false
) {
    val cornerRadius = 18.dp
    val smallCorner = 6.dp
    val tailCorner = 0.dp

    val context = LocalContext.current
    // ✅ ROOT FIX: val (not remember+var) so displayPath is recomputed every recompose.
    // Full video path takes priority; thumbnailPath is fallback for the static preview.
    // This ensures the bubble shows the correct thumbnail immediately on re-entry to
    // the chat — no LaunchedEffect delay, no one-frame blank/tiny-bubble flash.
    val displayPath: String? = content.path?.takeIf { it.isNotBlank() }
        ?: content.thumbnailPath?.takeIf { it.isNotBlank() }
    val hasFullVideo = !content.path.isNullOrBlank()
    val hasPath = !displayPath.isNullOrBlank()
    val videoCacheKey = remember(displayPath, content.fileId) {
        namespacedCacheKey("chat_video:${content.fileId}", displayPath)
    }
    val videoMiniCacheKey = remember(content.minithumbnail, content.fileId) {
        content.minithumbnail?.let { namespacedCacheKey("chat_video_mini:${content.fileId}", it) }
    }
    var isAutoDownloadSuppressed by remember(msg.id, content.fileId) { mutableStateOf(false) }

    // Same fix as PhotoMessageBubble: compute exact bubble size from pixel dimensions
    // instead of using fillMaxWidth + aspectRatio. This guarantees the correct size
    // from the very first frame regardless of download state.
    val maxBubbleW = 260.dp
    val maxBubbleH = 320.dp
    val minBubbleW = 120.dp
    val minBubbleH = 120.dp

    val bubbleSize = remember(content.width, content.height) {
        val pw = content.width.takeIf { it > 0 } ?: 4
        val ph = content.height.takeIf { it > 0 } ?: 3
        val scaleW = maxBubbleW.value / pw
        val scaleH = maxBubbleH.value / ph
        val scale = minOf(scaleW, scaleH, 1f)
        val w = (pw * scale).coerceAtLeast(minBubbleW.value)
        val h = (ph * scale).coerceAtLeast(minBubbleH.value)
        DpSize(w.dp, h.dp)
    }

    LaunchedEffect(content.path, content.fileId) {
        if (!content.path.isNullOrBlank()) {
            isAutoDownloadSuppressed = false
            AutoDownloadSuppression.clear(content.fileId)
        }
    }

    LaunchedEffect(content.path, content.isDownloading, autoDownloadMobile, autoDownloadWifi, autoDownloadRoaming) {
        if (!content.isViewOnce && content.path.isNullOrBlank() && !content.isDownloading && !content.supportsStreaming && !isAutoDownloadSuppressed && !AutoDownloadSuppression.isSuppressed(
                content.fileId
            )
        ) {
            val shouldDownload = when {
                downloadUtils.isWifiConnected() -> autoDownloadWifi
                downloadUtils.isRoaming() -> autoDownloadRoaming
                else -> autoDownloadMobile
            }
            if (shouldDownload) {
                onVideoClick(msg)
            }
        }
    }

    val topStart = if (!isOutgoing && isSameSenderAbove) smallCorner else cornerRadius
    val topEnd = if (isOutgoing && isSameSenderAbove) smallCorner else cornerRadius
    val bottomStart = if (!isOutgoing) {
        if (isSameSenderBelow) smallCorner else tailCorner
    } else cornerRadius
    val bottomEnd = if (isOutgoing) {
        if (isSameSenderBelow) smallCorner else tailCorner
    } else cornerRadius

    val bubbleShape = RoundedCornerShape(
        topStart = topStart,
        topEnd = topEnd,
        bottomEnd = bottomEnd,
        bottomStart = bottomStart
    )

    val layoutTracker = remember { VideoBubbleLayoutTracker() }
    var isMuted by remember { mutableStateOf(true) }
    var isVisible by remember { mutableStateOf(false) }
    // Progressive playback: true when user taps play on a not-yet-fully-downloaded video.
    // This triggers streaming playback (download + play simultaneously) even when autoplay is off.
    var isProgressivePlayActive by remember(msg.id, content.fileId) { mutableStateOf(false) }

    // Reset progressive play if the video finishes downloading (full path available)
    // so the next time it plays from the local file normally.
    LaunchedEffect(content.path) {
        if (!content.path.isNullOrBlank()) {
            isProgressivePlayActive = false
        }
    }
    val resources = LocalResources.current
    val screenHeightPx = remember { resources.displayMetrics.heightPixels }
    val revealedSpoilers = remember { mutableStateListOf<Int>() }
    var isMediaSpoilerRevealed by remember { mutableStateOf(!content.hasSpoiler) }
    val currentPositionSecondsState = remember(msg.id, content.fileId) { mutableIntStateOf(0) }
    val currentPositionSeconds = currentPositionSecondsState.intValue
    val onLongClickState by rememberUpdatedState(onLongClick)
    val onVideoClickState by rememberUpdatedState(onVideoClick)
    val onCancelDownloadState by rememberUpdatedState(onCancelDownload)

    Column(
        modifier = modifier.onGloballyPositioned {
            val rect = it.boundsInWindow()
            isVisible = rect.bottom > 0 && rect.top < screenHeightPx
        },
        horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = bubbleShape,
            color = run { val d = LocalDarkTheme.current; if (isOutgoing) (if (d) Color(0xFF2B5278) else Color(0xFFEEFFDE)) else (if (d) Color(0xFF182533) else Color(0xFFFFFFFF)) },
            contentColor = if (LocalDarkTheme.current) Color(0xFFFFFFFF) else Color(0xFF212121),
        ) {
            Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                msg.forwardInfo?.let { forward ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(run { val d = LocalDarkTheme.current; if (isOutgoing) (if (d) Color(0xFF2B5278) else Color(0xFFEEFFDE)) else (if (d) Color(0xFF182533) else Color(0xFFFFFFFF)) })
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .zIndex(1f)
                    ) {
                        ForwardContent(forward, isOutgoing, onForwardClick = onForwardOriginClick)
                    }
                }
                msg.replyToMsg?.let { reply ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(run { val d = LocalDarkTheme.current; if (isOutgoing) (if (d) Color(0xFF2B5278) else Color(0xFFEEFFDE)) else (if (d) Color(0xFF182533) else Color(0xFFFFFFFF)) })
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                            .zIndex(1f)
                    ) {
                        ReplyContent(
                            replyToMsg = reply,
                            isOutgoing = isOutgoing,
                            onClick = { onReplyClick(reply) }
                        )
                    }
                }

                val boxModifier = if (content.isViewOnce && !content.isViewOnceOpened) {
    // ✅ Matches PhotoMessageBubble's view-once box exactly: fixed 260x260dp square, no padding.
    Modifier
        .size(260.dp, 260.dp)
        .clipToBounds()
        .onGloballyPositioned { layoutTracker.videoPosition = it.positionInWindow() }
} else {
    // 🔴 FIX: ස්ථිර size එකක් දෙනවා වෙනුවට width එක flex කළා
    Modifier
        .widthIn(min = bubbleSize.width) // අඩුම තරමේ වීඩියෝ එකේ ඔරිජිනල් පළල ගන්නවා
        .fillMaxWidth()                  // කැප්ෂන් එක දිග නම් ඒ පළලටම වීඩියෝ බොක්ස් එකත් ලොකු වෙනවා
        .height(bubbleSize.height)       // උස කලින් සෙට් කරපු ගාණම තියෙනවා
        .clipToBounds()
        .onGloballyPositioned { layoutTracker.videoPosition = it.positionInWindow() }
}


                Box(modifier = boxModifier) {
                    if (content.isViewOnce && !content.isViewOnceOpened) {
                        // ✅ Matches PhotoMessageBubble's view-once overlay exactly:
                        // blurred thumbnail bg, 45% scrim, center icon that swaps
                        // between download / progress-ring / flame depending on
                        // state, "tap to view" label at the bottom. Tap routing is
                        // unchanged — still handled by VideoInteractionOverlay /
                        // onStartDownload below, only what's drawn changed here.
                        VideoViewOnceOverlay(content = content)
                    } else if (hasFullVideo || content.supportsStreaming || isProgressivePlayActive) {
                            // Show video player when:
                            // 1. Full video is downloaded (hasFullVideo)
                            // 2. Video supports streaming (supportsStreaming)
                            // 3. User tapped play to start progressive playback (isProgressivePlayActive)
                            if (autoplayVideos || isProgressivePlayActive) {
                                val videoPath = when {
                                    !content.path.isNullOrBlank() -> content.path!!
                                    content.supportsStreaming || isProgressivePlayActive -> "http://streaming/${content.fileId}"
                                    else -> displayPath ?: "http://streaming/${content.fileId}"
                                }
                                VideoStickerPlayer(
                                    path = videoPath,
                                    type = VideoType.Gif,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    animate = isVisible && !isAnyViewerOpen,
                                    volume = if (isMuted) 0f else 1f,
                                    reportProgress = true,
                                    onProgressUpdate = { pos ->
                                        val seconds = (pos / 1000).toInt()
                                        if (seconds != currentPositionSecondsState.intValue) {
                                            currentPositionSecondsState.intValue = seconds
                                        }
                                    },
                                    fileId = content.fileId,
                                    thumbnailData = content.thumbnailPath ?: content.minithumbnail
                                )

                                VideoMuteToggle(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp),
                                    isMuted = isMuted,
                                    onToggle = { isMuted = !isMuted }
                                )
                            } else {
                                if (hasPath) {
                                    Image(
                                        painter = rememberAsyncImagePainter(
                                            model = ImageRequest.Builder(context)
                                                .data(displayPath)
                                                .apply {
                                                    videoCacheKey?.let {
                                                        memoryCacheKey(it)
                                                        diskCacheKey(it)
                                                    }
                                                }
                                                .crossfade(false)
                                                .build()
                                        ),
                                        contentDescription = content.caption,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    if (content.minithumbnail != null) {
                                        Image(
                                            painter = rememberAsyncImagePainter(
                                                model = ImageRequest.Builder(context)
                                                    .data(content.minithumbnail)
                                                    .apply {
                                                        videoMiniCacheKey?.let {
                                                            memoryCacheKey(it)
                                                            diskCacheKey(it)
                                                        }
                                                    }
                                                    .build()
                                            ),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(48.dp)
                                        .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                                        .clickable {
                                            // Trigger progressive playback: start streaming + download simultaneously
                                            isProgressivePlayActive = true
                                            isAutoDownloadSuppressed = false
                                            AutoDownloadSuppression.clear(content.fileId)
                                            onVideoClickState(msg)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = stringResource(R.string.action_play),
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                    } else {
                        VideoLoadingLayer(
                            content = content,
                            isViewOnce = false,
                            onCancelDownload = {
                                isAutoDownloadSuppressed = true
                                AutoDownloadSuppression.suppress(content.fileId)
                                onCancelDownloadState(content.fileId)
                            },
                            onStartDownload = {
                                isAutoDownloadSuppressed = false
                                AutoDownloadSuppression.clear(content.fileId)
                                onVideoClickState(msg)
                            }
                        )
                    }

                    VideoInteractionOverlay(
                        modifier = Modifier.matchParentSize(),
                        content = content,
                        isMediaSpoilerRevealed = isMediaSpoilerRevealed,
                        videoPosition = { layoutTracker.videoPosition },
                        onRevealSpoiler = { isMediaSpoilerRevealed = true },
                        onCancelDownload = {
                            isAutoDownloadSuppressed = true
                            isProgressivePlayActive = false
                            AutoDownloadSuppression.suppress(content.fileId)
                            onCancelDownloadState(content.fileId)
                        },
                        onOpenVideo = {
                            isAutoDownloadSuppressed = false
                            AutoDownloadSuppression.clear(content.fileId)
                            if (!hasFullVideo && !content.supportsStreaming) {
                                isProgressivePlayActive = true
                            }
                            onVideoClickState(msg)
                        },
                        onLongClick = { anchor -> onLongClickState(anchor) }
                    )

                    VideoPlaybackBadge(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .zIndex(2f),
                        durationSeconds = content.duration,
                        currentPositionSeconds = currentPositionSeconds,
                        showCurrentProgress = (hasFullVideo || content.supportsStreaming || isProgressivePlayActive) && (autoplayVideos || isProgressivePlayActive),
                        isDownloading = content.isDownloading,
                        downloadProgress = content.downloadProgress,
                        fileSize = content.fileSize,
                        isFullyDownloaded = hasFullVideo,
                        onDownloadClick = {
                            isAutoDownloadSuppressed = false
                            AutoDownloadSuppression.clear(content.fileId)
                            onVideoClickState(msg)
                        },
                        onCancelClick = {
                            isAutoDownloadSuppressed = true
                            isProgressivePlayActive = false
                            AutoDownloadSuppression.suppress(content.fileId)
                            onCancelDownloadState(content.fileId)
                        }
                    )

                    VideoUploadOverlay(
                        isUploading = content.isUploading,
                        uploadProgress = content.uploadProgress
                    )

                    VideoSpoilerOverlay(
                        isRevealed = isMediaSpoilerRevealed
                    )

                    if (content.caption.isEmpty() && showMetadata) {
                        VideoMetadataBadge(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(6.dp)
                                .background(
                                    Color.Black.copy(alpha = 0.45f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            msg = msg,
                            isOutgoing = isOutgoing
                        )
                    }
                }

                if (content.caption.isNotEmpty()) {
                    val timeColor = if (LocalDarkTheme.current) Color(0xFFFFFFFF).copy(alpha = 0.7f) else Color(0xFF212121).copy(alpha = 0.7f)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(run { val d = LocalDarkTheme.current; if (isOutgoing) (if (d) Color(0xFF2B5278) else Color(0xFFEEFFDE)) else (if (d) Color(0xFF182533) else Color(0xFFFFFFFF)) })
                            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp)
                            .zIndex(1f)
                    ) {
                        val renderData = rememberMessageTextRenderData(
                            text = content.caption,
                            entities = content.entities,
                            allowBigEmoji = false,
                            isOutgoing = isOutgoing,
                            revealedSpoilers = revealedSpoilers,
                            fontSize = fontSize
                        )

                        if (renderData.isBigEmoji && renderData.bigEmojiItems.isNotEmpty()) {
                            BigEmojiContent(
                                items = renderData.bigEmojiItems,
                                sizeDp = fontSize * 5f,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        } else {
                            MessageText(
                                text = renderData.annotatedText,
                                rawText = content.caption,
                                inlineContent = renderData.inlineContent,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = fontSize.sp,
                                    letterSpacing = letterSpacing.sp,
                                    lineHeight = (fontSize * 1.375f).sp
                                ),
                                modifier = Modifier.padding(bottom = 4.dp),
                                onSpoilerClick = { index ->
                                    if (revealedSpoilers.contains(index)) {
                                        revealedSpoilers.remove(index)
                                    } else {
                                        revealedSpoilers.add(index)
                                    }
                                },
                                onClick = { offset -> onLongClickState(layoutTracker.videoPosition + offset) },
                                onLongClick = { offset -> onLongClickState(layoutTracker.videoPosition + offset) }
                            )
                        }
                        if (showMetadata) {
                            Box(modifier = Modifier.align(Alignment.End)) {
                                MessageMetadata(msg, isOutgoing, timeColor)
                            }
                        }
                    }
                }
            }
        }

        if (showReactions) {
            MessageReactionsView(
                reactions = msg.reactions,
                onReactionClick = onReactionClick,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp)
            )
        }
    }
}

// ==================== Helper Composables (unchanged) ====================

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun VideoViewOnceOverlay(
    content: MessageContent.Video
) {
    // ✅ Mirrors PhotoMessageBubble's view-once overlay 1:1 — same blur amount,
    // same scrim opacity, same icon sizes/backgrounds, same label placement.
    Box(modifier = Modifier.fillMaxSize()) {
        // Blurred thumbnail background
        MediaLoadingBackground(
            previewData = content.thumbnailPath ?: content.minithumbnail,
            contentScale = ContentScale.Crop,
            previewBlur = 20.dp
        )
        // Dark scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
        )
        // Center icon reflects the current download state:
        //   - no path/streaming, not downloading -> download icon
        //   - downloading -> progress ring (with small flame badge, like the photo bubble)
        //   - path/streaming ready -> flame icon (tap opens the view-once viewer)
        val hasVideoReady = !content.path.isNullOrBlank() || content.supportsStreaming
        Box(
            modifier = Modifier.align(Alignment.Center),
            contentAlignment = Alignment.Center
        ) {
            when {
                content.isDownloading -> {
                    Box(
                        modifier = Modifier.size(64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularWavyProgressIndicator(
                            progress = { content.downloadProgress },
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.25f),
                            modifier = Modifier.size(64.dp)
                        )
                        Icon(
                            imageVector = Icons.Default.Whatshot,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                !hasVideoReady -> {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color.White.copy(alpha = 0.18f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color.White.copy(alpha = 0.18f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Whatshot,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
        // Bottom label — same placement/style as the photo bubble.
        Text(
            text = "Video, tap to view",
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp)
        )
    }
}

@Composable
private fun VideoMuteToggle(
    modifier: Modifier = Modifier,
    isMuted: Boolean,
    onToggle: () -> Unit
) {
    Box(
        modifier = modifier
            .size(30.dp)
            .background(Color.Black.copy(alpha = 0.45f), CircleShape)
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
            contentDescription = stringResource(R.string.cd_toggle_sound),
            tint = Color.White,
            modifier = Modifier.size(16.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun VideoLoadingLayer(
    content: MessageContent.Video,
    isViewOnce: Boolean = false,
    onCancelDownload: () -> Unit,
    onStartDownload: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        MediaLoadingBackground(
            previewData = content.thumbnailPath ?: content.minithumbnail,
            contentScale = ContentScale.Crop,
            previewBlur = 0.dp
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.25f))
        )

        MediaLoadingAction(
            isDownloading = content.isDownloading,
            progress = content.downloadProgress,
            idleIcon = when {
                content.supportsStreaming -> Icons.Rounded.Stream
                else -> Icons.Default.Download
            },
            idleContentDescription = if (content.supportsStreaming) {
                stringResource(R.string.cd_stream)
            } else {
                stringResource(R.string.cd_download)
            },
            onCancelClick = onCancelDownload,
            onIdleClick = onStartDownload
        )

        if (isViewOnce) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color.White.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Whatshot,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoInteractionOverlay(
    modifier: Modifier = Modifier,
    content: MessageContent.Video,
    isMediaSpoilerRevealed: Boolean,
    videoPosition: () -> Offset,
    onRevealSpoiler: () -> Unit,
    onCancelDownload: () -> Unit,
    onOpenVideo: () -> Unit,
    onLongClick: (Offset) -> Unit
) {
    Box(
        modifier = modifier.pointerInput(
            content.isDownloading,
            content.fileId,
            isMediaSpoilerRevealed,
            content.supportsStreaming
        ) {
            detectTapGestures(
                onTap = {
                    if (!isMediaSpoilerRevealed) {
                        onRevealSpoiler()
                    } else if (content.isDownloading) {
                        onCancelDownload()
                    } else {
                        onOpenVideo()
                    }
                },
                onLongPress = { offset -> onLongClick(videoPosition() + offset) }
            )
        }
    )
}

@Composable
private fun VideoPlaybackBadge(
    modifier: Modifier = Modifier,
    durationSeconds: Int,
    currentPositionSeconds: Int,
    showCurrentProgress: Boolean,
    isDownloading: Boolean = false,
    downloadProgress: Float = 0f,
    fileSize: Long = 0L,
    isFullyDownloaded: Boolean = false,
    onDownloadClick: (() -> Unit)? = null,
    onCancelClick: (() -> Unit)? = null
) {
    // Show download info when: not fully downloaded AND has known size
    // (even during progressive play — show both playback time AND download progress)
    val showDownloadInfo = !isFullyDownloaded && fileSize > 0L

    // Badge is clickable only when showing download info:
    // - downloading → click = cancel
    // - not downloading, not downloaded → click = start download
    val badgeClickAction: (() -> Unit)? = when {
        !showDownloadInfo -> null
        isDownloading -> onCancelClick
        else -> onDownloadClick
    }

    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
            .then(
                if (badgeClickAction != null)
                    Modifier.clickable(onClick = badgeClickAction)
                else Modifier
            )
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        if (showDownloadInfo && fileSize > 0L) {
            val downloadedBytes = (fileSize * downloadProgress).toLong()
            val downloadedStr = formatFileSize(downloadedBytes)
            val totalStr = formatFileSize(fileSize)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                // Icon: ✕ when downloading (tap = cancel), ↓ when not (tap = download)
                Icon(
                    imageVector = if (isDownloading) Icons.Default.Close else Icons.Default.Download,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(10.dp)
                )
                Column {
                    // Show playback position when playing, otherwise show duration
                    Text(
                        text = if (showCurrentProgress) {
                            "${formatDuration(currentPositionSeconds)} / ${formatDuration(durationSeconds)}"
                        } else {
                            formatDuration(durationSeconds)
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Color.White,
                        lineHeight = 12.sp
                    )
                    // Always show download progress when not fully downloaded
                    Text(
                        text = if (isDownloading) "$downloadedStr / $totalStr" else totalStr,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Color.White,
                        lineHeight = 12.sp
                    )
                }
            }
        } else {
            Text(
                text = if (showCurrentProgress) {
                    "${formatDuration(currentPositionSeconds)} / ${formatDuration(durationSeconds)}"
                } else {
                    formatDuration(durationSeconds)
                },
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes <= 0L -> "0 B"
        bytes < 1024L -> "${bytes} B"
        bytes < 1024L * 1024L -> String.format("%.1f KB", bytes / 1024f)
        bytes < 1024L * 1024L * 1024L -> String.format("%.1f MB", bytes / (1024f * 1024f))
        else -> String.format("%.2f GB", bytes / (1024f * 1024f * 1024f))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun VideoUploadOverlay(
    isUploading: Boolean,
    uploadProgress: Float
) {
    if (!isUploading) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        if (uploadProgress > 0f) {
            CircularWavyProgressIndicator(
                progress = { uploadProgress },
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.3f)
            )
        } else {
            LoadingIndicator(
                color = Color.White
            )
        }
    }
}

@Composable
private fun VideoSpoilerOverlay(
    isRevealed: Boolean
) {
    SpoilerWrapper(isRevealed = isRevealed) {
        Box(modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun VideoMetadataBadge(
    modifier: Modifier = Modifier,
    msg: MessageModel,
    isOutgoing: Boolean
) {
    Box(modifier = modifier) {
        MessageMetadata(msg, isOutgoing, Color.White)
    }
}
