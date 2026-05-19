package com.spmods.spgram.presentation.features.chats.conversation.ui.channel

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import com.spmods.spgram.domain.models.ForwardInfo
import com.spmods.spgram.domain.models.MessageContent
import com.spmods.spgram.domain.models.MessageModel
import com.spmods.spgram.presentation.features.chats.conversation.ui.message.PollMessageBubble

@Composable
fun ChannelPollMessageBubble(
    content: MessageContent.Poll,
    msg: MessageModel,
    isSameSenderAbove: Boolean,
    isSameSenderBelow: Boolean,
    fontSize: Float,
    letterSpacing: Float,
    bubbleRadius: Float = 18f,
    onOptionClick: (Int) -> Unit,
    onRetractVote: () -> Unit = {},
    onReplyClick: (MessageModel) -> Unit = {},
    onReactionClick: (String) -> Unit = {},
    onShowVoters: (Int) -> Unit = {},
    onClosePoll: () -> Unit = {},
    onLongClick: (Offset) -> Unit = {},
    onCommentsClick: (Long) -> Unit = {},
    showComments: Boolean = true,
    toProfile: (Long) -> Unit = {},
    onForwardOriginClick: (ForwardInfo) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        PollMessageBubble(
            content = content,
            msg = msg,
            isOutgoing = false,
            isSameSenderAbove = isSameSenderAbove,
            isSameSenderBelow = isSameSenderBelow,
            fontSize = fontSize,
            letterSpacing = letterSpacing,
            bubbleRadius = bubbleRadius,
            onOptionClick = onOptionClick,
            onRetractVote = onRetractVote,
            onReplyClick = onReplyClick,
            onReactionClick = onReactionClick,
            onShowVoters = onShowVoters,
            onClosePoll = onClosePoll,
            onLongClick = onLongClick,
            hasCommentsButton = showComments && msg.canGetMessageThread,
            toProfile = toProfile,
            onForwardOriginClick = onForwardOriginClick,
            modifier = Modifier.fillMaxWidth()
        )

        if (showComments && msg.canGetMessageThread) {
            ChannelCommentsButton(
                replyCount = msg.replyCount,
                bubbleRadius = bubbleRadius,
                isSameSenderBelow = isSameSenderBelow,
                onClick = { onCommentsClick(msg.id) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
