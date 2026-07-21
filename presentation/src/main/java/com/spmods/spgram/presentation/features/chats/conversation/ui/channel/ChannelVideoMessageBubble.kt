package com.spmods.spgram.presentation.features.chats.conversation.ui.channel

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import com.spmods.spgram.presentation.ui.theme.LocalDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.Stream
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.spmods.spgram.presentation.features.chats.conversation.ui.message.BigEmojiContent
import com.spmods.spgram.presentation.features.chats.conversation.ui.message.ForwardContent
import com.spmods.spgram.presentation.features.chats.conversation.ui.message.MediaLoadingAction
import com.spmods.spgram.presentation.features.chats.conversation.ui.message.MediaLoadingBackground
import com.spmods.spgram.presentation.features.chats.conversation.ui.message.MessageMetadata
import com.spmods.spgram.presentation.features.chats.conversation.ui.message.MessageReactionsView
import com.spmods.spgram.presentation.features.chats.conversation.ui.message.MessageText
import com.spmods.spgram.presentation.features.chats.conversation.ui.message.ReplyContent
import com.spmods.spgram.presentation.features.chats.conversation.ui.message.rememberMessageTextRenderData

@Composable
fun ChannelVideoMessageBubble(
    content: MessageContent.Video,
    msg: MessageModel,
    fontSize: Float,
    letterSpacing: Float,
    autoDownloadMobile: Boolean,
    autoDownloadWifi: Boolean,
    autoDownloadRoaming: Boolean,
    autoplayVideos: Boolean,
    modifier: Modifier = Modifier,
    bubbleRadius: Float = 18f,
    isSameSenderBelow: Boolean = false,
    isSameSenderAbove: Boolean = false,
    onVideoClick: (MessageModel) -> Unit,
    onCancelDownload: (Int) -> Unit = {},
    onLongClick: (Offset) -> Unit,
    onReplyClick: (MessageModel) -> Unit = {},
    onReactionClick: (String) -> Unit = {},
    onCommentsClick: (Long) -> Unit = {},
    showComments: Boolean = true,
    showMetadata: Boolean = true,
    showReactions: Boolean = true,
    toProfile: (Long) -> Unit = {},
    onForwardOriginClick: (ForwardInfo) -> Unit = {},
    downloadUtils: IDownloadUtils,
    isAnyViewerOpen: Boolean = false
) {
    val cornerRadius = bubbleRadius.dp
    val smallCorner = (bubbleRadius / 4f).coerceAtLeast(4f).dp
    val tailCorner = 0.dp

    val topStart = if (isSameSenderAbove) smallCorner else cornerRadius
    val topEnd = cornerRadius
    val bottomStart = if (isSameSenderBelow) smallCorner else tailCorner
    val bottomEnd = cornerRadius

    val bubbleShape = RoundedCornerShape(
        topStart = topStart,
        topEnd = topEnd,
        bottomStart = bottomStart,
        bottomEnd = if (showComments && msg.canGetMessageThread) 4.dp else bottomEnd
    )

    var videoPosition by remember { mutableStateOf(Offset.Zero) }
    var isMuted by remember { mutableStateOf(true) }
    var currentPositionSeconds by remember { mutableIntStateOf(0) }
    var isVisible by remember { mutableStateOf(false) }
    var isProgressivePlayActive by remember(msg.id, content.fileId) { mutableStateOf(false) }

    LaunchedEffect(content.path) {
        if (!content.path.isNullOrBlank()) {
            isProgressivePlayActive = false
        }
    }

    val context = LocalContext.current
    val resource = LocalResources.current
    val screenHeightPx = remember { resource.displayMetrics.heightPixels }
    val revealedSpoilers = remember { mutableStateListOf<Int>() }

    var stablePath by remember(msg.id) { mutableStateOf(content.path) }
    val hasFullVideo = !stablePath.isNullOrBlank()
    // Same as normal VideoMessageBubble: thumbnailPath used as static preview
    // when full video not downloaded. Without this, channel bubbles fall back
    // to minithumbnail (tiny blurry image) causing the blur issue.
    val displayPath: String? = stablePath?.takeIf { it.isNotBlank() }
        ?: content.thumbnailPath?.takeIf { it.isNotBlank() }
    val hasPath = !displayPath.isNullOrBlank()
    val videoCacheKey = remember(displayPath, content.fileId) {
        namespacedCacheKey("channel_video:${content.fileId}", displayPath)
    }
    val videoMiniCacheKey = remember(content.minithumbnail, content.fileId) {
        content.minithumbnail?.let { namespacedCacheKey("channel_video_mini:${content.fileId}", it) }
    }
    // AutoDownloadSuppression is the single source of truth — no local mirror needed.

    val bubbleSize = remember(content.width, content.height) {
        val pw = content.width.takeIf { it > 0 } ?: 4
        val ph = content.height.takeIf { it > 0 } ?: 3
        val maxW = 260f; val maxH = 320f; val minW = 120f; val minH = 120f
        val scale = minOf(maxW / pw, maxH / ph, 1f)
        androidx.compose.ui.unit.DpSize(
            (pw * scale).coerceAtLeast(minW).dp,
            (ph * scale).coerceAtLeast(minH).dp
        )
    }
    val hasCaption = content.caption.isNotEmpty()

    LaunchedEffect(content.path) {
        if (!content.path.isNullOrBlank()) {
            stablePath = content.path
            AutoDownloadSuppression.clear(content.fileId)
        }
    }

    LaunchedEffect(content.path, autoDownloadMobile, autoDownloadWifi, autoDownloadRoaming) {
        if (content.path.isNullOrBlank() && !content.isDownloading && !content.supportsStreaming && !AutoDownloadSuppression.isSuppressed(content.fileId)
        ) {
            val shouldDownload = when {
                downloadUtils.isWifiConnected() -> autoDownloadWifi
                downloadUtils.isRoaming() -> autoDownloadRoaming
                else -> autoDownloadMobile
            }
            if (shouldDownload) onVideoClick(msg)
        }
    }

    Column(
        modifier = modifier
            .onGloballyPositioned {
                val rect = it.boundsInWindow()
                isVisible = rect.bottom > 0 && rect.top < screenHeightPx
            },
        horizontalAlignment = Alignment.Start
    ) {
        Surface(
            shape = bubbleShape,
            color = if (LocalDarkTheme.current) Color(0xFF182533) else Color(0xFFFFFFFF),
            contentColor = if (LocalDarkTheme.current) Color(0xFFFFFFFF) else Color(0xFF212121),
            tonalElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                // Headers
                if (msg.forwardInfo != null || msg.replyToMsg != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (LocalDarkTheme.current) Color(0xFF182533) else Color(0xFFFFFFFF))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .zIndex(1f)
                    ) {
                        msg.forwardInfo?.let {
                            ForwardContent(
                                it,
                                false,
                                onForwardClick = onForwardOriginClick
                            )
                        }
                        msg.replyToMsg?.let { ReplyContent(it, false, onClick = { onReplyClick(it) }) }
                    }
                }

                Box(
                    modifier = Modifier
                        .widthIn(min = bubbleSize.width)
                        .fillMaxWidth()
                        .height(bubbleSize.height)
                        .clip(
                            if (hasCaption) RoundedCornerShape(
                                topStart = topStart,
                                topEnd = topEnd
                            ) else bubbleShape
                        )
                        .clipToBounds()
                        .onGloballyPositioned { videoPosition = it.positionInWindow() }
                ) {
                    if (hasPath || content.supportsStreaming || isProgressivePlayActive) {
                        if ((autoplayVideos || isProgressivePlayActive) && isVisible) {
                            val videoPath = when {
                                !stablePath.isNullOrBlank() -> stablePath!!
                                content.supportsStreaming || isProgressivePlayActive -> "http://streaming/${content.fileId}"
                                else -> "http://streaming/${content.fileId}"
                            }
                            VideoStickerPlayer(
                                path = videoPath,
                                type = VideoType.Gif,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                animate = isVisible && !isAnyViewerOpen,
                                volume = if (isMuted) 0f else 1f,
                                reportProgress = true,
                                onProgressUpdate = {
                                    val seconds = (it / 1000).toInt()
                                    if (seconds != currentPositionSeconds) {
                                        currentPositionSeconds = seconds
                                    }
                                },
                                fileId = content.fileId,
                                thumbnailData = content.thumbnailPath
                            )

                            // Volume Toggle
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .size(30.dp)
                                    .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                                    .clickable { isMuted = !isMuted },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
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
                                    contentDescription = null,
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
                                        isProgressivePlayActive = true
                                        AutoDownloadSuppression.clear(content.fileId)
                                        onVideoClick(msg)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        // Duration / Download Badge
                        val showDownloadInfo = !hasPath && content.fileSize > 0L
                        val badgeClickAction: (() -> Unit)? = when {
                            !showDownloadInfo -> null
                            content.isDownloading -> ({
                                isProgressivePlayActive = false
                                AutoDownloadSuppression.suppress(content.fileId)
                                onCancelDownload(content.fileId)
                            })
                            else -> ({
                                AutoDownloadSuppression.clear(content.fileId)
                                onVideoClick(msg)
                            })
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                                .then(if (badgeClickAction != null) Modifier.clickable(onClick = badgeClickAction) else Modifier)
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            if (showDownloadInfo) {
                                val downloadedBytes = (content.fileSize * content.downloadProgress).toLong()
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = if (content.isDownloading) Icons.Default.Close else Icons.Default.Download,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    androidx.compose.foundation.layout.Column {
                                        Text(
                                            text = if (isProgressivePlayActive) {
                                                "${formatDuration(context, currentPositionSeconds)} / ${formatDuration(context, content.duration)}"
                                            } else {
                                                formatDuration(context, content.duration)
                                            },
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = Color.White,
                                            lineHeight = 12.sp
                                        )
                                        Text(
                                            text = if (content.isDownloading) {
                                                "${formatFileSize(downloadedBytes)} / ${formatFileSize(content.fileSize)}"
                                            } else {
                                                formatFileSize(content.fileSize)
                                            },
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = Color.White,
                                            lineHeight = 12.sp
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = if ((hasPath || content.supportsStreaming || isProgressivePlayActive) && (autoplayVideos || isProgressivePlayActive)) {
                                        "${formatDuration(context, currentPositionSeconds)} / ${formatDuration(context, content.duration)}"
                                    } else {
                                        formatDuration(context, content.duration)
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }
                        }
                    } else {
                        // Placeholder / Download State
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            MediaLoadingBackground(
                                previewData = content.thumbnailPath ?: content.minithumbnail,
                                contentScale = ContentScale.Crop,
                                previewBlur = 0.dp
                            )

                            MediaLoadingAction(
                                isDownloading = content.isDownloading,
                                progress = content.downloadProgress,
                                idleIcon = if (content.supportsStreaming) Icons.Rounded.Stream else Icons.Default.Download,
                                idleContentDescription = if (content.supportsStreaming) {
                                    stringResource(R.string.cd_stream)
                                } else {
                                    stringResource(R.string.cd_download)
                                },
                                onCancelClick = {
                                    AutoDownloadSuppression.suppress(content.fileId)
                                    onCancelDownload(content.fileId)
                                },
                                onIdleClick = {
                                    AutoDownloadSuppression.clear(content.fileId)
                                    onVideoClick(msg)
                                }
                            )
                        }
                    }

                    // Tap gesture overlay
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .pointerInput(
                                content.isDownloading,
                                content.fileId,
                                stablePath,
                                content.supportsStreaming,
                                isProgressivePlayActive
                            ) {
                                detectTapGestures(
                                    onTap = {
                                        if (content.isDownloading) {
                                            isProgressivePlayActive = false
                                            AutoDownloadSuppression.suppress(content.fileId)
                                            onCancelDownload(content.fileId)
                                        } else {
                                            AutoDownloadSuppression.clear(content.fileId)
                                            if (!hasPath && !content.supportsStreaming) {
                                                isProgressivePlayActive = true
                                            }
                                            onVideoClick(msg)
                                        }
                                    },
                                    onLongPress = { offset -> onLongClick(videoPosition + offset) }
                                )
                            }
                    )

                    if (!hasCaption && showMetadata) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(6.dp)
                                .background(
                                    Color.Black.copy(alpha = 0.45f),
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            MessageMetadata(msg, msg.isOutgoing, Color.White)
                        }
                    }
                }

                // Caption Section
                if (hasCaption) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (LocalDarkTheme.current) Color(0xFF182533) else Color(0xFFFFFFFF))
                            .padding(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 4.dp)
                            .zIndex(1f)
                    ) {
                        val renderData = rememberMessageTextRenderData(
                            text = content.caption,
                            entities = content.entities,
                            allowBigEmoji = false,
                            isOutgoing = false,
                            revealedSpoilers = revealedSpoilers,
                            fontSize = fontSize
                        )

                        if (renderData.isBigEmoji && renderData.bigEmojiItems.isNotEmpty()) {
                            BigEmojiContent(
                                items = renderData.bigEmojiItems,
                                sizeDp = fontSize * 5f
                            )
                        } else {
                            MessageText(
                                text = renderData.annotatedText,
                                rawText = content.caption,
                                inlineContent = renderData.inlineContent,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = fontSize.sp,
                                    letterSpacing = letterSpacing.sp,
                                    lineHeight = (fontSize * 1.35f).sp
                                ),
                                entities = content.entities,
                                isOutgoing = false,
                                onSpoilerClick = { index ->
                                    if (revealedSpoilers.contains(index)) {
                                        revealedSpoilers.remove(index)
                                    } else {
                                        revealedSpoilers.add(index)
                                    }
                                },
                                onClick = { offset -> onLongClick(videoPosition + offset) },
                                onLongClick = { offset -> onLongClick(videoPosition + offset) }
                            )
                        }

                        if (showMetadata) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                MessageMetadata(
                                    msg = msg,
                                    isOutgoing = msg.isOutgoing,
                                    contentColor = if (LocalDarkTheme.current) Color(0xFFFFFFFF).copy(alpha = 0.8f) else Color(0xFF212121).copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showComments && msg.canGetMessageThread) {
            ChannelCommentsButton(
                replyCount = msg.replyCount,
                bubbleRadius = bubbleRadius,
                isSameSenderBelow = isSameSenderBelow,
                onClick = { onCommentsClick(msg.id) },
                modifier = Modifier.fillMaxWidth()
            )
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

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes <= 0L -> "0 B"
        bytes < 1024L -> "${bytes} B"
        bytes < 1024L * 1024L -> String.format("%.1f KB", bytes / 1024f)
        bytes < 1024L * 1024L * 1024L -> String.format("%.1f MB", bytes / (1024f * 1024f))
        else -> String.format("%.2f GB", bytes / (1024f * 1024f * 1024f))
    }
}
