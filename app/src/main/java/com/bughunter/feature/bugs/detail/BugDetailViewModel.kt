package com.bughunter.feature.bugs.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bughunter.core.data.repository.BugsRepository
import com.bughunter.core.nav.BhRoute
import com.bughunter.core.network.Result2
import com.bughunter.core.network.dto.BugDetail
import com.bughunter.core.network.dto.CommentIn
import com.bughunter.core.ui.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal data class BugDetailScreenModel(
    val bug: BugDetail,
    val isPostingComment: Boolean = false,
    val commentDraft: String = "",
    val isReadOnly: Boolean = false,
) {
    val commentsNewestFirst get() = bug.comments.sortedByDescending { it.createdAt }
}

@HiltViewModel
internal class BugDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bugsRepository: BugsRepository,
) : ViewModel() {

    private val bugId: Int = savedStateHandle.get<String>(BhRoute.BugDetail.ARG_ID)?.toIntOrNull() ?: -1

    private val _state = MutableStateFlow<UiState<BugDetailScreenModel>>(UiState.Loading)
    val state: StateFlow<UiState<BugDetailScreenModel>> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        if (bugId <= 0) {
            _state.value = UiState.Error(com.bughunter.core.network.DomainError.NotFound)
            return
        }
        _state.value = UiState.Loading
        viewModelScope.launch {
            when (val result = bugsRepository.get(bugId)) {
                is Result2.Ok -> _state.value = UiState.Success(BugDetailScreenModel(bug = result.value))
                is Result2.Err -> _state.value = UiState.Error(result.error)
            }
        }
    }

    fun onCommentDraftChange(value: String) {
        val current = _state.value
        if (current is UiState.Success) {
            _state.value = current.copy(data = current.data.copy(commentDraft = value))
        }
    }

    fun postComment() {
        val current = _state.value as? UiState.Success ?: return
        val draft = current.data.commentDraft.trim()
        if (draft.isEmpty()) return
        if (current.data.isPostingComment) return
        _state.value = current.copy(data = current.data.copy(isPostingComment = true))
        viewModelScope.launch {
            when (val result = bugsRepository.addComment(bugId, CommentIn(body = draft))) {
                is Result2.Ok -> {
                    val refreshed = bugsRepository.get(bugId)
                    if (refreshed is Result2.Ok) {
                        _state.value = UiState.Success(
                            BugDetailScreenModel(
                                bug = refreshed.value,
                                isPostingComment = false,
                                commentDraft = "",
                                isReadOnly = current.data.isReadOnly,
                            ),
                        )
                    } else {
                        val updated = current.data.copy(
                            bug = current.data.bug.copy(
                                comments = current.data.bug.comments + result.value,
                            ),
                            commentDraft = "",
                            isPostingComment = false,
                        )
                        _state.value = UiState.Success(updated)
                    }
                }
                is Result2.Err -> {
                    _state.value = UiState.Success(
                        current.data.copy(isPostingComment = false),
                    )
                }
            }
        }
    }

    fun deleteComment(commentId: Int) {
        val current = _state.value as? UiState.Success ?: return
        viewModelScope.launch {
            val result = bugsRepository.deleteComment(bugId, commentId)
            if (result is Result2.Ok) {
                val refreshed = bugsRepository.get(bugId)
                if (refreshed is Result2.Ok) {
                    _state.value = UiState.Success(current.data.copy(bug = refreshed.value))
                }
            }
        }
    }

    fun deleteAttachment(attachmentId: Int) {
        val current = _state.value as? UiState.Success ?: return
        viewModelScope.launch {
            val result = bugsRepository.deleteAttachment(bugId, attachmentId)
            if (result is Result2.Ok) {
                val refreshed = bugsRepository.get(bugId)
                if (refreshed is Result2.Ok) {
                    _state.value = UiState.Success(current.data.copy(bug = refreshed.value))
                }
            }
        }
    }
}
