package com.spmods.spgram.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.spmods.spgram.engine.TelegramManager
import com.spmods.spgram.ui.components.ChatListItem
import com.spmods.spgram.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    manager: TelegramManager,
    isDark: Boolean,
    onToggleTheme: () -> Unit
) {
    val chatIds     by manager.chatIds.collectAsState()
    val chats       by manager.chats.collectAsState()
    val myName      by manager.myName.collectAsState()
    val myPhotoPath by manager.myPhotoPath.collectAsState()

    var openChatId by remember { mutableStateOf<Long?>(null) }

    // Navigate to chat screen
    if (openChatId != null) {
        ChatScreen(
            chatId  = openChatId!!,
            manager = manager,
            onBack  = { openChatId = null }
        )
        return
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text("SPGram", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = OnBackground) },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = OnSurfaceVar)
                    }
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme",
                            tint = OnSurfaceVar
                        )
                    }
                    Box(
                        modifier = Modifier.padding(end = 8.dp).size(36.dp).clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (myPhotoPath != null) {
                            AsyncImage(
                                model = myPhotoPath,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize().background(Primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(myName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {},
                containerColor = SurfaceVar,
                contentColor = OnBackground,
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Default.Edit, contentDescription = "New Chat") },
                text = { Text("New Chat") }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item { ArchivedChatsRow() }

            if (chatIds.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillParentMaxSize().padding(top = 80.dp),
                        contentAlignment = Alignment.TopCenter
                    ) { CircularProgressIndicator(color = Primary) }
                }
            } else {
                items(chatIds, key = { it }) { id ->
                    val chat = chats[id]
                    if (chat != null) {
                        ChatListItem(
                            chat    = chat,
                            manager = manager,
                            onClick = { openChatId = id }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 82.dp),
                            color = Divider,
                            thickness = 0.5.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchivedChatsRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(ArchiveBg)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(46.dp).clip(CircleShape).background(SurfaceVar),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Archive, contentDescription = null, tint = OnSurfaceVar, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("Archived Chats", fontWeight = FontWeight.SemiBold, color = OnBackground, fontSize = 15.sp)
            Text("Hidden from the main list", color = OnSurfaceVar, fontSize = 13.sp)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = OnSurfaceVar)
    }
}
