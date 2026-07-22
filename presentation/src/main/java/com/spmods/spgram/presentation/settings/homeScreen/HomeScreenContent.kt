package com.spmods.spgram.presentation.settings.homeScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.BottomNavigation
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.spmods.spgram.presentation.core.ui.ItemPosition
import com.spmods.spgram.presentation.core.ui.SettingsSwitchTile
import com.spmods.spgram.presentation.settings.homeScreen.components.HomeScreenPreview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(component: HomeScreenComponent) {
    val state by component.state.subscribeAsState()

    val blueColor = Color(0xFF4285F4)
    val greenColor = Color(0xFF34A853)
    val orangeColor = Color(0xFFF9AB00)
    val tealColor = Color(0xFF00BFA5)
    val purpleColor = Color(0xFF9C27B0)

    Scaffold(
        modifier = Modifier.semantics { contentDescription = "HomeScreenContent" },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Home Screen",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = component::onBackClicked) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Live preview
            item {
                HomeScreenPreview(
                    showStories = state.showStories,
                    showArchive = state.showArchive,
                    showBottomBarLabels = state.showBottomBarLabels,
                    showOnlineStatus = state.showOnlineStatus,
                    isCompactChatList = state.isCompactChatList
                )
            }

            // Chat List section
            item {
                SectionHeader("Chat List")
                SettingsSwitchTile(
                    icon = Icons.Rounded.AutoStories,
                    title = "Show Stories",
                    subtitle = "Display stories bar at the top of the chat list",
                    checked = state.showStories,
                    iconColor = purpleColor,
                    position = ItemPosition.TOP,
                    onCheckedChange = component::onShowStoriesChanged
                )
                SettingsSwitchTile(
                    icon = Icons.Rounded.Archive,
                    title = "Show Archived Chats",
                    subtitle = "Pin the archive folder at the top of the chat list",
                    checked = state.showArchive,
                    iconColor = orangeColor,
                    position = ItemPosition.MIDDLE,
                    onCheckedChange = component::onShowArchiveChanged
                )
                SettingsSwitchTile(
                    icon = Icons.Rounded.Compress,
                    title = "Compact Chat List",
                    subtitle = "Smaller chat rows with less padding",
                    checked = state.isCompactChatList,
                    iconColor = tealColor,
                    position = ItemPosition.BOTTOM,
                    onCheckedChange = component::onCompactChatListChanged
                )
            }

            // Navigation section
            item {
                SectionHeader("Navigation")
                SettingsSwitchTile(
                    icon = Icons.Rounded.BottomNavigation,
                    title = "Show Bottom Bar Labels",
                    subtitle = "Show text labels under the navigation icons",
                    checked = state.showBottomBarLabels,
                    iconColor = blueColor,
                    position = ItemPosition.STANDALONE,
                    onCheckedChange = component::onShowBottomBarLabelsChanged
                )
            }

            // Presence section
            item {
                SectionHeader("Presence")
                SettingsSwitchTile(
                    icon = Icons.Rounded.AccountCircle,
                    title = "Show Online Status",
                    subtitle = "Display a green dot on the avatar of online contacts",
                    checked = state.showOnlineStatus,
                    iconColor = greenColor,
                    position = ItemPosition.STANDALONE,
                    onCheckedChange = component::onShowOnlineStatusChanged
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        modifier = androidx.compose.ui.Modifier.padding(
            start = 12.dp,
            bottom = 8.dp,
            top = 16.dp
        ),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold
    )
}
