package com.spmods.spgram.presentation.features.chats.conversation.ui.message

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spmods.spgram.domain.models.MessageReactionModel
import com.spmods.spgram.presentation.core.ui.Avatar
import com.spmods.spgram.presentation.features.stickers.ui.view.StickerImage

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MessageReactionsView(
    reactions: List<MessageReactionModel>,
    onReactionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    emojiFontFamily: FontFamily = LocalMessageRenderDependencies.current.emojiFontFamily,
    customEmojiPathsById: Map<Long, String?> = LocalMessageRenderDependencies.current.customEmojiPaths
) {
    if (reactions.isEmpty()) return

    FlowRow(
        modifier = modifier.padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        reactions.forEachIndexed { index, reaction ->
            val reactionKey = reaction.emoji ?: reaction.customEmojiId ?: "unknown_$index"
            key(reactionKey) {
                MessageReactionItem(
                    reaction = reaction,
                    onReactionClick = onReactionClick,
                    emojiFontFamily = emojiFontFamily,
                    customEmojiPathsById = customEmojiPathsById
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageReactionItem(
    reaction: MessageReactionModel,
    onReactionClick: (String) -> Unit,
    emojiFontFamily: FontFamily,
    customEmojiPathsById: Map<Long, String?>
) {
    val customEmojiId = reaction.customEmojiId
    val emoji = reaction.emoji

    if (customEmojiId == null && emoji == null) return

    val isChosen = reaction.isChosen

    // Animated colors
    val backgroundColor by animateColorAsState(
        targetValue = if (isChosen) MaterialTheme.colorScheme.primaryContainer
                      else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
        animationSpec = spring(),
        label = "reactionBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isChosen) MaterialTheme.colorScheme.onPrimaryContainer
                      else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(),
        label = "reactionContent"
    )

    // Bounce scale animation on choose
    val scale by animateFloatAsState(
        targetValue = if (isChosen) 1.12f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 400f),
        label = "reactionScale"
    )

    val customEmojiPath = remember(customEmojiId, reaction.customEmojiPath, customEmojiPathsById) {
        reaction.customEmojiPath ?: customEmojiId?.let(customEmojiPathsById::get)
    }

    var showDropdown by remember { mutableStateOf(false) }
    val linkHandler = LocalLinkHandler.current

    Box(modifier = Modifier.scale(scale)) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(backgroundColor)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (customEmojiId != null) {
                StickerImage(
                    path = customEmojiPath,
                    modifier = Modifier.size(20.dp),
                    isInline = true
                )
            } else if (emoji != null) {
                Text(
                    text = emoji,
                    fontSize = 16.sp,
                    fontFamily = emojiFontFamily
                )
            }

            // Sender avatars (up to 3, shown when count <= 3)
            if (reaction.recentSenders.isNotEmpty() && reaction.count <= 3) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy((-6).dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    reaction.recentSenders.take(3).forEach { sender ->
                        Avatar(
                            path = sender.avatar,
                            name = sender.name,
                            size = 18.dp,
                            modifier = Modifier
                                .background(backgroundColor, CircleShape)
                                .padding(1.dp)
                        )
                    }
                }
            }

            // Count shown when > 3 senders or no sender info
            if (reaction.count > 3 || reaction.recentSenders.isEmpty()) {
                Text(
                    text = reaction.count.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor
                )
            }
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(50))
                .combinedClickable(
                    onClick = {
                        val reactionValue = emoji ?: customEmojiId?.toString()
                        if (reactionValue != null) onReactionClick(reactionValue)
                    },
                    onLongClick = {
                        if (reaction.recentSenders.isNotEmpty()) showDropdown = true
                    }
                )
        )

        DropdownMenu(
            expanded = showDropdown,
            onDismissRequest = { showDropdown = false },
            shape = RoundedCornerShape(16.dp)
        ) {
            reaction.recentSenders.forEach { sender ->
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Avatar(path = sender.avatar, name = sender.name, size = 28.dp)
                            Text(text = sender.name, style = MaterialTheme.typography.bodyMedium)
                        }
                    },
                    onClick = {
                        showDropdown = false
                        linkHandler("tg://user?id=${sender.id}")
                    }
                )
            }
        }
    }
}
