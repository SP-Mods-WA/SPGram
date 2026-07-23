package com.spmods.spgram.presentation.settings.homeScreen.components

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spmods.spgram.presentation.R
import com.spmods.spgram.presentation.core.ui.Avatar
import com.spmods.spgram.presentation.core.ui.ItemPosition

@Composable
fun HomeScreenPreview(
    isArchivePinned: Boolean,
    chatListMessageLines: Int,
    showChatListPhotos: Boolean,
    modifier: Modifier = Modifier,
    position: ItemPosition = ItemPosition.STANDALONE
) {
    val cornerRadius = 24.dp
    val shape = remember(position) {
        when (position) {
            ItemPosition.TOP -> RoundedCornerShape(
                topStart = cornerRadius, topEnd = cornerRadius,
                bottomStart = 4.dp, bottomEnd = 4.dp
            )
            ItemPosition.MIDDLE -> RoundedCornerShape(4.dp)
            ItemPosition.BOTTOM -> RoundedCornerShape(
                bottomStart = cornerRadius, bottomEnd = cornerRadius,
                topStart = 4.dp, topEnd = 4.dp
            )
            ItemPosition.STANDALONE -> RoundedCornerShape(cornerRadius)
        }
    }

    val activeColor = Color(0xFF52C5F5)
    val inactiveColor = Color(0xFF7A7A8A)
    val dividerColor = Color(0xFF2C2C2C)

    Column(modifier = modifier) {
        if (position == ItemPosition.TOP || position == ItemPosition.STANDALONE) {
            Text(
                text = stringResource(R.string.home_screen_preview_title),
                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp, top = 16.dp),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = shape,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            ) {

                // ── Top bar ────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SPGram",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    // Search icon placeholder
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    )
                    Spacer(Modifier.width(10.dp))
                    // Pencil/edit icon placeholder
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    )
                }

                // ── Stories row ────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val storyColors = listOf(
                        Color(0xFF4285F4), Color(0xFF34A853),
                        Color(0xFFF9AB00), Color(0xFFEA4335), Color(0xFF9C27B0)
                    )
                    repeat(5) { i ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(storyColors[i % storyColors.size].copy(alpha = 0.8f))
                            )
                            Spacer(Modifier.height(3.dp))
                            Box(
                                modifier = Modifier
                                    .width(36.dp)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            )
                        }
                    }
                }

                // ── Archive row ────────────────────────────────────
                AnimatedVisibility(
                    visible = isArchivePinned,
                    enter = expandVertically(spring(stiffness = Spring.StiffnessLow)) + fadeIn(),
                    exit = shrinkVertically(spring(stiffness = Spring.StiffnessLow)) + fadeOut()
                ) {
                    PreviewHomeListItem(
                        name = stringResource(R.string.preview_archive_label),
                        message = stringResource(R.string.preview_archive_msg),
                        time = "",
                        lines = chatListMessageLines,
                        showPhotos = showChatListPhotos,
                        isArchive = true
                    )
                }

                // ── Chat rows ──────────────────────────────────────
                PreviewHomeListItem(
                    name = "Sandun Piumal",
                    message = "Hey, I just reviewed the new UI design updates you sent. The c…",
                    time = stringResource(R.string.preview_time_konata),
                    lines = chatListMessageLines,
                    showPhotos = showChatListPhotos,
                    isKonata = true
                )
                PreviewHomeListItem(
                    name = stringResource(R.string.preview_group_name),
                    message = stringResource(R.string.preview_group_msg),
                    time = stringResource(R.string.preview_group_time),
                    lines = chatListMessageLines,
                    showPhotos = showChatListPhotos
                )

                // ── Bottom nav bar ─────────────────────────────────
                val divColor = dividerColor
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            drawLine(
                                color = divColor,
                                start = Offset(0f, 0f),
                                end = Offset(size.width, 0f),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(
                        "Chats" to true,
                        "Stories" to false,
                        "Calls" to false,
                        "Download" to false
                    ).forEach { (label, selected) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (selected) {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 40.dp, height = 28.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(activeColor.copy(alpha = 0.15f))
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(
                                            if (selected) activeColor.copy(alpha = 0.85f)
                                            else inactiveColor.copy(alpha = 0.45f)
                                        )
                                )
                            }
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = label,
                                fontSize = 9.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) activeColor else inactiveColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewHomeListItem(
    name: String,
    message: String,
    time: String,
    lines: Int,
    showPhotos: Boolean,
    isArchive: Boolean = false,
    isKonata: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedVisibility(
            visible = showPhotos,
            enter = fadeIn() + expandHorizontally(),
            exit = fadeOut() + shrinkHorizontally()
        ) {
            Row {
                if (isArchive) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📁", fontSize = 20.sp)
                    }
                } else {
                    Avatar(
                        path = if (isKonata) "local" else null,
                        name = name,
                        size = 48.dp,
                        isLocal = isKonata
                    )
                }
                Spacer(Modifier.width(12.dp))
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (time.isNotEmpty()) {
                    Text(
                        text = time,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = lines,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
