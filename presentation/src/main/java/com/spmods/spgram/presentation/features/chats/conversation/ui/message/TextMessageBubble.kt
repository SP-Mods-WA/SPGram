package com.spmods.spgram.presentation.features.chats.conversation.ui.message

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.koinInject
import com.spmods.spgram.app.ui.theme.BubbleIncomingDark
import com.spmods.spgram.app.ui.theme.BubbleIncomingLight
import com.spmods.spgram.app.ui.theme.BubbleOutgoingDark
import com.spmods.spgram.app.ui.theme.BubbleOutgoingLight
import com.spmods.spgram.app.ui.theme.BubbleIncomingContentDark
import com.spmods.spgram.app.ui.theme.BubbleIncomingContentLight
import com.spmods.spgram.app.ui.theme.BubbleOutgoingContentDark
import com.spmods.spgram.app.ui.theme.BubbleOutgoingContentLight
import com.spmods.spgram.domain.models.ForwardInfo
import com.spmods.spgram.domain.models.MessageContent
import com.spmods.spgram.domain.models.MessageModel
import com.spmods.spgram.presentation.R
import com.spmods.spgram.presentation.core.util.DateFormatManager
import com.spmods.spgram.presentation.features.chats.conversation.ui.TelegramBubbleShape


@Composable
fun TextMessageBubble(
    content: MessageContent.Text,
    msg: MessageModel,
    isOutgoing: Boolean,
    isSameSenderAbove: Boolean,
    isSameSenderBelow: Boolean,
    fontSize: Float,
    letterSpacing: Float,
    isGroup: Boolean = false,
    bubbleRadius: Float = 18f,
    showLinkPreviews: Boolean = true,
    onReplyClick: (MessageModel) -> Unit = {},
    onReactionClick: (String) -> Unit = {},
    onInstantViewClick: ((String) -> Unit)? = null,
    onYouTubeClick: ((String) -> Unit)? = null,
    onClick: (Offset) -> Unit = {},
    onLongClick: (Offset) -> Unit = {},
    showReactions: Boolean = true,
    toProfile: (Long) -> Unit = {},
    onForwardOriginClick: (ForwardInfo) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val isDark = isSystemInDarkTheme()

    // ── Telegram-exact colours ────────────────────────────────────────────
    val backgroundColor = when {
        isOutgoing && isDark  -> BubbleOutgoingDark
        isOutgoing && !isDark -> BubbleOutgoingLight
        !isOutgoing && isDark -> BubbleIncomingDark
        else                  -> BubbleIncomingLight
    }
    val contentColor = when {
        isOutgoing && isDark  -> BubbleOutgoingContentDark
        isOutgoing && !isDark -> BubbleOutgoingContentLight
        !isOutgoing && isDark -> BubbleIncomingContentDark
        else                  -> BubbleIncomingContentLight
    }
    val timeColor = contentColor.copy(alpha = 0.6f)

    // ── Telegram bubble shape ─────────────────────────────────────────────
    val hasTail = !isSameSenderBelow
    val cornerRadius = bubbleRadius.dp
    val smallCorner  = (bubbleRadius / 4f).coerceAtLeast(4f).dp

    val bubbleShape = remember(isOutgoing, isSameSenderAbove, hasTail, cornerRadius, smallCorner) {
        TelegramBubbleShape(
            isOutgoing       = isOutgoing,
            hasTail          = hasTail,
            isSameSenderAbove = isSameSenderAbove,
            cornerRadius     = cornerRadius,
            smallCorner      = smallCorner,
        )
    }

    val dateFormatManager: DateFormatManager = koinInject()
    val timeFormat = dateFormatManager.getHourMinuteFormat()

    val revealedSpoilers = remember { mutableStateListOf<Int>() }
    val renderData = rememberMessageTextRenderData(
        text            = content.text,
        entities        = content.entities,
        isOutgoing      = isOutgoing,
        revealedSpoilers = revealedSpoilers,
        fontSize        = fontSize,
    )
    val isBigEmoji = renderData.isBigEmoji && renderData.bigEmojiItems.isNotEmpty()

    val hasReactions = showReactions && msg.reactions.isNotEmpty()

    // ── Outer column: bubble surface + reactions floating below ───────────
    Column(
        modifier = modifier
            .width(IntrinsicSize.Max)
            .widthIn(min = 60.dp),
        horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start,
    ) {

        // ── Bubble surface ────────────────────────────────────────────────
        Surface(
            shape         = bubbleShape,
            color         = if (isBigEmoji) androidx.compose.ui.graphics.Color.Transparent
                            else backgroundColor,
            contentColor  = contentColor,
            tonalElevation  = if (isBigEmoji) 0.dp else 0.dp,
            shadowElevation = if (isBigEmoji) 0.dp else 1.dp,
        ) {
            Column(
                modifier = Modifier.padding(
                    start  = 12.dp,
                    end    = 12.dp,
                    top    = 8.dp,
                    // Add a little extra bottom padding when reactions will overlap
                    bottom = if (hasReactions) 8.dp else if (isBigEmoji) 0.dp else 6.dp,
                ),
            ) {
                // Sender name (groups only)
                if (isGroup && !isOutgoing && !isSameSenderAbove) {
                    MessageSenderName(msg, toProfile = toProfile)
                }

                // Forward header
                msg.forwardInfo?.let { forward ->
                    ForwardContent(
                        forward      = forward,
                        isOutgoing   = isOutgoing,
                        onForwardClick = onForwardOriginClick,
                    )
                }

                // Reply quote
                msg.replyToMsg?.let { reply ->
                    ReplyContent(
                        replyToMsg = reply,
                        isOutgoing = isOutgoing,
                        onClick    = { onReplyClick(reply) },
                    )
                }

                // Message content (big emoji or normal text)
                val finalFontSize = if (renderData.isBigEmoji) fontSize * 5f else fontSize

                if (isBigEmoji) {
                    BigEmojiContent(
                        items   = renderData.bigEmojiItems,
                        sizeDp  = finalFontSize,
                        modifier = Modifier.padding(bottom = 2.dp),
                    )
                } else {
                    MessageText(
                        text          = renderData.annotatedText,
                        rawText       = content.text,
                        entities      = content.entities,
                        inlineContent = renderData.inlineContent,
                        style         = MaterialTheme.typography.bodyLarge.copy(
                            fontSize     = finalFontSize.sp,
                            letterSpacing = letterSpacing.sp,
                            lineHeight   = (finalFontSize * 1.1f).sp,
                        ),
                        modifier      = Modifier.padding(bottom = 2.dp),
                        isOutgoing    = isOutgoing,
                        onSpoilerClick = { index ->
                            if (revealedSpoilers.contains(index)) {
                                revealedSpoilers.remove(index)
                            } else {
                                revealedSpoilers.add(index)
                            }
                        },
                        onClick    = onClick,
                        onLongClick = onLongClick,
                    )
                }

                // Link preview
                if (showLinkPreviews) {
                    content.webPage?.let { webPage ->
                        LinkPreview(
                            webPage           = webPage,
                            isOutgoing        = isOutgoing,
                            onInstantViewClick = onInstantViewClick,
                            onYouTubeClick    = onYouTubeClick,
                        )
                    }
                }

                // Timestamp + sending status (inside bubble, bottom-right)
                Row(
                    modifier          = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (msg.editDate > 0) {
                        Icon(
                            imageVector     = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.info_edited),
                            modifier        = Modifier.size(14.dp),
                            tint            = timeColor,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text  = formatTime(msg.date, timeFormat),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = timeColor,
                    )
                    if (isOutgoing) {
                        Spacer(modifier = Modifier.width(4.dp))
                        MessageSendingStatusIcon(
                            sendingState = msg.sendingState,
                            isRead       = msg.isRead,
                            baseColor    = timeColor,
                            size         = 14.dp,
                        )
                    }
                }
            } // end bubble Column
        } // end Surface

        // ── Reactions — rendered OUTSIDE the bubble, overlapping its bottom ──
        // offset(y = -8.dp) makes them visually overlap the bubble edge,
        // matching the official Telegram "floating below" appearance.
        if (hasReactions) {
            MessageReactionsView(
                reactions      = msg.reactions,
                isOutgoing     = isOutgoing,
                onReactionClick = onReactionClick,
                modifier       = Modifier
                    .offset(y = (-8).dp)
                    .padding(horizontal = 6.dp),
            )
        }
    }
}
