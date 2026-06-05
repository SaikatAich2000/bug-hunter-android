package com.bughunter.feature.organizations.members

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bughunter.core.data.repository.UsersRepository
import com.bughunter.core.network.Result2
import com.bughunter.core.network.dto.Role
import com.bughunter.core.network.dto.UserOut
import com.bughunter.core.network.dto.UserUpdate
import com.bughunter.core.ui.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal data class MembersModel(
    val users: List<UserOut>,
    val query: String = "",
    val page: Int = 0,
    val pageSize: Int = 20,
) {
    val filtered: List<UserOut>
        get() = if (query.isBlank()) users else users.filter {
            it.name.contains(query, ignoreCase = true) || it.email.contains(query, ignoreCase = true)
        }

    val pagedUsers: List<UserOut>
        get() {
            val start = page * pageSize
            return filtered.drop(start).take(pageSize)
        }

    val totalPages: Int get() = ((filtered.size + pageSize - 1) / pageSize).coerceAtLeast(1)
}

@HiltViewModel
internal class MembersViewModel @Inject constructor(
    private val repo: UsersRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<MembersModel>>(UiState.Loading)
    val state: StateFlow<UiState<MembersModel>> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            when (val result = repo.list(includeInactive = true)) {
                is Result2.Ok -> _state.value = UiState.Success(MembersModel(users = result.value))
                is Result2.Err -> _state.value = UiState.Error(result.error)
            }
        }
    }

    fun onQueryChange(value: String) = mutate { it.copy(query = value, page = 0) }
    fun nextPage() = mutate { it.copy(page = (it.page + 1).coerceAtMost(it.totalPages - 1)) }
    fun prevPage() = mutate { it.copy(page = (it.page - 1).coerceAtLeast(0)) }

    fun changeRole(userId: Int, role: Role) {
        viewModelScope.launch {
            when (repo.update(userId, UserUpdate(role = role))) {
                is Result2.Ok -> load()
                is Result2.Err -> Unit
            }
        }
    }

    fun deleteMember(userId: Int) {
        viewModelScope.launch {
            when (repo.delete(userId)) {
                is Result2.Ok -> load()
                is Result2.Err -> Unit
            }
        }
    }

    private inline fun mutate(crossinline f: (MembersModel) -> MembersModel) {
        _state.update { current ->
            if (current is UiState.Success) current.copy(data = f(current.data)) else current
        }
    }
}
