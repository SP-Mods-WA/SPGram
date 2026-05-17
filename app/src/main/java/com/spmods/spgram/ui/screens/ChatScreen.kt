package com.spmods.spgram.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.spmods.spgram.engine.TelegramManager
import com.spmods.spgram.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: Long,
    manager: TelegramManager,
    onBack: () -> Unit
) {
    val chats           by manager.chats.collectAsState()
    val allMessages     by manager.messages.collectAsState()
    val typingStatus    by manager.typingStatus.collectAsState()
    val outboxReadId    by manager.outboxReadId.collectAsState()
    val downloadedFiles by manager.downloadedFiles.collectAsState()

    val chat     = chats[chatId]
    val messages = allMessages[chatId] ?: emptyList()
    val typing   = typingStatus[chatId]
    val lastRead = outboxReadId[chatId] ?: 0L
    val myId     = manager.getMyUserId()

    var inputText      by remember { mutableStateOf("") }
    var replyToMessage by remember { mutableStateOf<TdApi.Message?>(null) }

    // Undo delete state
    var pendingDeleteId  by remember { mutableStateOf<Long?>(null) }
    var pendingDeleteAll by remember { mutableStateOf(false) }
    var undoCountdown    by remember { mutableIntStateOf(5) }

    val listState = rememberLazyListState()
    val scope     = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(chatId) { manager.openChat(chatId) }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty())
            scope.launch { listState.animateScrollToItem(messages.size - 1) }
    }
    DisposableEffect(chatId) { onDispose { manager.closeChat(chatId) } }

    // Undo countdown timer
    LaunchedEffect(pendingDeleteId) {
        val id = pendingDeleteId ?: return@LaunchedEffect
        undoCountdown = 5
        repeat(5) {
            delay(1000)
            undoCountdown--
        }
        // Time's up — do actual delete
        if (pendingDeleteId == id) {
            manager.deleteMessage(chatId, id, pendingDeleteAll)
            pendingDeleteId = null
        }
    }

    Scaffold(
        containerColor = Background,
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        topBar = {
            ChatTopBar(
                chat            = chat,
                typing          = typing,
                downloadedFiles = downloadedFiles,
                manager         = manager,
                onBack          = { manager.closeChat(chatId); onBack() }
            )
        },
        bottomBar = {
            ChatInputBar(
                text          = inputText,
                replyTo       = replyToMessage,
                onTextChange  = { inputText = it },
                onSend        = {
                    if (inputText.isNotBlank()) {
                        manager.sendMessage(chatId, inputText.trim(), replyToMessage?.id ?: 0L)
                        inputText      = ""
                        replyToMessage = null
                    }
                },
                onCancelReply = { replyToMessage = null }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                state   = listState,
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    val isMe   = message.senderId.let {
                        it is TdApi.MessageSenderUser && it.userId == myId
                    }
                    val isRead = isMe && message.id <= lastRead
                    val isPendingDelete = message.id == pendingDeleteId

                    SwipeableMessageBubble(
                        message         = message,
                        isMe            = isMe,
                        isRead          = isRead,
                        isPendingDelete = isPendingDelete,
                        undoCountdown   = undoCountdown,
                        allMessages     = messages,
                        downloadedFiles = downloadedFiles,
                        manager         = manager,
                        onSwipeReply    = { replyToMessage = message },
                        onDeleteRequest = { forAll ->
                            pendingDeleteId  = message.id
                            pendingDeleteAll = forAll
                        },
                        onUndoDelete    = { pendingDeleteId = null }
                    )
                }
            }
        }
    }
}

// ── Swipeable Message Bubble ─────────────────────────────────
@Composable
private fun SwipeableMessageBubble(
    message: TdApi.Message,
    isMe: Boolean,
    isRead: Boolean,
    isPendingDelete: Boolean,
    undoCountdown: Int,
    allMessages: List<TdApi.Message>,
    downloadedFiles: Map<Int, String>,
    manager: TelegramManager,
    onSwipeReply: () -> Unit,
    onDeleteRequest: (Boolean) -> Unit,
    onUndoDelete: () -> Unit
) {
    var offsetX        by remember { mutableFloatStateOf(0f) }
    var showContextMenu by remember { mutableStateOf(false) }
    val animOffset      = animateFloatAsState(
        targetValue    = offsetX,
        animationSpec  = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label          = "swipe"
    )
    val swipeThreshold = 80f

    // Reply icon alpha — appears as you swipe
    val replyAlpha = (offsetX / swipeThreshold).coerceIn(0f, 1f)

    if (showContextMenu) {
        MessageContextMenu(
            message      = message,
            myId         = manager.getMyUserId(),
            onReply      = { onSwipeReply(); showContextMenu = false },
            onDelete     = { forAll -> onDeleteRequest(forAll); showContextMenu = false },
            onDismiss    = { showContextMenu = false }
        )
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        // Reply icon hint (behind bubble)
        if (replyAlpha > 0f && isMe.not()) {
            Icon(
                Icons.Default.Reply,
                contentDescription = null,
                tint   = Primary.copy(alpha = replyAlpha),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp)
                    .size(24.dp)
            )
        }
        if (replyAlpha > 0f && isMe) {
            Icon(
                Icons.Default.Reply,
                contentDescription = null,
                tint   = Primary.copy(alpha = replyAlpha),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
                    .size(24.dp)
            )
        }

        // Bubble with swipe + long press
        Box(
            modifier = Modifier
                .offset { IntOffset(animOffset.value.toInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (kotlin.math.abs(offsetX) >= swipeThreshold) {
                                onSwipeReply()
                            }
                            offsetX = 0f
                        },
                        onDragCancel = { offsetX = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            // Right swipe for received, left swipe for sent
                            if (!isMe && dragAmount > 0) {
                                offsetX = (offsetX + dragAmount).coerceIn(0f, 100f)
                            } else if (isMe && dragAmount < 0) {
                                offsetX = (offsetX + dragAmount).coerceIn(-100f, 0f)
                            }
                        }
                    )
                }
        ) {
            if (isPendingDelete) {
                // Show undo bar instead of bubble
                UndoDeleteBar(countdown = undoCountdown, onUndo = onUndoDelete, isMe = isMe)
            } else {
                MessageBubble(
                    message         = message,
                    isMe            = isMe,
                    isRead          = isRead,
                    allMessages     = allMessages,
                    downloadedFiles = downloadedFiles,
                    manager         = manager,
                    onLongPress     = { showContextMenu = true }
                )
            }
        }
    }
}

// ── Undo Delete Bar ───────────────────────────────────────────
@Composable
private fun UndoDeleteBar(countdown: Int, onUndo: () -> Unit, isMe: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (LocalDarkTheme.current) Color(0xFF2A2A2A) else Color(0xFFEEEEEE))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Icon(Icons.Default.Delete, contentDescription = null, tint = OnSurfaceVar, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text("Message deleted", color = OnSurfaceVar, fontSize = 13.sp, modifier = Modifier.weight(1f))
        TextButton(onClick = onUndo) {
            Text("UNDO ($countdown)", color = Primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

// ── Top Bar ───────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopBar(
    chat: TdApi.Chat?,
    typing: String?,
    downloadedFiles: Map<Int, String>,
    manager: TelegramManager,
    onBack: () -> Unit
) {
    val photoFile = chat?.photo?.small
    val photoPath: String? = when {
        photoFile == null                      -> null
        photoFile.local.isDownloadingCompleted -> photoFile.local.path
        downloadedFiles[photoFile.id] != null  -> downloadedFiles[photoFile.id]
        else -> { manager.downloadFile(photoFile.id); null }
    }

    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = OnBackground)
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(38.dp).clip(CircleShape), contentAlignment = Alignment.Center) {
                    if (photoPath != null) {
                        AsyncImage(
                            model = photoPath, contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize().background(Primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                chat?.title?.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp
                            )
                        }
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(chat?.title ?: "", color = OnBackground, fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    AnimatedVisibility(visible = typing != null) {
                        Text(typing ?: "", color = Primary, fontSize = 12.sp)
                    }
                }
            }
        },
        actions = {
            IconButton(onClick = {}) {
                Icon(Icons.Default.MoreVert, contentDescription = "More", tint = OnSurfaceVar)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
    )
}

// ── Message Bubble ────────────────────────────────────────────
@Composable
private fun MessageBubble(
    message: TdApi.Message,
    isMe: Boolean,
    isRead: Boolean,
    allMessages: List<TdApi.Message>,
    downloadedFiles: Map<Int, String>,
    manager: TelegramManager,
    onLongPress: () -> Unit
) {
    val bubbleColor = if (isMe)
        if (LocalDarkTheme.current) Color(0xFF1E3A5F) else Color(0xFFDCF8C6)
    else
        if (LocalDarkTheme.current) DarkSurfaceVar else LightSurfaceVar

    val textColor = if (LocalDarkTheme.current) Color(0xFFE8E8E8) else Color(0xFF111111)
    val alignment = if (isMe) Alignment.End else Alignment.Start
    val shape     = if (isMe) RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
                    else       RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)

    val replyTo = if (message.replyTo is TdApi.MessageReplyToMessage) {
        val r = message.replyTo as TdApi.MessageReplyToMessage
        allMessages.firstOrNull { it.id == r.messageId }
    } else null

    val timeStr = remember(message.date) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.date * 1000L))
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(shape)
                .background(bubbleColor)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { onLongPress() }
                    )
                }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Column {
                if (replyTo != null) { ReplyPreview(replyTo); Spacer(Modifier.height(4.dp)) }
                MessageContent(message, downloadedFiles, manager, textColor)
                Row(
                    modifier = Modifier.align(Alignment.End).padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(timeStr, color = OnSurfaceVar, fontSize = 11.sp)
                    if (isMe) { Spacer(Modifier.width(3.dp)); ReadReceipt(message, isRead) }
                }
            }
        }
    }
}

// ── Read Receipt (WhatsApp style) ─────────────────────────────
@Composable
private fun ReadReceipt(message: TdApi.Message, isRead: Boolean) {
    val sending = message.sendingState
    when {
        sending?.constructor == TdApi.MessageSendingStatePending.CONSTRUCTOR ->
            Icon(Icons.Default.Schedule, contentDescription = null, tint = OnSurfaceVar, modifier = Modifier.size(14.dp))
        sending?.constructor == TdApi.MessageSendingStateFailed.CONSTRUCTOR ->
            Icon(Icons.Default.Error, contentDescription = null, tint = Color.Red, modifier = Modifier.size(14.dp))
        isRead -> DoubleCheck(Color(0xFF4FC3F7))   // Blue — Read
        else   -> DoubleCheck(OnSurfaceVar)         // Grey — Delivered
    }
}

@Composable
private fun DoubleCheck(color: Color) {
    Row(modifier = Modifier.height(14.dp)) {
        Icon(Icons.Default.Done, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Icon(Icons.Default.Done, contentDescription = null, tint = color,
            modifier = Modifier.size(14.dp).offset(x = (-6).dp))
    }
}

// ── Message Content ───────────────────────────────────────────
@Composable
private fun MessageContent(
    message: TdApi.Message,
    downloadedFiles: Map<Int, String>,
    manager: TelegramManager,
    textColor: Color
) {
    when (val c = message.content) {
        is TdApi.MessageText -> Text(c.text.text, color = textColor, fontSize = 15.sp)
        is TdApi.MessagePhoto -> {
            val photo = c.photo.sizes.lastOrNull()?.photo
            val path  = photo?.let {
                if (it.local.isDownloadingCompleted) it.local.path
                else { manager.downloadFile(it.id); downloadedFiles[it.id] }
            }
            if (path != null) {
                AsyncImage(model = path, contentDescription = null,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Image, contentDescription = null, tint = OnSurfaceVar, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Photo", color = OnSurfaceVar, fontSize = 14.sp)
                }
            }
            if (!c.caption.text.isNullOrEmpty()) Text(c.caption.text, color = textColor, fontSize = 14.sp)
        }
        is TdApi.MessageVideo -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Videocam, contentDescription = null, tint = OnSurfaceVar, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Video", color = textColor, fontSize = 14.sp)
            }
        }
        is TdApi.MessageDocument -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = Primary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(6.dp))
                Text(c.document.fileName, color = textColor, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        is TdApi.MessageSticker -> Text("🧩 Sticker", color = textColor, fontSize = 14.sp)
        is TdApi.MessageVoiceNote -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Mic, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                val s = c.voiceNote.duration
                Text("Voice  ${s / 60}:${"%02d".format(s % 60)}", color = textColor, fontSize = 14.sp)
            }
        }
        is TdApi.MessageCall -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (c.isVideo) Icons.Default.Videocam else Icons.Default.Call,
                    contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (c.isVideo) "Video Call" else "Voice Call", color = textColor, fontSize = 14.sp)
            }
        }
        is TdApi.MessageLocation -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Red, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Location", color = textColor, fontSize = 14.sp)
            }
        }
        else -> Text("Message", color = OnSurfaceVar, fontSize = 14.sp, fontStyle = FontStyle.Italic)
    }
}

// ── Reply Preview ─────────────────────────────────────────────
@Composable
private fun ReplyPreview(replyTo: TdApi.Message) {
    val text = when (val c = replyTo.content) {
        is TdApi.MessageText  -> c.text.text
        is TdApi.MessagePhoto -> "📷 Photo"
        is TdApi.MessageVideo -> "🎥 Video"
        else -> "Message"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (LocalDarkTheme.current) Color(0x22FFFFFF) else Color(0x22000000))
            .padding(6.dp)
    ) {
        Box(modifier = Modifier.width(3.dp).height(32.dp).background(Primary))
        Spacer(Modifier.width(6.dp))
        Text(text, color = OnSurfaceVar, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

// ── Input Bar ──────────────────────────────────────────────────
@Composable
private fun ChatInputBar(
    text: String,
    replyTo: TdApi.Message?,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onCancelReply: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().background(Background)) {
        AnimatedVisibility(visible = replyTo != null) {
            if (replyTo != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(SurfaceVar)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Reply, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        when (val c = replyTo.content) {
                            is TdApi.MessageText -> c.text.text
                            is TdApi.MessagePhoto -> "📷 Photo"
                            else -> "Message"
                        },
                        color = OnSurfaceVar, fontSize = 13.sp, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onCancelReply, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = OnSurfaceVar)
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = text, onValueChange = onTextChange,
                placeholder = { Text("Message...", color = OnSurfaceVar) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                maxLines = 5,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor      = Primary,
                    unfocusedBorderColor    = SurfaceVar,
                    focusedContainerColor   = SurfaceVar,
                    unfocusedContainerColor = SurfaceVar,
                    focusedTextColor        = OnBackground,
                    unfocusedTextColor      = OnBackground
                )
            )
            Spacer(Modifier.width(8.dp))
            FloatingActionButton(
                onClick = onSend,
                modifier = Modifier.size(48.dp),
                containerColor = Primary,
                contentColor = Color.White,
                shape = CircleShape
            ) { Icon(Icons.Default.Send, contentDescription = "Send") }
        }
    }
}

// ── Context Menu ───────────────────────────────────────────────
@Composable
private fun MessageContextMenu(
    message: TdApi.Message,
    myId: Long,
    onReply: () -> Unit,
    onDelete: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val isMe = message.senderId.let { it is TdApi.MessageSenderUser && it.userId == myId }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete message") },
            text  = { Text(if (isMe) "Delete for everyone or just for you?" else "Delete for yourself?") },
            confirmButton = {
                if (isMe) {
                    TextButton(onClick = { onDelete(true); showDeleteDialog = false }) {
                        Text("For everyone", color = Color.Red)
                    }
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { onDelete(false); showDeleteDialog = false }) {
                        Text("For me")
                    }
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            },
            containerColor = Surface
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            text = {
                Column {
                    TextButton(onClick = onReply, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Reply, contentDescription = null, tint = Primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Reply", color = OnBackground)
                    }
                    TextButton(onClick = { showDeleteDialog = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
                        Spacer(Modifier.width(8.dp))
                        Text("Delete", color = Color.Red)
                    }
                    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text("Cancel", color = OnSurfaceVar)
                    }
                }
            },
            confirmButton  = {},
            containerColor = Surface
        )
    }
}
