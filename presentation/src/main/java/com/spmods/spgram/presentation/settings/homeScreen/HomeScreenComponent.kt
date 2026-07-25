package com.spmods.spgram.presentation.settings.homeScreen

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import com.spmods.spgram.domain.models.UserModel
import com.spmods.spgram.domain.repository.UserRepository
import com.spmods.spgram.presentation.core.util.AppPreferences
import com.spmods.spgram.presentation.core.util.componentScope
import com.spmods.spgram.presentation.root.AppComponentContext

interface HomeScreenComponent {
    val state: Value<State>

    fun onBackClicked()
    fun onArchivePinnedChanged(enabled: Boolean)
    fun onArchiveAlwaysVisibleChanged(enabled: Boolean)
    fun onShowLinkPreviewsChanged(enabled: Boolean)
    fun onTabletInterfaceEnabledChanged(enabled: Boolean)
    fun onDragToBackChanged(enabled: Boolean)
    fun onChatListMessageLinesChanged(lines: Int)
    fun onShowChatListPhotosChanged(enabled: Boolean)

    data class State(
        val isArchivePinned: Boolean = true,
        val isArchiveAlwaysVisible: Boolean = false,
        val showLinkPreviews: Boolean = true,
        val isTabletInterfaceEnabled: Boolean = true,
        val isDragToBackEnabled: Boolean = true,
        val chatListMessageLines: Int = 1,
        val showChatListPhotos: Boolean = true,
        val isTablet: Boolean = false,
        val currentUser: UserModel? = null
    )
}

class DefaultHomeScreenComponent(
    context: AppComponentContext,
    private val onBack: () -> Unit,
    isTablet: Boolean = false
) : HomeScreenComponent, AppComponentContext by context {

    private val appPreferences: AppPreferences = container.preferences.appPreferences
    private val userRepository: UserRepository = container.repositories.userRepository
    private val scope = componentScope

    private val _state = MutableValue(
        HomeScreenComponent.State(
            isArchivePinned = appPreferences.isArchivePinned.value,
            isArchiveAlwaysVisible = appPreferences.isArchiveAlwaysVisible.value,
            showLinkPreviews = appPreferences.showLinkPreviews.value,
            isTabletInterfaceEnabled = appPreferences.isTabletInterfaceEnabled.value,
            isDragToBackEnabled = appPreferences.isDragToBackEnabled.value,
            chatListMessageLines = appPreferences.chatListMessageLines.value,
            showChatListPhotos = appPreferences.showChatListPhotos.value,
            isTablet = isTablet
        )
    )
    override val state: Value<HomeScreenComponent.State> = _state

    init {
        appPreferences.isArchivePinned
            .onEach { v -> _state.update { it.copy(isArchivePinned = v) } }
            .launchIn(scope)
        appPreferences.isArchiveAlwaysVisible
            .onEach { v -> _state.update { it.copy(isArchiveAlwaysVisible = v) } }
            .launchIn(scope)
        appPreferences.showLinkPreviews
            .onEach { v -> _state.update { it.copy(showLinkPreviews = v) } }
            .launchIn(scope)
        appPreferences.isTabletInterfaceEnabled
            .onEach { v -> _state.update { it.copy(isTabletInterfaceEnabled = v) } }
            .launchIn(scope)
        appPreferences.isDragToBackEnabled
            .onEach { v -> _state.update { it.copy(isDragToBackEnabled = v) } }
            .launchIn(scope)
        appPreferences.chatListMessageLines
            .onEach { v -> _state.update { it.copy(chatListMessageLines = v) } }
            .launchIn(scope)
        appPreferences.showChatListPhotos
            .onEach { v -> _state.update { it.copy(showChatListPhotos = v) } }
            .launchIn(scope)
        userRepository.currentUserFlow
            .onEach { user -> _state.update { it.copy(currentUser = user) } }
            .launchIn(scope)
    }

    override fun onBackClicked() = onBack()
    override fun onArchivePinnedChanged(enabled: Boolean) { appPreferences.setArchivePinned(enabled) }
    override fun onArchiveAlwaysVisibleChanged(enabled: Boolean) { appPreferences.setArchiveAlwaysVisible(enabled) }
    override fun onShowLinkPreviewsChanged(enabled: Boolean) { appPreferences.setShowLinkPreviews(enabled) }
    override fun onTabletInterfaceEnabledChanged(enabled: Boolean) { appPreferences.setTabletInterfaceEnabled(enabled) }
    override fun onDragToBackChanged(enabled: Boolean) { appPreferences.setDragToBackEnabled(enabled) }
    override fun onChatListMessageLinesChanged(lines: Int) { appPreferences.setChatListMessageLines(lines) }
    override fun onShowChatListPhotosChanged(enabled: Boolean) { appPreferences.setShowChatListPhotos(enabled) }
}
