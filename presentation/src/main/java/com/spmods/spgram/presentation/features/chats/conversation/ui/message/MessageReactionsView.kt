package com.spmods.spgram.presentation.features.chats.conversation.ui.message

import androidx.compose.animation.animateColorAsState
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
import com.spmods.spgram.domain.models.MessageReactionModel
import com.spmods.spgram.presentation.core.ui.Avatar
import com.spmods.spgram.presentation.features.stickers.ui.view.StickerImage

// ── Telegram-exact reaction colours (inline) ────────────────────────────────
private val TgReactionChosenBg      = Color(0xFF3390EC)  // Telegram blue — both themes
private val TgReactionChosenText    = Color.White
private val TgReactionNeutralBgL    = Color(0xFFEDEEEF)
private val TgReactionNeutralBgD    = Color(0xFF2C2C2E)
private val TgReactionNeutralBorderL = Color(0xFFDEDEDE)
private val TgReactionNeutralBorderD = Color(0xFF3A3A3C)
private val TgReactionNeutralTextL  = Color(0xFF000000)
private val TgReactionNeutralTextD  = Color(0xFFFFFFFF)

// ── Star / Paid reaction colours (gold) ────────────────────────────────────
private val TgStarReactionBg      = Color(0xFFFF9900)   // Telegram gold amber
private val TgStarReactionContent = Color.White


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MessageReactionsView(
    reactions: List<MessageReactionModel>,
    onReactionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    isOutgoing: Boolean = false,
    emojiFontFamily: FontFamily = LocalMessageRenderDependencies.current.emojiFontFamily,
    customEmojiPathsById: Map<Long, String?> = LocalMessageRenderDependencies.current.customEmojiPaths,
    showAddButton: Boolean = true,
) {
    if (reactions.isEmpty()) return

    FlowRow(
        modifier              = modifier.padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement   = Arrangement.spacedBy(4.dp)
    ) {
        // Reaction chips
        reactions.forEachIndexed { index, reaction ->
            val reactionKey = reaction.emoji ?: reaction.customEmojiId ?: "unknown_$index"
            key(reactionKey) {
                MessageReactionItem(
                    reaction             = reaction,
                    onReactionClick      = onReactionClick,
                    emojiFontFamily      = emojiFontFamily,
                    customEmojiPathsById = customEmojiPathsById
                )
            }
        }

        // [3.3] "+" add-reaction button — only for group/private, not channels
        if (showAddButton) AddReactionButton()
    }
}

// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun MessageReactionItem(
    reaction: MessageReactionModel,
    onReactionClick: (String) -> Unit,
    emojiFontFamily: FontFamily,
    customEmojiPathsById: Map<Long, String?>
) {
    val customEmojiId = reaction.customEmojiId
    val emoji         = reaction.emoji
    if (customEmojiId == null && emoji == null) return

    val isChosen = reaction.isChosen
    val isDark   = isSystemInDarkTheme()

    // [2.6] Telegram-exact colours — gold for paid/star, blue for chosen, grey for neutral
    val isPaid      = reaction.isPaid
    val bgColor     = when {
        isPaid   -> TgStarReactionBg
        isChosen -> TgReactionChosenBg
        isDark   -> TgReactionNeutralBgD
        else     -> TgReactionNeutralBgL
    }
    val borderColor = when {
        isPaid   -> Color.Transparent
        isChosen -> Color.Transparent
        isDark   -> TgReactionNeutralBorderD
        else     -> TgReactionNeutralBorderL
    }
    val textColor   = when {
        isPaid || isChosen -> TgReactionChosenContent
        isDark             -> TgReactionNeutralTextD
        else               -> TgReactionNeutralTextL
    }

    // Bounce scale animation when chosen
    val scale by animateFloatAsState(
        targetValue   = if (isChosen) 1.12f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 400f),
        label         = "reactionScale"
    )

    val customEmojiPath = remember(customEmojiId, reaction.customEmojiPath, customEmojiPathsById) {
        reaction.customEmojiPath ?: customEmojiId?.let(customEmojiPathsById::get)
    }

    // [3.4] Long-press → ModalBottomSheet (who reacted)
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val linkHandler = LocalLinkHandler.current

    Box(modifier = Modifier.scale(scale)) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(bgColor)
                .border(1.dp, borderColor, RoundedCornerShape(50))
                .combinedClickable(
                    onClick = {
                        val value = emoji ?: customEmojiId?.toString()
                        if (value != null) onReactionClick(value)
                    },
                    onLongClick = {
                        if (reaction.recentSenders.isNotEmpty()) showSheet = true
                    }
                )
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment    = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Emoji or custom sticker
            if (customEmojiId != null) {
                StickerImage(
                    path     = customEmojiPath,
                    modifier = Modifier.size(18.dp),
                    isInline = true
                )
            } else if (emoji != null) {
                Text(
                    text       = emoji,
                    fontSize   = 14.sp,
                    fontFamily = emojiFontFamily
                )
            }

            // [3.6] Count — ALWAYS shown (removed the ≤ 3 hide logic)
            Text(
                text       = reaction.count.toString(),
                style      = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp),
                fontWeight = FontWeight.SemiBold,
                color      = textColor
            )
        }
    }

    // [3.4] "Who reacted" bottom sheet
    if (showSheet && reaction.recentSenders.isNotEmpty()) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState       = sheetState
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header: emoji + total count
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (emoji != null) {
                        Text(text = emoji, fontSize = 24.sp, fontFamily = emojiFontFamily)
                    }
                    Text(
                        text  = reaction.count.toString(),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                HorizontalDivider()

                // Sender list
                reaction.recentSenders.forEach { sender ->
                    Row(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick    = {
                                    showSheet = false
                                    linkHandler("tg://user?id=${sender.id}")
                                },
                                onLongClick = {}
                            )
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Avatar(path = sender.avatar, name = sender.name, size = 36.dp)
                        Text(
                            text  = sender.name,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// [3.3] "+" add-reaction chip
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AddReactionButton(onClick: () -> Unit = {}) {
    val isDark      = isSystemInDarkTheme()
    val bgColor     = if (isDark) TgReactionNeutralBgD    else TgReactionNeutralBgL
    val borderColor = if (isDark) TgReactionNeutralBorderD else TgReactionNeutralBorderL
    val iconColor   = if (isDark) TgReactionNeutralTextD  else TgReactionNeutralTextL

    Box(
        modifier         = Modifier
            .clip(CircleShape)
            .background(bgColor)
            .border(1.dp, borderColor, CircleShape)
            .combinedClickable(onClick = onClick, onLongClick = {})
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector        = Icons.Default.Add,
            contentDescription = "Add reaction",
            modifier           = Modifier.size(15.dp),
            tint               = iconColor
        )
    }
}
