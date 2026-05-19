package com.spmods.spgram.presentation.di

import coil3.ImageLoader
import kotlinx.coroutines.CoroutineScope
import com.spmods.spgram.core.DispatcherProvider
import com.spmods.spgram.core.Logger
import com.spmods.spgram.domain.managers.AssetsManager
import com.spmods.spgram.domain.managers.ClipManager
import com.spmods.spgram.domain.managers.DistrManager
import com.spmods.spgram.domain.managers.DomainManager
import com.spmods.spgram.domain.managers.PhoneManager
import com.spmods.spgram.domain.repository.AppPreferencesProvider
import com.spmods.spgram.domain.repository.AttachMenuBotRepository
import com.spmods.spgram.domain.repository.AuthRepository
import com.spmods.spgram.domain.repository.BotPreferencesProvider
import com.spmods.spgram.domain.repository.BotRepository
import com.spmods.spgram.domain.repository.CacheProvider
import com.spmods.spgram.domain.repository.ChatCreationRepository
import com.spmods.spgram.domain.repository.ChatEventLogRepository
import com.spmods.spgram.domain.repository.ChatFolderRepository
import com.spmods.spgram.domain.repository.ChatInfoRepository
import com.spmods.spgram.domain.repository.ChatListRepository
import com.spmods.spgram.domain.repository.ChatOperationsRepository
import com.spmods.spgram.domain.repository.ChatSearchRepository
import com.spmods.spgram.domain.repository.ChatSettingsRepository
import com.spmods.spgram.domain.repository.ChatStatisticsRepository
import com.spmods.spgram.domain.repository.EditorSnippetProvider
import com.spmods.spgram.domain.repository.EmojiRepository
import com.spmods.spgram.domain.repository.ExternalNavigator
import com.spmods.spgram.domain.repository.FileRepository
import com.spmods.spgram.domain.repository.ForumTopicsRepository
import com.spmods.spgram.domain.repository.GifRepository
import com.spmods.spgram.domain.repository.InlineBotRepository
import com.spmods.spgram.domain.repository.LinkHandlerRepository
import com.spmods.spgram.domain.repository.LocationRepository
import com.spmods.spgram.domain.repository.MessageAiRepository
import com.spmods.spgram.domain.repository.MessageDisplayer
import com.spmods.spgram.domain.repository.MessageRepository
import com.spmods.spgram.domain.repository.NetworkStatisticsRepository
import com.spmods.spgram.domain.repository.NotificationSettingsRepository
import com.spmods.spgram.domain.repository.PaymentRepository
import com.spmods.spgram.domain.repository.PremiumRepository
import com.spmods.spgram.domain.repository.PrivacyRepository
import com.spmods.spgram.domain.repository.ProfilePhotoRepository
import com.spmods.spgram.domain.repository.ProxyDiagnosticsRepository
import com.spmods.spgram.domain.repository.ProxyRepository
import com.spmods.spgram.domain.repository.PushDebugRepository
import com.spmods.spgram.domain.repository.SessionRepository
import com.spmods.spgram.domain.repository.SponsorRepository
import com.spmods.spgram.domain.repository.StickerRepository
import com.spmods.spgram.domain.repository.StorageRepository
import com.spmods.spgram.domain.repository.StringProvider
import com.spmods.spgram.domain.repository.UpdateRepository
import com.spmods.spgram.domain.repository.UserProfileEditRepository
import com.spmods.spgram.domain.repository.UserRepository
import com.spmods.spgram.domain.repository.WallpaperRepository
import com.spmods.spgram.domain.repository.WebAppRepository
import com.spmods.spgram.presentation.core.util.AppPreferences
import com.spmods.spgram.presentation.core.util.IDownloadUtils
import com.spmods.spgram.presentation.core.media.ExoPlayerCache
import com.spmods.spgram.presentation.core.media.VideoPlayerPool
import com.spmods.spgram.presentation.settings.storage.CacheController

interface AppContainer {
    val preferences: PreferencesContainer
    val repositories: RepositoriesContainer
    val utils: UtilsContainer
    val cacheProvider: CacheProvider
}

interface PreferencesContainer {
    val appPreferences: AppPreferences
    val appPreferencesProvider: AppPreferencesProvider
    val botPreferencesProvider: BotPreferencesProvider
    val editorSnippetProvider: EditorSnippetProvider
}

interface RepositoriesContainer {
    val authRepository: AuthRepository
    val chatListRepository: ChatListRepository
    val chatFolderRepository: ChatFolderRepository
    val chatOperationsRepository: ChatOperationsRepository
    val chatSearchRepository: ChatSearchRepository
    val forumTopicsRepository: ForumTopicsRepository
    val chatSettingsRepository: ChatSettingsRepository
    val chatCreationRepository: ChatCreationRepository
    val messageRepository: MessageRepository
    val inlineBotRepository: InlineBotRepository
    val chatEventLogRepository: ChatEventLogRepository
    val messageAiRepository: MessageAiRepository
    val paymentRepository: PaymentRepository
    val fileRepository: FileRepository
    val webAppRepository: WebAppRepository
    val userRepository: UserRepository
    val userProfileEditRepository: UserProfileEditRepository
    val profilePhotoRepository: ProfilePhotoRepository
    val chatInfoRepository: ChatInfoRepository
    val premiumRepository: PremiumRepository
    val botRepository: BotRepository
    val chatStatisticsRepository: ChatStatisticsRepository
    val sponsorRepository: SponsorRepository
    val notificationSettingsRepository: NotificationSettingsRepository
    val sessionRepository: SessionRepository
    val wallpaperRepository: WallpaperRepository
    val storageRepository: StorageRepository
    val networkStatisticsRepository: NetworkStatisticsRepository
    val attachMenuBotRepository: AttachMenuBotRepository
    val locationRepository: LocationRepository
    val privacyRepository: PrivacyRepository
    val linkHandlerRepository: LinkHandlerRepository
    val proxyRepository: ProxyRepository
    val proxyDiagnosticsRepository: ProxyDiagnosticsRepository
    val stickerRepository: StickerRepository
    val gifRepository: GifRepository
    val emojiRepository: EmojiRepository
    val updateRepository: UpdateRepository
    val pushDebugRepository: PushDebugRepository
}

interface UtilsContainer {
    val videoPlayerPool: VideoPlayerPool
    val exoPlayerCache: ExoPlayerCache
    val cacheController: CacheController
    val imageLoader: ImageLoader
    val appCoroutineScope: CoroutineScope
    val clipManager: ClipManager
    val dispatcherProvider: DispatcherProvider
    val logger: Logger
    fun messageDisplayer(): MessageDisplayer
    fun externalNavigator(): ExternalNavigator
    fun phoneManager(): PhoneManager
    fun domainManager(): DomainManager
    fun assetsManager(): AssetsManager
    fun distrManager(): DistrManager
    fun downloadUtils(): IDownloadUtils
    fun stringProvider(): StringProvider
}
