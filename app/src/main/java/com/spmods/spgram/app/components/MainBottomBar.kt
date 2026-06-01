package com.spmods.spgram.app.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------------------
// Tab enum — 3 tabs only
// ---------------------------------------------------------------------------

enum class MainTab { Chats, Stories, Calls }

// ---------------------------------------------------------------------------
// Icons — inline vectors
// ---------------------------------------------------------------------------

private val IconChats: ImageVector by lazy {
    ImageVector.Builder("Chats", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(20f, 2f); horizontalLineTo(4f)
            curveTo(2.9f, 2f, 2f, 2.9f, 2f, 4f); verticalLineTo(22f)
            lineTo(6f, 18f); horizontalLineTo(20f)
            curveTo(21.1f, 18f, 22f, 17.1f, 22f, 16f); verticalLineTo(4f)
            curveTo(22f, 2.9f, 21.1f, 2f, 20f, 2f); close()
            moveTo(20f, 16f); horizontalLineTo(6f); lineTo(4f, 18f)
            verticalLineTo(4f); horizontalLineTo(20f); verticalLineTo(16f); close()
        }
    }.build()
}

private val IconStories: ImageVector by lazy {
    ImageVector.Builder("Stories", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.Black)) {
            // Circle ring (story ring)
            moveTo(12f, 2f)
            curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
            curveTo(2f, 17.52f, 6.48f, 22f, 12f, 22f)
            curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
            curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
            close()
            moveTo(12f, 20f)
            curveTo(7.59f, 20f, 4f, 16.41f, 4f, 12f)
            curveTo(4f, 7.59f, 7.59f, 4f, 12f, 4f)
            curveTo(16.41f, 4f, 20f, 7.59f, 20f, 12f)
            curveTo(20f, 16.41f, 16.41f, 20f, 12f, 20f)
            close()
            // Play triangle inside
            moveTo(10f, 8.5f); lineTo(16f, 12f); lineTo(10f, 15.5f); close()
        }
    }.build()
}

private val IconCalls: ImageVector by lazy {
    ImageVector.Builder("Calls", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(6.62f, 10.79f)
            curveTo(8.06f, 13.62f, 10.38f, 15.93f, 13.21f, 17.38f)
            lineTo(15.41f, 15.18f)
            curveTo(15.68f, 14.91f, 16.08f, 14.82f, 16.43f, 14.94f)
            curveTo(17.55f, 15.31f, 18.76f, 15.51f, 20f, 15.51f)
            curveTo(20.55f, 15.51f, 21f, 15.96f, 21f, 16.51f)
            verticalLineTo(20f)
            curveTo(21f, 20.55f, 20.55f, 21f, 20f, 21f)
            curveTo(10.61f, 21f, 3f, 13.39f, 3f, 4f)
            curveTo(3f, 3.45f, 3.45f, 3f, 4f, 3f)
            horizontalLineTo(7.5f)
            curveTo(8.05f, 3f, 8.5f, 3.45f, 8.5f, 4f)
            curveTo(8.5f, 5.25f, 8.7f, 6.45f, 9.07f, 7.57f)
            curveTo(9.19f, 7.92f, 9.1f, 8.31f, 8.82f, 8.59f)
            lineTo(6.62f, 10.79f)
            close()
        }
    }.build()
}

// ---------------------------------------------------------------------------
// Data
// ---------------------------------------------------------------------------

private data class TabItem(
    val tab: MainTab,
    val label: String,
    val icon: ImageVector,
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
        TabItem(MainTab.Chats,   "Chats",   IconChats,   badgeCount = chatsUnread),
        TabItem(MainTab.Stories, "Stories", IconStories, hasDot = hasStories),
        TabItem(MainTab.Calls,   "Calls",   IconCalls,   hasDot = hasMissedCalls),
    )

    val surfaceColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primary

    // Liquid Glass container
    Column(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                // Frosted glass base
                drawRect(surfaceColor.copy(alpha = 0.72f))
                // Top shimmer line
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
                // Subtle gradient overlay (glass depth)
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
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.12f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "Scale",
    )
    val pillWidth by animateDpAsState(
        targetValue = if (selected) 56.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 350f),
        label = "PillWidth",
    )
    // Liquid glass pill glow when selected
    val glowAlpha by animateFloatAsState(
        targetValue = if (selected) 0.18f else 0f,
        animationSpec = tween(300), label = "GlowAlpha",
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (selected)
                    MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha)
                else Color.Transparent
            )
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
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = iconColor,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale },
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
