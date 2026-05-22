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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spmods.spgram.R

/**
 * Bottom navigation tab for SPGram.
 *
 * Tabs:
 *  - Chats  → shows ChatListContent (bots & regular chats both live here via folders)
 *  - Groups → navigates to a Groups-filtered view (future: dedicated screen)
 *  - Updates → navigates to Updates screen (channels + stories)
 *  - Settings → navigates to Settings
 */

enum class MainTab {
    Chats, Groups, Updates, Settings
}

data class MainTabItem(
    val tab: MainTab,
    val labelRes: Int,
    val iconRes: Int,
    val badgeCount: Int = 0,
    val hasDot: Boolean = false,
)

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
        MainTabItem(
            tab = MainTab.Chats,
            labelRes = R.string.tab_chats,
            iconRes = R.drawable.tab_chats,
            badgeCount = chatsUnread,
        ),
        MainTabItem(
            tab = MainTab.Groups,
            labelRes = R.string.tab_groups,
            iconRes = R.drawable.tab_groups,
            badgeCount = groupsUnread,
        ),
        MainTabItem(
            tab = MainTab.Updates,
            labelRes = R.string.tab_updates,
            iconRes = R.drawable.tab_updates,
            hasDot = hasUpdates,
        ),
        MainTabItem(
            tab = MainTab.Settings,
            labelRes = R.string.tab_settings,
            iconRes = R.drawable.tab_settings,
        ),
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
            // Respect system navigation bar insets
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

@Composable
private fun BottomBarItem(
    item: MainTabItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val iconColor by animateColorAsState(
        targetValue = if (selected)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(220),
        label = "IconColor",
    )

    val labelColor by animateColorAsState(
        targetValue = if (selected)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(220),
        label = "LabelColor",
    )

    val scale by animateFloatAsState(
        targetValue = if (selected) 1.12f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "Scale",
    )

    val pillWidth by animateDpAsState(
        targetValue = if (selected) 64.dp else 0.dp,
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
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Selected pill indicator
        Box(
            modifier = Modifier
                .size(width = pillWidth, height = 3.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primary),
        )

        Spacer(Modifier.height(4.dp))

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
                    item.hasDot -> Badge(
                        containerColor = MaterialTheme.colorScheme.primary,
                    )
                    else -> {}
                }
            },
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(item.iconRes),
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
            )
        }

        Spacer(Modifier.height(2.dp))

        Text(
            text = androidx.compose.ui.res.stringResource(item.labelRes),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 11.sp,
            ),
            color = labelColor,
        )
    }
}
