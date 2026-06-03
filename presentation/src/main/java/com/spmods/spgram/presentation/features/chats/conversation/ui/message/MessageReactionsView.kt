package com.spmods.spgram.presentation.features.chats.conversation.ui.message

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spmods.spgram.app.ui.theme.ReactionChosenBg
import com.spmods.spgram.app.ui.theme.ReactionChosenContent
import com.spmods.spgram.app.ui.theme.ReactionNeutralBgDark
import com.spmods.spgram.app.ui.theme.ReactionNeutralBgLight
import com.spmods.spgram.app.ui.theme.ReactionNeutralBorderDark
import com.spmods.spgram.app.ui.theme.ReactionNeutralBorderLight
import com.spmods.spgram.app.ui.theme.ReactionNeutralContentDark
import com.spmods.spgram.app.ui.theme.ReactionNeutralContentLight
import com.spmods.spgram.domain.models.MessageReactionModel
import com.spmods.spgram.presentation.core.ui.Avatar
import com.spmods.spgram.presentation.features.stickers.ui.view.StickerImage

// ─────────────────────────────────────────────────────────────────────────────
//  MessageReactionsView
//  Renders reaction chips that float below the bubble (rendered outside Surface
//  in TextMessageBubble via offset).
//
//  Changes vs original:
//  [3.1] Position → handled by caller (offset in TextMessageBubble)
//  [3.4] Long-press → ModalBottomSheet showing who reacted (replaces DropdownMenu)
//  [3.6] Count → ALWAYS shown after the emoji (removed the ≤3 hide logic)
//  [3.3] + button → always shown at the end of reactions row
//  [2.6] Colors → Telegram-exact (blue chosen, grey neutral)
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MessageReactionsView(
    reactions: List<MessageReactionModel>,
    onReactionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    // [3.1] isOutgoing is used by the caller for alignment — kept for API consistency
    isOutgoing: Boolean = false,
    // [3.3] Supply a callback to handle the + button tap (e.g., open emoji picker)
    onAddReactionClick: (() -> Unit)? = null,
    emojiFontFamily: FontFamily = LocalMessageRenderDependencies.current.emojiFontFamily,
    customEmojiPathsById: Map<Long, String?> = LocalMessageRenderDependencies.current.customEmojiPaths,
) {
    if (reactions.isEmpty()) return

    FlowRow(
        modifier            = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement   = Arrangement.spacedBy(4.dp),
    ) {
        // Reaction chips
        reactions.forEachIndexed { index, reaction ->
            val reactionKey = reaction.emoji ?: reaction.customEmojiId ?: "unknown_$index"
            key(reactionKey) {
                MessageReactionChip(
                    reaction             = reaction,
                    onReactionClick      = onReactionClick,
                    emojiFontFamily      = emojiFontFamily,
                    customEmojiPathsById = customEmojiPathsById,
                )
            }
        }

        // [3.3] "+" add-reaction button
        AddReactionButton(onClick = onAddReactionClick ?: {})
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Individual reaction chip
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun MessageReactionChip(
    reaction: MessageReactionModel,
    onReactionClick: (String) -> Unit,
    emojiFontFamily: FontFamily,
    customEmojiPathsById: Map<Long, String?>,
) {
    val customEmojiId = reaction.customEmojiId
    val emoji         = reaction.emoji
    if (customEmojiId == null && emoji == null) return

    val isChosen  = reaction.isChosen
    val isDark    = isSystemInDarkTheme()
    val linkHandler = LocalLinkHandler.current

    // ── [2.6] Exact Telegram reaction colours ───────────────────────────
    val bgColor = if (isChosen) ReactionChosenBg
                  else if (isDark) ReactionNeutralBgDark
                  else ReactionNeutralBgLight

    val borderColor = if (isChosen) Color.Transparent
                      else if (isDark) ReactionNeutralBorderDark
                      else ReactionNeutralBorderLight

    val textColor = if (isChosen) ReactionChosenContent
                    else if (isDark) ReactionNeutralContentDark
                    else ReactionNeutralContentLight

    // ── Bounce animation when chosen ────────────────────────────────────
    val scale by animateFloatAsState(
        targetValue  = if (isChosen) 1.10f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 380f),
        label        = "reactionScale",
    )

    val customEmojiPath = remember(customEmojiId, reaction.customEmojiPath, customEmojiPathsById) {
        reaction.customEmojiPath ?: customEmojiId?.let(customEmojiPathsById::get)
    }

    // ── [3.4] Long-press → ModalBottomSheet (who reacted) ───────────────
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Box(modifier = Modifier.scale(scale)) {
        // Chip row
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(bgColor)
                .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(50))
                .combinedClickable(
                    onClick = {
                        val value = emoji ?: customEmojiId?.toString()
                        if (value != null) onReactionClick(value)
                    },
                    onLongClick = {
                        if (reaction.recentSenders.isNotEmpty()) showSheet = true
                    },
                )
                .padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment    = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Emoji or custom sticker
            if (customEmojiId != null) {
                StickerImage(
                    path     = customEmojiPath,
                    modifier = Modifier.size(18.dp),
                    isInline = true,
                )
            } else if (emoji != null) {
                Text(
                    text       = emoji,
                    fontSize   = 15.sp,
                    fontFamily = emojiFontFamily,
                )
            }

            // [3.6] Count — ALWAYS shown (removed ≤3 hide logic)
            Text(
                text       = reaction.count.toString(),
                style      = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp),
                fontWeight = FontWeight.SemiBold,
                color      = textColor,
            )
        }
    }

    // ── [3.4] "Who reacted" bottom sheet ────────────────────────────────
    if (showSheet && reaction.recentSenders.isNotEmpty()) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState       = sheetState,
        ) {
            // Sheet header: emoji + count
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment    = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (emoji != null) {
                        Text(text = emoji, fontSize = 24.sp, fontFamily = emojiFontFamily)
                    }
                    Text(
                        text  = reaction.count.toString(),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                HorizontalDivider()

                // Sender list
                reaction.recentSenders.forEach { sender ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick    = {
                                    showSheet = false
                                    linkHandler("tg://user?id=${sender.id}")
                                },
                                onLongClick = {},
                            )
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment    = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Avatar(path = sender.avatar, name = sender.name, size = 36.dp)
                        Text(
                            text  = sender.name,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }

                // Bottom safe-area spacing
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  [3.3] "+" add-reaction button
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AddReactionButton(onClick: () -> Unit) {
    val isDark      = isSystemInDarkTheme()
    val bgColor     = if (isDark) ReactionNeutralBgDark  else ReactionNeutralBgLight
    val borderColor = if (isDark) ReactionNeutralBorderDark else ReactionNeutralBorderLight
    val iconColor   = if (isDark) ReactionNeutralContentDark else ReactionNeutralContentLight

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(bgColor)
            .border(width = 1.dp, color = borderColor, shape = CircleShape)
            .combinedClickable(onClick = onClick, onLongClick = {})
            .padding(horizontal = 8.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector        = Icons.Default.Add,
            contentDescription = "Add reaction",
            modifier           = Modifier.size(16.dp),
            tint               = iconColor,
        )
    }
}
