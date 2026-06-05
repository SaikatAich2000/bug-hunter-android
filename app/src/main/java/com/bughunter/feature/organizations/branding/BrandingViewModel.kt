package com.bughunter.feature.organizations.branding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bughunter.core.data.repository.BrandingRepository
import com.bughunter.core.network.Result2
import com.bughunter.core.network.dto.BrandingIn
import com.bughunter.core.ui.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal data class BrandingModel(
    val primary: String,
    val secondary: String,
    val accent: String,
    val logoDataUrl: String?,
    val faviconUrl: String?,
    val saving: Boolean = false,
    val saveError: String? = null,
)

@HiltViewModel
internal class BrandingViewModel @Inject constructor(
    private val repo: BrandingRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<BrandingModel>>(UiState.Loading)
    val state: StateFlow<UiState<BrandingModel>> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            when (val result = repo.get()) {
                is Result2.Ok -> _state.value = UiState.Success(
                    BrandingModel(
                        primary = result.value.accentColor ?: "#6366f1",
                        secondary = "#38bdf8",
                        accent = "#818cf8",
                        logoDataUrl = result.value.logoDataUrl,
                        faviconUrl = null,
                    ),
                )
                is Result2.Err -> _state.value = UiState.Error(result.error)
            }
        }
    }

    fun onPrimary(value: String) = mutate { it.copy(primary = value) }
    fun onSecondary(value: String) = mutate { it.copy(secondary = value) }
    fun onAccent(value: String) = mutate { it.copy(accent = value) }
    fun onLogo(dataUrl: String?) = mutate { it.copy(logoDataUrl = dataUrl) }
    fun onFavicon(url: String?) = mutate { it.copy(faviconUrl = url) }

    fun save(onDone: (Boolean) -> Unit) {
        val current = (_state.value as? UiState.Success)?.data ?: return
        if (current.saving) return
        mutate { it.copy(saving = true, saveError = null) }
        viewModelScope.launch {
            val body = BrandingIn(
                logoDataUrl = current.logoDataUrl,
                accentColor = current.primary,
            )
            when (repo.update(body)) {
                is Result2.Ok -> {
                    mutate { it.copy(saving = false) }
                    onDone(true)
                }
                is Result2.Err -> {
                    mutate { it.copy(saving = false, saveError = "Couldn't save branding.") }
                    onDone(false)
                }
            }
        }
    }

    private inline fun mutate(crossinline f: (BrandingModel) -> BrandingModel) {
        _state.update { current ->
            if (current is UiState.Success) current.copy(data = f(current.data)) else current
        }
    }
}
