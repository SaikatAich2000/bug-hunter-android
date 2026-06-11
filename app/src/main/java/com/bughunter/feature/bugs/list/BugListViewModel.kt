package com.bughunter.feature.bugs.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bughunter.core.data.repository.BugsRepository
import com.bughunter.core.data.repository.ProjectsRepository
import com.bughunter.core.data.repository.UsersRepository
import com.bughunter.core.network.Result2
import com.bughunter.core.network.dto.BugOut
import com.bughunter.core.network.dto.ProjectOut
import com.bughunter.core.network.dto.UserOut
import com.bughunter.core.ui.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal data class BugListPage(
    val items: List<BugOut>,
    val page: Int,
    val totalPages: Int,
    val total: Int,
)

internal data class BugListScreenModel(
    val filters: BugListFilters = BugListFilters(),
    val pages: List<BugListPage> = emptyList(),
    val isLoadingMore: Boolean = false,
    val projects: List<ProjectOut> = emptyList(),
    val users: List<UserOut> = emptyList(),
) {
    val allItems: List<BugOut> get() = pages.flatMap { it.items }
    val hasMore: Boolean get() {
        val last = pages.lastOrNull() ?: return false
        return last.page < last.totalPages
    }
}

@HiltViewModel
internal class BugListViewModel @Inject constructor(
    private val bugsRepository: BugsRepository,
    private val projectsRepository: ProjectsRepository,
    private val usersRepository: UsersRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<BugListScreenModel>>(UiState.Loading)
    val state: StateFlow<UiState<BugListScreenModel>> = _state.asStateFlow()

    private val _model = MutableStateFlow(BugListScreenModel())

    init {
        loadFacets()
        refresh()
    }

    fun refresh() {
        val filters = _model.value.filters.copy(page = 1)
        _model.update { it.copy(filters = filters, pages = emptyList()) }
        _state.value = UiState.Loading
        fetchPage(filters)
    }

    fun loadMore() {
        val model = _model.value
        if (_state.value !is UiState.Success) return
        if (model.isLoadingMore || !model.hasMore) return
        val next = model.filters.copy(page = (model.pages.lastOrNull()?.page ?: 0) + 1)
        _model.update { it.copy(filters = next, isLoadingMore = true) }
        fetchPage(next, append = true)
    }

    fun applyFilters(transform: (BugListFilters) -> BugListFilters) {
        val updated = transform(_model.value.filters).copy(page = 1)
        _model.update { it.copy(filters = updated) }
        refresh()
    }

    fun clearFilters() {
        _model.update { it.copy(filters = it.filters.cleared()) }
        refresh()
    }

    fun onQueryChange(value: String) {
        applyFilters { it.withQuery(value) }
    }

    fun toggleStatus(value: String) = applyFilters { it.toggleStatus(value) }
    fun togglePriority(value: String) = applyFilters { it.togglePriority(value) }
    fun toggleEnvironment(value: String) = applyFilters { it.toggleEnvironment(value) }
    fun toggleItemType(value: String) = applyFilters { it.toggleItemType(value) }
    fun setOnlyItemType(value: String?) = applyFilters { it.setOnlyItemType(value) }
    fun toggleProject(id: Int) = applyFilters { it.toggleProject(id) }
    fun toggleAssignee(id: Int) = applyFilters { it.toggleAssignee(id) }

    private fun fetchPage(filters: BugListFilters, append: Boolean = false) {
        viewModelScope.launch {
            when (val result = bugsRepository.list(filters.toRepo())) {
                is Result2.Ok -> {
                    val newPage = BugListPage(
                        items = result.value.items,
                        page = result.value.page,
                        totalPages = result.value.pages,
                        total = result.value.total,
                    )
                    val nextPages = if (append) _model.value.pages + newPage else listOf(newPage)
                    _model.update { it.copy(pages = nextPages, isLoadingMore = false) }
                    // Even an empty result set is a successful load — the
                    // list screen renders its own empty-state row.
                    _state.value = UiState.Success(_model.value)
                }
                is Result2.Err -> {
                    _model.update { it.copy(isLoadingMore = false) }
                    if (!append) {
                        _state.value = UiState.Error(result.error)
                    } else {
                        _state.value = UiState.Success(_model.value)
                    }
                }
            }
        }
    }

    private fun loadFacets() {
        viewModelScope.launch {
            val projects = projectsRepository.list()
            if (projects is Result2.Ok) {
                _model.update { it.copy(projects = projects.value) }
            }
            val users = usersRepository.list(includeInactive = false, query = null)
            if (users is Result2.Ok) {
                _model.update { it.copy(users = users.value) }
            }
            val current = _state.value
            if (current is UiState.Success) {
                _state.value = UiState.Success(_model.value)
            }
        }
    }
}
