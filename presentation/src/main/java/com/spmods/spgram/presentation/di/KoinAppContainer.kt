package com.spmods.spgram.presentation.di

import coil3.ImageLoader
import kotlinx.coroutines.CoroutineScope
import org.koin.core.Koin
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

class KoinAppContainer(koin: Koin) : AppContainer {
    override val preferences = KoinPreferencesContainer(koin)
    override val cacheProvider: CacheProvider by lazy { koin.get() }
    override val repositories = KoinRepositoriesContainer(koin)
    override val utils = KoinUtilsContainer(koin)
}

class KoinPreferencesContainer(private val koin: Koin) : PreferencesContainer {
    override val appPreferences: AppPreferences by lazy { koin.get() }
    override val appPreferencesProvider: AppPreferencesProvider by lazy { koin.get() }
    override val botPreferencesProvider: BotPreferencesProvider by lazy { koin.get() }
    override val editorSnippetProvider: EditorSnippetProvider by lazy { koin.get() }
}

class KoinRepositoriesContainer(private val koin: Koin) : RepositoriesContainer {
    override val authRepository: AuthRepository by lazy { koin.get() }
    override val chatListRepository: ChatListRepository by lazy { koin.get() }
    override val chatFolderRepository: ChatFolderRepository by lazy { koin.get() }
    override val chatOperationsRepository: ChatOperationsRepository by lazy { koin.get() }
    override val chatSearchRepository: ChatSearchRepository by lazy { koin.get() }
    override val forumTopicsRepository: ForumTopicsRepository by lazy { koin.get() }
    override val chatSettingsRepository: ChatSettingsRepository by lazy { koin.get() }
    override val chatCreationRepository: ChatCreationRepository by lazy { koin.get() }
    override val messageRepository: MessageRepository by lazy { koin.get() }
    override val inlineBotRepository: InlineBotRepository by lazy { koin.get() }
    override val chatEventLogRepository: ChatEventLogRepository by lazy { koin.get() }
    override val messageAiRepository: MessageAiRepository by lazy { koin.get() }
    override val paymentRepository: PaymentRepository by lazy { koin.get() }
    override val fileRepository: FileRepository by lazy { koin.get() }
    override val webAppRepository: WebAppRepository by lazy { koin.get() }
    override val userRepository: UserRepository by lazy { koin.get() }
    override val userProfileEditRepository: UserProfileEditRepository by lazy { koin.get() }
    override val profilePhotoRepository: ProfilePhotoRepository by lazy { koin.get() }
    override val chatInfoRepository: ChatInfoRepository by lazy { koin.get() }
    override val premiumRepository: PremiumRepository by lazy { koin.get() }
    override val botRepository: BotRepository by lazy { koin.get() }
    override val chatStatisticsRepository: ChatStatisticsRepository by lazy { koin.get() }
    override val sponsorRepository: SponsorRepository by lazy { koin.get() }
    override val notificationSettingsRepository: NotificationSettingsRepository by lazy { koin.get() }
    override val sessionRepository: SessionRepository by lazy { koin.get() }
    override val wallpaperRepository: WallpaperRepository by lazy { koin.get() }
    override val storageRepository: StorageRepository by lazy { koin.get() }
    override val networkStatisticsRepository: NetworkStatisticsRepository by lazy { koin.get() }
    override val attachMenuBotRepository: AttachMenuBotRepository by lazy { koin.get() }
    override val locationRepository: LocationRepository by lazy { koin.get() }
    override val privacyRepository: PrivacyRepository by lazy { koin.get() }
    override val linkHandlerRepository: LinkHandlerRepository by lazy { koin.get() }
    override val proxyRepository: ProxyRepository by lazy { koin.get() }
    override val proxyDiagnosticsRepository: ProxyDiagnosticsRepository by lazy { koin.get() }
    override val stickerRepository: StickerRepository by lazy { koin.get() }
    override val gifRepository: GifRepository by lazy { koin.get() }
    override val emojiRepository: EmojiRepository by lazy { koin.get() }
    override val updateRepository: UpdateRepository by lazy { koin.get() }
    override val pushDebugRepository: PushDebugRepository by lazy { koin.get() }
}

class KoinUtilsContainer(private val koin: Koin) : UtilsContainer {
    override val appCoroutineScope: CoroutineScope by lazy { koin.get() }
    override val videoPlayerPool: VideoPlayerPool by lazy { koin.get() }
    override val exoPlayerCache: ExoPlayerCache by lazy { koin.get() }
    override val cacheController: CacheController by lazy { koin.get() }
    override val imageLoader: ImageLoader by lazy { koin.get() }
    override val clipManager: ClipManager by lazy { koin.get() }
    override val dispatcherProvider: DispatcherProvider by lazy { koin.get() }
    override val logger: Logger by lazy { koin.get() }

    override fun messageDisplayer(): MessageDisplayer = koin.get()
    override fun externalNavigator(): ExternalNavigator = koin.get()
    override fun phoneManager(): PhoneManager = koin.get()
    override fun domainManager(): DomainManager = koin.get()
    override fun assetsManager(): AssetsManager = koin.get()
    override fun distrManager(): DistrManager = koin.get()

    override fun downloadUtils(): IDownloadUtils = koin.get()
    override fun stringProvider(): StringProvider = koin.get()
}