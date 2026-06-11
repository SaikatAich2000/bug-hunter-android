package com.bughunter.feature.bugs.edit

import com.bughunter.core.network.DomainError
import com.bughunter.core.network.dto.BugDetail
import com.bughunter.core.network.dto.Role
import com.bughunter.core.network.dto.UserBrief
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant

internal class BugEditFormStateTest {

    @Test
    fun canSubmit_andCreate_updateMapping_andToggleAssignee() {
        // canSubmit false: no projectId, blank title.
        val empty = BugEditFormState()
        assertThat(empty.canSubmit).isFalse()
        assertThat(empty.toCreate()).isNull()

        // canSubmit false: title too short even with a project.
        assertThat(BugEditFormState(projectId = 1, title = "ab").canSubmit).isFalse()

        // canSubmit false: submitting in progress.
        assertThat(
            BugEditFormState(projectId = 1, title = "Valid title", isSubmitting = true).canSubmit,
        ).isFalse()

        // canSubmit true once project + 3..200 char title + not submitting.
        val state = BugEditFormState(
            projectId = 7,
            title = "  A real bug  ",
            description = "details",
            reporterId = 11,
            assigneeIds = setOf(2, 3),
            status = "Open",
            priority = "High",
            environment = "PROD",
            dueDate = "2026-07-01",
            itemType = "Requirement",
            eventId = 99,
            error = DomainError.NotFound,
            fieldErrors = mapOf("title" to "bad"),
        )
        assertThat(state.canSubmit).isTrue()

        // toCreate trims the title and blank-filters the due date.
        val create = state.toCreate()!!
        assertThat(create.projectId).isEqualTo(7)
        assertThat(create.title).isEqualTo("A real bug")
        assertThat(create.description).isEqualTo("details")
        assertThat(create.reporterId).isEqualTo(11)
        assertThat(create.assigneeIds).containsExactly(2, 3)
        assertThat(create.status).isEqualTo("Open")
        assertThat(create.priority).isEqualTo("High")
        assertThat(create.environment).isEqualTo("PROD")
        assertThat(create.dueDate).isEqualTo("2026-07-01")
        assertThat(create.itemType).isEqualTo("Requirement")
        assertThat(create.eventId).isEqualTo(99)

        // Blank due date collapses to null in toCreate.
        val blankDue = state.copy(dueDate = "   ").toCreate()!!
        assertThat(blankDue.dueDate).isNull()

        // toUpdate trims title and passes through other fields.
        val update = state.toUpdate()
        assertThat(update.projectId).isEqualTo(7)
        assertThat(update.title).isEqualTo("A real bug")
        assertThat(update.assigneeIds).containsExactly(2, 3)
        assertThat(update.dueDate).isEqualTo("2026-07-01")
        assertThat(update.eventId).isEqualTo(99)

        // Blank title becomes null in toUpdate.
        val blankTitleUpdate = state.copy(title = "   ").toUpdate()
        assertThat(blankTitleUpdate.title).isNull()

        // toggleAssignee adds when absent and removes when present.
        val added = state.toggleAssignee(5)
        assertThat(added.assigneeIds).containsExactly(2, 3, 5)
        val removed = added.toggleAssignee(2)
        assertThat(removed.assigneeIds).containsExactly(3, 5)
    }

    @Test
    fun fromDetail_mapsEveryField() {
        val detail = BugDetail(
            id = 100,
            projectId = 42,
            itemType = "Requirement",
            eventId = 8,
            title = "Detail title",
            description = "from detail",
            reporter = UserBrief(id = 1, name = "Rep", email = "rep@x.io", role = Role.ADMIN),
            assignees = listOf(
                UserBrief(id = 4, name = "A", email = "a@x.io", role = Role.MEMBER),
                UserBrief(id = 6, name = "B", email = "b@x.io", role = Role.MANAGER),
            ),
            status = "In Progress",
            priority = "Low",
            environment = "STAGE",
            dueDate = "2026-08-09",
            createdAt = Instant.EPOCH,
            updatedAt = Instant.EPOCH,
        )

        val state = BugEditFormState.fromDetail(detail)
        assertThat(state.projectId).isEqualTo(42)
        assertThat(state.title).isEqualTo("Detail title")
        assertThat(state.description).isEqualTo("from detail")
        assertThat(state.reporterId).isEqualTo(1)
        assertThat(state.assigneeIds).containsExactly(4, 6)
        assertThat(state.status).isEqualTo("In Progress")
        assertThat(state.priority).isEqualTo("Low")
        assertThat(state.environment).isEqualTo("STAGE")
        assertThat(state.dueDate).isEqualTo("2026-08-09")
        assertThat(state.itemType).isEqualTo("Requirement")
        assertThat(state.eventId).isEqualTo(8)
        assertThat(state.canSubmit).isTrue()

        // Null reporter maps to null reporterId (other branch of detail.reporter?.id).
        val noReporter = BugEditFormState.fromDetail(detail.copy(reporter = null, assignees = emptyList()))
        assertThat(noReporter.reporterId).isNull()
        assertThat(noReporter.assigneeIds).isEmpty()
    }
}
