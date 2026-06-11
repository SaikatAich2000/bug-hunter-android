package com.bughunter.feature.bugs.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bughunter.core.data.repository.BugsRepository
import com.bughunter.core.nav.BhRoute
import com.bughunter.core.network.DomainError
import com.bughunter.core.network.Result2
import com.bughunter.core.network.dto.BugDetail
import com.bughunter.core.network.dto.CommentIn
import com.bughunter.core.ui.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

internal data class BugDetailScreenModel(
    val bug: BugDetail,
    val isPostingComment: Boolean = false,
    val commentDraft: String = "",
    val isReadOnly: Boolean = false,
    // Surfaced to a BhErrorBanner above the composer/attachments. Any
    // failed action (post comment, delete comment, delete attachment)
    // writes its error here so the button never just "stops" silently —
    // the user gets a tinted banner explaining what went wrong.
    val actionError: DomainError? = null,
) {
    val commentsNewestFirst get() = bug.comments.sortedByDescending { it.createdAt }
}

@HiltViewModel
internal class BugDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bugsRepository: BugsRepository,
) : ViewModel() {

    // Nav declares `bugId` as NavType.IntType in BhNavHost, so SavedStateHandle
    // stores it as java.lang.Integer. Reading it as `<String>` (the old code)
    // threw ClassCastException at first navigation — every "open bug detail"
    // crashed the app the moment Hilt instantiated this VM. Read as <Int>.
    // Fallback to -1 covers the unhappy case where the arg is missing entirely;
    // refresh() then short-circuits to UiState.Error(NotFound).
    private val bugId: Int = savedStateHandle[BhRoute.BugDetail.ARG_ID] ?: -1

    private val _state = MutableStateFlow<UiState<BugDetailScreenModel>>(UiState.Loading)
    val state: StateFlow<UiState<BugDetailScreenModel>> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        if (bugId <= 0) {
            _state.value = UiState.Error(DomainError.NotFound)
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
            // Typing also clears any stale action error from the previous
            // attempt — the user is recovering and the banner is no longer
            // relevant.
            _state.value = current.copy(data = current.data.copy(commentDraft = value, actionError = null))
        }
    }

    fun postComment() {
        val current = _state.value as? UiState.Success ?: return
        val draft = current.data.commentDraft.trim()
        if (draft.isEmpty()) return
        if (current.data.isPostingComment) return
        _state.value = current.copy(
            data = current.data.copy(isPostingComment = true, actionError = null),
        )
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
                        current.data.copy(
                            isPostingComment = false,
                            actionError = result.error,
                        ),
                    )
                }
            }
        }
    }

    fun deleteComment(commentId: Int) {
        val current = _state.value as? UiState.Success ?: return
        viewModelScope.launch {
            when (val result = bugsRepository.deleteComment(bugId, commentId)) {
                is Result2.Ok -> {
                    val refreshed = bugsRepository.get(bugId)
                    if (refreshed is Result2.Ok) {
                        _state.value = UiState.Success(
                            current.data.copy(bug = refreshed.value, actionError = null),
                        )
                    }
                }
                is Result2.Err -> _state.value = UiState.Success(
                    current.data.copy(actionError = result.error),
                )
            }
        }
    }

    fun deleteAttachment(attachmentId: Int) {
        val current = _state.value as? UiState.Success ?: return
        viewModelScope.launch {
            when (val result = bugsRepository.deleteAttachment(bugId, attachmentId)) {
                is Result2.Ok -> {
                    val refreshed = bugsRepository.get(bugId)
                    if (refreshed is Result2.Ok) {
                        _state.value = UiState.Success(
                            current.data.copy(bug = refreshed.value, actionError = null),
                        )
                    }
                }
                is Result2.Err -> _state.value = UiState.Success(
                    current.data.copy(actionError = result.error),
                )
            }
        }
    }

    fun dismissActionError() {
        val current = _state.value as? UiState.Success ?: return
        _state.value = UiState.Success(current.data.copy(actionError = null))
    }
}
