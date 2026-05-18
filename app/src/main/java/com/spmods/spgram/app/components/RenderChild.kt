package com.spmods.spgram.app.components

import androidx.compose.runtime.Composable
import com.spmods.spgram.presentation.features.auth.AuthContent
import com.spmods.spgram.presentation.features.chats.conversation.ChatContent
import com.spmods.spgram.presentation.features.chats.creation.NewChatContent
import com.spmods.spgram.presentation.features.chats.list.ChatListContent
import com.spmods.spgram.presentation.features.profile.ProfileContent
import com.spmods.spgram.presentation.features.profile.admin.AdminManageContent
import com.spmods.spgram.presentation.features.profile.admin.ChatEditContent
import com.spmods.spgram.presentation.features.profile.admin.ChatPermissionsContent
import com.spmods.spgram.presentation.features.profile.admin.MemberListContent
import com.spmods.spgram.presentation.features.profile.logs.ProfileLogsContent
import com.spmods.spgram.presentation.features.webview.InternalWebView
import com.spmods.spgram.presentation.root.RootComponent
import com.spmods.spgram.presentation.root.StartupContent
import com.spmods.spgram.presentation.settings.about.AboutContent
import com.spmods.spgram.presentation.settings.adblock.AdBlockContent
import com.spmods.spgram.presentation.settings.chatSettings.ChatSettingsContent
import com.spmods.spgram.presentation.settings.dataStorage.DataStorageContent
import com.spmods.spgram.presentation.settings.debug.DebugContent
import com.spmods.spgram.presentation.settings.folders.FoldersContent
import com.spmods.spgram.presentation.settings.networkUsage.NetworkUsageContent
import com.spmods.spgram.presentation.settings.notifications.NotificationsContent
import com.spmods.spgram.presentation.settings.powersaving.PowerSavingContent
import com.spmods.spgram.presentation.settings.premium.PremiumContent
import com.spmods.spgram.presentation.settings.privacy.PasscodeContent
import com.spmods.spgram.presentation.settings.privacy.PrivacyContent
import com.spmods.spgram.presentation.settings.profile.EditProfileContent
import com.spmods.spgram.presentation.settings.proxy.ProxyContent
import com.spmods.spgram.presentation.settings.sessions.SessionsContent
import com.spmods.spgram.presentation.settings.settings.SettingsContent
import com.spmods.spgram.presentation.settings.stickers.StickersContent
import com.spmods.spgram.presentation.settings.storage.StorageUsageContent

@Composable
fun RenderChild(
    child: RootComponent.Child,
    isOverlay: Boolean = false,
    onSwipeBackBlockedChanged: (Boolean) -> Unit = {},
) {
    when (child) {
        is RootComponent.Child.StartupChild -> StartupContent(child.component)
        is RootComponent.Child.AuthChild -> AuthContent(child.component)
        is RootComponent.Child.ChatsChild -> ChatListContent(child.component)
        is RootComponent.Child.NewChatChild -> NewChatContent(child.component)
        is RootComponent.Child.ChatDetailChild -> ChatContent(
            component = child.component,
            isOverlay = isOverlay,
            onSwipeBackBlockedChanged = onSwipeBackBlockedChanged,
        )

        is RootComponent.Child.SettingsChild -> SettingsContent(child.component)
        is RootComponent.Child.EditProfileChild -> EditProfileContent(child.component)
        is RootComponent.Child.SessionsChild -> SessionsContent(child.component)
        is RootComponent.Child.FoldersChild -> FoldersContent(child.component)
        is RootComponent.Child.ChatSettingsChild -> ChatSettingsContent(child.component)
        is RootComponent.Child.DataStorageChild -> DataStorageContent(child.component)
        is RootComponent.Child.StorageUsageChild -> StorageUsageContent(child.component)
        is RootComponent.Child.NetworkUsageChild -> NetworkUsageContent(child.component)
        is RootComponent.Child.ProfileChild -> ProfileContent(child.component)
        is RootComponent.Child.PremiumChild -> PremiumContent(child.component)
        is RootComponent.Child.PrivacyChild -> PrivacyContent(child.component)
        is RootComponent.Child.AdBlockChild -> AdBlockContent(child.component)
        is RootComponent.Child.PowerSavingChild -> PowerSavingContent(child.component)
        is RootComponent.Child.NotificationsChild -> NotificationsContent(child.component)
        is RootComponent.Child.ProxyChild -> ProxyContent(child.component)
        is RootComponent.Child.ProfileLogsChild -> ProfileLogsContent(child.component)
        is RootComponent.Child.AdminManageChild -> AdminManageContent(child.component)
        is RootComponent.Child.ChatEditChild -> ChatEditContent(child.component)
        is RootComponent.Child.MemberListChild -> MemberListContent(child.component)
        is RootComponent.Child.ChatPermissionsChild -> ChatPermissionsContent(child.component)
        is RootComponent.Child.PasscodeChild -> PasscodeContent(child.component)
        is RootComponent.Child.StickersChild -> StickersContent(child.component)
        is RootComponent.Child.AboutChild -> AboutContent(child.component)
        is RootComponent.Child.DebugChild -> DebugContent(child.component)
        is RootComponent.Child.WebViewChild -> InternalWebView(
            url = child.component.url,
            onDismiss = child.component::onDismiss,
        )
    }
}
