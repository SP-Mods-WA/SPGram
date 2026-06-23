package com.spmods.spgram.app.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import com.spmods.spgram.app.R

// ---------------------------------------------------------------------------
// Tab enum  —  3 tabs only
// ---------------------------------------------------------------------------

enum class MainTab { Chats, Stories, Calls }

// ---------------------------------------------------------------------------
// Internal model
// ---------------------------------------------------------------------------

private data class TabItem(
    val tab: MainTab,
    val label: String,
    val fillRes: Int,
    val unfillRes: Int,
    val badgeCount: Int = 0,
    val hasDot: Boolean = false,
)

// ---------------------------------------------------------------------------
// Bottom bar
// ---------------------------------------------------------------------------

@Composable
fun MainBottomBar(
    selectedTab: MainTab,
    chatsUnread: Int = 0,
    hasStories: Boolean = false,
    hasMissedCalls: Boolean = false,
    isDark: Boolean = false,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = isDark

    val tabs = listOf(
        TabItem(
            tab        = MainTab.Chats,
            label      = "Chats",
            fillRes    = R.drawable.sp_chat_fill,
            unfillRes  = R.drawable.sp_chat_unfill,
            badgeCount = chatsUnread,
        ),
        TabItem(
            tab       = MainTab.Stories,
            label     = "Stories",
            fillRes   = R.drawable.sp_story_fill,
            unfillRes = R.drawable.sp_story_unfill,
            hasDot    = hasStories,
        ),
        TabItem(
            tab       = MainTab.Calls,
            label     = "Calls",
            fillRes   = R.drawable.sp_call_fill,
            unfillRes = R.drawable.sp_call_unfill,
            hasDot    = hasMissedCalls,
        ),
    )

    // Dark theme  → near-black surface with subtle top border
    // Light theme → white surface with subtle shadow line
    val barBg   = if (isDark) Color(0xFF1A1A1A) else Color(0xFFFFFFFF)
    val divider = if (isDark) Color(0xFF2C2C2C) else Color(0xFFE0E0E0)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                // solid background
                drawRect(barBg)
                // 1 px top divider
                drawLine(
                    color       = divider,
                    start       = Offset(0f, 0f),
                    end         = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx(),
                )
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            tabs.forEach { item ->
                BottomBarItem(
                    item     = item,
                    selected = selectedTab == item.tab,
                    isDark   = isDark,
                    onClick  = { onTabSelected(item.tab) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

// ---------------------------------------------------------------------------
// Single tab item
// ---------------------------------------------------------------------------

@Composable
private fun BottomBarItem(
    item: TabItem,
    selected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Active colour: cyan-ish for dark, brand primary for light
    val activeColor   = if (isDark) Color(0xFF52C5F5) else MaterialTheme.colorScheme.primary
    val inactiveColor = if (isDark) Color(0xFF7A7A8A) else Color(0xFF8E8E93)

    val iconColor by animateColorAsState(
        targetValue    = if (selected) activeColor else inactiveColor,
        animationSpec  = tween(200),
        label          = "iconColor",
    )
    val labelColor by animateColorAsState(
        targetValue    = if (selected) activeColor else inactiveColor,
        animationSpec  = tween(200),
        label          = "labelColor",
    )

    // Pill indicator behind icon when selected
    val pillWidth by animateDpAsState(
        targetValue   = if (selected) 48.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label         = "pillWidth",
    )
    val pillColor = if (isDark)
        Color(0xFF52C5F5).copy(alpha = 0.15f)
    else
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)

    Column(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onClick,
            )
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(contentAlignment = Alignment.Center) {
            // pill background
            if (pillWidth > 0.dp) {
                Box(
                    modifier = Modifier
                        .size(width = pillWidth, height = 32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(pillColor)
                )
            }

            BadgedBox(
                badge = {
                    when {
                        item.badgeCount > 0 -> Badge(
                            containerColor = if (isDark) Color(0xFFFF453A) else Color(0xFFFF3B30),
                            contentColor   = Color.White,
                        ) {
                            // Unlimited badge — show full number up to 9999, then "∞"
                            val label = when {
                                item.badgeCount >= 10000 -> "∞"
                                else                     -> item.badgeCount.toString()
                            }
                            Text(
                                text       = label,
                                fontSize   = 10.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        item.hasDot -> Badge(
                            containerColor = if (isDark) Color(0xFF52C5F5) else MaterialTheme.colorScheme.primary,
                            modifier       = Modifier.size(8.dp),
                        )
                        else -> {}
                    }
                },
            ) {
                Image(
                    painter     = painterResource(
                        id = if (selected) item.fillRes else item.unfillRes
                    ),
                    contentDescription = item.label,
                    modifier    = Modifier.size(26.dp),
                    colorFilter = ColorFilter.tint(iconColor),
                )
            }
        }

        Spacer(Modifier.height(3.dp))

        Text(
            text  = item.label,
            color = labelColor,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                fontSize   = 11.sp,
            ),
        )
    }
}
