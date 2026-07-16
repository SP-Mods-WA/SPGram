package com.spmods.spgram.presentation.features.chats.list.components

import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spmods.spgram.domain.models.ChatModel
import com.spmods.spgram.domain.models.ChatType
import com.spmods.spgram.domain.models.StoryModel
import com.spmods.spgram.domain.models.UserModel
import com.spmods.spgram.presentation.core.ui.Avatar

// ── Ring visual states ────────────────────────────────────────────────────────
private enum class StoryRingState { UNREAD, VIEWED, ONLINE_ONLY }

// ── Actions passed back to parent ─────────────────────────────────────────────
sealed interface StoryBarAction {
    data class MyStoryTap(val hasStory: Boolean) : StoryBarAction
    data class OpenStory(val chatId: Long) : StoryBarAction
    data class OpenChat(val chatId: Long) : StoryBarAction
}

// ── Main bar ──────────────────────────────────────────────────────────────────
@Composable
fun StoryBar(
    currentUser: UserModel?,
    currentUserId: Long?,
    chatListChats: List<ChatModel>,
    myStories: List<StoryModel>,
    onAction: (StoryBarAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredChats = remember(chatListChats) {
    chatListChats.filter { !it.isGroup && !it.isChannel }
}
    val unread = remember(filteredChats) {
        filteredChats.filter { it.activeStoryStateType == "unread" }.sortedByDescending { it.order }
    }
    val viewed = remember(filteredChats) {
        filteredChats.filter {
            it.activeStoryStateType == "watched" || it.activeStoryStateType == "viewed"
        }.sortedByDescending { it.order }
    }
    // Non-contacts online (chat list only, no story)
    val onlineNonContacts = remember(filteredChats) {
        filteredChats.filter {
            it.isOnline && it.activeStoryStateType == null && it.type == ChatType.PRIVATE
        }.sortedByDescending { it.order }
    }

    // Track which chatId is currently loading (spinning ring)
    var loadingChatId by remember { mutableStateOf<Long?>(null) }

    LazyRow(
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
        contentPadding = PaddingValues(horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // 1. My Story
        item(key = "my_story") {
            MyStoryBubble(
                user = currentUser,
                hasStory = myStories.isNotEmpty(),
                onClick = { onAction(StoryBarAction.MyStoryTap(myStories.isNotEmpty())) }
            )
        }

        // 2. Unread stories — ring spins only while loading
        items(items = unread, key = { "unread_${it.id}" }) { chat ->
            StoryBubble(
                chat = chat,
                ringState = StoryRingState.UNREAD,
                isLoading = loadingChatId == chat.id,
                onClick = {
                    loadingChatId = chat.id
                    onAction(StoryBarAction.OpenStory(chat.id))
                }
            )
        }

        // 3. Viewed stories
        items(items = viewed, key = { "viewed_${it.id}" }) { chat ->
            StoryBubble(
                chat = chat,
                ringState = StoryRingState.VIEWED,
                isLoading = false,
                onClick = { onAction(StoryBarAction.OpenStory(chat.id)) }
            )
        }

        // 4. Online non-contacts (no story)
        items(items = onlineNonContacts, key = { "online_nc_${it.id}" }) { chat ->
            StoryBubble(
                chat = chat,
                ringState = StoryRingState.ONLINE_ONLY,
                isLoading = false,
                onClick = { onAction(StoryBarAction.OpenChat(chat.id)) }
            )
        }
    }

    // When story viewer opens, stop the loading spinner
    LaunchedEffect(loadingChatId) {
        if (loadingChatId != null) {
            kotlinx.coroutines.delay(3000) // fallback: stop after 3s if viewer didn't open
            loadingChatId = null
        }
    }
}

// ── Expose a way for parent to stop loading indicator once viewer is shown ────
// Parent calls StoryBar with stopLoadingSignal when storyViewerStories becomes non-empty.
// We handle this via a separate composable so state lives in StoryBar's scope above.

// ── My Story bubble ───────────────────────────────────────────────────────────
@Composable
private fun MyStoryBubble(
    user: UserModel?,
    hasStory: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "MyStoryScale"
    )

    Column(
        modifier = Modifier
            .width(70.dp)
            .scale(scale)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            val ringBrush = if (hasStory)
                Brush.linearGradient(listOf(Color(0xFF833AB4), Color(0xFFE1306C), Color(0xFFFCAF45)))
            else
                Brush.linearGradient(listOf(MaterialTheme.colorScheme.outline, MaterialTheme.colorScheme.outline))

            Box(
                modifier = Modifier.size(62.dp).clip(CircleShape).background(ringBrush),
                contentAlignment = Alignment.Center
            ) {
                Avatar(
                    path = user?.avatarPath,
                    name = listOfNotNull(user?.firstName, user?.lastName).joinToString(" ").ifBlank { "Me" },
                    size = 56.dp,
                    isOnline = false
                )
            }
            Box(
                modifier = Modifier
                    .size(20.dp).offset(x = 2.dp, y = 2.dp)
                    .clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Add, null, tint = Color.White, modifier = Modifier.size(13.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "My Story",
            style = MaterialTheme.typography.labelSmall, fontSize = 10.sp,
            fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ── Story / contact bubble ────────────────────────────────────────────────────
@Composable
private fun StoryBubble(
    chat: ChatModel,
    ringState: StoryRingState?,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "BubbleScale"
    )

    // Ring rotation — always computed but only applied when isLoading=true
    val infiniteTransition = rememberInfiniteTransition(label = "ring_anim")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(600, easing = LinearEasing)),
        label = "RingRotation"
    )

    Column(
        modifier = Modifier
            .width(70.dp).scale(scale)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            when (ringState) {
                StoryRingState.UNREAD -> {
                    val brush = Brush.sweepGradient(
                        listOf(Color(0xFF833AB4), Color(0xFFE1306C), Color(0xFFFCAF45), Color(0xFF833AB4))
                    )
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            // Rotation ONLY applied when isLoading (i.e. after tap, while fetching)
                            .graphicsLayer { if (isLoading) rotationZ = rotation }
                            .clip(CircleShape)
                            .background(brush),
                        contentAlignment = Alignment.Center
                    ) {
                        Avatar(
                            path = chat.avatarPath, fallbackPath = chat.personalAvatarPath,
                            name = chat.title, size = 56.dp, isOnline = false
                        )
                    }
                }

                StoryRingState.VIEWED -> {
                    Box(
                        modifier = Modifier
                            .size(62.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Avatar(
                            path = chat.avatarPath, fallbackPath = chat.personalAvatarPath,
                            name = chat.title, size = 56.dp, isOnline = false
                        )
                    }
                }

                StoryRingState.ONLINE_ONLY -> {
                    Box(
                        modifier = Modifier
                            .size(62.dp).clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Color(0xFF00C6FF), Color(0xFF0072FF)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Avatar(
                            path = chat.avatarPath, fallbackPath = chat.personalAvatarPath,
                            name = chat.title, size = 56.dp, isOnline = false
                        )
                    }
                    // Green online dot
                    Box(
                        modifier = Modifier
                            .size(15.dp).offset(x = 2.dp, y = 2.dp)
                            .clip(CircleShape).background(MaterialTheme.colorScheme.surface)
                            .padding(2.5.dp).clip(CircleShape).background(Color(0xFF4CAF50))
                    )
                }

                null -> {
                    Avatar(
                        path = chat.avatarPath, fallbackPath = chat.personalAvatarPath,
                        name = chat.title, size = 62.dp, isOnline = chat.isOnline
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(
            text = chat.title,
            style = MaterialTheme.typography.labelSmall, fontSize = 10.sp,
            fontWeight = if (ringState == StoryRingState.UNREAD) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
            color = if (ringState == StoryRingState.VIEWED)
                MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ── My Story bottom sheet ─────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyStoryOptionsSheet(
    hasStory: Boolean,
    onViewStory: () -> Unit,
    onUploadStory: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "My Story",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (hasStory) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth().clickable { onViewStory(); onDismiss() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(Icons.Rounded.PlayCircle, null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                        Text("View Story", style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth().clickable { onUploadStory(); onDismiss() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(Icons.Rounded.PhotoCamera, null,
                        tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
                    Text("Upload Story", style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}
