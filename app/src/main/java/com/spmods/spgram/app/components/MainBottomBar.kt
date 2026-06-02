package com.spmods.spgram.app.components

import com.spmods.spgram.app.R

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.Image

// ---------------------------------------------------------------------------
// Tab enum
// ---------------------------------------------------------------------------

enum class MainTab { Chats, Stories, Calls }

// ---------------------------------------------------------------------------
// Data
// ---------------------------------------------------------------------------

private data class TabItem(
    val tab: MainTab,
    val label: String,
    val fillIcon: Int,
    val unfillIcon: Int,
    val badgeCount: Int = 0,
    val hasDot: Boolean = false,
)

// ---------------------------------------------------------------------------
// Liquid Glass bottom bar
// ---------------------------------------------------------------------------

@Composable
fun MainBottomBar(
    selectedTab: MainTab,
    chatsUnread: Int = 0,
    hasStories: Boolean = false,
    hasMissedCalls: Boolean = false,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = listOf(
        TabItem(MainTab.Chats,   "Chats",   R.drawable.sp_chat_fill,   R.drawable.sp_chat_unfill,   badgeCount = chatsUnread),
        TabItem(MainTab.Stories, "Stories", R.drawable.sp_story_fill,  R.drawable.sp_story_unfill,  hasDot = hasStories),
        TabItem(MainTab.Calls,   "Calls",   R.drawable.sp_call_fill,   R.drawable.sp_call_unfill,   hasDot = hasMissedCalls),
    )

    val surfaceColor = MaterialTheme.colorScheme.surface

    Column(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawRect(surfaceColor.copy(alpha = 0.72f))
                drawRect(
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.18f),
                            Color.White.copy(alpha = 0.32f),
                            Color.White.copy(alpha = 0.18f),
                            Color.Transparent,
                        )
                    ),
                    topLeft = Offset.Zero,
                    size = size.copy(height = 1.dp.toPx())
                )
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.10f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.04f),
                        )
                    )
                )
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { item ->
                BottomBarItem(
                    item = item,
                    selected = selectedTab == item.tab,
                    onClick = { onTabSelected(item.tab) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

// ---------------------------------------------------------------------------
// Tab item
// ---------------------------------------------------------------------------

@Composable
private fun BottomBarItem(
    item: TabItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val iconColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(220), label = "IconColor",
    )
    val pillWidth by animateDpAsState(
        targetValue = if (selected) 56.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 350f),
        label = "PillWidth",
    )

    Column(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Top pill indicator
        Box(
            modifier = Modifier
                .size(width = pillWidth, height = 3.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primary),
        )

        Spacer(Modifier.height(6.dp))

        BadgedBox(
            badge = {
                when {
                    item.badgeCount > 0 -> Badge(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ) {
                        Text(
                            text = if (item.badgeCount > 99) "99+" else item.badgeCount.toString(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    item.hasDot -> Badge(containerColor = MaterialTheme.colorScheme.primary)
                    else -> {}
                }
            },
        ) {
            Image(
                painter = painterResource(
                    id = if (selected) item.fillIcon else item.unfillIcon
                ),
                contentDescription = item.label,
                modifier = Modifier.size(24.dp),
                colorFilter = ColorFilter.tint(iconColor),
            )
        }

        Spacer(Modifier.height(3.dp))

        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 11.sp,
            ),
            color = iconColor,
        )
    }
}
