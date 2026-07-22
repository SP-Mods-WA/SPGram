package com.spmods.spgram.presentation.settings.homeScreen

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import com.spmods.spgram.presentation.core.util.AppPreferences
import com.spmods.spgram.presentation.core.util.componentScope
import com.spmods.spgram.presentation.root.AppComponentContext

interface HomeScreenComponent {
    val state: Value<State>

    fun onBackClicked()
    fun onShowStoriesChanged(enabled: Boolean)
    fun onShowArchiveChanged(enabled: Boolean)
    fun onShowBottomBarLabelsChanged(enabled: Boolean)
    fun onShowOnlineStatusChanged(enabled: Boolean)
    fun onCompactChatListChanged(enabled: Boolean)

    data class State(
        val showStories: Boolean = true,
        val showArchive: Boolean = true,
        val showBottomBarLabels: Boolean = true,
        val showOnlineStatus: Boolean = true,
        val isCompactChatList: Boolean = false
    )
}

class DefaultHomeScreenComponent(
    context: AppComponentContext,
    private val onBack: () -> Unit
) : HomeScreenComponent, AppComponentContext by context {

    private val appPreferences: AppPreferences = container.preferences.appPreferences
    private val scope = componentScope

    private val _state = MutableValue(
        HomeScreenComponent.State(
            showStories = appPreferences.showStories.value,
            showArchive = appPreferences.isArchivePinned.value,
            showBottomBarLabels = appPreferences.showBottomBarLabels.value,
            showOnlineStatus = appPreferences.showOnlineStatus.value,
            isCompactChatList = appPreferences.isCompactChatList.value
        )
    )
    override val state: Value<HomeScreenComponent.State> = _state

    init {
        appPreferences.showStories
            .onEach { enabled -> _state.update { it.copy(showStories = enabled) } }
            .launchIn(scope)

        appPreferences.isArchivePinned
            .onEach { enabled -> _state.update { it.copy(showArchive = enabled) } }
            .launchIn(scope)

        appPreferences.showBottomBarLabels
            .onEach { enabled -> _state.update { it.copy(showBottomBarLabels = enabled) } }
            .launchIn(scope)

        appPreferences.showOnlineStatus
            .onEach { enabled -> _state.update { it.copy(showOnlineStatus = enabled) } }
            .launchIn(scope)

        appPreferences.isCompactChatList
            .onEach { enabled -> _state.update { it.copy(isCompactChatList = enabled) } }
            .launchIn(scope)
    }

    override fun onBackClicked() = onBack()

    override fun onShowStoriesChanged(enabled: Boolean) {
        appPreferences.setShowStories(enabled)
    }

    override fun onShowArchiveChanged(enabled: Boolean) {
        appPreferences.setArchivePinned(enabled)
    }

    override fun onShowBottomBarLabelsChanged(enabled: Boolean) {
        appPreferences.setShowBottomBarLabels(enabled)
    }

    override fun onShowOnlineStatusChanged(enabled: Boolean) {
        appPreferences.setShowOnlineStatus(enabled)
    }

    override fun onCompactChatListChanged(enabled: Boolean) {
        appPreferences.setCompactChatList(enabled)
    }
}
