package com.bughunter.feature.projects.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bughunter.core.data.repository.MembershipsRepository
import com.bughunter.core.data.repository.ProjectsRepository
import com.bughunter.core.network.Result2
import com.bughunter.core.network.dto.ProjectMembershipOut
import com.bughunter.core.network.dto.ProjectOut
import com.bughunter.core.ui.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

internal data class ProjectDetailModel(
    val project: ProjectOut,
    val members: List<ProjectMembershipOut>,
)

@HiltViewModel
internal class ProjectDetailViewModel @Inject constructor(
    private val projectsRepo: ProjectsRepository,
    private val membersRepo: MembershipsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<ProjectDetailModel>>(UiState.Loading)
    val state: StateFlow<UiState<ProjectDetailModel>> = _state.asStateFlow()

    fun load(projectId: Int) {
        viewModelScope.launch {
            _state.value = UiState.Loading
            val projectResult = projectsRepo.get(projectId)
            val membersResult = membersRepo.list(projectId)
            when {
                projectResult is Result2.Err -> _state.value = UiState.Error(projectResult.error)
                membersResult is Result2.Err -> _state.value = UiState.Error(membersResult.error)
                projectResult is Result2.Ok && membersResult is Result2.Ok ->
                    _state.value = UiState.Success(
                        ProjectDetailModel(projectResult.value, membersResult.value),
                    )
                else -> _state.value = UiState.Loading
            }
        }
    }
}
