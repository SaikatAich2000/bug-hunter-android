package com.bughunter.feature.bugs.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bughunter.core.data.repository.BugsRepository
import com.bughunter.core.data.repository.ProjectsRepository
import com.bughunter.core.data.repository.UsersRepository
import com.bughunter.core.network.Result2
import com.bughunter.core.network.dto.ProjectOut
import com.bughunter.core.network.dto.UserOut
import com.bughunter.feature.bugs.edit.BugEditFormState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal data class BugCreateScreenModel(
    val form: BugEditFormState = BugEditFormState(),
    val projects: List<ProjectOut> = emptyList(),
    val users: List<UserOut> = emptyList(),
    val createdBugId: Int? = null,
)

@HiltViewModel
internal class BugCreateViewModel @Inject constructor(
    private val bugsRepository: BugsRepository,
    private val projectsRepository: ProjectsRepository,
    private val usersRepository: UsersRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(BugCreateScreenModel())
    val state: StateFlow<BugCreateScreenModel> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val projects = projectsRepository.list()
            if (projects is Result2.Ok) {
                _state.update { it.copy(projects = projects.value) }
            }
            val users = usersRepository.list(includeInactive = false)
            if (users is Result2.Ok) {
                _state.update { it.copy(users = users.value) }
            }
        }
    }

    fun onTitleChange(value: String) = updateForm { it.copy(title = value) }
    fun onDescriptionChange(value: String) = updateForm { it.copy(description = value) }
    fun onProjectSelect(projectId: Int) = updateForm { it.copy(projectId = projectId) }
    fun onItemTypeSelect(value: String) = updateForm { it.copy(itemType = value) }
    fun onStatusSelect(value: String) = updateForm { it.copy(status = value) }
    fun onPrioritySelect(value: String) = updateForm { it.copy(priority = value) }
    fun onEnvironmentSelect(value: String) = updateForm { it.copy(environment = value) }
    fun onDueDateChange(value: String?) = updateForm { it.copy(dueDate = value) }
    fun toggleAssignee(userId: Int) = updateForm { it.toggleAssignee(userId) }

    fun submit() {
        val current = _state.value
        val payload = current.form.toCreate() ?: return
        if (!current.form.canSubmit) return
        _state.update { it.copy(form = it.form.copy(isSubmitting = true, error = null)) }
        viewModelScope.launch {
            when (val result = bugsRepository.create(payload)) {
                is Result2.Ok -> _state.update {
                    it.copy(
                        form = it.form.copy(isSubmitting = false),
                        createdBugId = result.value.id,
                    )
                }
                is Result2.Err -> _state.update {
                    it.copy(form = it.form.copy(isSubmitting = false, error = result.error))
                }
            }
        }
    }

    fun consumeCreated() {
        _state.update { it.copy(createdBugId = null) }
    }

    private fun updateForm(transform: (BugEditFormState) -> BugEditFormState) {
        _state.update { it.copy(form = transform(it.form)) }
    }
}
