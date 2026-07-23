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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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

    val storyRingColors = listOf(
        listOf(Color(0xFFFF6D00), Color(0xFFAA00FF)),
        listOf(Color(0xFFFF6D66), Color(0xFFFF9800)),
        listOf(Color(0xFF9E9E9E), Color(0xFF757575)),
        listOf(Color(0xFFFF9800), Color(0xFFFFEB3B)),
        listOf(Color(0xFF9E9E9E), Color(0xFF607D8B))
    )

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

                // ── Top bar: SPGRAM + profile ──────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 14.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.app_name_spgram),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    )
                }

                // ── Search bar ─────────────────────────────────────────────
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.search_conversations_placeholder),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // ── Stories row ────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(5) { i ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(56.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
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
                                    .width(40.dp)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            )
                        }
                    }
                }

                // ── Archive tile (exact ArchiveHeaderCard) ─────────────────
                AnimatedVisibility(
                    visible = isArchivePinned,
                    enter = expandVertically(spring(stiffness = Spring.StiffnessLow)) + fadeIn(),
                    exit = shrinkVertically(spring(stiffness = Spring.StiffnessLow)) + fadeOut()
                ) {
                    Card(
                        shape = RoundedCornerShape(20),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        elevation = CardDefaults.cardElevation(0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 4.dp)
                            .height(78.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Archive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.archived_chats_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = stringResource(R.string.archived_chats_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Rounded.PushPin,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp).rotate(45f),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // ── Chat rows ──────────────────────────────────────────────
                Column(modifier = Modifier.padding(16.dp)) {
                    PreviewHomeListItem(
                        name = "Sandun Piumal",
                        message = "Hey, I just reviewed the new UI design updates you sent. The chat list layout looks super clean!",
                        time = stringResource(R.string.preview_time_konata),
                        lines = chatListMessageLines,
                        showPhotos = showChatListPhotos,
                        isKonata = true
                    )
                    Spacer(Modifier.height(12.dp))
                    PreviewHomeListItem(
                        name = stringResource(R.string.preview_group_name),
                        message = stringResource(R.string.preview_group_msg),
                        time = stringResource(R.string.preview_group_time),
                        lines = chatListMessageLines,
                        showPhotos = showChatListPhotos
                    )
                }
            }
        }

        if (position != ItemPosition.BOTTOM && position != ItemPosition.STANDALONE) {
            Spacer(Modifier.size(2.dp))
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
    isKonata: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedVisibility(
            visible = showPhotos,
            enter = fadeIn() + expandHorizontally(),
            exit = fadeOut() + shrinkHorizontally()
        ) {
            Row {
                Avatar(
                    path = if (isKonata) "local" else null,
                    name = name,
                    size = 48.dp,
                    isLocal = isKonata
                )
                Spacer(modifier = Modifier.width(12.dp))
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
                Text(
                    text = time,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
