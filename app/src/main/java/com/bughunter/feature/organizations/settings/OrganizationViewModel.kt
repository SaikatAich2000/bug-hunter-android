package com.bughunter.feature.organizations.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bughunter.core.data.repository.OrganizationRepository
import com.bughunter.core.network.Result2
import com.bughunter.core.network.dto.OrganizationOut
import com.bughunter.core.network.dto.OrganizationUpdate
import com.bughunter.core.ui.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal data class OrganizationModel(
    val org: OrganizationOut,
    val name: String,
    val description: String,
    val saving: Boolean = false,
    val saveError: String? = null,
)

@HiltViewModel
internal class OrganizationViewModel @Inject constructor(
    private val repo: OrganizationRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<OrganizationModel>>(UiState.Loading)
    val state: StateFlow<UiState<OrganizationModel>> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            when (val result = repo.get()) {
                is Result2.Ok -> _state.value = UiState.Success(
                    OrganizationModel(
                        org = result.value,
                        name = result.value.name,
                        description = result.value.description,
                    ),
                )
                is Result2.Err -> _state.value = UiState.Error(result.error)
            }
        }
    }

    fun onName(value: String) = mutate { it.copy(name = value) }
    fun onDescription(value: String) = mutate { it.copy(description = value) }

    fun save(onDone: (Boolean) -> Unit) {
        val current = (_state.value as? UiState.Success)?.data ?: return
        if (current.saving) return
        mutate { it.copy(saving = true, saveError = null) }
        viewModelScope.launch {
            val body = OrganizationUpdate(name = current.name, description = current.description)
            when (val result = repo.update(body)) {
                is Result2.Ok -> {
                    mutate { it.copy(saving = false, org = result.value) }
                    onDone(true)
                }
                is Result2.Err -> {
                    mutate { it.copy(saving = false, saveError = "Couldn't save changes.") }
                    onDone(false)
                }
            }
        }
    }

    private inline fun mutate(crossinline f: (OrganizationModel) -> OrganizationModel) {
        _state.update { current ->
            if (current is UiState.Success) current.copy(data = f(current.data)) else current
        }
    }
}
