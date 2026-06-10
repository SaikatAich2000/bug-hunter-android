package com.bughunter.feature.audit

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bughunter.core.data.repository.AuditRepository
import com.bughunter.core.network.DomainError
import com.bughunter.core.network.Result2
import com.bughunter.core.network.dto.ActivityOut
import com.bughunter.core.ui.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.format.DateTimeFormatter
import javax.inject.Inject

internal data class AuditFilters(
    val entityType: String? = null,
    val actorUserIdText: String = "",
    val query: String = "",
)

internal data class AuditUiState(
    val list: UiState<List<ActivityOut>> = UiState.Loading,
    val filters: AuditFilters = AuditFilters(),
    val isExporting: Boolean = false,
    val exportedFileName: String? = null,
    // Surfaced if the CSV export call fails. Previously the Result2.Err
    // branch just toggled isExporting=false with no message, so the
    // Export button quietly stopped spinning and the user assumed it had
    // already finished (or worse, that "nothing" happened).
    val exportError: DomainError? = null,
)

@HiltViewModel
internal class AuditViewModel @Inject constructor(
    private val repository: AuditRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(AuditUiState())
    val state: StateFlow<AuditUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val f = _state.value.filters
            val actorId = f.actorUserIdText.toIntOrNull()
            _state.update { it.copy(list = UiState.Loading) }
            when (val result = repository.list(
                entityType = f.entityType,
                actorUserId = actorId,
                query = f.query.ifBlank { null },
            )) {
                is Result2.Ok -> _state.update {
                    it.copy(
                        list = if (result.value.isEmpty()) UiState.Empty
                        else UiState.Success(result.value),
                    )
                }
                is Result2.Err -> _state.update {
                    it.copy(list = UiState.Error(result.error))
                }
            }
        }
    }

    fun onEntityTypeChange(value: String?) {
        _state.update { it.copy(filters = it.filters.copy(entityType = value)) }
        refresh()
    }

    fun onActorIdChange(value: String) {
        _state.update { it.copy(filters = it.filters.copy(actorUserIdText = value)) }
    }

    fun onQueryChange(value: String) {
        _state.update { it.copy(filters = it.filters.copy(query = value)) }
    }

    fun applyFilters() {
        refresh()
    }

    fun clearFilters() {
        _state.update { it.copy(filters = AuditFilters()) }
        refresh()
    }

    fun exportCsv() {
        viewModelScope.launch {
            _state.update { it.copy(isExporting = true, exportError = null) }
            val f = _state.value.filters
            val actorId = f.actorUserIdText.toIntOrNull()
            when (val result = repository.exportCsvStream(
                entityType = f.entityType,
                actorUserId = actorId,
                query = f.query.ifBlank { null },
            )) {
                is Result2.Ok -> {
                    val fileName = writeToDownloads(result.value.bytes())
                    _state.update {
                        it.copy(isExporting = false, exportedFileName = fileName)
                    }
                }
                is Result2.Err -> _state.update {
                    it.copy(isExporting = false, exportError = result.error)
                }
            }
        }
    }

    fun dismissExportToast() {
        _state.update { it.copy(exportedFileName = null) }
    }

    fun dismissExportError() {
        _state.update { it.copy(exportError = null) }
    }

    private suspend fun writeToDownloads(bytes: ByteArray): String? = withContext(Dispatchers.IO) {
        val ts = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
            .format(java.time.LocalDateTime.now())
        val fileName = "audit-export-$ts.csv"
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/BugHunter")
            }
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return@withContext null
        resolver.openOutputStream(uri)?.use { it.write(bytes) }
        fileName
    }
}
