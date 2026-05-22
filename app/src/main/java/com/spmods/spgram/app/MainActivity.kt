package com.spmods.spgram.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.layout.WindowInfoTracker
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.retainedComponent
import org.koin.android.ext.android.inject
import com.spmods.spgram.app.ui.theme.AppThemeContainer
import com.spmods.spgram.data.service.TdNotificationService
import com.spmods.spgram.domain.repository.PushProvider
import com.spmods.spgram.presentation.core.util.AppPreferences
import com.spmods.spgram.presentation.core.util.LocalVideoPlayerPool
import com.spmods.spgram.presentation.core.util.NightMode
import com.spmods.spgram.presentation.features.chats.conversation.ui.message.LocalLinkHandler
import com.spmods.spgram.presentation.root.DefaultAppComponentContext
import com.spmods.spgram.presentation.root.DefaultRootComponent
import com.spmods.spgram.presentation.root.RootComponent
import java.util.Calendar

class MainActivity : FragmentActivity() {
    private lateinit var root: RootComponent
    private val appPreferences: AppPreferences by inject()

    @Volatile
    private var keepSplashOnScreen: Boolean = true
    private var isDarkTheme: Boolean = false

    @OptIn(ExperimentalDecomposeApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(resolveStartupTheme())
        // Set isDarkTheme early so status bar icons are correct from the start
        isDarkTheme = resolveIsDarkTheme()
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { keepSplashOnScreen }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            splashScreen.setOnExitAnimationListener { provider ->
                provider.view.animate()
                    .alpha(0f)
                    .setDuration(220L)
                    .withEndAction { provider.remove() }
                    .start()
            }
        }
        super.onCreate(savedInstanceState)

        // Edge-to-edge: let Theme.kt control status/nav bar icon colors
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Apply correct status bar icon colors immediately (light/dark)
        val isLight = !isDarkTheme
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = isLight
            isAppearanceLightNavigationBars = isLight
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }

        root = retainedComponent { componentContext ->
            DefaultRootComponent(
                DefaultAppComponentContext(
                    componentContext = componentContext,
                    container = (application as App).container,
                )
            )
        }

        handleIntent(intent)

        val windowInfoTracker = WindowInfoTracker.getOrCreate(this)

        setContent {
            LaunchedEffect(Unit) {
                keepSplashOnScreen = false
                startNotificationService()
            }

            val windowLayoutInfo by windowInfoTracker.windowLayoutInfo(this)
                .collectAsStateWithLifecycle(initialValue = null)

            AppThemeContainer(root.appPreferences) {
                CompositionLocalProvider(
                    LocalLinkHandler provides root::handleLink,
                    LocalVideoPlayerPool provides root.videoPlayerPool
                ) {
                    MainContent(
                        root = root,
                        windowLayoutInfo = windowLayoutInfo
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val data = intent.dataString
        if (data != null) {
            root.handleLink(data)
            return
        }

        val chatId = intent.getLongExtra("chat_id", 0L)
        if (chatId != 0L) {
            root.navigateToChat(chatId)
        }
    }

    fun updateTheme(darkTheme: Boolean) {
        isDarkTheme = darkTheme
    }

    private fun startNotificationService() {
        if (appPreferences.pushProvider.value != PushProvider.GMS_LESS) return
        val intent = Intent(this, TdNotificationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun resolveIsDarkTheme(): Boolean {
        return when (appPreferences.nightMode.value) {
            NightMode.SYSTEM -> {
                val uiMode = resources.configuration.uiMode and
                        android.content.res.Configuration.UI_MODE_NIGHT_MASK
                uiMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
            }
            NightMode.LIGHT -> false
            NightMode.DARK -> true
            NightMode.SCHEDULED -> {
                val calendar = Calendar.getInstance()
                val now = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
                val start = appPreferences.nightModeStartTime.value
                    .split(":").takeIf { it.size == 2 }
                    ?.let { it[0].toIntOrNull()?.times(60)?.plus(it[1].toIntOrNull() ?: 0) }
                    ?: 22 * 60
                val end = appPreferences.nightModeEndTime.value
                    .split(":").takeIf { it.size == 2 }
                    ?.let { it[0].toIntOrNull()?.times(60)?.plus(it[1].toIntOrNull() ?: 0) }
                    ?: 7 * 60
                if (start < end) now in start until end
                else now >= start || now < end
            }
            NightMode.BRIGHTNESS -> {
                val brightness = runCatching {
                    Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, 255)
                }.getOrDefault(255)
                brightness / 255f <= appPreferences.nightModeBrightnessThreshold.value
            }
        }
    }

    private fun resolveStartupTheme(): Int {
        return when (appPreferences.nightMode.value) {
            NightMode.SYSTEM -> R.style.Theme_SPGram_Startup
            NightMode.LIGHT -> R.style.Theme_SPGram_Startup_Light
            NightMode.DARK -> R.style.Theme_SPGram_Startup_Dark
            NightMode.SCHEDULED -> {
                val calendar = Calendar.getInstance()
                val now = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

                val start = appPreferences.nightModeStartTime.value
                    .split(":")
                    .takeIf { it.size == 2 }
                    ?.let { it[0].toIntOrNull()?.times(60)?.plus(it[1].toIntOrNull() ?: 0) }
                    ?: 22 * 60

                val end = appPreferences.nightModeEndTime.value
                    .split(":")
                    .takeIf { it.size == 2 }
                    ?.let { it[0].toIntOrNull()?.times(60)?.plus(it[1].toIntOrNull() ?: 0) }
                    ?: 7 * 60

                if (start < end) {
                    if (now in start until end) {
                        R.style.Theme_SPGram_Startup_Dark
                    } else {
                        R.style.Theme_SPGram_Startup_Light
                    }
                } else {
                    if (now >= start || now < end) {
                        R.style.Theme_SPGram_Startup_Dark
                    } else {
                        R.style.Theme_SPGram_Startup_Light
                    }
                }
            }

            NightMode.BRIGHTNESS -> {
                val brightness = runCatching {
                    Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, 255)
                }.getOrDefault(255)
                if (brightness / 255f <= appPreferences.nightModeBrightnessThreshold.value) {
                    R.style.Theme_SPGram_Startup_Dark
                } else {
                    R.style.Theme_SPGram_Startup_Light
                }
            }
        }
    }
}
