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
import com.spmods.spgram.presentation.root.RootComponent
import kotlinx.coroutines.launch
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

    // Active tab — 3 tabs: Chats, Stories, Calls
    var selectedTab by remember { mutableStateOf(MainTab.Chats) }

    val activeChild = stack.active.instance
    val isOnChatsRoot by remember(activeChild) {
        derivedStateOf { activeChild is RootComponent.Child.ChatsChild }
    }
    val isOnSettingsRoot by remember(activeChild) {
        derivedStateOf {
            activeChild is RootComponent.Child.SettingsChild ||
            activeChild is RootComponent.Child.EditProfileChild ||
            activeChild is RootComponent.Child.SessionsChild ||
            activeChild is RootComponent.Child.DataStorageChild ||
            activeChild is RootComponent.Child.StorageUsageChild ||
            activeChild is RootComponent.Child.NetworkUsageChild ||
            activeChild is RootComponent.Child.AdBlockChild ||
            activeChild is RootComponent.Child.PowerSavingChild ||
            activeChild is RootComponent.Child.NotificationsChild ||
            activeChild is RootComponent.Child.ProxyChild ||
            activeChild is RootComponent.Child.PrivacyChild ||
            activeChild is RootComponent.Child.AboutChild ||
            activeChild is RootComponent.Child.StickersChild ||
            activeChild is RootComponent.Child.DebugChild
        }
    }
    val showBottomBar = isOnChatsRoot

    LaunchedEffect(isOnChatsRoot, isOnSettingsRoot) {
        when {
            isOnChatsRoot && selectedTab != MainTab.Chats &&
            selectedTab != MainTab.Stories && selectedTab != MainTab.Calls ->
                selectedTab = MainTab.Chats
        }
    }

    LaunchedEffect(canUseDragToBack) {
        if (!canUseDragToBack && dragOffsetX > 0f) {
            dragOffsetX = 0f
            isCompletingSwipeBack = false
        }
    }

    // ── Unread count — must be at Composable scope ─────────────────────────
    // subscribeAsState() is a @Composable and MUST be called at top-level scope.
    val chatsChild = stack.active.instance as? RootComponent.Child.ChatsChild
    val chatsComponent = chatsChild?.component
    // subscribeAsState() called here at Composable scope (not inside remember/let/lambda)
    val chatsStateHolder = chatsComponent?.state?.subscribeAsState()
    val chatsUnread = chatsStateHolder?.value?.chats
        ?.sumOf { chat -> chat.unreadCount }
        ?.coerceAtMost(99) ?: 0

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {

            // ── Previous screen (swipe-back peek) ─────────────────────────
            if (dragOffsetX > 0f && previous != null) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                translationX = ((dragProgress - 1f) * widthPx * 0.08f)
                            },
                    ) { RenderChild(previous) }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f * (1f - dragProgress))),
                    )
                }
            }

            // ── Main navigation stack ──────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { widthPx = it.width.toFloat() }
                    .then(
                        if (canUseDragToBack) {
                            Modifier.pointerInput(canUseDragToBack) {
                                awaitEachGesture {
                                    if (size.width == 0) return@awaitEachGesture
                                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Main)
                                    val pointerId = down.id
                                    val touchSlop = viewConfiguration.touchSlop
                                    val velocityTracker = VelocityTracker()
                                    velocityTracker.addPosition(down.uptimeMillis, down.position)
                                    var totalDx = 0f; var totalDy = 0f
                                    var isDragging = false; var shouldAnimateBack = false

                                    while (true) {
                                        val event = awaitPointerEvent(PointerEventPass.Main)
                                        if (event.changes.any { it.isConsumed } && !isDragging) { dragOffsetX = 0f; break }
                                        val change = event.changes.fastFirstOrNull { it.id == pointerId } ?: break
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
                                                        animate(dragOffsetX, width, animationSpec = tween(180)) { v, _ -> dragOffsetX = v }
                                                        root.onBack(); dragOffsetX = 0f; isCompletingSwipeBack = false
                                                    }
                                                } else { shouldAnimateBack = true }
                                            }
                                            break
                                        }

                                        val delta = change.position - change.previousPosition
                                        if (!isDragging) {
                                            totalDx += delta.x; totalDy += delta.y
                                            if (totalDx < -touchSlop) { dragOffsetX = 0f; break }
                                            if (!(totalDx > touchSlop && abs(totalDx) > abs(totalDy))) continue
                                            isDragging = true
                                        }
                                        if (delta != Offset.Zero) change.consume()
                                        dragOffsetX = (dragOffsetX + delta.x).coerceIn(0f, size.width.toFloat())
                                    }

                                    if (shouldAnimateBack && dragOffsetX > 0f && !isCompletingSwipeBack) {
                                        coroutineScope.launch {
                                            animate(dragOffsetX, 0f, animationSpec = spring()) { v, _ -> dragOffsetX = v }
                                        }
                                    }
                                }
                            }
                        } else Modifier
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
                                if (stack.active.instance === child.instance) isSwipeBackBlocked = blocked
                            },
                        )
                    }
                }
            }

            // ── Tab content overlays ───────────────────────────────────────
            if (isOnChatsRoot) {
                when (selectedTab) {
                    MainTab.Chats -> { /* ChatListContent already rendered in Children stack */ }
                    MainTab.Stories -> StoriesPlaceholderContent()
                    MainTab.Calls -> CallsPlaceholderContent()
                }
            }
        }

        // ── Bottom Navigation Bar ──────────────────────────────────────────
        AnimatedVisibility(
            visible = showBottomBar,
            enter = slideInVertically { it } + fadeIn(tween(200)),
            exit = slideOutVertically { it } + fadeOut(tween(200)),
        ) {
            MainBottomBar(
                selectedTab = selectedTab,
                chatsUnread = chatsUnread,
                onTabSelected = { tab ->
                    when (tab) {
                        MainTab.Chats -> {
                            if (!isOnChatsRoot) root.onChatsClick()
                            selectedTab = MainTab.Chats
                        }
                        MainTab.Stories -> {
                            if (!isOnChatsRoot) root.onChatsClick()
                            selectedTab = MainTab.Stories
                        }
                        MainTab.Calls -> {
                            if (!isOnChatsRoot) root.onChatsClick()
                            selectedTab = MainTab.Calls
                        }
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
        is RootComponent.Child.NewChatChild,
        is RootComponent.Child.DebugChild,
        is RootComponent.Child.PasscodeChild -> true
        else -> false
    }
