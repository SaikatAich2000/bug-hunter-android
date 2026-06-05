package com.bughunter.feature.bugs.edit

import com.bughunter.core.network.DomainError
import com.bughunter.core.network.dto.BugCreate
import com.bughunter.core.network.dto.BugDetail
import com.bughunter.core.network.dto.BugUpdate

internal data class BugEditFormState(
    val projectId: Int? = null,
    val title: String = "",
    val description: String = "",
    val reporterId: Int? = null,
    val assigneeIds: Set<Int> = emptySet(),
    val status: String = "New",
    val priority: String = "Medium",
    val environment: String = "DEV",
    val dueDate: String? = null,
    val itemType: String = "Bug",
    val eventId: Int? = null,
    val isSubmitting: Boolean = false,
    val error: DomainError? = null,
    val fieldErrors: Map<String, String> = emptyMap(),
) {
    val canSubmit: Boolean
        get() = projectId != null && title.trim().length in 3..200 && !isSubmitting

    fun toCreate(): BugCreate? {
        val pid = projectId ?: return null
        return BugCreate(
            projectId = pid,
            title = title.trim(),
            description = description,
            reporterId = reporterId,
            assigneeIds = assigneeIds.toList(),
            status = status,
            priority = priority,
            environment = environment,
            dueDate = dueDate?.takeIf { it.isNotBlank() },
            itemType = itemType,
            eventId = eventId,
        )
    }

    fun toUpdate(): BugUpdate = BugUpdate(
        projectId = projectId,
        title = title.trim().ifBlank { null },
        description = description,
        reporterId = reporterId,
        assigneeIds = assigneeIds.toList(),
        status = status,
        priority = priority,
        environment = environment,
        dueDate = dueDate,
        itemType = itemType,
        eventId = eventId,
    )

    fun toggleAssignee(id: Int): BugEditFormState =
        copy(assigneeIds = if (id in assigneeIds) assigneeIds - id else assigneeIds + id)

    companion object {
        fun fromDetail(detail: BugDetail): BugEditFormState = BugEditFormState(
            projectId = detail.projectId,
            title = detail.title,
            description = detail.description,
            reporterId = detail.reporter?.id,
            assigneeIds = detail.assignees.map { it.id }.toSet(),
            status = detail.status,
            priority = detail.priority,
            environment = detail.environment,
            dueDate = detail.dueDate,
            itemType = detail.itemType,
            eventId = detail.eventId,
        )
    }
}
