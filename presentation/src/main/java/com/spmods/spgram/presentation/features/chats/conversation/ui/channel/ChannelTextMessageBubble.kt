package com.spmods.spgram.presentation.features.chats.conversation.ui.channel

import com.spmods.spgram.presentation.ui.theme.LocalDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.koinInject
import com.spmods.spgram.domain.models.ForwardInfo
import com.spmods.spgram.domain.models.MessageContent
import com.spmods.spgram.domain.models.MessageModel
import com.spmods.spgram.presentation.core.util.DateFormatManager
import com.spmods.spgram.presentation.features.chats.conversation.ui.TelegramBubbleShape
import com.spmods.spgram.presentation.features.chats.conversation.ui.message.BigEmojiContent
import com.spmods.spgram.presentation.features.chats.conversation.ui.message.ForwardContent
import com.spmods.spgram.presentation.features.chats.conversation.ui.message.LinkPreview
import com.spmods.spgram.presentation.features.chats.conversation.ui.message.MessageReactionsView
import com.spmods.spgram.presentation.features.chats.conversation.ui.message.MessageSendingStatusIcon
import com.spmods.spgram.presentation.features.chats.conversation.ui.message.MessageText
import com.spmods.spgram.presentation.features.chats.conversation.ui.message.ReplyContent
import com.spmods.spgram.presentation.features.chats.conversation.ui.message.rememberMessageTextRenderData

// Telegram-exact channel bubble colors (inline)
private val TgChBubbleLight = Color(0xFFFFFFFF)
private val TgChBubbleDark  = Color(0xFF182533)
private val TgChTimeLight   = Color(0x99000000)
private val TgChTimeDark    = Color(0x99FFFFFF)

@Composable
fun ChannelTextMessageBubble(
    content: MessageContent.Text,
    msg: MessageModel,
    isSameSenderAbove: Boolean = false,
    isSameSenderBelow: Boolean = false,
    fontSize: Float,
    letterSpacing: Float,
    bubbleRadius: Float,
    showLinkPreviews: Boolean = true,
    onReplyClick: (MessageModel) -> Unit = {},
    onReactionClick: (String) -> Unit = {},
    onInstantViewClick: ((String) -> Unit)? = null,
    onYouTubeClick: ((String) -> Unit)? = null,
    onClick: (Offset) -> Unit = {},
    onLongClick: (Offset) -> Unit = {},
    onCommentsClick: (Long) -> Unit = {},
    showComments: Boolean = true,
    showReactions: Boolean = true,
    toProfile: (Long) -> Unit = {},
    onForwardOriginClick: (ForwardInfo) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context  = LocalContext.current
    val isDark   = LocalDarkTheme.current

    val bubbleColor  = if (isDark) TgChBubbleDark  else TgChBubbleLight
    val contentColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000)
    val timeColor    = if (isDark) TgChTimeDark     else TgChTimeLight

    // TelegramBubbleShape — channel posts are incoming (isOutgoing = false)
    val bubbleShape = remember(isSameSenderAbove, isSameSenderBelow, bubbleRadius) {
        TelegramBubbleShape(
            isOutgoing        = false,
            hasTail           = !isSameSenderBelow,
            isSameSenderAbove = isSameSenderAbove,
            cornerRadius      = bubbleRadius.dp,
            smallCorner       = 6.dp,
        )
    }

    val dateFormatManager: DateFormatManager = koinInject()
    val timeFormat = dateFormatManager.getHourMinuteFormat()

    val revealedSpoilers = remember { mutableStateListOf<Int>() }
    val hasReactions     = showReactions && msg.reactions.isNotEmpty()

    Column(
        modifier            = modifier.widthIn(min = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape           = bubbleShape,
            color           = bubbleColor,
            contentColor    = contentColor,
            tonalElevation  = 0.dp,
            shadowElevation = 1.dp,
            modifier        = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 6.dp)
            ) {
                msg.forwardInfo?.let { forward ->
                    ForwardContent(forward, false, onForwardClick = onForwardOriginClick)
                }
                msg.replyToMsg?.let { reply ->
                    ReplyContent(
                        replyToMsg = reply,
                        isOutgoing = false,
                        onClick    = { onReplyClick(reply) }
                    )
                }

                val renderData = rememberMessageTextRenderData(
                    text             = content.text,
                    entities         = content.entities,
                    isOutgoing       = false,
                    revealedSpoilers = revealedSpoilers,
                    fontSize         = fontSize
                )
                val finalFontSize = if (renderData.isBigEmoji) fontSize * 5f else fontSize

                if (renderData.isBigEmoji && renderData.bigEmojiItems.isNotEmpty()) {
                    BigEmojiContent(
                        items    = renderData.bigEmojiItems,
                        sizeDp   = finalFontSize,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp)
                    )
                } else {
                    MessageText(
                        text          = renderData.annotatedText,
                        rawText       = content.text,
                        inlineContent = renderData.inlineContent,
                        style         = MaterialTheme.typography.bodyLarge.copy(
                            fontSize      = finalFontSize.sp,
                            letterSpacing = letterSpacing.sp,
                            lineHeight    = (finalFontSize * 1.1f).sp
                        ),
                        modifier      = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                        onSpoilerClick = { index ->
                            if (revealedSpoilers.contains(index)) revealedSpoilers.remove(index)
                            else revealedSpoilers.add(index)
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

                // Metadata row: views + time + sending status
                Row(
                    modifier          = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    msg.views?.let { viewsCount ->
                        if (viewsCount > 0) {
                            Icon(
                                imageVector        = Icons.Outlined.Visibility,
                                contentDescription = null,
                                modifier           = Modifier.size(14.dp),
                                tint               = timeColor
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text  = formatViews(context, viewsCount),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = timeColor
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                    }
                    Text(
                        text  = formatTime(msg.date, timeFormat),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = timeColor
                    )
                    if (msg.isOutgoing) {
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
                modifier        = Modifier
                    .padding(top = 4.dp, start = 4.dp, end = 4.dp)
                    .align(Alignment.Start)
            )
        }

        if (showComments && msg.canGetMessageThread) {
            ChannelCommentsButton(
                replyCount        = msg.replyCount,
                bubbleRadius      = bubbleRadius,
                isSameSenderBelow = isSameSenderBelow,
                onClick           = { onCommentsClick(msg.id) },
                modifier          = Modifier.fillMaxWidth()
            )
        }
    }
}
