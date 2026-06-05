package com.bughunter.feature.projects.settings

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

internal data class ProjectSettingsModel(
    val project: ProjectOut,
    val name: String,
    val key: String,
    val color: String,
    val description: String,
    val defaultItemType: String,
    val saving: Boolean = false,
    val saveError: String? = null,
)

@HiltViewModel
internal class ProjectSettingsViewModel @Inject constructor(
    private val repo: ProjectsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<ProjectSettingsModel>>(UiState.Loading)
    val state: StateFlow<UiState<ProjectSettingsModel>> = _state.asStateFlow()

    fun load(projectId: Int) {
        viewModelScope.launch {
            _state.value = UiState.Loading
            when (val result = repo.get(projectId)) {
                is Result2.Ok -> _state.value = UiState.Success(
                    ProjectSettingsModel(
                        project = result.value,
                        name = result.value.name,
                        key = result.value.key,
                        color = result.value.color,
                        description = result.value.description,
                        defaultItemType = "Bug",
                    ),
                )
                is Result2.Err -> _state.value = UiState.Error(result.error)
            }
        }
    }

    fun onName(value: String) = mutate { it.copy(name = value) }
    fun onKey(value: String) = mutate { it.copy(key = value.uppercase()) }
    fun onColor(value: String) = mutate { it.copy(color = value) }
    fun onDescription(value: String) = mutate { it.copy(description = value) }
    fun onDefaultItemType(value: String) = mutate { it.copy(defaultItemType = value) }

    fun save(onDone: (Boolean) -> Unit) {
        val current = (_state.value as? UiState.Success)?.data ?: return
        if (current.saving) return
        _state.update { (it as? UiState.Success)?.copy(data = current.copy(saving = true, saveError = null)) ?: it }
        viewModelScope.launch {
            val body = ProjectIn(
                name = current.name,
                key = current.key.ifBlank { null },
                description = current.description,
                color = current.color,
            )
            when (val result = repo.update(current.project.id, body)) {
                is Result2.Ok -> {
                    _state.update {
                        (it as? UiState.Success)?.copy(
                            data = current.copy(saving = false, project = result.value),
                        ) ?: it
                    }
                    onDone(true)
                }
                is Result2.Err -> {
                    _state.update {
                        (it as? UiState.Success)?.copy(
                            data = current.copy(saving = false, saveError = "Couldn't save."),
                        ) ?: it
                    }
                    onDone(false)
                }
            }
        }
    }

    private inline fun mutate(crossinline f: (ProjectSettingsModel) -> ProjectSettingsModel) {
        _state.update { current ->
            if (current is UiState.Success) current.copy(data = f(current.data)) else current
        }
    }
}
