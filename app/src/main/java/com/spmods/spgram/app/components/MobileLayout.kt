package com.spmods.spgram.app.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.util.fastFirstOrNull
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.plus
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.predictiveBackAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import kotlinx.coroutines.launch
import com.spmods.spgram.presentation.root.RootComponent
import kotlin.math.abs

@OptIn(ExperimentalDecomposeApi::class)
@Composable
fun MobileLayout(root: RootComponent) {
    val stack by root.childStack.subscribeAsState()
    val isDragToBackEnabled by root.appPreferences.isDragToBackEnabled.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val previous = stack.items.dropLast(1).lastOrNull()?.instance
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var isCompletingSwipeBack by remember { mutableStateOf(false) }
    var widthPx by remember { mutableFloatStateOf(0f) }
    var isSwipeBackBlocked by remember { mutableStateOf(false) }
    val canUseDragToBack =
        isDragToBackEnabled && isSwipeBackSupported(stack.active.instance) && !isSwipeBackBlocked
    val dragProgress = if (widthPx > 0f) (dragOffsetX / widthPx).coerceIn(0f, 1f) else 0f

    LaunchedEffect(canUseDragToBack) {
        if (!canUseDragToBack && dragOffsetX > 0f) {
            dragOffsetX = 0f
            isCompletingSwipeBack = false
        }
    }

    // Determine active tab from current child
    val activeChild = stack.active.instance
    val selectedTab by remember(activeChild) {
        derivedStateOf {
            when (activeChild) {
                is RootComponent.Child.ChatsChild -> MainTab.Chats
                is RootComponent.Child.SettingsChild,
                is RootComponent.Child.EditProfileChild,
                is RootComponent.Child.SessionsChild,
                is RootComponent.Child.DataStorageChild,
                is RootComponent.Child.StorageUsageChild,
                is RootComponent.Child.NetworkUsageChild,
                is RootComponent.Child.AdBlockChild,
                is RootComponent.Child.PowerSavingChild,
                is RootComponent.Child.NotificationsChild,
                is RootComponent.Child.ProxyChild,
                is RootComponent.Child.PrivacyChild,
                is RootComponent.Child.AboutChild,
                is RootComponent.Child.StickersChild,
                is RootComponent.Child.DebugChild -> MainTab.Settings
                else -> null // not a root tab
            }
        }
    }

    // Show bottom bar only on root-level tab screens
    val showBottomBar = selectedTab != null

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            if (dragOffsetX > 0f && previous != null) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                translationX = ((dragProgress - 1f) * widthPx * 0.08f)
                            },
                    ) {
                        RenderChild(previous)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Color.Black.copy(
                                    alpha = 0.3f * (1f - dragProgress),
                                ),
                            ),
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged {
                        widthPx = it.width.toFloat()
                    }
                    .then(
                        if (canUseDragToBack) {
                            Modifier.pointerInput(canUseDragToBack) {
                                awaitEachGesture {
                                    if (size.width == 0) return@awaitEachGesture

                                    val down = awaitFirstDown(
                                        requireUnconsumed = false,
                                        pass = PointerEventPass.Main,
                                    )
                                    val pointerId = down.id
                                    val touchSlop = viewConfiguration.touchSlop
                                    val velocityTracker = VelocityTracker()
                                    velocityTracker.addPosition(down.uptimeMillis, down.position)

                                    var totalDx = 0f
                                    var totalDy = 0f
                                    var isDragging = false
                                    var shouldAnimateBack = false

                                    while (true) {
                                        val event = awaitPointerEvent(PointerEventPass.Main)
                                        if (event.changes.any { it.isConsumed } && !isDragging) {
                                            dragOffsetX = 0f
                                            break
                                        }

                                        val change =
                                            event.changes.fastFirstOrNull { it.id == pointerId } ?: break
                                        velocityTracker.addPosition(change.uptimeMillis, change.position)

                                        if (change.changedToUpIgnoreConsumed()) {
                                            if (isDragging) {
                                                val width = size.width.toFloat()
                                                val progress = (dragOffsetX / width).coerceIn(0f, 1f)
                                                val velocityX = velocityTracker.calculateVelocity().x
                                                val shouldCommit = progress >= 0.22f || velocityX >= 1400f

                                                if (shouldCommit) {
                                                    coroutineScope.launch {
                                                        isCompletingSwipeBack = true
                                                        animate(
                                                            initialValue = dragOffsetX,
                                                            targetValue = width,
                                                            animationSpec = tween(durationMillis = 180),
                                                        ) { value, _ ->
                                                            dragOffsetX = value
                                                        }
                                                        root.onBack()
                                                        dragOffsetX = 0f
                                                        isCompletingSwipeBack = false
                                                    }
                                                } else {
                                                    shouldAnimateBack = true
                                                }
                                            }

                                            break
                                        }

                                        val delta = change.position - change.previousPosition

                                        if (!isDragging) {
                                            totalDx += delta.x
                                            totalDy += delta.y

                                            val passedHorizontalSlop =
                                                totalDx > touchSlop && abs(totalDx) > abs(totalDy)
                                            val movedLeft = totalDx < -touchSlop

                                            if (movedLeft) {
                                                dragOffsetX = 0f
                                                break
                                            }

                                            if (!passedHorizontalSlop) {
                                                continue
                                            }

                                            isDragging = true
                                        }

                                        if (delta != Offset.Zero) {
                                            change.consume()
                                        }
                                        dragOffsetX =
                                            (dragOffsetX + delta.x).coerceIn(0f, size.width.toFloat())
                                    }

                                    if (shouldAnimateBack && dragOffsetX > 0f && !isCompletingSwipeBack) {
                                        coroutineScope.launch {
                                            animate(
                                                initialValue = dragOffsetX,
                                                targetValue = 0f,
                                                animationSpec = spring(),
                                            ) { value, _ ->
                                                dragOffsetX = value
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Modifier
                        }
                    )
                    .graphicsLayer {
                        translationX = dragOffsetX
                        shadowElevation = if (dragOffsetX > 0f) 12f else 0f
                    },
            ) {
                Children(
                    stack = root.childStack,
                    animation = predictiveBackAnimation(
                        backHandler = root.backHandler,
                        onBack = root::onBack,
                        fallbackAnimation = if (!isCompletingSwipeBack) stackAnimation(slide() + fade()) else null,
                    ),
                ) { child ->
                    key(child.key) {
                        RenderChild(
                            child = child.instance,
                            isOverlay = false,
                            onSwipeBackBlockedChanged = { blocked ->
                                if (stack.active.instance === child.instance) {
                                    isSwipeBackBlocked = blocked
                                }
                            },
                        )
                    }
                }
            }
        }

        // Bottom Navigation Bar — visible on Chats and Settings root screens
        AnimatedVisibility(
            visible = showBottomBar,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(tween(200)),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(tween(200)),
        ) {
            MainBottomBar(
                selectedTab = selectedTab ?: MainTab.Chats,
                onTabSelected = { tab ->
                    when (tab) {
                        MainTab.Chats -> root.onChatsClick()
                        MainTab.Groups -> root.onChatsClick() // Groups uses chat list with group filter
                        MainTab.Updates -> root.onChatsClick() // Updates uses chat list with channel filter
                        MainTab.Settings -> root.onSettingsClick()
                    }
                },
            )
        }
    }
}

private fun isSwipeBackSupported(child: RootComponent.Child): Boolean =
    when (child) {
        is RootComponent.Child.ChatDetailChild,
        is RootComponent.Child.ProfileChild,
        is RootComponent.Child.SettingsChild,
        is RootComponent.Child.EditProfileChild,
        is RootComponent.Child.SessionsChild,
        is RootComponent.Child.FoldersChild,
        is RootComponent.Child.ChatSettingsChild,
        is RootComponent.Child.DataStorageChild,
        is RootComponent.Child.StorageUsageChild,
        is RootComponent.Child.NetworkUsageChild,
        is RootComponent.Child.PremiumChild,
        is RootComponent.Child.PrivacyChild,
        is RootComponent.Child.AdBlockChild,
        is RootComponent.Child.PowerSavingChild,
        is RootComponent.Child.NotificationsChild,
        is RootComponent.Child.ProxyChild,
        is RootComponent.Child.ProfileLogsChild,
        is RootComponent.Child.AdminManageChild,
        is RootComponent.Child.ChatEditChild,
        is RootComponent.Child.MemberListChild,
        is RootComponent.Child.ChatPermissionsChild,
        is RootComponent.Child.StickersChild,
        is RootComponent.Child.AboutChild,
        is RootComponent.Child.NewChatChild -> true
        is RootComponent.Child.DebugChild -> true

        else -> false
    }
