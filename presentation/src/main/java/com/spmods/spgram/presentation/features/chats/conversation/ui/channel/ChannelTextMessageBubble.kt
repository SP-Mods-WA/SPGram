package com.spmods.spgram.presentation.features.chats.conversation.ui.channel

import com.spmods.spgram.presentation.ui.theme.LocalDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
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

// Telegram-exact channel bubble colors
private val TgChBubbleLight = Color(0xFFFFFFFF)
private val TgChBubbleDark  = Color(0xFF182533)
private val TgChTimeLight   = Color(0x99000000)
private val TgChTimeDark    = Color(0x99FFFFFF)

// Invisible spacer appended to message text so the last line never
// overlaps the floating time badge — matches Telegram's exact approach.
// Width = eye icon(14) + gap(4) + ~"3.3K"(28) + gap(8) + time(~36) + optional tick(18) = ~108dp
// We use em-spaces (U+2003) to push the last line; actual px is handled by trailing spaces.
private const val TIME_PLACEHOLDER = "        " // 8 hair spaces ≈ time row width at 11sp

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

    // Build the views + time string that floats bottom-right inside the bubble
    val viewsStr = msg.views?.takeIf { it > 0 }?.let { formatViews(context, it) }
    val timeStr  = formatTime(msg.date, timeFormat)

    // Pixel-width of the metadata badge — used to pad last text line
    // Telegram uses ~6 en-spaces per character at 11sp; we approximate with spaces
    val metaBadgeSpaces = buildString {
        if (viewsStr != null) repeat(viewsStr.length + 3) { append('\u2002') } // eye + space + count + gap
        repeat(timeStr.length + 1) { append('\u2002') }
        if (msg.isOutgoing) repeat(3) { append('\u2002') } // tick icon
    }

    Column(
        modifier            = modifier.wrapContentWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Surface(
            shape           = bubbleShape,
            color           = bubbleColor,
            contentColor    = contentColor,
            tonalElevation  = 0.dp,
            shadowElevation = 1.dp,
            modifier        = Modifier.widthIn(min = 80.dp)
        ) {
            Column(
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)
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
                if (msg.replyToMsg == null && msg.replyToStoryId != null) {
                    val storyId = msg.replyToStoryId
                    val onStoryReplyClick = com.spmods.spgram.presentation.features.chats.conversation.ui.message.LocalStoryReplyClickHandler.current
                    com.spmods.spgram.presentation.features.chats.conversation.ui.message.StoryReplyContent(
                        isOutgoing = false,
                        senderName = msg.senderName,
                        onClick = {
                            if (storyId != null) {
                                val posterChatId = msg.replyToStoryPosterChatId ?: msg.chatId
                                onStoryReplyClick(posterChatId, storyId)
                            }
                        }
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
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                } else {
                    // Append invisible trailing spaces so last line clears the floating time badge
                    val paddedText = buildAnnotatedString {
                        append(renderData.annotatedText)
                        withStyle(SpanStyle(color = Color.Transparent)) {
                            append(metaBadgeSpaces)
                        }
                    }
                    MessageText(
                        text          = paddedText,
                        rawText       = content.text,
                        inlineContent = renderData.inlineContent,
                        style         = MaterialTheme.typography.bodyLarge.copy(
                            fontSize      = finalFontSize.sp,
                            letterSpacing = letterSpacing.sp,
                            lineHeight    = (finalFontSize * 1.35f).sp
                        ),
                        modifier      = Modifier.padding(bottom = 0.dp),
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
                            webPage            = webPage,
                            isOutgoing         = msg.isOutgoing,
                            onInstantViewClick = onInstantViewClick,
                            onYouTubeClick     = onYouTubeClick
                        )
                    }
                }

                // ── Floating metadata row pinned to bottom-right ──────────────────
                // Sits in its own Row, right-aligned, with negative top offset so it
                // visually overlaps the last line of text — exactly like Telegram.
                Row(
                    modifier          = Modifier
                        .align(Alignment.End)
                        .padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    viewsStr?.let {
                        Icon(
                            imageVector        = Icons.Outlined.Visibility,
                            contentDescription = null,
                            modifier           = Modifier.size(13.dp),
                            tint               = timeColor
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text  = it,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = timeColor
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text  = timeStr,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = timeColor
                    )
                    if (msg.isOutgoing) {
                        Spacer(Modifier.width(3.dp))
                        MessageSendingStatusIcon(
                            sendingState = msg.sendingState,
                            isRead       = msg.isRead,
                            baseColor    = timeColor,
                            size         = 13.dp
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
                modifier          = Modifier.widthIn(min = 80.dp)
            )
        }
    }
}
