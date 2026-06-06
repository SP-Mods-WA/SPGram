package com.spmods.spgram.presentation.features.chats.conversation.ui.message

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.koinInject
import com.spmods.spgram.domain.models.ForwardInfo
import com.spmods.spgram.domain.models.MessageContent
import com.spmods.spgram.domain.models.MessageModel
import com.spmods.spgram.presentation.R
import com.spmods.spgram.presentation.core.util.DateFormatManager
import com.spmods.spgram.presentation.features.chats.conversation.ui.TelegramBubbleShape

// Telegram-exact bubble colours (inline)
private val TgBubbleOutLight = Color(0xFFEEFFDE)
private val TgBubbleOutDark  = Color(0xFF2B5278)
private val TgBubbleInLight  = Color(0xFFFFFFFF)
private val TgBubbleInDark   = Color(0xFF182533)


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
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    val backgroundColor = when {
        isOutgoing && isDark  -> TgBubbleOutDark
        isOutgoing            -> TgBubbleOutLight
        isDark                -> TgBubbleInDark
        else                  -> TgBubbleInLight
    }
    val contentColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF212121)
    val timeColor    = contentColor.copy(alpha = 0.6f)

    val bubbleShape = remember(isOutgoing, isSameSenderAbove, isSameSenderBelow) {
        TelegramBubbleShape(
            isOutgoing        = isOutgoing,
            hasTail           = !isSameSenderBelow,
            isSameSenderAbove = isSameSenderAbove,
            cornerRadius      = bubbleRadius.dp,
            smallCorner       = 6.dp,
        )
    }

    val dateFormatManager: DateFormatManager = koinInject()
    val timeFormat = dateFormatManager.getHourMinuteFormat()

    val revealedSpoilers = remember { mutableStateListOf<Int>() }
    val renderData = rememberMessageTextRenderData(
        text             = content.text,
        entities         = content.entities,
        isOutgoing       = isOutgoing,
        revealedSpoilers = revealedSpoilers,
        fontSize         = fontSize
    )
    val isBigEmoji   = renderData.isBigEmoji && renderData.bigEmojiItems.isNotEmpty()
    val hasReactions = showReactions && msg.reactions.isNotEmpty()

    // Reactions sit directly below the bubble, left-aligned — no overlap
    Column(
        modifier            = modifier.widthIn(min = 60.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Surface(
            shape           = bubbleShape,
            color           = if (isBigEmoji) Color.Transparent else backgroundColor,
            contentColor    = contentColor,
            tonalElevation  = 0.dp,
            shadowElevation = if (isBigEmoji) 0.dp else 1.dp,
        ) {
            Column(
                modifier = Modifier.padding(
                    start  = 12.dp,
                    end    = 12.dp,
                    top    = 8.dp,
                    bottom = if (isBigEmoji) 0.dp else 6.dp
                )
            ) {
                if (isGroup && !isOutgoing && !isSameSenderAbove) {
                    MessageSenderName(msg, toProfile = toProfile)
                }

                msg.forwardInfo?.let { forward ->
                    ForwardContent(forward, isOutgoing, onForwardClick = onForwardOriginClick)
                }
                msg.replyToMsg?.let { reply ->
                    ReplyContent(
                        replyToMsg = reply,
                        isOutgoing = isOutgoing,
                        onClick    = { onReplyClick(reply) }
                    )
                }

                val finalFontSize = if (renderData.isBigEmoji) fontSize * 5f else fontSize

                if (isBigEmoji) {
                    BigEmojiContent(
                        items    = renderData.bigEmojiItems,
                        sizeDp   = finalFontSize,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                } else {
                    MessageText(
                        text          = renderData.annotatedText,
                        rawText       = content.text,
                        entities      = content.entities,
                        inlineContent = renderData.inlineContent,
                        style         = MaterialTheme.typography.bodyLarge.copy(
                            fontSize      = finalFontSize.sp,
                            letterSpacing = letterSpacing.sp,
                            lineHeight    = (finalFontSize * 1.1f).sp
                        ),
                        modifier      = Modifier.padding(bottom = 2.dp),
                        isOutgoing    = isOutgoing,
                        onSpoilerClick = { index ->
                            if (revealedSpoilers.contains(index))
                                revealedSpoilers.remove(index)
                            else
                                revealedSpoilers.add(index)
                        },
                        onClick     = onClick,
                        onLongClick = onLongClick
                    )
                }

                if (showLinkPreviews) {
                    content.webPage?.let { webPage ->
                        LinkPreview(
                            webPage           = webPage,
                            isOutgoing        = msg.isOutgoing,
                            onInstantViewClick = onInstantViewClick,
                            onYouTubeClick    = onYouTubeClick
                        )
                    }
                }

                Row(
                    modifier          = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (msg.editDate > 0) {
                        Icon(
                            imageVector        = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.info_edited),
                            modifier           = Modifier.size(14.dp),
                            tint               = timeColor
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        text  = formatTime(msg.date, timeFormat),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = timeColor
                    )
                    if (isOutgoing) {
                        Spacer(Modifier.width(4.dp))
                        MessageSendingStatusIcon(
                            sendingState = msg.sendingState,
                            isRead       = msg.isRead,
                            baseColor    = timeColor,
                            size         = 14.dp
                        )
                    }
                }
            }
        }

        if (hasReactions) {
            MessageReactionsView(
                reactions       = msg.reactions,
                onReactionClick = onReactionClick,
                isOutgoing      = isOutgoing,
                modifier        = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp)
            )
        }
    }
}
