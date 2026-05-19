package com.spmods.spgram.presentation.settings.networkUsage

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import kotlinx.coroutines.launch
import com.spmods.spgram.domain.models.NetworkUsageModel
import com.spmods.spgram.domain.repository.NetworkStatisticsRepository
import com.spmods.spgram.presentation.core.util.componentScope
import com.spmods.spgram.presentation.root.AppComponentContext

interface NetworkUsageComponent {
    val state: Value<State>
    fun onBackClicked()
    fun onResetClicked()
    fun onToggleNetworkStats(enabled: Boolean)

    data class State(
        val usage: NetworkUsageModel? = null,
        val isLoading: Boolean = true,
        val isNetworkStatsEnabled: Boolean = true
    )
}

class DefaultNetworkUsageComponent(
    context: AppComponentContext,
    private val onBack: () -> Unit
) : NetworkUsageComponent, AppComponentContext by context {

    private val networkStatisticsRepository: NetworkStatisticsRepository =
        container.repositories.networkStatisticsRepository
    private val _state = MutableValue(NetworkUsageComponent.State())
    override val state: Value<NetworkUsageComponent.State> = _state
    private val scope = componentScope

    init {
        loadStatistics()
    }

    private fun loadStatistics() {
        _state.update { it.copy(isLoading = true) }
        scope.launch {
            val isEnabled = networkStatisticsRepository.getNetworkStatisticsEnabled()
            val usage = if (isEnabled) networkStatisticsRepository.getNetworkUsage() else null
            _state.update { it.copy(usage = usage, isLoading = false, isNetworkStatsEnabled = isEnabled) }
        }
    }

    override fun onBackClicked() {
        onBack()
    }

    override fun onResetClicked() {
        scope.launch {
            val success = networkStatisticsRepository.resetNetworkStatistics()
            if (success) {
                loadStatistics()
            }
        }
    }

    override fun onToggleNetworkStats(enabled: Boolean) {
        scope.launch {
            networkStatisticsRepository.setNetworkStatisticsEnabled(enabled)
            if (!enabled) {
                networkStatisticsRepository.resetNetworkStatistics()
            }
            _state.update { it.copy(isNetworkStatsEnabled = enabled) }
            if (enabled) {
                loadStatistics()
            } else {
                _state.update { it.copy(usage = null) }
            }
        }
    }
}
