package com.spmods.spgram.presentation.features.chats.personal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spmods.spgram.domain.models.ChatType
import com.spmods.spgram.presentation.features.chats.conversation.ui.message.getEmojiFontFamily
import com.spmods.spgram.presentation.features.chats.list.ChatListComponent
import com.spmods.spgram.presentation.features.chats.list.components.ChatListItem
import com.spmods.spgram.presentation.features.chats.list.components.EmptyStateView

/**
 * Chats tab — shows only personal/private chats (PRIVATE and SECRET types).
 * Bot chats are excluded (isBot=true).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalChatsContent(component: ChatListComponent) {
    val foldersState by component.foldersState.collectAsState()
    val selectionState by component.selectionState.collectAsState()
    val uiState by component.uiState.collectAsState()

    val context = LocalContext.current
    val emojiStyle by component.appPreferences.emojiStyle.collectAsState()
    val emojiFontFamily = remember(context, emojiStyle) { getEmojiFontFamily(context, emojiStyle) }
    val messageLines by component.appPreferences.chatListMessageLines.collectAsState()
    val showPhotos by component.appPreferences.showChatListPhotos.collectAsState()

    // Personal chats only: PRIVATE or SECRET, excluding bots
    val personalChats = remember(foldersState.chatsByFolder) {
        foldersState.chatsByFolder.values
            .flatten()
            .distinctBy { it.id }
            .filter { chat ->
                (chat.type == ChatType.PRIVATE || chat.type == ChatType.SECRET) && !chat.isBot
            }
            .sortedByDescending { it.order }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Chats",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                windowInsets = WindowInsets.statusBars,
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { innerPadding ->
        if (personalChats.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                EmptyStateView()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding() + 80.dp,
                ),
            ) {
                items(items = personalChats, key = { it.id }) { chat ->
                    ChatListItem(
                        chat = chat,
                        currentUserId = uiState.currentUser?.id,
                        isSelected = chat.id in selectionState.selectedChatIds,
                        onClick = { component.onChatClicked(chat.id) },
                        onLongClick = { component.onChatLongClicked(chat.id) },
                        emojiFontFamily = emojiFontFamily,
                        messageLines = messageLines,
                        showPhotos = showPhotos,
                    )
                }
            }
        }
    }
}
