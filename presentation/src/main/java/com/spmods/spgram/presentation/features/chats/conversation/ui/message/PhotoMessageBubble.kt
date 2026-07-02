package com.spmods.spgram.presentation.features.chats.conversation.ui.message

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable  // ✅ මෙතන add කරන්න!
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.spmods.spgram.presentation.ui.theme.LocalDarkTheme
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.spmods.spgram.domain.models.ForwardInfo
import com.spmods.spgram.domain.models.MessageContent
import com.spmods.spgram.domain.models.MessageModel
import com.spmods.spgram.presentation.R
import com.spmods.spgram.presentation.core.util.IDownloadUtils
import com.spmods.spgram.presentation.core.util.namespacedCacheKey
import com.spmods.spgram.presentation.features.chats.conversation.AutoDownloadSuppression

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PhotoMessageBubble(
    content: MessageContent.Photo,
    msg: MessageModel,
    isOutgoing: Boolean,
    isSameSenderAbove: Boolean,
    isSameSenderBelow: Boolean,
    fontSize: Float,
    letterSpacing: Float,
    isGroup: Boolean = false,
    autoDownloadMobile: Boolean,
    autoDownloadWifi: Boolean,
    autoDownloadRoaming: Boolean,
    onPhotoClick: (MessageModel) -> Unit,
    onOpenViewOnce: (MessageModel) -> Unit = {},
    onDownloadPhoto: (Int) -> Unit = {},
    onCancelDownload: (Int) -> Unit = {},
    onLongClick: (Offset) -> Unit,
    onReplyClick: (MessageModel) -> Unit = {},
    onReactionClick: (String) -> Unit = {},
    showMetadata: Boolean = true,
    showReactions: Boolean = true,
    toProfile: (Long) -> Unit = {},
    onForwardOriginClick: (ForwardInfo) -> Unit = {},
    modifier: Modifier = Modifier,
    downloadUtils: IDownloadUtils
) {
    val context = LocalContext.current
    val cornerRadius = 18.dp
    val smallCorner = 6.dp
    val tailCorner = 0.dp

    val displayPath: String? = content.path?.takeIf { it.isNotBlank() }
        ?: content.thumbnailPath?.takeIf { it.isNotBlank() }
    val hasFullPhoto = !content.path.isNullOrBlank()
    val hasPath = !displayPath.isNullOrBlank()

    val photoCacheKey = remember(displayPath, content.fileId) {
        namespacedCacheKey("chat_photo:${content.fileId}", displayPath)
    }

    LaunchedEffect(content.path, content.fileId) {
        if (!content.path.isNullOrBlank()) {
            AutoDownloadSuppression.clear(content.fileId)
        }
    }

    LaunchedEffect(content.path, content.isDownloading, autoDownloadMobile, autoDownloadWifi, autoDownloadRoaming) {
        if (!content.isViewOnce && content.path.isNullOrBlank() && !content.isDownloading && !AutoDownloadSuppression.isSuppressed(content.fileId)) {
            val shouldDownload = when {
                downloadUtils.isWifiConnected() -> autoDownloadWifi
                downloadUtils.isRoaming() -> autoDownloadRoaming
                else -> autoDownloadMobile
            }
            if (shouldDownload) {
                onDownloadPhoto(content.fileId)
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

    var imagePosition by remember { mutableStateOf(Offset.Zero) }
    val revealedSpoilers = remember { mutableStateListOf<Int>() }
    var isMediaSpoilerRevealed by remember { mutableStateOf(!content.hasSpoiler) }

    // Photo aspect ratio for sizing — official Telegram approach:
    // fillMaxWidth() + aspectRatio() so bubble fills the available width
    // and height follows naturally from the photo's proportions.
    // Capped at maxHeight to avoid portrait photos becoming enormous.
    val photoAspectRatio = remember(content.width, content.height) {
        val pw = content.width.takeIf { it > 0 } ?: 4
        val ph = content.height.takeIf { it > 0 } ?: 3
        pw.toFloat() / ph.toFloat()
    }

    Column(
        modifier = modifier,
        horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = bubbleShape,
            color = run { val d = LocalDarkTheme.current; if (isOutgoing) (if (d) Color(0xFF2B5278) else Color(0xFFEEFFDE)) else (if (d) Color(0xFF182533) else Color(0xFFFFFFFF)) },
            contentColor = if (LocalDarkTheme.current) Color(0xFFFFFFFF) else Color(0xFF212121),
        ) {
            Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                if (isGroup && !isOutgoing && !isSameSenderAbove) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(run { val d = LocalDarkTheme.current; if (isOutgoing) (if (d) Color(0xFF2B5278) else Color(0xFFEEFFDE)) else (if (d) Color(0xFF182533) else Color(0xFFFFFFFF)) })
                            .padding(start = 12.dp, end = 12.dp, top = 8.dp)
                            .zIndex(1f)
                    ) {
                        MessageSenderName(msg)
                    }
                }

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

                // View-once: fixed 260×260. Normal: fillMaxWidth + aspectRatio capped at 320dp.
                val boxModifier = if (content.isViewOnce && !content.isViewOnceOpened) {
                    Modifier
                        .size(260.dp, 260.dp)
                        .clipToBounds()
                        .onGloballyPositioned { imagePosition = it.positionInWindow() }
                } else {
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .aspectRatio(photoAspectRatio)
                        .clipToBounds()
                        .onGloballyPositioned { imagePosition = it.positionInWindow() }
                }

                Box(
                    modifier = boxModifier
                        .pointerInput(
                            content.isViewOnce,
                            content.isViewOnceOpened,
                            content.path,
                            content.isDownloading,
                            content.hasSpoiler,
                            content.fileId,
                            hasFullPhoto,
                            isMediaSpoilerRevealed
                        ) {
                            detectTapGestures(
                                onTap = {
                                    when {
                                        content.isViewOnce && !content.isViewOnceOpened && content.path == null -> {
                                            onOpenViewOnce(msg)
                                        }
                                        content.hasSpoiler -> {
                                            isMediaSpoilerRevealed = !isMediaSpoilerRevealed
                                        }
                                        content.isDownloading -> {
                                            AutoDownloadSuppression.suppress(content.fileId)
                                            onCancelDownload(content.fileId)
                                        }
                                        else -> {
                                            AutoDownloadSuppression.clear(content.fileId)
                                            if (hasFullPhoto) onPhotoClick(msg) else onDownloadPhoto(content.fileId)
                                        }
                                    }
                                },
                                onLongPress = { offset -> onLongClick(imagePosition + offset) }
                            )
                        }
                ) {
                    // --- Background layer (non-view-once only) ---
                    if (!content.isViewOnce && !hasPath) {
                        MediaLoadingBackground(
                            previewData = content.thumbnailPath ?: content.minithumbnail,
                            contentScale = ContentScale.Crop
                        )
                    }

                    if (hasPath && !(content.isViewOnce && !content.isViewOnceOpened)) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(displayPath)
                                .apply {
                                    photoCacheKey?.let {
                                        memoryCacheKey(it)
                                        diskCacheKey(it)
                                    }
                                }
                                .crossfade(true)
                                .build(),
                            contentDescription = content.caption,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                            ) 
                    }

                    // --- Download action (CENTER) ---
                    if (!hasFullPhoto && !content.isViewOnce) {
                        Box(
                            modifier = Modifier.matchParentSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (content.isDownloading) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularWavyProgressIndicator(
                                        progress = { content.downloadProgress },
                                        color = Color.White,
                                        trackColor = Color.White.copy(alpha = 0.25f),
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clickable {
                                                AutoDownloadSuppression.suppress(content.fileId)
                                                onCancelDownload(content.fileId)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = stringResource(R.string.cancel_button),
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                        .clickable {
                                            AutoDownloadSuppression.clear(content.fileId)
                                            onDownloadPhoto(content.fileId)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = stringResource(R.string.cd_download),
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }

                    // --- View once overlay ---
                    if (content.isViewOnce && !content.isViewOnceOpened) {
                        MediaLoadingBackground(
                            previewData = content.thumbnailPath ?: content.minithumbnail,
                            contentScale = ContentScale.Crop,
                            previewBlur = 20.dp
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.45f))
                        )
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
                                content.path == null -> {
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
                        val timerLabel = when {
                            content.selfDestructSeconds <= 0 -> "Photo, tap to view"
                            else -> {
                                val s = content.selfDestructSeconds
                                "🔥 ${s}s · tap to view"
                            }
                        }
                        androidx.compose.material3.Text(
                            text = timerLabel,
                            color = Color.White,
                            style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 10.dp)
                        )
                    }

                    // --- Upload progress ---
                    if (content.isUploading) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (content.uploadProgress > 0f) {
                                CircularWavyProgressIndicator(
                                    progress = { content.uploadProgress },
                                    color = Color.White,
                                    trackColor = Color.White.copy(alpha = 0.3f),
                                )
                            } else {
                                LoadingIndicator(color = Color.White)
                            }
                        }
                    }

                    // --- Spoiler overlay ---
                    if (content.hasSpoiler) {
                        SpoilerWrapper(isRevealed = isMediaSpoilerRevealed) {
                            Box(modifier = Modifier.fillMaxSize())
                        }
                    }

                    // --- Metadata overlay ---
                    if (!content.isViewOnce && content.caption.isEmpty() && showMetadata) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(6.dp)
                                .background(
                                    Color.Black.copy(alpha = 0.45f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            MessageMetadata(msg, isOutgoing, Color.White)
                        }
                    }
                }

                // --- Caption ---
                if (!content.isViewOnce && content.caption.isNotEmpty()) {
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
                                onClick = { offset -> onLongClick(imagePosition + offset) },
                                onLongClick = { offset -> onLongClick(imagePosition + offset) }
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

        if (showReactions && msg.reactions.isNotEmpty()) {
            MessageReactionsView(
                reactions = msg.reactions,
                onReactionClick = onReactionClick,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp)
            )
        }
    }
}
