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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------------------
// Tab enum
// ---------------------------------------------------------------------------

enum class MainTab { Chats, Groups, Updates, Settings }

// ---------------------------------------------------------------------------
// Icons — inline vectors, zero R references
// ---------------------------------------------------------------------------

private val IconChats: ImageVector by lazy {
    ImageVector.Builder("Chats", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(androidx.compose.ui.graphics.Color.Black)) {
            // speech bubble outline
            moveTo(20f, 2f)
            horizontalLineTo(4f)
            curveTo(2.9f, 2f, 2f, 2.9f, 2f, 4f)
            verticalLineTo(22f)
            lineTo(6f, 18f)
            horizontalLineTo(20f)
            curveTo(21.1f, 18f, 22f, 17.1f, 22f, 16f)
            verticalLineTo(4f)
            curveTo(22f, 2.9f, 21.1f, 2f, 20f, 2f)
            close()
            moveTo(20f, 16f)
            horizontalLineTo(6f)
            lineTo(4f, 18f)
            verticalLineTo(4f)
            horizontalLineTo(20f)
            verticalLineTo(16f)
            close()
        }
    }.build()
}

private val IconGroups: ImageVector by lazy {
    ImageVector.Builder("Groups", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(androidx.compose.ui.graphics.Color.Black)) {
            // person 1
            moveTo(16f, 11f)
            curveTo(17.66f, 11f, 18.99f, 9.66f, 18.99f, 8f)
            curveTo(18.99f, 6.34f, 17.66f, 5f, 16f, 5f)
            curveTo(14.34f, 5f, 13f, 6.34f, 13f, 8f)
            curveTo(13f, 9.66f, 14.34f, 11f, 16f, 11f)
            close()
            // person 2
            moveTo(8f, 11f)
            curveTo(9.66f, 11f, 10.99f, 9.66f, 10.99f, 8f)
            curveTo(10.99f, 6.34f, 9.66f, 5f, 8f, 5f)
            curveTo(6.34f, 5f, 5f, 6.34f, 5f, 8f)
            curveTo(5f, 9.66f, 6.34f, 11f, 8f, 11f)
            close()
            // group body left
            moveTo(8f, 13f)
            curveTo(5.67f, 13f, 1f, 14.17f, 1f, 16.5f)
            verticalLineTo(19f)
            horizontalLineTo(15f)
            verticalLineTo(16.5f)
            curveTo(15f, 14.17f, 10.33f, 13f, 8f, 13f)
            close()
            // group body right
            moveTo(16f, 13f)
            curveTo(15.71f, 13f, 15.38f, 13.02f, 15.03f, 13.05f)
            curveTo(16.19f, 13.89f, 17f, 15.02f, 17f, 16.5f)
            verticalLineTo(19f)
            horizontalLineTo(23f)
            verticalLineTo(16.5f)
            curveTo(23f, 14.17f, 18.33f, 13f, 16f, 13f)
            close()
        }
    }.build()
}

private val IconUpdates: ImageVector by lazy {
    ImageVector.Builder("Updates", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(androidx.compose.ui.graphics.Color.Black)) {
            // megaphone
            moveTo(18f, 11f)
            verticalLineTo(13f)
            horizontalLineTo(22f)
            verticalLineTo(11f)
            horizontalLineTo(18f)
            close()
            moveTo(16f, 17.61f)
            curveTo(16.96f, 18.32f, 18.21f, 19.26f, 19.2f, 20f)
            curveTo(19.6f, 19.47f, 20f, 18.93f, 20.4f, 18.4f)
            curveTo(19.41f, 17.66f, 18.16f, 16.72f, 17.2f, 16f)
            curveTo(16.8f, 16.54f, 16.4f, 17.08f, 16f, 17.61f)
            close()
            moveTo(20.4f, 5.6f)
            curveTo(20f, 5.07f, 19.6f, 4.53f, 19.2f, 4f)
            curveTo(18.21f, 4.74f, 16.96f, 5.68f, 16f, 6.4f)
            curveTo(16.4f, 6.93f, 16.8f, 7.47f, 17.2f, 8f)
            curveTo(18.16f, 7.29f, 19.41f, 6.35f, 20.4f, 5.6f)
            close()
            moveTo(4f, 9f)
            curveTo(2.9f, 9f, 2f, 9.9f, 2f, 11f)
            verticalLineTo(13f)
            curveTo(2f, 14.1f, 2.9f, 15f, 4f, 15f)
            horizontalLineTo(5f)
            verticalLineTo(19f)
            horizontalLineTo(7f)
            verticalLineTo(15f)
            horizontalLineTo(8f)
            lineTo(13f, 18f)
            verticalLineTo(6f)
            lineTo(8f, 9f)
            horizontalLineTo(4f)
            close()
            moveTo(15.5f, 12f)
            curveTo(15.5f, 10.67f, 14.92f, 9.47f, 14f, 8.65f)
            verticalLineTo(15.34f)
            curveTo(14.92f, 14.53f, 15.5f, 13.33f, 15.5f, 12f)
            close()
        }
    }.build()
}

private val IconSettings: ImageVector by lazy {
    ImageVector.Builder("Settings", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(androidx.compose.ui.graphics.Color.Black)) {
            moveTo(19.14f, 12.94f)
            curveTo(19.18f, 12.64f, 19.2f, 12.33f, 19.2f, 12f)
            curveTo(19.2f, 11.68f, 19.18f, 11.36f, 19.13f, 11.06f)
            lineTo(21.16f, 9.48f)
            curveTo(21.34f, 9.34f, 21.39f, 9.07f, 21.28f, 8.87f)
            lineTo(19.36f, 5.55f)
            curveTo(19.24f, 5.33f, 18.99f, 5.26f, 18.77f, 5.33f)
            lineTo(16.38f, 6.29f)
            curveTo(15.88f, 5.91f, 15.35f, 5.59f, 14.76f, 5.35f)
            lineTo(14.4f, 2.81f)
            curveTo(14.36f, 2.57f, 14.16f, 2.4f, 13.92f, 2.4f)
            horizontalLineTo(10.08f)
            curveTo(9.84f, 2.4f, 9.65f, 2.57f, 9.61f, 2.81f)
            lineTo(9.25f, 5.35f)
            curveTo(8.66f, 5.59f, 8.12f, 5.92f, 7.63f, 6.29f)
            lineTo(5.24f, 5.33f)
            curveTo(5.02f, 5.25f, 4.77f, 5.33f, 4.65f, 5.55f)
            lineTo(2.74f, 8.87f)
            curveTo(2.62f, 9.08f, 2.66f, 9.34f, 2.86f, 9.48f)
            lineTo(4.89f, 11.06f)
            curveTo(4.84f, 11.36f, 4.8f, 11.69f, 4.8f, 12f)
            curveTo(4.8f, 12.31f, 4.82f, 12.64f, 4.87f, 12.94f)
            lineTo(2.84f, 14.52f)
            curveTo(2.66f, 14.66f, 2.61f, 14.93f, 2.72f, 15.13f)
            lineTo(4.64f, 18.45f)
            curveTo(4.76f, 18.67f, 5.01f, 18.74f, 5.23f, 18.67f)
            lineTo(7.62f, 17.71f)
            curveTo(8.12f, 18.09f, 8.65f, 18.41f, 9.24f, 18.65f)
            lineTo(9.6f, 21.19f)
            curveTo(9.65f, 21.43f, 9.84f, 21.6f, 10.08f, 21.6f)
            horizontalLineTo(13.92f)
            curveTo(14.16f, 21.6f, 14.36f, 21.43f, 14.39f, 21.19f)
            lineTo(14.75f, 18.65f)
            curveTo(15.34f, 18.41f, 15.88f, 18.09f, 16.37f, 17.71f)
            lineTo(18.76f, 18.67f)
            curveTo(18.98f, 18.75f, 19.23f, 18.67f, 19.35f, 18.45f)
            lineTo(21.27f, 15.13f)
            curveTo(21.39f, 14.91f, 21.34f, 14.66f, 21.15f, 14.52f)
            lineTo(19.14f, 12.94f)
            close()
            moveTo(12f, 15.6f)
            curveTo(10.02f, 15.6f, 8.4f, 13.98f, 8.4f, 12f)
            curveTo(8.4f, 10.02f, 10.02f, 8.4f, 12f, 8.4f)
            curveTo(13.98f, 8.4f, 15.6f, 10.02f, 15.6f, 12f)
            curveTo(15.6f, 13.98f, 13.98f, 15.6f, 12f, 15.6f)
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
// Public composable
// ---------------------------------------------------------------------------

@Composable
fun MainBottomBar(
    selectedTab: MainTab,
    chatsUnread: Int = 0,
    groupsUnread: Int = 0,
    hasUpdates: Boolean = false,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = listOf(
        TabItem(MainTab.Chats,    "Chats",    IconChats,    badgeCount = chatsUnread),
        TabItem(MainTab.Groups,   "Groups",   IconGroups,   badgeCount = groupsUnread),
        TabItem(MainTab.Updates,  "Updates",  IconUpdates,  hasDot = hasUpdates),
        TabItem(MainTab.Settings, "Settings", IconSettings),
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
    ) {
        Column {
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
}

// ---------------------------------------------------------------------------
// Private item composable
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
                      else         MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(220),
        label = "IconColor",
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
        // Pill indicator at top
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
                        contentColor  = MaterialTheme.colorScheme.onPrimary,
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
