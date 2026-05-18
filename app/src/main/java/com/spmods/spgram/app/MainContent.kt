package com.spmods.spgram.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInBack
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.zIndex
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowLayoutInfo
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import kotlinx.coroutines.delay
import com.spmods.spgram.app.components.ChatConfirmJoinSheet
import com.spmods.spgram.app.components.LockScreen
import com.spmods.spgram.app.components.MobileLayout
import com.spmods.spgram.app.components.ProxyConfirmSheet
import com.spmods.spgram.app.components.TabletLayout
import com.spmods.spgram.presentation.core.util.LocalTabletInterfaceEnabled
import com.spmods.spgram.presentation.features.chats.conversation.ui.StickerSetSheet
import com.spmods.spgram.presentation.features.chats.conversation.ui.content.ChatContentViewers
import com.spmods.spgram.presentation.features.profile.ProfileViewers
import com.spmods.spgram.presentation.features.stickers.core.toDomain
import com.spmods.spgram.presentation.root.RootComponent
import com.spmods.spgram.presentation.root.StartupComponent
import com.spmods.spgram.presentation.root.StartupContent
import androidx.window.core.layout.WindowSizeClass as WindowSizeClassCore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(
    root: RootComponent,
    windowLayoutInfo: WindowLayoutInfo?
) {
    val childStack by root.childStack.subscribeAsState()
    val isLocked by root.isLocked.collectAsState()
    val isTabletInterfaceEnabled by root.appPreferences.isTabletInterfaceEnabled.collectAsState()
    val localClipboard = LocalClipboard.current
    
    val isExpanded = currentWindowAdaptiveInfo().windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClassCore.WIDTH_DP_MEDIUM_LOWER_BOUND) ||
            windowLayoutInfo?.displayFeatures?.filterIsInstance<FoldingFeature>()?.any {
                it.orientation == FoldingFeature.Orientation.VERTICAL && it.isSeparating
            } == true

    val activeChild = childStack.active.instance
    val isStartupActive = activeChild is RootComponent.Child.StartupChild
    val startupChild = activeChild as? RootComponent.Child.StartupChild
    var startupOverlayComponent by remember { mutableStateOf<StartupComponent?>(null) }
    var startupOverlayVisible by remember { mutableStateOf(false) }
    var wasStartupActive by remember { mutableStateOf(isStartupActive) }

    LaunchedEffect(isStartupActive, startupChild?.component) {
        if (isStartupActive) {
            startupOverlayComponent = startupChild?.component
            startupOverlayVisible = false
            wasStartupActive = true
            return@LaunchedEffect
        }

        if (wasStartupActive && startupOverlayComponent != null) {
            startupOverlayVisible = true
            delay(90)
            startupOverlayVisible = false
            delay(320)
            startupOverlayComponent = null
            wasStartupActive = false
            return@LaunchedEffect
        }

        wasStartupActive = false
    }

    CompositionLocalProvider(LocalTabletInterfaceEnabled provides isTabletInterfaceEnabled) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
        val contentScale by animateFloatAsState(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 300),
            label = "ContentScale"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = contentScale
                    scaleY = contentScale
                },
        ) {
            if (isExpanded &&
                isTabletInterfaceEnabled &&
                activeChild !is RootComponent.Child.AuthChild &&
                activeChild !is RootComponent.Child.StartupChild
            ) {
                TabletLayout(childStack, windowLayoutInfo)
            } else {
                MobileLayout(root)
            }
        }

        val stickerPreviewState by root.stickerSetToPreview.collectAsState()
        stickerPreviewState.stickerSet?.let { set ->
            StickerSetSheet(
                stickerSet = set.toDomain(),
                onDismiss = root::dismissStickerPreview,
                onStickerClick = { _, _ -> }
            )
        }

        if (activeChild !is RootComponent.Child.AuthChild &&
            activeChild !is RootComponent.Child.StartupChild
        ) {
            ProxyConfirmSheet(root)
            ChatConfirmJoinSheet(root)
        }

        if (!isStartupActive && startupOverlayComponent != null) {
            AnimatedVisibility(
                visible = startupOverlayVisible,
                enter = fadeIn(tween(80)),
                exit = fadeOut(tween(260)),
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(50f)
            ) {
                StartupContent(
                    component = startupOverlayComponent!!,
                    animateIn = false
                )
            }
        }

        AnimatedVisibility(
            visible = isLocked,
            enter = fadeIn(tween(400)) + scaleIn(
                animationSpec = tween(400, easing = EaseOutBack),
                initialScale = 0.9f
            ),
            exit = fadeOut(tween(400)) + scaleOut(
                animationSpec = tween(400, easing = EaseInBack),
                targetScale = 0.9f
            ),
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1000f)
        ) {
            LockScreen(root)
        }

        when (activeChild) {
            is RootComponent.Child.ChatDetailChild -> {
                if (isExpanded && isTabletInterfaceEnabled) {
                    val chatState by activeChild.component.state.collectAsState()
                    ChatContentViewers(
                        state = chatState,
                        component = activeChild.component,
                        localClipboard = localClipboard
                    )
                }
            }
            is RootComponent.Child.ProfileChild -> {
                val profileState by activeChild.component.state.subscribeAsState()
                ProfileViewers(
                    state = profileState,
                    component = activeChild.component
                )
            }
            else -> {}
        }
        }
    }
}
