package com.spmods.spgram.presentation.settings.homeScreen.components

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
    onMessageLinesChanged: (Int) -> Unit = {},
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

                // ── Top bar ────────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 14.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SPGram",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(14.dp))
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // ── Stories row ────────────────────────────────────────────
                val storyRingColors = listOf(
                    listOf(Color(0xFF4285F4), Color(0xFF9C27B0)),
                    listOf(Color(0xFF34A853), Color(0xFF00BFA5)),
                    listOf(Color(0xFFF9AB00), Color(0xFFEA4335)),
                    listOf(Color(0xFFEA4335), Color(0xFFFF6D66)),
                    listOf(Color(0xFF9C27B0), Color(0xFF4285F4))
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    repeat(5) { i ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(52.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = 2.dp,
                                        brush = Brush.linearGradient(storyRingColors[i % storyRingColors.size]),
                                        shape = CircleShape
                                    )
                                    .padding(3.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            )
                            Spacer(Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(38.dp)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            )
                        }
                    }
                }

                // ── Two-line / Three-line toggle ───────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1 to stringResource(R.string.two_line_label), 2 to stringResource(R.string.three_line_label))
                        .forEach { (lines, label) ->
                            val selected = chatListMessageLines == lines
                            Surface(
                                onClick = { onMessageLinesChanged(lines) },
                                shape = RoundedCornerShape(16.dp),
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerLow,
                                border = if (selected) androidx.compose.foundation.BorderStroke(
                                    2.dp, MaterialTheme.colorScheme.primary
                                ) else null,
                                modifier = Modifier.weight(1f).height(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                }

                // ── Archive row ────────────────────────────────────────────
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

                // ── Chat rows ──────────────────────────────────────────────
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
                    showPhotos = showChatListPhotos,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
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
    isKonata: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
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
