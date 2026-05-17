package com.spmods.spgram.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.spmods.spgram.engine.TelegramManager
import com.spmods.spgram.ui.theme.*
import org.drinkless.tdlib.TdApi
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatListItem(chat: TdApi.Chat, manager: TelegramManager) {
    val downloadedFiles by manager.downloadedFiles.collectAsState()

    val photoFile = chat.photo?.small
    val imagePath: String? = when {
        photoFile == null                        -> null
        photoFile.local.isDownloadingCompleted   -> photoFile.local.path
        downloadedFiles[photoFile.id] != null    -> downloadedFiles[photoFile.id]
        else -> { manager.downloadFile(photoFile.id); null }
    }

    // Last message preview
    val lastMsgText = remember(chat.lastMessage) {
        when (val c = chat.lastMessage?.content) {
            is TdApi.MessageText    -> c.text.text
            is TdApi.MessagePhoto   -> "📷 Photo"
            is TdApi.MessageVideo   -> "🎥 Video"
            is TdApi.MessageAudio   -> "🎵 Audio"
            is TdApi.MessageDocument -> "📄 Document"
            is TdApi.MessageSticker -> "🧩 Sticker"
            is TdApi.MessageVoiceNote -> "🎤 Voice"
            is TdApi.MessageVideoNote -> "📹 Video note"
            is TdApi.MessageAnimation -> "🎞 GIF"
            null -> "No messages"
            else -> "Message"
        }
    }

    // Timestamp
    val timeStr = remember(chat.lastMessage) {
        chat.lastMessage?.date?.let { ts ->
            val cal = Calendar.getInstance()
            val msgCal = Calendar.getInstance().apply { timeInMillis = ts * 1000L }
            if (cal.get(Calendar.DATE) == msgCal.get(Calendar.DATE))
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts * 1000L))
            else
                SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(ts * 1000L))
        } ?: ""
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        if (imagePath != null) {
            AsyncImage(
                model = imagePath,
                contentDescription = null,
                modifier = Modifier.size(52.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(AvatarBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = chat.title.take(1).uppercase(),
                    color = OnBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        }

        Spacer(Modifier.width(14.dp))

        // Text column
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = chat.title,
                    color = OnBackground,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (timeStr.isNotEmpty()) {
                    Text(
                        text = timeStr,
                        color = OnSurfaceVar,
                        fontSize = 12.sp
                    )
                }
            }
            Text(
                text = lastMsgText,
                color = OnSurfaceVar,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
