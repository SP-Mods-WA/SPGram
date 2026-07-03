package com.spmods.spgram.presentation.features.chats.conversation.ui.message

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.CallMissedOutgoing
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spmods.spgram.domain.models.CallDiscardReason
import com.spmods.spgram.domain.models.ForwardInfo
import com.spmods.spgram.domain.models.MessageContent
import com.spmods.spgram.domain.models.MessageModel
import com.spmods.spgram.presentation.ui.theme.LocalDarkTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CallMessageBubble(
    content: MessageContent.Call,
    msg: MessageModel,
    isOutgoing: Boolean,
    isSameSenderAbove: Boolean,
    isSameSenderBelow: Boolean,
    bubbleRadius: Float,
    isGroup: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onReplyClick: (MessageModel) -> Unit = {},
    onReactionClick: (String) -> Unit = {},
    toProfile: (Long) -> Unit = {},
    onForwardOriginClick: (ForwardInfo) -> Unit = {}
) {
    // ── Bubble shape ──────────────────────────────────────────────────────────
    val cornerRadius = bubbleRadius.dp
    val smallCorner  = (bubbleRadius / 4f).coerceAtLeast(4f).dp
    val tailCorner   = 0.dp

    val bubbleShape = RoundedCornerShape(
        topStart    = if (!isOutgoing && isSameSenderAbove) smallCorner else cornerRadius,
        topEnd      = if ( isOutgoing && isSameSenderAbove) smallCorner else cornerRadius,
        bottomStart = if (!isOutgoing) (if (isSameSenderBelow) smallCorner else tailCorner) else cornerRadius,
        bottomEnd   = if ( isOutgoing) (if (isSameSenderBelow) smallCorner else tailCorner) else cornerRadius
    )

    // ── Colors ────────────────────────────────────────────────────────────────
    val isDark          = LocalDarkTheme.current
    val backgroundColor = if (isOutgoing) Color(0xFFEEFFDE) else Color(0xFFFFFFFF)
    val contentColor    = if (isDark) Color(0xFFFFFFFF) else Color(0xFF212121)
    val timeColor       = contentColor.copy(alpha = 0.6f)

    // ── Call semantics ────────────────────────────────────────────────────────
    // Determine type for label, icon and icon tint
    // Incoming:  MISSED = missed incoming | DECLINED = declined by receiver (we didn't answer) |
    //            answered = HUNG_UP / DISCONNECTED / EMPTY with duration > 0
    // Outgoing:  DECLINED = we cancelled before answer | answered = HUNG_UP / DISCONNECTED / EMPTY
    data class CallStyle(
        val label: String,
        val icon: ImageVector,
        val iconTint: Color
    )

    val style: CallStyle = remember(content) {
        val green  = Color(0xFF4CAF50)
        val red    = Color(0xFFF44336)
        val orange = Color(0xFFFF9800)

        if (content.isVideo) {
            when {
                !isOutgoing && (content.discardReason.isMissed || content.discardReason.isDeclined) ->
                    CallStyle("Missed video call", Icons.Default.VideocamOff, red)
                !isOutgoing && content.duration > 0 ->
                    CallStyle("Incoming video call", Icons.Default.CallReceived, green)
                !isOutgoing ->
                    CallStyle("Incoming video call", Icons.Default.CallReceived, green)
                isOutgoing && content.discardReason.isDeclined ->
                    CallStyle("Cancelled video call", Icons.Default.CallMissedOutgoing, orange)
                isOutgoing && content.duration > 0 ->
                    CallStyle("Outgoing video call", Icons.Default.CallMade, green)
                else ->
                    CallStyle("Outgoing video call", Icons.Default.CallMade, green)
            }
        } else {
            when {
                !isOutgoing && (content.discardReason.isMissed || content.discardReason.isDeclined) ->
                    CallStyle("Missed voice call", Icons.Default.CallMissed, red)
                !isOutgoing && content.duration > 0 ->
                    CallStyle("Incoming voice call", Icons.Default.CallReceived, green)
                !isOutgoing ->
                    CallStyle("Incoming voice call", Icons.Default.CallReceived, green)
                isOutgoing && content.discardReason.isDeclined ->
                    CallStyle("Cancelled voice call", Icons.Default.CallMissedOutgoing, orange)
                isOutgoing && content.duration > 0 ->
                    CallStyle("Outgoing voice call", Icons.Default.CallMade, green)
                else ->
                    CallStyle("Outgoing voice call", Icons.Default.CallMade, green)
            }
        }
    }

    // Format call duration  e.g. "2:34"
    val durationText = remember(content.duration) {
        if (content.duration > 0) formatCallDuration(content.duration) else null
    }

    // Format message time  e.g. "11:42 AM"
    val timeText = remember(msg.date) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(msg.date * 1000L))
    }

    // ── Layout ────────────────────────────────────────────────────────────────
    Column(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .widthIn(min = 200.dp, max = 280.dp),
        horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape           = bubbleShape,
            color           = backgroundColor,
            contentColor    = contentColor,
            tonalElevation  = 0.dp,
            modifier        = Modifier.combinedClickable(
                onClick     = onClick,
                onLongClick = onLongClick
            )
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {

                // Group sender name
                if (isGroup && !isOutgoing && !isSameSenderAbove) {
                    MessageSenderName(msg, toProfile = toProfile)
                }

                // Forward info
                msg.forwardInfo?.let { forward ->
                    ForwardHeader(forward, onForwardOriginClick)
                }

                // Reply
                msg.replyToMsg?.let { reply ->
                    ReplyPreview(
                        replyMsg   = reply,
                        onReplyClick = { onReplyClick(reply) }
                    )
                }

                // ── Call row ──────────────────────────────────────────────────
                Row(
                    verticalAlignment   = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Icon circle
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(style.iconTint.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector        = style.icon,
                            contentDescription = null,
                            tint               = style.iconTint,
                            modifier           = Modifier.size(18.dp)
                        )
                    }

                    // Labels column
                    Column {
                        Text(
                            text       = style.label,
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 14.sp,
                            color      = contentColor
                        )
                        durationText?.let {
                            Text(
                                text     = it,
                                fontSize = 12.sp,
                                color    = contentColor.copy(alpha = 0.65f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Time
                    Text(
                        text     = timeText,
                        fontSize = 11.sp,
                        color    = timeColor
                    )
                }
            }
        }

        // Reactions
        if (msg.reactions.isNotEmpty()) {
            MessageReactionsRow(
                reactions    = msg.reactions,
                onReactionClick = onReactionClick,
                isOutgoing   = isOutgoing
            )
        }
    }
}

/** Format seconds → "m:ss" or "h:mm:ss" */
private fun formatCallDuration(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) {
        "%d:%02d:%02d".format(h, m, s)
    } else {
        "%d:%02d".format(m, s)
    }
}
