package com.spmods.spgram.presentation.settings.homeScreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.SwipeLeft
import androidx.compose.material.icons.rounded.TabletAndroid
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.window.core.layout.WindowSizeClass
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.spmods.spgram.presentation.R
import com.spmods.spgram.presentation.core.ui.ItemPosition
import com.spmods.spgram.presentation.core.ui.SettingsSwitchTile
import com.spmods.spgram.presentation.settings.chatSettings.components.ChatListPreview
import com.spmods.spgram.presentation.settings.homeScreen.components.HomeScreenPreview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(component: HomeScreenComponent) {
    val state by component.state.subscribeAsState()
    val adaptiveInfo = currentWindowAdaptiveInfo()
    val isTablet = adaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

    val greenColor = Color(0xFF34A853)
    val orangeColor = Color(0xFFF9AB00)
    val tealColor = Color(0xFF00BFA5)

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
                            contentDescription = stringResource(R.string.cd_back)
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

            // Home Screen live preview
            item {
                HomeScreenPreview(
                    isArchivePinned = state.isArchivePinned,
                    chatListMessageLines = state.chatListMessageLines,
                    showChatListPhotos = state.showChatListPhotos
                )
            }

            // Chat List section
            item {
                SectionHeader(stringResource(R.string.chat_list_header))
                SettingsSwitchTile(
                    icon = Icons.Rounded.Archive,
                    title = stringResource(R.string.pin_archived_chats_title),
                    subtitle = stringResource(R.string.pin_archived_chats_subtitle),
                    checked = state.isArchivePinned,
                    iconColor = orangeColor,
                    position = ItemPosition.TOP,
                    onCheckedChange = component::onArchivePinnedChanged
                )
                AnimatedVisibility(
                    visible = state.isArchivePinned,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    SettingsSwitchTile(
                        icon = Icons.Rounded.Archive,
                        title = stringResource(R.string.always_show_pinned_archive_title),
                        subtitle = stringResource(R.string.always_show_pinned_archive_subtitle),
                        checked = state.isArchiveAlwaysVisible,
                        iconColor = orangeColor,
                        position = ItemPosition.MIDDLE,
                        onCheckedChange = component::onArchiveAlwaysVisibleChanged
                    )
                }
                if (isTablet) {
                    SettingsSwitchTile(
                        icon = Icons.Rounded.TabletAndroid,
                        title = stringResource(R.string.tablet_interface_title),
                        subtitle = stringResource(R.string.tablet_interface_subtitle),
                        checked = state.isTabletInterfaceEnabled,
                        iconColor = greenColor,
                        position = ItemPosition.MIDDLE,
                        onCheckedChange = component::onTabletInterfaceEnabledChanged
                    )
                }
                SettingsSwitchTile(
                    icon = Icons.Rounded.SwipeLeft,
                    title = stringResource(R.string.drag_to_back_title),
                    subtitle = stringResource(R.string.drag_to_back_subtitle),
                    checked = state.isDragToBackEnabled,
                    iconColor = tealColor,
                    position = ItemPosition.BOTTOM,
                    onCheckedChange = component::onDragToBackChanged
                )
            }

            // Chat List preview + Two-line/Three-line + Show Photos
            item {
                ChatListPreview(
                    messageLines = state.chatListMessageLines,
                    showPhotos = state.showChatListPhotos,
                    position = ItemPosition.TOP
                )

                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(1, 2).forEach { lines ->
                                val label = if (lines == 1) stringResource(R.string.two_line_label)
                                else stringResource(R.string.three_line_label)
                                Surface(
                                    onClick = { component.onChatListMessageLinesChanged(lines) },
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (state.chatListMessageLines == lines)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceContainerLow,
                                    border = if (state.chatListMessageLines == lines) BorderStroke(
                                        2.dp, MaterialTheme.colorScheme.primary
                                    ) else null,
                                    modifier = Modifier.weight(1f).height(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (state.chatListMessageLines == lines)
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = if (state.chatListMessageLines == lines)
                                                FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))

                SettingsSwitchTile(
                    icon = Icons.Rounded.AccountCircle,
                    title = stringResource(R.string.show_photos_title),
                    subtitle = stringResource(R.string.show_photos_subtitle),
                    checked = state.showChatListPhotos,
                    iconColor = Color(0xFFFF6D66),
                    position = ItemPosition.BOTTOM,
                    onCheckedChange = component::onShowChatListPhotosChanged
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp, top = 16.dp),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold
    )
}
