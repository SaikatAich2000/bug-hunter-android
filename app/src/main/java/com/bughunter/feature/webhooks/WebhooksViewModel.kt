package com.bughunter.feature.webhooks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bughunter.core.data.repository.WebhooksRepository
import com.bughunter.core.network.Result2
import com.bughunter.core.network.dto.WebhookIn
import com.bughunter.core.network.dto.WebhookOut
import com.bughunter.core.network.dto.WebhookUpdateIn
import com.bughunter.core.ui.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

internal data class WebhooksModel(
    val webhooks: List<WebhookOut>,
)

@HiltViewModel
internal class WebhooksViewModel @Inject constructor(
    private val repo: WebhooksRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<WebhooksModel>>(UiState.Loading)
    val state: StateFlow<UiState<WebhooksModel>> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            when (val result = repo.list()) {
                is Result2.Ok -> _state.value = if (result.value.isEmpty()) UiState.Empty
                else UiState.Success(WebhooksModel(webhooks = result.value))
                is Result2.Err -> _state.value = UiState.Error(result.error)
            }
        }
    }

    fun create(url: String, secret: String?, eventTypes: List<String>, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val body = WebhookIn(url = url, secret = secret, active = true, eventTypes = eventTypes)
            when (repo.create(body)) {
                is Result2.Ok -> { load(); onDone(true) }
                is Result2.Err -> onDone(false)
            }
        }
    }

    fun update(id: Int, url: String?, secret: String?, active: Boolean?, eventTypes: List<String>?) {
        viewModelScope.launch {
            val body = WebhookUpdateIn(url = url, secret = secret, active = active, eventTypes = eventTypes)
            when (repo.update(id, body)) {
                is Result2.Ok -> load()
                is Result2.Err -> Unit
            }
        }
    }

    fun toggleActive(hook: WebhookOut) = update(hook.id, null, null, !hook.active, null)

    fun delete(id: Int) {
        viewModelScope.launch {
            when (repo.delete(id)) {
                is Result2.Ok -> load()
                is Result2.Err -> Unit
            }
        }
    }

    fun test(id: Int) {
        viewModelScope.launch {
            repo.test(id) // fire-and-forget; UI shows toast elsewhere
        }
    }
}
