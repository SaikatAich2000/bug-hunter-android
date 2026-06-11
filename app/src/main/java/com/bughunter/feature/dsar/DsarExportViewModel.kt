package com.bughunter.feature.dsar

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bughunter.core.data.repository.AuthRepository
import com.bughunter.core.network.DomainError
import com.bughunter.core.network.Result2
import com.bughunter.core.network.dto.DeleteAccountIn
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
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

internal data class DsarExportEntry(
    val fileName: String,
    val timestampLabel: String,
)

internal data class DsarExportUiState(
    val isExporting: Boolean = false,
    val exports: List<DsarExportEntry> = emptyList(),
    val error: DomainError? = null,
    val isDeleteDialogOpen: Boolean = false,
    val isDeleting: Boolean = false,
    val deleteError: DomainError? = null,
    val deleteCompleted: Boolean = false,
)

@HiltViewModel
internal class DsarExportViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context,
    moshi: Moshi,
) : ViewModel() {

    private val mapAdapter: JsonAdapter<Map<String, Any?>> = moshi.adapter(
        Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java),
    )

    private val _state = MutableStateFlow(DsarExportUiState())
    val state: StateFlow<DsarExportUiState> = _state.asStateFlow()

    fun exportData() {
        viewModelScope.launch {
            _state.update { it.copy(isExporting = true, error = null) }
            when (val result = authRepository.dataExport()) {
                is Result2.Ok -> {
                    val name = writeJson(mapAdapter.toJson(result.value))
                    val entry = DsarExportEntry(
                        fileName = name ?: "data-export.json",
                        timestampLabel = nowLabel(),
                    )
                    _state.update {
                        it.copy(
                            isExporting = false,
                            exports = listOf(entry) + it.exports,
                        )
                    }
                }
                is Result2.Err -> _state.update {
                    it.copy(isExporting = false, error = result.error)
                }
            }
        }
    }

    fun openDeleteDialog() {
        _state.update { it.copy(isDeleteDialogOpen = true, deleteError = null) }
    }

    fun dismissDeleteDialog() {
        _state.update { it.copy(isDeleteDialogOpen = false) }
    }

    fun confirmDelete(password: String) {
        if (password.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(isDeleting = true, deleteError = null) }
            when (val result = authRepository.deleteAccount(DeleteAccountIn(password = password))) {
                is Result2.Ok -> _state.update {
                    it.copy(isDeleting = false, isDeleteDialogOpen = false, deleteCompleted = true)
                }
                is Result2.Err -> _state.update {
                    it.copy(isDeleting = false, deleteError = result.error)
                }
            }
        }
    }

    private suspend fun writeJson(json: String): String? = withContext(Dispatchers.IO) {
        val ts = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
            .format(java.time.LocalDateTime.now())
        val fileName = "bh-data-export-$ts.json"
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
            // minSdk 29 → scoped storage is always available.
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/BugHunter")
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return@withContext null
        resolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
        fileName
    }

    private fun nowLabel(): String = DateTimeFormatter.ofPattern("d MMM yyyy HH:mm")
        .format(java.time.LocalDateTime.now())
}
