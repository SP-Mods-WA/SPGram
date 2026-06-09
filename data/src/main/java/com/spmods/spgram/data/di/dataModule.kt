package com.spmods.spgram.data.di

import android.content.Context
import android.net.ConnectivityManager
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import com.spmods.spgram.core.DispatcherProvider
import com.spmods.spgram.data.BuildConfig
import com.spmods.spgram.data.chats.ChatCache
import com.spmods.spgram.data.datasource.FileDataSource
import com.spmods.spgram.data.datasource.PlayerDataSourceFactoryImpl
import com.spmods.spgram.data.datasource.TdFileDataSource
import com.spmods.spgram.data.datasource.cache.ChatLocalDataSource
import com.spmods.spgram.data.datasource.cache.ChatsCacheDataSource
import com.spmods.spgram.data.datasource.cache.InMemorySettingsCacheDataSource
import com.spmods.spgram.data.datasource.cache.RoomChatLocalDataSource
import com.spmods.spgram.data.datasource.cache.RoomStickerLocalDataSource
import com.spmods.spgram.data.datasource.cache.RoomUserLocalDataSource
import com.spmods.spgram.data.datasource.cache.SettingsCacheDataSource
import com.spmods.spgram.data.datasource.cache.StickerLocalDataSource
import com.spmods.spgram.data.datasource.cache.UserCacheDataSource
import com.spmods.spgram.data.datasource.cache.UserLocalDataSource
import com.spmods.spgram.data.datasource.remote.AuthRemoteDataSource
import com.spmods.spgram.data.datasource.remote.ChatRemoteSource
import com.spmods.spgram.data.datasource.remote.ChatsRemoteDataSource
import com.spmods.spgram.data.datasource.remote.EmojiRemoteSource
import com.spmods.spgram.data.datasource.remote.GifRemoteSource
import com.spmods.spgram.data.datasource.remote.LinkRemoteDataSource
import com.spmods.spgram.data.datasource.remote.MessageFileApi
import com.spmods.spgram.data.datasource.remote.MessageFileCoordinator
import com.spmods.spgram.data.datasource.remote.MessageRemoteDataSource
import com.spmods.spgram.data.datasource.remote.NominatimRemoteDataSource
import com.spmods.spgram.data.datasource.remote.PrivacyRemoteDataSource
import com.spmods.spgram.data.datasource.remote.ProxyRemoteDataSource
import com.spmods.spgram.data.datasource.remote.SettingsRemoteDataSource
import com.spmods.spgram.data.datasource.remote.StickerRemoteSource
import com.spmods.spgram.data.datasource.remote.TdAuthRemoteDataSource
import com.spmods.spgram.data.datasource.remote.TdChatRemoteSource
import com.spmods.spgram.data.datasource.remote.TdChatsRemoteDataSource
import com.spmods.spgram.data.datasource.remote.TdEmojiRemoteSource
import com.spmods.spgram.data.datasource.remote.TdGifRemoteSource
import com.spmods.spgram.data.datasource.remote.TdLinkRemoteDataSource
import com.spmods.spgram.data.datasource.remote.TdMessageRemoteDataSource
import com.spmods.spgram.data.datasource.remote.TdPrivacyRemoteDataSource
import com.spmods.spgram.data.datasource.remote.TdProxyRemoteDataSource
import com.spmods.spgram.data.datasource.remote.TdSettingsRemoteDataSource
import com.spmods.spgram.data.datasource.remote.TdStickerRemoteSource
import com.spmods.spgram.data.datasource.remote.TdUpdateRemoteDataSource
import com.spmods.spgram.data.datasource.remote.TdUserRemoteDataSource
import com.spmods.spgram.data.datasource.remote.UpdateRemoteDateSource
import com.spmods.spgram.data.datasource.remote.UserRemoteDataSource
import com.spmods.spgram.data.db.SpgramDatabase
import com.spmods.spgram.data.db.SpgramMigrations
import com.spmods.spgram.data.gateway.TelegramGateway
import com.spmods.spgram.data.gateway.TelegramGatewayImpl
import com.spmods.spgram.data.gateway.UpdateDispatcher
import com.spmods.spgram.data.gateway.UpdateDispatcherImpl
import com.spmods.spgram.data.infra.AndroidStringProvider
import com.spmods.spgram.data.infra.ConnectionManager
import com.spmods.spgram.data.infra.DataMemoryDiagnostics
import com.spmods.spgram.data.infra.DataMemoryPressureHandler
import com.spmods.spgram.data.infra.DefaultDispatcherProvider
import com.spmods.spgram.data.infra.FileDownloadQueue
import com.spmods.spgram.data.infra.FileMessageRegistry
import com.spmods.spgram.data.infra.FileObserverHub
import com.spmods.spgram.data.infra.FileUpdateHandler
import com.spmods.spgram.data.infra.OfflineWarmup
import com.spmods.spgram.data.infra.SponsorSyncManager
import com.spmods.spgram.data.infra.TdLibParametersProvider
import com.spmods.spgram.data.mapper.ChatMapper
import com.spmods.spgram.data.mapper.CustomEmojiLoader
import com.spmods.spgram.data.mapper.MessageMapper
import com.spmods.spgram.data.mapper.NetworkMapper
import com.spmods.spgram.data.mapper.StorageMapper
import com.spmods.spgram.data.mapper.TdFileHelper
import com.spmods.spgram.data.mapper.WebPageMapper
import com.spmods.spgram.data.mapper.message.MessageContentMapper
import com.spmods.spgram.data.mapper.message.MessagePersistenceMapper
import com.spmods.spgram.data.mapper.message.MessageSenderResolver
import com.spmods.spgram.data.notifications.NotificationMuteResolver
import com.spmods.spgram.data.push.PushSyncTrigger
import com.spmods.spgram.data.push.UnifiedPushManager
import com.spmods.spgram.data.repository.AttachMenuBotRepositoryImpl
import com.spmods.spgram.data.repository.AuthRepositoryImpl
import com.spmods.spgram.data.repository.BotRepositoryImpl
import com.spmods.spgram.data.repository.ChatInfoRepositoryImpl
import com.spmods.spgram.data.repository.ChatStatisticsRepositoryImpl
import com.spmods.spgram.data.repository.ChatsListRepositoryImpl
import com.spmods.spgram.data.repository.EmojiRepositoryImpl
import com.spmods.spgram.data.repository.GifRepositoryImpl
import com.spmods.spgram.data.repository.LinkHandlerRepositoryImpl
import com.spmods.spgram.data.repository.LinkParser
import com.spmods.spgram.data.repository.LocationRepositoryImpl
import com.spmods.spgram.data.repository.MessageRepositoryImpl
import com.spmods.spgram.data.repository.NetworkStatisticsRepositoryImpl
import com.spmods.spgram.data.repository.NotificationSettingsRepositoryImpl
import com.spmods.spgram.data.repository.PollRepositoryImpl
import com.spmods.spgram.data.repository.PremiumRepositoryImpl
import com.spmods.spgram.data.repository.PrivacyRepositoryImpl
import com.spmods.spgram.data.repository.ProfilePhotoRepositoryImpl
import com.spmods.spgram.data.repository.ProxyDiagnosticsRepositoryImpl
import com.spmods.spgram.data.repository.ProxyRepositoryImpl
import com.spmods.spgram.data.repository.PushDebugRepositoryImpl
import com.spmods.spgram.data.repository.SessionRepositoryImpl
import com.spmods.spgram.data.repository.SponsorRepositoryImpl
import com.spmods.spgram.data.repository.StickerRepositoryImpl
import com.spmods.spgram.data.repository.StorageRepositoryImpl
import com.spmods.spgram.data.repository.StreamingRepositoryImpl
import com.spmods.spgram.data.repository.UpdateRepositoryImpl
import com.spmods.spgram.data.repository.UserProfileEditRepositoryImpl
import com.spmods.spgram.data.repository.WallpaperRepositoryImpl
import com.spmods.spgram.data.repository.user.UserRepositoryImpl
import com.spmods.spgram.data.stickers.StickerFileManager
import com.spmods.spgram.domain.repository.AttachMenuBotRepository
import com.spmods.spgram.domain.repository.AuthRepository
import com.spmods.spgram.domain.repository.BotRepository
import com.spmods.spgram.domain.repository.ChatCreationRepository
import com.spmods.spgram.domain.repository.ChatEventLogRepository
import com.spmods.spgram.domain.repository.ChatFolderRepository
import com.spmods.spgram.domain.repository.ChatInfoRepository
import com.spmods.spgram.domain.repository.ChatListRepository
import com.spmods.spgram.domain.repository.ChatOperationsRepository
import com.spmods.spgram.domain.repository.ChatSearchRepository
import com.spmods.spgram.domain.repository.ChatSettingsRepository
import com.spmods.spgram.domain.repository.ChatStatisticsRepository
import com.spmods.spgram.domain.repository.EmojiRepository
import com.spmods.spgram.domain.repository.FileRepository
import com.spmods.spgram.domain.repository.ForumTopicsRepository
import com.spmods.spgram.domain.repository.GifRepository
import com.spmods.spgram.domain.repository.InlineBotRepository
import com.spmods.spgram.domain.repository.LinkHandlerRepository
import com.spmods.spgram.domain.repository.LocationRepository
import com.spmods.spgram.domain.repository.MessageAiRepository
import com.spmods.spgram.domain.repository.MessageRepository
import com.spmods.spgram.domain.repository.NetworkStatisticsRepository
import com.spmods.spgram.domain.repository.NotificationSettingsRepository
import com.spmods.spgram.domain.repository.PaymentRepository
import com.spmods.spgram.domain.repository.PlayerDataSourceFactory
import com.spmods.spgram.domain.repository.PollRepository
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
import com.spmods.spgram.domain.repository.StreamingRepository
import com.spmods.spgram.domain.repository.StringProvider
import com.spmods.spgram.domain.repository.UpdateRepository
import com.spmods.spgram.domain.repository.UserProfileEditRepository
import com.spmods.spgram.domain.repository.UserRepository
import com.spmods.spgram.domain.repository.WallpaperRepository
import com.spmods.spgram.domain.repository.WebAppRepository

val dataModule = module {
    single { CoroutineScope(SupervisorJob() + get<DispatcherProvider>().default) }

    single(createdAtStart = true) { TdLibClient() }

    single<DispatcherProvider> { DefaultDispatcherProvider() }
    single<StringProvider> { AndroidStringProvider(androidContext()) }
    single(createdAtStart = true) { TdLibParametersProvider(androidContext()) }
    single(createdAtStart = true) {
        OfflineWarmup(
            scope = get(),
            dispatchers = get(),
            gateway = get(),
            chatDao = get(),
            messageDao = get(),
            userDao = get(),
            userFullInfoDao = get(),
            chatFullInfoDao = get(),
            messageMapper = get(),
            chatCache = get(),
            stickerRepository = get()
        )
    }
    single(createdAtStart = true) {
        SponsorSyncManager(
            scope = get(),
            gateway = get(),
            sponsorDao = get(),
            authRepository = get()
        )
    }

    single { ChatCache() }
    single<TelegramGateway>(createdAtStart = true) {
        TelegramGatewayImpl(get())
    }
    single<UpdateDispatcher> {
        UpdateDispatcherImpl(
            gateway = get()
        )
    }
    single<FileDataSource> {
        TdFileDataSource(
            gateway = get(),
            fileDownloadQueue = get()
        )
    }

    factory<AuthRemoteDataSource> {
        TdAuthRemoteDataSource(
            gateway = get()
        )
    }

    single {
        NominatimRemoteDataSource()
    }

    factory<PlayerDataSourceFactory> {
        PlayerDataSourceFactoryImpl(
            fileDataSource = get()
        )
    }

    single<AuthRepository>(createdAtStart = true) {
        AuthRepositoryImpl(
            parametersProvider = get(),
            remote = get(),
            updates = get(),
            scope = get()
        )
    }

    factory<UserRemoteDataSource> {
        TdUserRemoteDataSource(
            gateway = get()
        )
    }

    factory<LinkRemoteDataSource> {
        TdLinkRemoteDataSource(
            gateway = get()
        )
    }

    // Database
    single {
        Room.databaseBuilder(
            androidContext(),
            SpgramDatabase::class.java,
            "spgram_db"
        )
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .addMigrations(
                SpgramMigrations.MIGRATION_26_27,
                SpgramMigrations.MIGRATION_27_28,
                SpgramMigrations.MIGRATION_28_29,
                SpgramMigrations.MIGRATION_29_30,
                SpgramMigrations.MIGRATION_30_31
            )
            .build()
    }
    single { get<SpgramDatabase>().chatDao() }
    single { get<SpgramDatabase>().messageDao() }
    single { get<SpgramDatabase>().userDao() }
    single { get<SpgramDatabase>().chatFullInfoDao() }
    single { get<SpgramDatabase>().topicDao() }
    single { get<SpgramDatabase>().userFullInfoDao() }
    single { get<SpgramDatabase>().stickerSetDao() }
    single { get<SpgramDatabase>().recentEmojiDao() }
    single { get<SpgramDatabase>().searchHistoryDao() }
    single { get<SpgramDatabase>().chatFolderDao() }
    single { get<SpgramDatabase>().attachBotDao() }
    single { get<SpgramDatabase>().keyValueDao() }
    single { get<SpgramDatabase>().notificationSettingDao() }
    single { get<SpgramDatabase>().notificationExceptionDao() }
    single { get<SpgramDatabase>().wallpaperDao() }
    single { get<SpgramDatabase>().stickerPathDao() }
    single { get<SpgramDatabase>().sponsorDao() }
    single { get<SpgramDatabase>().textCompositionStyleDao() }

    single<UserLocalDataSource> {
        RoomUserLocalDataSource(
            userDao = get(),
            userFullInfoDao = get()
        )
    }

    single<ChatLocalDataSource> {
        RoomChatLocalDataSource(
            database = get(),
            chatDao = get(),
            messageDao = get(),
            chatFullInfoDao = get(),
            topicDao = get()
        )
    }

    single<StickerLocalDataSource> {
        RoomStickerLocalDataSource(
            stickerSetDao = get(),
            recentEmojiDao = get(),
            stickerPathDao = get()
        )
    }

    single<UserRepository> {
        UserRepositoryImpl(
            remote = get(),
            userLocal = get(),
            chatLocal = get(),
            chatCache = get(),
            updates = get(),
            scope = get(),
            gateway = get(),
            fileQueue = get(),
            fileObserverHub = get(),
            keyValueDao = get(),
            cacheProvider = get()
        )
    }

    single<UserProfileEditRepository> {
        UserProfileEditRepositoryImpl(
            remote = get()
        )
    }

    single<ProfilePhotoRepository> {
        ProfilePhotoRepositoryImpl(
            remote = get(),
            chatLocal = get(),
            gateway = get(),
            fileQueue = get(),
            fileObserverHub = get()
        )
    }

    single<ChatInfoRepository> {
        ChatInfoRepositoryImpl(
            remote = get(),
            chatLocal = get(),
            userRepository = get()
        )
    }

    single<PremiumRepository> {
        PremiumRepositoryImpl(
            remote = get()
        )
    }

    single<BotRepository> {
        BotRepositoryImpl(
            remote = get()
        )
    }

    single<ChatStatisticsRepository> {
        ChatStatisticsRepositoryImpl(
            remote = get()
        )
    }

    single<SponsorRepository> {
        SponsorRepositoryImpl(
            sponsorSyncManager = get()
        )
    }

    factory<ChatsRemoteDataSource> {
        TdChatsRemoteDataSource(
            gateway = get()
        )
    }

    single<ChatsCacheDataSource> {
        get<ChatCache>()
    }

    single<ChatRemoteSource> {
        TdChatRemoteSource(
            gateway = get(),
            connectivityManager = androidContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager,
        )
    }

    factory<ProxyRemoteDataSource> {
        TdProxyRemoteDataSource(
            gateway = get()
        )
    }

    single {
        ChatMapper(get(), get())
    }

    single {
        StorageMapper(get())
    }

    single {
        NetworkMapper(get(), get())
    }

    single<MessageFileApi> {
        MessageFileCoordinator(
            fileDownloadQueue = get()
        )
    }

    single<UserCacheDataSource> {
        get<ChatCache>()
    }

    single {
        TdFileHelper(
            connectivityManager = androidContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager,
            fileApi = get(),
            appPreferences = get(),
            cache = get()
        )
    }

    single {
        CustomEmojiLoader(
            gateway = get(),
            fileApi = get(),
            fileUpdateHandler = get(),
            fileHelper = get()
        )
    }

    single {
        WebPageMapper(
            fileHelper = get(),
            appPreferences = get()
        )
    }

    single {
        MessageContentMapper(
            fileHelper = get(),
            appPreferences = get(),
            customEmojiLoader = get(),
            webPageMapper = get(),
            cache = get(),
            scope = get(),
            stringProvider = get()
        )
    }

    single {
        MessageSenderResolver(
            gateway = get(),
            userRepository = get(),
            chatInfoRepository = get(),
            cache = get(),
            fileHelper = get(),
            stringProvider = get()
        )
    }

    single {
        MessagePersistenceMapper(
            cache = get(),
            fileHelper = get(),
            stringProvider = get()
        )
    }

    single {
        MessageMapper(
            gateway = get(),
            userRepository = get(),
            cache = get(),
            fileHelper = get(),
            senderResolver = get(),
            contentMapper = get(),
            persistenceMapper = get(),
            customEmojiLoader = get(),
            stringProvider = get()
        )
    }

    single {
        ConnectionManager(
            chatRemoteSource = get(),
            proxyRemoteSource = get(),
            updates = get(),
            appPreferences = get(),
            dispatchers = get(),
            connectivityManager = androidContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager,
            scope = get()
        )
    }

    single { PushSyncTrigger(connectionManager = get(), gateway = get()) }
    single { UnifiedPushManager(androidContext()) }
    single { NotificationMuteResolver() }

    single {
        ChatsListRepositoryImpl(
            remoteDataSource = get(),
            chatRemoteSource = get(),
            updates = get(),
            appPreferences = get(),
            cacheProvider = get(),
            dispatchers = get(),
            cache = get(),
            chatMapper = get(),
            messageMapper = get(),
            gateway = get(),
            scope = get(),
            chatLocalDataSource = get(),
            connectionManager = get(),
            databaseFile = androidContext().getDatabasePath("spgram_db"),
            searchHistoryDao = get(),
            chatFolderDao = get(),
            userFullInfoDao = get(),
            fileQueue = get(),
            fileUpdateHandler = get(),
            stringProvider = get()
        )
    }
    single<ChatListRepository> { get<ChatsListRepositoryImpl>() }
    single<ChatFolderRepository> { get<ChatsListRepositoryImpl>() }
    single<ChatOperationsRepository> { get<ChatsListRepositoryImpl>() }
    single<ChatSearchRepository> { get<ChatsListRepositoryImpl>() }
    single<ForumTopicsRepository> { get<ChatsListRepositoryImpl>() }
    single<ChatSettingsRepository> { get<ChatsListRepositoryImpl>() }
    single<ChatCreationRepository> { get<ChatsListRepositoryImpl>() }

    factory<SettingsRemoteDataSource> {
        TdSettingsRemoteDataSource(
            gateway = get(),
            fileQueue = get()
        )
    }

    single<SettingsCacheDataSource> {
        InMemorySettingsCacheDataSource()
    }

    single<NotificationSettingsRepository> {
        NotificationSettingsRepositoryImpl(
            remote = get(),
            cache = get(),
            chatsRemote = get(),
            notificationExceptionDao = get(),
            updates = get(),
            scope = get(),
            dispatchers = get()
        )
    }

    single<SessionRepository> {
        SessionRepositoryImpl(
            remote = get()
        )
    }

    single<WallpaperRepository> {
        WallpaperRepositoryImpl(
            remote = get(),
            wallpaperDao = get(),
            fileObserverHub = get(),
            dispatchers = get(),
            scope = get()
        )
    }

    single<StorageRepository> {
        StorageRepositoryImpl(
            remote = get(),
            cache = get(),
            chatsRemote = get(),
            dispatchers = get(),
            storageMapper = get(),
            stringProvider = get()
        )
    }

    single<NetworkStatisticsRepository> {
        NetworkStatisticsRepositoryImpl(
            remote = get(),
            networkMapper = get()
        )
    }

    single<AttachMenuBotRepository> {
        AttachMenuBotRepositoryImpl(
            remote = get(),
            cache = get(),
            cacheProvider = get(),
            updates = get(),
            fileObserverHub = get(),
            dispatchers = get(),
            attachBotDao = get(),
            scope = get()
        )
    }

    single<PollRepository> {
        PollRepositoryImpl()
    }

    single<MessageRemoteDataSource> {
        TdMessageRemoteDataSource(
            gateway = get(),
            messageMapper = get(),
            userRepository = get(),
            chatListRepository = get(),
            cache = get(),
            pollRepository = get(),
            fileDownloadQueue = get(),
            fileUpdateHandler = get(),
            dispatcherProvider = get(),
            scope = get()
        )
    }

    single<MessageRepository> {
        MessageRepositoryImpl(
            context = androidContext(),
            gateway = get(),
            updates = get(),
            messageMapper = get(),
            messageRemoteDataSource = get(),
            cache = get(),
            fileHelper = get(),
            dispatcherProvider = get(),
            scope = get(),
            fileDataSource = get(),
            chatLocalDataSource = get(),
            userLocalDataSource = get(),
            stickerPathDao = get(),
            keyValueDao = get(),
            textCompositionStyleDao = get()
        )
    }

    single<InlineBotRepository> { get<MessageRepository>() }
    single<ChatEventLogRepository> { get<MessageRepository>() }
    single<MessageAiRepository> { get<MessageRepository>() }
    single<PaymentRepository> { get<MessageRepository>() }
    single<FileRepository> { get<MessageRepository>() }
    single<WebAppRepository> { get<MessageRepository>() }

    factory<StickerRemoteSource> {
        TdStickerRemoteSource(
            gateway = get()
        )
    }

    factory<GifRemoteSource> {
        TdGifRemoteSource(
            gateway = get()
        )
    }

    factory<EmojiRemoteSource> {
        TdEmojiRemoteSource(
            gateway = get()
        )
    }

    single {
        FileMessageRegistry()
    }

    single {
        FileDownloadQueue(
            gateway = get(),
            registry = get(),
            cache = get(),
            scope = get(),
            dispatcherProvider = get(),
            context = androidContext()
        )
    }

    single {
        FileUpdateHandler(
            registry = get(),
            queue = get(),
            updates = get(),
            scope = get()
        )
    }

    single {
        FileObserverHub(
            queue = get(),
            fileUpdateHandler = get()
        )
    }

    single {
        DataMemoryPressureHandler(
            chatsListRepository = get(),
            fileUpdateHandler = get()
        )
    }

    if (BuildConfig.DEBUG) {
        single(createdAtStart = true) {
            DataMemoryDiagnostics(
                scope = get(),
                memoryPressureHandler = get()
            )
        }
    }

    single {
        StickerFileManager(
            localDataSource = get(),
            fileDataSource = get(),
            fileQueue = get(),
            fileUpdateHandler = get(),
            dispatchers = get(),
            scope = get()
        )
    }

    single<StickerRepository> {
        StickerRepositoryImpl(
            remote = get(),
            fileManager = get(),
            updates = get(),
            cacheProvider = get(),
            dispatchers = get(),
            localDataSource = get(),
            scope = get()
        )
    }

    single<GifRepository> {
        GifRepositoryImpl(
            remote = get(),
            cacheProvider = get(),
            stickerFileManager = get()
        )
    }

    single<EmojiRepository> {
        EmojiRepositoryImpl(
            remote = get(),
            localDataSource = get(),
            cacheProvider = get(),
            dispatchers = get(),
            context = androidContext(),
            scope = get()
        )
    }

    factory<PrivacyRemoteDataSource> {
        TdPrivacyRemoteDataSource(
            gateway = get()
        )
    }

    single<PrivacyRepository> {
        PrivacyRepositoryImpl(
            remote = get(),
            updates = get()
        )
    }

    single {
        LinkParser()
    }

    single<LinkHandlerRepository> {
        LinkHandlerRepositoryImpl(get(), get(), get(), get(), get())
    }

    single<StreamingRepository> {
        StreamingRepositoryImpl(
            fileDataSource = get(),
            fileObserverHub = get()
        )
    }

    single<ProxyRepository> {
        ProxyRepositoryImpl(
            remote = get(),
            appPreferences = get()
        )
    }

    single<ProxyDiagnosticsRepository> {
        ProxyDiagnosticsRepositoryImpl(
            remote = get()
        )
    }

    single<LocationRepository> {
        LocationRepositoryImpl(
            remote = get()
        )
    }

    factory<UpdateRemoteDateSource> {
        TdUpdateRemoteDataSource(
            gateway = get()
        )
    }

    single<UpdateRepository> {
        UpdateRepositoryImpl(
            context = androidContext(),
            remote = get(),
            fileQueue = get(),
            fileUpdateHandler = get(),
            authRepository = get(),
            scope = get(),
            stringProvider = get(),
        )
    }

    single<PushDebugRepository> {
        PushDebugRepositoryImpl(
            context = androidContext(),
            appPreferences = get(),
            unifiedPushManager = get(),
            pushSyncTrigger = get(),
            scope = get()
        )
    }

    single(createdAtStart = true) {
        TdNotificationManager(
            androidContext(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get()
        )
    }
}
