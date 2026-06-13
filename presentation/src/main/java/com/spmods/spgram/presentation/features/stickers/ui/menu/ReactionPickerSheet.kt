package com.spmods.spgram.presentation.features.stickers.ui.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.koinInject
import com.spmods.spgram.domain.models.StickerModel
import com.spmods.spgram.domain.repository.EmojiRepository
import com.spmods.spgram.presentation.core.util.AppPreferences
import com.spmods.spgram.presentation.features.chats.conversation.ui.message.getEmojiFontFamily
import com.spmods.spgram.presentation.features.stickers.ui.view.StickerImage

/**
 * Full-screen reaction picker sheet — shown when the ▾ dropdown is tapped in the
 * reaction pill. Matches original Telegram behaviour: all available reactions in a
 * scrollable grid (8 columns), animated sticker for each emoji.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReactionPickerSheet(
    availableReactions: List<String>,
    chosenReactions: Set<String> = emptySet(),
    onReaction: (String) -> Unit,
    onDismiss: () -> Unit,
    emojiRepository: EmojiRepository = koinInject(),
    appPreferences: AppPreferences = koinInject()
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val emojiStyle by appPreferences.emojiStyle.collectAsState()
    val emojiFontFamily = remember(context, emojiStyle) { getEmojiFontFamily(context, emojiStyle) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    // Load animated stickers for all reactions
    var reactionStickers by remember(availableReactions) {
        mutableStateOf<Map<String, StickerModel>>(emptyMap())
    }
    LaunchedEffect(availableReactions) {
        val loaded = mutableMapOf<String, StickerModel>()
        availableReactions.forEach { emoji ->
            val sticker = emojiRepository.getReactionSticker(emoji)
            if (sticker != null) {
                loaded[emoji] = sticker
                reactionStickers = loaded.toMap()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.height(4.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(8),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(availableReactions, key = { it }) { emoji ->
                    val isChosen = emoji in chosenReactions
                    val bgColor = if (isChosen)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f)

                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .background(bgColor)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onReaction(emoji)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val sticker = reactionStickers[emoji]
                        if (sticker != null) {
                            StickerImage(
                                path = sticker.path,
                                modifier = Modifier.size(32.dp),
                                animate = true
                            )
                        } else {
                            Text(
                                text = emoji,
                                fontSize = 24.sp,
                                fontFamily = emojiFontFamily
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
