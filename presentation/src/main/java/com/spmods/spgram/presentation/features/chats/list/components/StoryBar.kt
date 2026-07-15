package com.spmods.spgram.presentation.features.chats.list.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spmods.spgram.domain.models.ChatModel
import com.spmods.spgram.presentation.core.ui.Avatar

// Story ring state
private enum class StoryRingState { MY_STORY, UNREAD, VIEWED, ONLINE_ONLY }

@Composable
fun StoryBar(
    currentUser: com.spmods.spgram.domain.models.UserModel?,
    chatListChats: List<ChatModel>,
    onMyStoryClick: () -> Unit,
    onStoryClick: (Long) -> Unit,
    onContactClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    // Separate and order chats:
    // 1. Chats with unread stories (from chat list - contacts & non-contacts)
    // 2. Chats with viewed stories
    // 3. Online chats (chat list first, then saved contacts without stories)
    // 4. All other contacts (no story, no online)

    val chatsWithUnreadStory = remember(chatListChats) {
        chatListChats.filter { it.activeStoryStateType == "unread" }
            .sortedByDescending { it.order }
    }
    val chatsWithViewedStory = remember(chatListChats) {
        chatListChats.filter { it.activeStoryStateType == "watched" || it.activeStoryStateType == "viewed" }
            .sortedByDescending { it.order }
    }
    // Online chats without stories (chat list contacts first)
    val onlineChats = remember(chatListChats) {
        chatListChats.filter { it.isOnline && it.activeStoryStateType == null }
            .sortedByDescending { it.order }
    }
    // Other contacts without stories and not online
    val otherContacts = remember(chatListChats) {
        chatListChats
            .filter {
                !it.isOnline &&
                it.activeStoryStateType == null &&
                !it.isGroup &&
                !it.isChannel
            }
            .take(10) // limit to avoid too many
    }

    val hasAnyContent = chatsWithUnreadStory.isNotEmpty() ||
            chatsWithViewedStory.isNotEmpty() ||
            onlineChats.isNotEmpty() ||
            otherContacts.isNotEmpty()

    if (!hasAnyContent && currentUser == null) return

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. My Story (always first)
        item(key = "my_story") {
            MyStoryItem(
                user = currentUser,
                onClick = onMyStoryClick
            )
        }

        // 2. Unread stories
        items(items = chatsWithUnreadStory, key = { "unread_${it.id}" }) { chat ->
            StoryItem(
                chat = chat,
                ringState = StoryRingState.UNREAD,
                onClick = { onStoryClick(chat.id) }
            )
        }

        // 3. Viewed stories
        items(items = chatsWithViewedStory, key = { "viewed_${it.id}" }) { chat ->
            StoryItem(
                chat = chat,
                ringState = StoryRingState.VIEWED,
                onClick = { onStoryClick(chat.id) }
            )
        }

        // 4. Online contacts (no story)
        items(items = onlineChats, key = { "online_${it.id}" }) { chat ->
            StoryItem(
                chat = chat,
                ringState = StoryRingState.ONLINE_ONLY,
                onClick = { onContactClick(chat.id) }
            )
        }

        // 5. Other contacts
        items(items = otherContacts, key = { "contact_${it.id}" }) { chat ->
            StoryItem(
                chat = chat,
                ringState = null,
                onClick = { onContactClick(chat.id) }
            )
        }
    }
}

@Composable
private fun MyStoryItem(
    user: com.spmods.spgram.domain.models.UserModel?,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "MyStoryScale"
    )

    Column(
        modifier = Modifier
            .width(68.dp)
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.BottomEnd
        ) {
            // Gradient ring for my story
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF833AB4), Color(0xFFE1306C), Color(0xFFFCAF45))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Avatar(
                    path = user?.avatarPath,
                    name = listOfNotNull(user?.firstName, user?.lastName).joinToString(" ").ifBlank { "Me" },
                    size = 54.dp,
                    isOnline = false
                )
            }

            // + Add button
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .offset(x = 2.dp, y = 2.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(Modifier.height(5.dp))
        Text(
            text = "My Story",
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun StoryItem(
    chat: ChatModel,
    ringState: StoryRingState?,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "StoryItemScale"
    )

    Column(
        modifier = Modifier
            .width(68.dp)
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            when (ringState) {
                StoryRingState.MY_STORY,
                StoryRingState.UNREAD -> {
                    // Vibrant gradient ring (unread story)
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFF833AB4), Color(0xFFE1306C), Color(0xFFFCAF45))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Avatar(
                            path = chat.avatarPath,
                            fallbackPath = chat.personalAvatarPath,
                            name = chat.title,
                            size = 54.dp,
                            isOnline = false
                        )
                    }
                }

                StoryRingState.VIEWED -> {
                    // Grey dashed ring (viewed story)
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(2.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Avatar(
                            path = chat.avatarPath,
                            fallbackPath = chat.personalAvatarPath,
                            name = chat.title,
                            size = 54.dp,
                            isOnline = false
                        )
                    }
                }

                StoryRingState.ONLINE_ONLY -> {
                    // Blue/green ring for online
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFF00C6FF), Color(0xFF0078FF))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Avatar(
                            path = chat.avatarPath,
                            fallbackPath = chat.personalAvatarPath,
                            name = chat.title,
                            size = 54.dp,
                            isOnline = false
                        )
                    }

                    // Online dot
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .offset(x = 2.dp, y = 2.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50))
                    )
                }

                null -> {
                    // No ring - plain avatar
                    Avatar(
                        path = chat.avatarPath,
                        fallbackPath = chat.personalAvatarPath,
                        name = chat.title,
                        size = 60.dp,
                        isOnline = chat.isOnline
                    )
                }
            }
        }

        Spacer(Modifier.height(5.dp))
        Text(
            text = chat.title,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color = when (ringState) {
                StoryRingState.VIEWED -> MaterialTheme.colorScheme.onSurfaceVariant
                else -> MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
