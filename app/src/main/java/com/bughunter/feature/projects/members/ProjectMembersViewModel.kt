package com.bughunter.feature.projects.members

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bughunter.core.data.repository.MembershipsRepository
import com.bughunter.core.data.repository.UsersRepository
import com.bughunter.core.network.Result2
import com.bughunter.core.network.dto.ProjectMembershipIn
import com.bughunter.core.network.dto.ProjectMembershipOut
import com.bughunter.core.network.dto.ProjectMembershipUpdate
import com.bughunter.core.network.dto.ProjectRole
import com.bughunter.core.network.dto.UserOut
import com.bughunter.core.ui.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal data class ProjectMembersModel(
    val members: List<ProjectMembershipOut>,
    val allUsers: List<UserOut>,
    val query: String = "",
) {
    val candidates: List<UserOut>
        get() {
            val taken = members.map { it.userId }.toSet()
            val pool = allUsers.filter { it.id !in taken }
            return if (query.isBlank()) pool else pool.filter {
                it.name.contains(query, ignoreCase = true) || it.email.contains(query, ignoreCase = true)
            }
        }
}

@HiltViewModel
internal class ProjectMembersViewModel @Inject constructor(
    private val membersRepo: MembershipsRepository,
    private val usersRepo: UsersRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<ProjectMembersModel>>(UiState.Loading)
    val state: StateFlow<UiState<ProjectMembersModel>> = _state.asStateFlow()

    private var projectId: Int = 0

    fun load(projectId: Int) {
        this.projectId = projectId
        viewModelScope.launch {
            _state.value = UiState.Loading
            val mList = membersRepo.list(projectId)
            val uList = usersRepo.list(includeInactive = false)
            when {
                mList is Result2.Err -> _state.value = UiState.Error(mList.error)
                uList is Result2.Err -> _state.value = UiState.Error(uList.error)
                mList is Result2.Ok && uList is Result2.Ok ->
                    _state.value = UiState.Success(
                        ProjectMembersModel(members = mList.value, allUsers = uList.value),
                    )
                else -> _state.value = UiState.Loading
            }
        }
    }

    fun onQueryChange(value: String) {
        _state.update { current ->
            if (current is UiState.Success) current.copy(data = current.data.copy(query = value)) else current
        }
    }

    fun add(userId: Int, role: ProjectRole) {
        viewModelScope.launch {
            when (membersRepo.add(projectId, ProjectMembershipIn(userId = userId, role = role))) {
                is Result2.Ok -> load(projectId)
                is Result2.Err -> Unit
            }
        }
    }

    fun changeRole(userId: Int, role: ProjectRole) {
        viewModelScope.launch {
            when (membersRepo.update(projectId, userId, ProjectMembershipUpdate(role = role))) {
                is Result2.Ok -> load(projectId)
                is Result2.Err -> Unit
            }
        }
    }

    fun remove(userId: Int) {
        viewModelScope.launch {
            when (membersRepo.remove(projectId, userId)) {
                is Result2.Ok -> load(projectId)
                is Result2.Err -> Unit
            }
        }
    }
}
