package com.spmods.spgram.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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

// ── Wallpaper colors ─────────────────────────────────────────
private val WallpaperDark  = Color(0xFF0F1923)
private val WallpaperLight = Color(0xFFDAE5F5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: Long,
    manager: TelegramManager,
    onBack: () -> Unit
) {
    val context         = LocalContext.current
    val chats           by manager.chats.collectAsState()
    val allMessages     by manager.messages.collectAsState()
    val typingStatus    by manager.typingStatus.collectAsState()
    val outboxReadId    by manager.outboxReadId.collectAsState()
    val downloadedFiles by manager.downloadedFiles.collectAsState()
    val userStatus      by manager.userStatus.collectAsState()

    val chat     = chats[chatId]
    val messages = allMessages[chatId] ?: emptyList()
    val typing   = typingStatus[chatId]
    val status   = if (typing != null) typing else userStatus[chatId] ?: ""
    val lastRead = outboxReadId[chatId] ?: 0L
    val myId     = manager.getMyUserId()

    var inputText      by remember { mutableStateOf("") }
    var replyToMessage by remember { mutableStateOf<TdApi.Message?>(null) }

    // Undo delete
    var pendingDeleteId  by remember { mutableStateOf<Long?>(null) }
    var pendingDeleteAll by remember { mutableStateOf(false) }
    var undoCountdown    by remember { mutableIntStateOf(5) }

    val listState = rememberLazyListState()
    val scope     = rememberCoroutineScope()

    // Show scroll-to-bottom button
    val showScrollBtn by remember {
        derivedStateOf { listState.firstVisibleItemIndex < messages.size - 3 }
    }

    LaunchedEffect(chatId) { manager.openChat(chatId) }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }
    DisposableEffect(chatId) { onDispose { manager.closeChat(chatId) } }

    // Undo timer
    LaunchedEffect(pendingDeleteId) {
        val id = pendingDeleteId ?: return@LaunchedEffect
        undoCountdown = 5
        repeat(5) { delay(1000); undoCountdown-- }
        if (pendingDeleteId == id) {
            manager.deleteMessage(chatId, id, pendingDeleteAll)
            pendingDeleteId = null
        }
    }

    // Group messages by date
    val groupedMessages = remember(messages) {
        messages.groupBy { msg ->
            val cal = Calendar.getInstance().apply { timeInMillis = msg.date * 1000L }
            Triple(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
        }.entries.toList()
    }

    val wallpaperColor = if (LocalDarkTheme.current) WallpaperDark else WallpaperLight

    Scaffold(
        containerColor = wallpaperColor,
        topBar = {
            ChatTopBar(
                chat            = chat,
                status          = status,
                downloadedFiles = downloadedFiles,
                manager         = manager,
                onBack          = { manager.closeChat(chatId); onBack() }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            // Messages
            LazyColumn(
                state   = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 72.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                groupedMessages.forEach { (dateKey, dayMessages) ->
                    // Date separator
                    item(key = "date-${dateKey.first}-${dateKey.second}-${dateKey.third}") {
                        DateSeparator(dayMessages.first().date)
                    }
                    items(dayMessages, key = { it.id }) { message ->
                        val isMe   = message.senderId.let {
                            it is TdApi.MessageSenderUser && it.userId == myId
                        }
                        val isRead = isMe && message.id <= lastRead

                        if (message.id == pendingDeleteId) {
                            UndoDeleteBar(countdown = undoCountdown, onUndo = { pendingDeleteId = null }, isMe = isMe)
                        } else {
                            SwipeableMessageBubble(
                                message         = message,
                                isMe            = isMe,
                                isRead          = isRead,
                                allMessages     = messages,
                                downloadedFiles = downloadedFiles,
                                manager         = manager,
                                context         = context,
                                onSwipeReply    = { replyToMessage = message },
                                onDeleteRequest = { forAll ->
                                    pendingDeleteId  = message.id
                                    pendingDeleteAll = forAll
                                }
                            )
                        }
                    }
                }
            }

            // Scroll to bottom FAB
            if (showScrollBtn) {
                FloatingActionButton(
                    onClick        = { scope.launch { listState.animateScrollToItem(messages.size - 1) } },
                    modifier       = Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 80.dp).size(40.dp),
                    containerColor = if (LocalDarkTheme.current) Color(0xFF2A2A2A) else Color.White,
                    contentColor   = Primary,
                    shape          = CircleShape,
                    elevation      = FloatingActionButtonDefaults.elevation(4.dp)
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, null, modifier = Modifier.size(20.dp))
                }
            }

            // Input bar pinned to bottom
            Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
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
        }
    }
}

// ── Date Separator ────────────────────────────────────────────
@Composable
private fun DateSeparator(timestamp: Int) {
    val now = Calendar.getInstance()
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp * 1000L }
    val label = when {
        now.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR) &&
        now.get(Calendar.YEAR) == cal.get(Calendar.YEAR) -> "Today"
        now.get(Calendar.DAY_OF_YEAR) - cal.get(Calendar.DAY_OF_YEAR) == 1 &&
        now.get(Calendar.YEAR) == cal.get(Calendar.YEAR) -> "Yesterday"
        else -> SimpleDateFormat("MMMM d", Locale.getDefault()).format(Date(timestamp * 1000L))
    }
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (LocalDarkTheme.current) Color(0x99000000) else Color(0x99FFFFFF))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(label, color = if (LocalDarkTheme.current) Color(0xCCFFFFFF) else Color(0xCC000000), fontSize = 12.sp)
        }
    }
}

// ── Undo Bar ──────────────────────────────────────────────────
@Composable
private fun UndoDeleteBar(countdown: Int, onUndo: () -> Unit, isMe: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (LocalDarkTheme.current) Color(0xFF2A2A2A) else Color(0xFFEEEEEE))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Delete, null, tint = OnSurfaceVar, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text("Message deleted", color = OnSurfaceVar, fontSize = 13.sp, modifier = Modifier.weight(1f))
        TextButton(onClick = onUndo) {
            Text("UNDO ($countdown)", color = Primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

// ── Swipeable Bubble ──────────────────────────────────────────
@Composable
private fun SwipeableMessageBubble(
    message: TdApi.Message,
    isMe: Boolean,
    isRead: Boolean,
    allMessages: List<TdApi.Message>,
    downloadedFiles: Map<Int, String>,
    manager: TelegramManager,
    context: Context,
    onSwipeReply: () -> Unit,
    onDeleteRequest: (Boolean) -> Unit
) {
    var offsetX          by remember { mutableFloatStateOf(0f) }
    var showContextMenu  by remember { mutableStateOf(false) }
    val animOffset        = animateFloatAsState(
        targetValue   = offsetX,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label         = "swipe"
    )
    val swipeThreshold = 72f
    val replyAlpha     = (kotlin.math.abs(offsetX) / swipeThreshold).coerceIn(0f, 1f)

    if (showContextMenu) {
        ReactionAndContextMenu(
            message   = message,
            myId      = manager.getMyUserId(),
            onReply   = { onSwipeReply(); showContextMenu = false },
            onCopy    = {
                val txt = manager.copyMessageText(message)
                if (txt != null) {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("msg", txt))
                }
                showContextMenu = false
            },
            onDelete  = { forAll -> onDeleteRequest(forAll); showContextMenu = false },
            onDismiss = { showContextMenu = false }
        )
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        // Reply hint
        if (replyAlpha > 0.1f) {
            Box(
                modifier = Modifier
                    .align(if (isMe) Alignment.CenterEnd else Alignment.CenterStart)
                    .padding(horizontal = 6.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Primary.copy(alpha = replyAlpha * 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Reply, null, tint = Primary.copy(alpha = replyAlpha), modifier = Modifier.size(18.dp))
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(animOffset.value.toInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (kotlin.math.abs(offsetX) >= swipeThreshold) onSwipeReply()
                            offsetX = 0f
                        },
                        onDragCancel = { offsetX = 0f },
                        onHorizontalDrag = { _, drag ->
                            if (!isMe && drag > 0) offsetX = (offsetX + drag).coerceIn(0f, 96f)
                            else if (isMe && drag < 0) offsetX = (offsetX + drag).coerceIn(-96f, 0f)
                        }
                    )
                }
        ) {
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

// ── Top Bar ───────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopBar(
    chat: TdApi.Chat?,
    status: String,
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
    val statusColor = if (status == "online") Color(0xFF4CAF50) else OnSurfaceVar

    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, null, tint = OnBackground)
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar with online dot
                Box(modifier = Modifier.size(38.dp)) {
                    Box(modifier = Modifier.fillMaxSize().clip(CircleShape), contentAlignment = Alignment.Center) {
                        if (photoPath != null) {
                            AsyncImage(model = photoPath, contentDescription = null,
                                modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                        } else {
                            Box(modifier = Modifier.fillMaxSize().background(Primary), contentAlignment = Alignment.Center) {
                                Text(chat?.title?.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                    color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                    // Online dot
                    if (status == "online") {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50))
                                .border(1.5.dp, Background, CircleShape)
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(chat?.title ?: "", color = OnBackground, fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (status.isNotEmpty()) {
                        Text(status, color = statusColor, fontSize = 12.sp, maxLines = 1)
                    }
                }
            }
        },
        actions = {
            IconButton(onClick = {}) {
                Icon(Icons.Default.Call, null, tint = OnSurfaceVar)
            }
            IconButton(onClick = {}) {
                Icon(Icons.Default.MoreVert, null, tint = OnSurfaceVar)
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
    val dark = LocalDarkTheme.current
    val bubbleColor = when {
        isMe && dark  -> Color(0xFF2B5278)
        isMe          -> Color(0xFFEFFBDB)
        dark          -> Color(0xFF1E2936)
        else          -> Color.White
    }
    val textColor = if (dark) Color(0xFFEEEEEE) else Color(0xFF111111)
    val alignment = if (isMe) Alignment.End else Alignment.Start
    val shape     = if (isMe) RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp)
                    else       RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)

    val replyTo = (message.replyTo as? TdApi.MessageReplyToMessage)?.let { r ->
        allMessages.firstOrNull { it.id == r.messageId }
    }

    val timeStr = remember(message.date) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(message.date * 1000L))
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(min = 80.dp, max = 290.dp)
                .clip(shape)
                .background(bubbleColor)
                .pointerInput(Unit) { detectTapGestures(onLongPress = { onLongPress() }) }
                .padding(horizontal = 10.dp, vertical = 7.dp)
        ) {
            Column {
                if (replyTo != null) {
                    ReplyPreview(replyTo)
                    Spacer(Modifier.height(4.dp))
                }
                MessageContent(message, downloadedFiles, manager, textColor)
                Spacer(Modifier.height(2.dp))
                Row(modifier = Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                    Text(timeStr, color = OnSurfaceVar, fontSize = 11.sp)
                    if (isMe) {
                        Spacer(Modifier.width(4.dp))
                        ReadReceipt(message, isRead)
                    }
                }
            }
        }
    }
}

// ── Read Receipt ──────────────────────────────────────────────
@Composable
private fun ReadReceipt(message: TdApi.Message, isRead: Boolean) {
    when {
        message.sendingState?.constructor == TdApi.MessageSendingStatePending.CONSTRUCTOR ->
            Icon(Icons.Default.Schedule, null, tint = OnSurfaceVar, modifier = Modifier.size(14.dp))
        message.sendingState?.constructor == TdApi.MessageSendingStateFailed.CONSTRUCTOR ->
            Icon(Icons.Default.ErrorOutline, null, tint = Color.Red, modifier = Modifier.size(14.dp))
        isRead -> DoubleCheck(Color(0xFF4FC3F7))
        else   -> DoubleCheck(OnSurfaceVar)
    }
}

@Composable
private fun DoubleCheck(color: Color) {
    Row(modifier = Modifier.height(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Done, null, tint = color, modifier = Modifier.size(14.dp))
        Icon(Icons.Default.Done, null, tint = color, modifier = Modifier.size(14.dp).offset(x = (-5).dp))
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
                    modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Image, null, tint = OnSurfaceVar, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp)); Text("Photo", color = OnSurfaceVar, fontSize = 14.sp)
                }
            }
            if (c.caption.text.isNotEmpty()) Text(c.caption.text, color = textColor, fontSize = 14.sp)
        }
        is TdApi.MessageVideo -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Videocam, null, tint = Primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp)); Text("Video", color = textColor, fontSize = 14.sp)
            }
            if (c.caption.text.isNotEmpty()) Text(c.caption.text, color = textColor, fontSize = 14.sp)
        }
        is TdApi.MessageDocument -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.InsertDriveFile, null, tint = Primary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(6.dp))
                Text(c.document.fileName, color = textColor, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        is TdApi.MessageVoiceNote -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Primary.copy(0.2f)),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.PlayArrow, null, tint = Primary, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    // Simple waveform visual
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(24.dp)) {
                        repeat(20) { i ->
                            val h = (4 + (i % 5) * 4).dp
                            Box(modifier = Modifier.width(2.dp).height(h).clip(RoundedCornerShape(1.dp))
                                .background(Primary.copy(alpha = 0.7f)))
                            Spacer(Modifier.width(2.dp))
                        }
                    }
                    val s = c.voiceNote.duration
                    Text("${s / 60}:${"%02d".format(s % 60)}", color = OnSurfaceVar, fontSize = 11.sp)
                }
            }
        }
        is TdApi.MessageSticker   -> Text("🧩 Sticker", color = textColor, fontSize = 32.sp)
        is TdApi.MessageAnimation -> Text("🎞 GIF", color = textColor, fontSize = 14.sp)
        is TdApi.MessageCall -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (c.isVideo) Icons.Default.Videocam else Icons.Default.Call,
                    null, tint = Primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (c.isVideo) "Video Call" else "Voice Call", color = textColor, fontSize = 14.sp)
            }
        }
        is TdApi.MessageLocation -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, null, tint = Color.Red, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp)); Text("Location", color = textColor, fontSize = 14.sp)
            }
        }
        else -> Text("Message", color = OnSurfaceVar, fontSize = 13.sp, fontStyle = FontStyle.Italic)
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
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
        .background(if (LocalDarkTheme.current) Color(0x33FFFFFF) else Color(0x33000000)).padding(6.dp)) {
        Box(modifier = Modifier.width(3.dp).heightIn(min = 24.dp).background(Primary))
        Spacer(Modifier.width(6.dp))
        Text(text, color = OnSurfaceVar, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

// ── Input Bar ─────────────────────────────────────────────────
@Composable
private fun ChatInputBar(
    text: String,
    replyTo: TdApi.Message?,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onCancelReply: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Background, shadowElevation = 8.dp) {
        Column {
            AnimatedVisibility(visible = replyTo != null) {
                if (replyTo != null) {
                    Row(modifier = Modifier.fillMaxWidth().background(SurfaceVar)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.width(3.dp).height(32.dp).background(Primary))
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Reply", color = Primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                when (val c = replyTo.content) {
                                    is TdApi.MessageText  -> c.text.text
                                    is TdApi.MessagePhoto -> "📷 Photo"
                                    is TdApi.MessageVideo -> "🎥 Video"
                                    else -> "Message"
                                },
                                color = OnSurfaceVar, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = onCancelReply, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, null, tint = OnSurfaceVar)
                        }
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Bottom) {
                // Emoji
                IconButton(onClick = {}, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.EmojiEmotions, null, tint = OnSurfaceVar)
                }
                OutlinedTextField(
                    value         = text,
                    onValueChange = onTextChange,
                    placeholder   = { Text("Message", color = OnSurfaceVar) },
                    modifier      = Modifier.weight(1f),
                    shape         = RoundedCornerShape(24.dp),
                    maxLines      = 5,
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = Color.Transparent,
                        unfocusedBorderColor    = Color.Transparent,
                        focusedContainerColor   = SurfaceVar,
                        unfocusedContainerColor = SurfaceVar,
                        focusedTextColor        = OnBackground,
                        unfocusedTextColor      = OnBackground
                    )
                )
                Spacer(Modifier.width(6.dp))
                if (text.isBlank()) {
                    // Attach / mic
                    IconButton(onClick = {}, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.AttachFile, null, tint = OnSurfaceVar)
                    }
                    FloatingActionButton(onClick = {}, modifier = Modifier.size(48.dp),
                        containerColor = Primary, contentColor = Color.White, shape = CircleShape) {
                        Icon(Icons.Default.Mic, "Voice")
                    }
                } else {
                    FloatingActionButton(onClick = onSend, modifier = Modifier.size(48.dp),
                        containerColor = Primary, contentColor = Color.White, shape = CircleShape) {
                        Icon(Icons.Default.Send, "Send")
                    }
                }
            }
        }
    }
}

// ── Reaction + Context Menu ───────────────────────────────────
@Composable
private fun ReactionAndContextMenu(
    message: TdApi.Message,
    myId: Long,
    onReply: () -> Unit,
    onCopy: () -> Unit,
    onDelete: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val isMe = message.senderId.let { it is TdApi.MessageSenderUser && it.userId == myId }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val reactions = listOf("❤️", "👍", "👎", "🔥", "🥰", "👏", "😁")

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor   = Surface,
            title = { Text("Delete message") },
            text  = { Text(if (isMe) "Delete for everyone or just for you?" else "Delete for yourself?") },
            confirmButton = {
                if (isMe) TextButton(onClick = { onDelete(true); showDeleteDialog = false }) {
                    Text("For everyone", color = Color.Red)
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { onDelete(false); showDeleteDialog = false }) { Text("For me") }
                    TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
                }
            }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = Surface,
        text = {
            Column {
                // Reaction bar
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly) {
                    reactions.forEach { emoji ->
                        Text(emoji, fontSize = 24.sp, modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onDismiss() }
                            .padding(4.dp))
                    }
                    Box(modifier = Modifier.size(32.dp).clip(CircleShape)
                        .background(SurfaceVar).clickable { onDismiss() },
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.ExpandMore, null, tint = OnSurfaceVar, modifier = Modifier.size(18.dp))
                    }
                }
                Divider(color = Divider)
                ContextMenuItem(Icons.Default.Reply,    "Reply",   OnBackground) { onReply() }
                ContextMenuItem(Icons.Default.ContentCopy, "Copy", OnBackground) { onCopy() }
                ContextMenuItem(Icons.Default.Forward,  "Forward", OnBackground) { onDismiss() }
                ContextMenuItem(Icons.Default.PushPin,  "Pin",     OnBackground) { onDismiss() }
                ContextMenuItem(Icons.Default.Translate,"Translate",OnBackground){ onDismiss() }
                ContextMenuItem(Icons.Default.Delete,   "Delete",  Color.Red) { showDeleteDialog = true }
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun ContextMenuItem(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
        .clickable { onClick() }.padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, color = tint, fontSize = 15.sp)
    }
}
