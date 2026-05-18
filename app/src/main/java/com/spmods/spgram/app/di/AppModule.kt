package com.spmods.spgram.app.di

import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Context
import android.telephony.TelephonyManager
import android.text.format.DateFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import com.spmods.spgram.core.Logger
import com.spmods.spgram.data.di.dataModule
import com.spmods.spgram.domain.managers.AssetsManager
import com.spmods.spgram.domain.managers.ClipManager
import com.spmods.spgram.domain.managers.DistrManager
import com.spmods.spgram.domain.managers.DomainManager
import com.spmods.spgram.domain.managers.PhoneManager
import com.spmods.spgram.domain.repository.AppPreferencesProvider
import com.spmods.spgram.domain.repository.BotPreferencesProvider
import com.spmods.spgram.domain.repository.CacheProvider
import com.spmods.spgram.domain.repository.EditorSnippetProvider
import com.spmods.spgram.domain.repository.ExternalNavigator
import com.spmods.spgram.domain.repository.MessageDisplayer
import com.spmods.spgram.presentation.core.media.ExoPlayerCache
import com.spmods.spgram.presentation.core.media.VideoPlayerPool
import com.spmods.spgram.presentation.core.util.AppPreferences
import com.spmods.spgram.presentation.core.util.BotPreferences
import com.spmods.spgram.presentation.core.util.CachePreferences
import com.spmods.spgram.presentation.core.util.DateFormatManager
import com.spmods.spgram.presentation.core.util.DateFormatManagerImpl
import com.spmods.spgram.presentation.core.util.DownloadUtils
import com.spmods.spgram.presentation.core.util.EditorSnippetPreferences
import com.spmods.spgram.presentation.core.util.ExternalNavigatorImpl
import com.spmods.spgram.presentation.core.util.IDownloadUtils
import com.spmods.spgram.presentation.core.util.ToastMessageDisplayer
import com.spmods.spgram.presentation.di.uiModule
import com.spmods.spgram.presentation.settings.storage.CacheController

@SuppressLint("WrongConstant")
val appModule = module {
    includes(uiModule, dataModule)

    single { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    single<AppPreferencesProvider> { AppPreferences(androidContext(), get()) }
    single { get<AppPreferencesProvider>() as AppPreferences }
    single<EditorSnippetProvider> { EditorSnippetPreferences(androidContext()) }
    single<CacheProvider> { CachePreferences(androidContext()) }
    single<BotPreferencesProvider> { BotPreferences(androidContext()) }

    single { ExoPlayerCache() }
    single { CacheController(androidContext(), get()) }
    single { VideoPlayerPool(androidContext(), get(), get()) }
    single<ClipManager> {
        ClipManagerImpl(
            androidContext().getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager,
        )
    }
    single<Logger> { LoggerImpl() }

    single<DateFormatManager> { DateFormatManagerImpl(DateFormat.is24HourFormat(androidContext())) }

    factory<PhoneManager> {
        PhoneManagerImpl(
            androidContext().getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager,
        )
    }
    factory<DomainManager> { DomainManagerImpl(androidContext(), get<ExternalNavigator>().packageName) }
    factory<AssetsManager> { AssetsManagerImpl(androidContext()) }
    factory<DistrManager> { DistrManagerImpl(androidContext()) }
    factory<MessageDisplayer> { ToastMessageDisplayer(androidContext()) }
    factory<ExternalNavigator> { ExternalNavigatorImpl(androidContext()) }
    factory<IDownloadUtils> { DownloadUtils(androidContext(), get()) }
}
