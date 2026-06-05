package com.bughunter.feature.bugs.savedViews

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bughunter.core.data.repository.SavedViewsRepository
import com.bughunter.core.network.Result2
import com.bughunter.core.network.dto.SavedViewIn
import com.bughunter.core.network.dto.SavedViewOut
import com.bughunter.core.ui.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal data class SavedViewsScreenModel(
    val views: List<SavedViewOut> = emptyList(),
    val isSaving: Boolean = false,
    val draftName: String = "",
)

@HiltViewModel
internal class SavedViewsViewModel @Inject constructor(
    private val savedViewsRepository: SavedViewsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<SavedViewsScreenModel>>(UiState.Loading)
    val state: StateFlow<UiState<SavedViewsScreenModel>> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            when (val result = savedViewsRepository.list()) {
                is Result2.Ok -> _state.value = UiState.Success(SavedViewsScreenModel(views = result.value))
                is Result2.Err -> _state.value = UiState.Error(result.error)
            }
        }
    }

    fun onDraftNameChange(value: String) {
        val current = _state.value
        if (current is UiState.Success) {
            _state.value = UiState.Success(current.data.copy(draftName = value))
        }
    }

    fun saveCurrent(filtersBlob: Map<String, Any?>) {
        val current = _state.value as? UiState.Success ?: return
        val name = current.data.draftName.trim()
        if (name.isEmpty() || current.data.isSaving) return
        _state.value = UiState.Success(current.data.copy(isSaving = true))
        viewModelScope.launch {
            val result = savedViewsRepository.create(
                SavedViewIn(name = name, filters = filtersBlob),
            )
            if (result is Result2.Ok) {
                _state.update {
                    val s = (it as? UiState.Success)?.data ?: return@update it
                    UiState.Success(
                        s.copy(
                            views = s.views + result.value,
                            draftName = "",
                            isSaving = false,
                        ),
                    )
                }
            } else {
                _state.update {
                    val s = (it as? UiState.Success)?.data ?: return@update it
                    UiState.Success(s.copy(isSaving = false))
                }
            }
        }
    }

    fun delete(viewId: Int) {
        viewModelScope.launch {
            val result = savedViewsRepository.delete(viewId)
            if (result is Result2.Ok) {
                _state.update {
                    val s = (it as? UiState.Success)?.data ?: return@update it
                    UiState.Success(s.copy(views = s.views.filter { v -> v.id != viewId }))
                }
            }
        }
    }
}
