package com.bughunter.feature.projects.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bughunter.core.data.repository.ProjectsRepository
import com.bughunter.core.network.Result2
import com.bughunter.core.network.dto.ProjectIn
import com.bughunter.core.network.dto.ProjectOut
import com.bughunter.core.ui.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal data class ProjectsListModel(
    val projects: List<ProjectOut>,
    val query: String,
    val isAdmin: Boolean,
) {
    val filtered: List<ProjectOut>
        get() = if (query.isBlank()) projects else projects.filter {
            it.name.contains(query, ignoreCase = true) || it.key.contains(query, ignoreCase = true)
        }
}

@HiltViewModel
internal class ProjectsViewModel @Inject constructor(
    private val repo: ProjectsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<ProjectsListModel>>(UiState.Loading)
    val state: StateFlow<UiState<ProjectsListModel>> = _state.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            when (val result = repo.list()) {
                is Result2.Ok -> _state.value = UiState.Success(
                    ProjectsListModel(
                        projects = result.value,
                        query = _query.value,
                        isAdmin = result.value.any { it.canManage },
                    ),
                )
                is Result2.Err -> _state.value = UiState.Error(result.error)
            }
        }
    }

    fun onQueryChange(value: String) {
        _query.value = value
        _state.update { current ->
            if (current is UiState.Success) current.copy(data = current.data.copy(query = value)) else current
        }
    }

    fun createProject(name: String, key: String?, color: String, description: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val body = ProjectIn(name = name, key = key, description = description, color = color)
            when (repo.create(body)) {
                is Result2.Ok -> {
                    onDone(true)
                    load()
                }
                is Result2.Err -> onDone(false)
            }
        }
    }
}
