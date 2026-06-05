package com.bughunter.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bughunter.core.data.repository.BugListFilters
import com.bughunter.core.data.repository.BugsRepository
import com.bughunter.core.data.repository.StatsRepository
import com.bughunter.core.domain.usecase.ToggleKpiFilterUseCase
import com.bughunter.core.network.Result2
import com.bughunter.core.network.dto.BugOut
import com.bughunter.core.ui.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal enum class DashboardTypeTab(val itemType: String?) {
    ALL(null),
    BUGS("Bug"),
    REQUIREMENTS("Requirement"),
    TASKS("Task"),
}

internal data class DashboardScreenModel(
    val stats: StatsView,
    val recentBugs: List<BugOut>,
    val activeTile: ToggleKpiFilterUseCase.KpiTile?,
    val tab: DashboardTypeTab,
)

@HiltViewModel
internal class DashboardViewModel @Inject constructor(
    private val statsRepository: StatsRepository,
    private val bugsRepository: BugsRepository,
    private val toggleKpiFilter: ToggleKpiFilterUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<DashboardScreenModel>>(UiState.Loading)
    val state: StateFlow<UiState<DashboardScreenModel>> = _state.asStateFlow()

    private val _filters = MutableStateFlow(BugListFilters(pageSize = RECENT_PAGE_SIZE))
    val filters: StateFlow<BugListFilters> = _filters.asStateFlow()

    private val _tab = MutableStateFlow(DashboardTypeTab.ALL)
    val tab: StateFlow<DashboardTypeTab> = _tab.asStateFlow()

    private val _activeTile = MutableStateFlow<ToggleKpiFilterUseCase.KpiTile?>(null)

    init {
        refresh()
    }

    fun refresh() {
        _state.update { UiState.Loading }
        viewModelScope.launch {
            val statsResult = statsRepository.get(itemType = _tab.value.itemType)
            val bugsResult = bugsRepository.list(filtersForListing())
            when {
                statsResult is Result2.Err -> _state.update { UiState.Error(statsResult.error) }
                bugsResult is Result2.Err -> _state.update { UiState.Error(bugsResult.error) }
                statsResult is Result2.Ok && bugsResult is Result2.Ok -> {
                    val view = StatsView.from(statsResult.value)
                    val recent = bugsResult.value.items.take(RECENT_PAGE_SIZE)
                    val model = DashboardScreenModel(
                        stats = view,
                        recentBugs = recent,
                        activeTile = _activeTile.value,
                        tab = _tab.value,
                    )
                    _state.update { UiState.Success(model) }
                }
            }
        }
    }

    fun onTabChange(next: DashboardTypeTab) {
        if (_tab.value == next) return
        _tab.update { next }
        _filters.update { it.copy(itemTypes = listOfNotNull(next.itemType), page = 1) }
        refresh()
    }

    fun onKpiToggle(tile: ToggleKpiFilterUseCase.KpiTile) {
        val nextFilters = toggleKpiFilter(_filters.value, tile)
        val nextTile = if (nextFilters.statuses.isEmpty()) null else tile
        _filters.update { nextFilters }
        _activeTile.update { nextTile }
        // Re-fetch recent bugs scoped by the KPI without re-fetching stats.
        viewModelScope.launch {
            when (val result = bugsRepository.list(filtersForListing())) {
                is Result2.Ok -> {
                    val current = (_state.value as? UiState.Success)?.data ?: return@launch
                    _state.update {
                        UiState.Success(
                            current.copy(
                                recentBugs = result.value.items.take(RECENT_PAGE_SIZE),
                                activeTile = nextTile,
                            ),
                        )
                    }
                }
                is Result2.Err -> _state.update { UiState.Error(result.error) }
            }
        }
    }

    private fun filtersForListing(): BugListFilters = _filters.value.copy(
        pageSize = RECENT_PAGE_SIZE,
        page = 1,
    )

    companion object {
        private const val RECENT_PAGE_SIZE: Int = 6
    }
}
