package com.spmods.spgram.presentation.features.calls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CallMade
import androidx.compose.material.icons.rounded.CallMissed
import androidx.compose.material.icons.rounded.CallReceived
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spmods.spgram.core.date.toDate
import com.spmods.spgram.domain.models.ChatModel
import com.spmods.spgram.presentation.core.ui.AvatarForChat
import com.spmods.spgram.presentation.core.util.DateFormatManager
import com.spmods.spgram.presentation.core.util.toShortRelativeDate
import com.spmods.spgram.presentation.features.chats.list.ChatListComponent
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallsContent(
    component: ChatListComponent,
) {
    val state by component.state.collectAsState()
    val timeFormat = koinInject<DateFormatManager>().use24HourFormat

    // Filter all loaded chats where last message was a call
    val callChats = remember(state.chatsByFolder) {
        state.chatsByFolder
            .values
            .flatten()
            .distinctBy { it.id }
            .filter { it.lastMessageContentType == "call" }
            .sortedByDescending { it.lastMessageDate }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Calls",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                modifier = Modifier.statusBarsPadding()
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { padding ->
        if (callChats.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CallReceived,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Text(
                        text = "No recent calls",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(callChats, key = { it.id }) { chat ->
                    CallItem(
                        chat = chat,
                        timeFormat = timeFormat,
                    )
                }
            }
        }
    }
}

@Composable
private fun CallItem(
    chat: ChatModel,
    timeFormat: Boolean,
) {
    // Determine call direction from last message text
    // lastMessageText contains formatted call info from ServiceMessageFormatter
    val isMissed = chat.lastMessageText.contains("missed", ignoreCase = true) ||
                   chat.lastMessageText.contains("declined", ignoreCase = true)
    val isOutgoing = chat.isLastMessageOutgoing

    val callIcon = when {
        isMissed   -> Icons.Rounded.CallMissed
        isOutgoing -> Icons.Rounded.CallMade
        else       -> Icons.Rounded.CallReceived
    }
    val callIconTint = when {
        isMissed -> MaterialTheme.colorScheme.error
        else     -> MaterialTheme.colorScheme.primary
    }

    val timeStr = remember(chat.lastMessageDate) {
        chat.lastMessageDate.toDate().toShortRelativeDate(timeFormat)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Avatar
        AvatarForChat(
            path          = chat.avatarPath,
            fallbackPath  = chat.personalAvatarPath,
            name          = chat.title,
            size          = 52.dp,
            isOnline      = chat.isOnline,
        )

        Spacer(Modifier.width(14.dp))

        // Name + call info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text     = chat.title,
                style    = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color    = if (isMissed) MaterialTheme.colorScheme.error
                           else MaterialTheme.colorScheme.onSurface,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = callIcon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = callIconTint,
                )
                Text(
                    text  = chat.lastMessageText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        // Time
        Text(
            text  = timeStr,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
            color = if (isMissed) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
