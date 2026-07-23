package com.spmods.spgram.presentation.settings.homeScreen.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val activeColor = Color(0xFF52C5F5)
private val inactiveColor = Color(0xFF7A7A8A)
private val dividerColor = Color(0xFF2C2C2C)

@Composable
fun HomeScreenPreview(
    showStories: Boolean,
    showArchive: Boolean,
    showBottomBarLabels: Boolean,
    showOnlineStatus: Boolean,
    isCompactChatList: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column {

            // ── Top bar ────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SPGram",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(10.dp))
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            // ── Stories row ────────────────────────────────────────────────
            AnimatedVisibility(
                visible = showStories,
                enter = expandVertically(spring(stiffness = Spring.StiffnessLow)) + fadeIn(),
                exit = shrinkVertically(spring(stiffness = Spring.StiffnessLow)) + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
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
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(storyColors[i % storyColors.size].copy(alpha = 0.75f))
                            )
                            Spacer(Modifier.height(3.dp))
                            Box(
                                modifier = Modifier
                                    .width(32.dp)
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            )
                        }
                    }
                }
            }

            // ── Archive row ────────────────────────────────────────────────
            AnimatedVisibility(
                visible = showArchive,
                enter = expandVertically(spring(stiffness = Spring.StiffnessLow)) + fadeIn(),
                exit = shrinkVertically(spring(stiffness = Spring.StiffnessLow)) + fadeOut()
            ) {
                PreviewChatRow(
                    avatarText = "📁",
                    name = "Archived",
                    message = "3 unread messages",
                    time = "",
                    onlineDot = false,
                    compact = isCompactChatList,
                    avatarColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            }

            // ── Chat rows ──────────────────────────────────────────────────
            listOf(
                Triple("Sandun Piumal", "Hey, just reviewed the new UI!", "12:45"),
                Triple("SPGram News", "New update released 🚀", "11:20"),
                Triple("Dev Group", "PR merged ✅", "Yesterday"),
            ).forEachIndexed { i, (name, msg, time) ->
                val colors = listOf(Color(0xFF4285F4), Color(0xFF34A853), Color(0xFFF9AB00))
                PreviewChatRow(
                    avatarText = name.first().uppercase(),
                    name = name,
                    message = msg,
                    time = time,
                    onlineDot = showOnlineStatus && i == 0,
                    compact = isCompactChatList,
                    avatarColor = colors[i % colors.size].copy(alpha = 0.8f)
                )
            }

            // ── Bottom nav bar ─────────────────────────────────────────────
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
                    .padding(vertical = if (showBottomBarLabels) 6.dp else 10.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("Chats" to true, "Stories" to false, "Calls" to false, "Download" to false)
                    .forEach { (label, selected) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // Pill + icon placeholder
                            Box(contentAlignment = Alignment.Center) {
                                if (selected) {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 36.dp, height = 26.dp)
                                            .clip(RoundedCornerShape(13.dp))
                                            .background(activeColor.copy(alpha = 0.15f))
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            if (selected) activeColor.copy(alpha = 0.9f)
                                            else inactiveColor.copy(alpha = 0.5f)
                                        )
                                )
                            }
                            AnimatedVisibility(
                                visible = showBottomBarLabels,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 9.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) activeColor else inactiveColor,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
            }
        }
    }
}

@Composable
private fun PreviewChatRow(
    avatarText: String,
    name: String,
    message: String,
    time: String,
    onlineDot: Boolean,
    compact: Boolean,
    avatarColor: Color
) {
    val avatarSize = if (compact) 34.dp else 44.dp
    val vPad = if (compact) 5.dp else 8.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = vPad),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Box(
                modifier = Modifier
                    .size(avatarSize)
                    .clip(CircleShape)
                    .background(avatarColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = avatarText,
                    fontSize = if (compact) 12.sp else 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            if (onlineDot) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF34A853))
                        .align(Alignment.BottomEnd)
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (time.isNotEmpty()) {
                    Text(
                        text = time,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (!compact) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
