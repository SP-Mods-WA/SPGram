package com.spmods.spgram.presentation.root

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackHandler
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import com.spmods.spgram.domain.models.ChatFullInfoModel
import com.spmods.spgram.domain.models.ChatModel
import com.spmods.spgram.domain.models.ProxyTypeModel
import com.spmods.spgram.presentation.core.util.AppPreferences
import com.spmods.spgram.presentation.features.auth.AuthComponent
import com.spmods.spgram.presentation.features.chats.list.ChatListComponent
import com.spmods.spgram.presentation.features.chats.conversation.ChatComponent
import com.spmods.spgram.presentation.core.media.VideoPlayerPool
import com.spmods.spgram.presentation.features.chats.creation.NewChatComponent
import com.spmods.spgram.presentation.settings.folders.FoldersComponent
import com.spmods.spgram.presentation.features.profile.ProfileComponent
import com.spmods.spgram.presentation.features.profile.admin.AdminManageComponent
import com.spmods.spgram.presentation.features.profile.admin.ChatEditComponent
import com.spmods.spgram.presentation.features.profile.admin.ChatPermissionsComponent
import com.spmods.spgram.presentation.features.profile.admin.MemberListComponent
import com.spmods.spgram.presentation.features.profile.logs.ProfileLogsComponent
import com.spmods.spgram.presentation.features.stickers.core.StickerSetUiModel
import com.spmods.spgram.presentation.features.webview.WebViewComponent
import com.spmods.spgram.presentation.settings.about.AboutComponent
import com.spmods.spgram.presentation.settings.adblock.AdBlockComponent
import com.spmods.spgram.presentation.settings.chatSettings.ChatSettingsComponent
import com.spmods.spgram.presentation.settings.dataStorage.DataStorageComponent
import com.spmods.spgram.presentation.settings.debug.DebugComponent
import com.spmods.spgram.presentation.settings.networkUsage.NetworkUsageComponent
import com.spmods.spgram.presentation.settings.notifications.NotificationsComponent
import com.spmods.spgram.presentation.settings.powersaving.PowerSavingComponent
import com.spmods.spgram.presentation.settings.premium.PremiumComponent
import com.spmods.spgram.presentation.settings.privacy.PasscodeComponent
import com.spmods.spgram.presentation.settings.privacy.PrivacyComponent
import com.spmods.spgram.presentation.settings.profile.EditProfileComponent
import com.spmods.spgram.presentation.settings.proxy.ProxyComponent
import com.spmods.spgram.presentation.settings.sessions.SessionsComponent
import com.spmods.spgram.presentation.settings.settings.SettingsComponent
import com.spmods.spgram.presentation.settings.stickers.StickersComponent
import com.spmods.spgram.presentation.settings.storage.StorageUsageComponent

interface RootComponent {
    val backHandler: BackHandler
    val childStack: Value<ChildStack<*, Child>>
    val stickerSetToPreview: StateFlow<StickerPreviewState>
    val proxyToConfirm: StateFlow<ProxyConfirmState>
    val chatToConfirmJoin: StateFlow<ChatConfirmJoinState>
    val isLocked: StateFlow<Boolean>
    val isBiometricEnabled: StateFlow<Boolean>
    val videoPlayerPool: VideoPlayerPool
    val appPreferences: AppPreferences

    fun onBack()
    fun handleLink(link: String)
    fun dismissStickerPreview()
    fun onSettingsClick()
    fun onChatsClick()
    fun dismissProxyConfirm()
    fun confirmProxy(server: String, port: Int, type: ProxyTypeModel)
    fun recheckProxyPing()
    fun dismissChatConfirmJoin()
    fun confirmJoinChat(chatId: Long)
    fun confirmJoinInviteLink(inviteLink: String)
    fun unlock(passcode: String): Boolean
    fun unlockWithBiometrics()
    fun logout()
    fun navigateToChat(chatId: Long, messageId: Long? = null)

    sealed class Child {
        class StartupChild(val component: StartupComponent) : Child()
        class AuthChild(val component: AuthComponent) : Child()
        class ChatsChild(val component: ChatListComponent) : Child()
        class NewChatChild(val component: NewChatComponent) : Child()
        class ChatDetailChild(val component: ChatComponent) : Child()
        class SettingsChild(val component: SettingsComponent) : Child()
        class EditProfileChild(val component: EditProfileComponent) : Child()
        class SessionsChild(val component: SessionsComponent) : Child()
        class FoldersChild(val component: FoldersComponent) : Child()
        class ChatSettingsChild(val component: ChatSettingsComponent) : Child()
        class DataStorageChild(val component: DataStorageComponent) : Child()
        class StorageUsageChild(val component: StorageUsageComponent) : Child()
        class NetworkUsageChild(val component: NetworkUsageComponent) : Child()
        class ProfileChild(val component: ProfileComponent) : Child()
        class PremiumChild(val component: PremiumComponent) : Child()
        class PrivacyChild(val component: PrivacyComponent) : Child()
        class AdBlockChild(val component: AdBlockComponent) : Child()
        class PowerSavingChild(val component: PowerSavingComponent) : Child()
        class NotificationsChild(val component: NotificationsComponent) : Child()
        class ProxyChild(val component: ProxyComponent) : Child()
        class ProfileLogsChild(val component: ProfileLogsComponent) : Child()
        class AdminManageChild(val component: AdminManageComponent) : Child()
        class ChatEditChild(val component: ChatEditComponent) : Child()
        class MemberListChild(val component: MemberListComponent) : Child()
        class ChatPermissionsChild(val component: ChatPermissionsComponent) : Child()
        class PasscodeChild(val component: PasscodeComponent) : Child()
        class StickersChild(val component: StickersComponent) : Child()
        class AboutChild(val component: AboutComponent) : Child()
        class DebugChild(val component: DebugComponent) : Child()
        class WebViewChild(val component: WebViewComponent) : Child()
    }

    @Serializable
    data class StickerPreviewState(
        val stickerSet: StickerSetUiModel? = null
    )

    data class ProxyConfirmState(
        val server: String? = null,
        val port: Int? = null,
        val type: ProxyTypeModel? = null,
        val ping: Long? = null,
        val isChecking: Boolean = false
    )

    data class ChatConfirmJoinState(
        val chat: ChatModel? = null,
        val fullInfo: ChatFullInfoModel? = null,
        val inviteLink: String? = null,
        val inviteTitle: String? = null,
        val inviteDescription: String? = null,
        val inviteMemberCount: Int = 0,
        val inviteAvatarPath: String? = null,
        val inviteIsChannel: Boolean = false
    )
}