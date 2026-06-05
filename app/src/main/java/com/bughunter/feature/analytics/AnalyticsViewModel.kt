package com.bughunter.feature.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bughunter.core.data.repository.StatsRepository
import com.bughunter.core.network.Result2
import com.bughunter.core.ui.util.UiState
import com.bughunter.feature.dashboard.DashboardTypeTab
import com.bughunter.feature.dashboard.StatsView
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal data class AnalyticsScreenModel(
    val stats: StatsView,
    val tab: DashboardTypeTab,
)

@HiltViewModel
internal class AnalyticsViewModel @Inject constructor(
    private val statsRepository: StatsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<AnalyticsScreenModel>>(UiState.Loading)
    val state: StateFlow<UiState<AnalyticsScreenModel>> = _state.asStateFlow()

    private val _tab = MutableStateFlow(DashboardTypeTab.ALL)
    val tab: StateFlow<DashboardTypeTab> = _tab.asStateFlow()

    init {
        refresh()
    }

    fun onTabChange(next: DashboardTypeTab) {
        if (_tab.value == next) return
        _tab.update { next }
        refresh()
    }

    fun refresh() {
        _state.update { UiState.Loading }
        viewModelScope.launch {
            when (val result = statsRepository.get(itemType = _tab.value.itemType)) {
                is Result2.Ok -> {
                    val view = StatsView.from(result.value)
                    _state.update {
                        UiState.Success(AnalyticsScreenModel(stats = view, tab = _tab.value))
                    }
                }
                is Result2.Err -> _state.update { UiState.Error(result.error) }
            }
        }
    }
}
