package com.bughunter.core.network.dto

import com.bughunter.core.data.repository.RepoTestSupport
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant

/**
 * Round-trips a set of network DTOs through the fully-wired Moshi instance
 * ([RepoTestSupport.moshi]) to exercise the generated adapters end-to-end:
 * parsing fully-populated JSON, parsing JSON with optional fields omitted
 * (defaults / nulls), and serializing instances back out.
 */
class DtoSerializationTest {

    private val moshi = RepoTestSupport.moshi()

    // ---------------------------------------------------------------------
    // EventItemBrief
    // ---------------------------------------------------------------------

    @Test
    fun `EventItemBrief parses all fields`() {
        val adapter = moshi.adapter(EventItemBrief::class.java)
        val json = """
            {
              "id": 7,
              "item_type": "Bug",
              "title": "Login crashes",
              "project_id": 3,
              "project_name": "Checkout",
              "project_key": "CHK",
              "status": "In Progress",
              "priority": "High",
              "environment": "PROD",
              "due_date": "2026-02-01",
              "assignees": [
                { "id": 11, "name": "Ada", "email": "ada@x.io", "role": "admin" }
              ],
              "attachment_count": 4
            }
        """.trimIndent()

        val parsed = adapter.fromJson(json)!!
        assertThat(parsed.id).isEqualTo(7)
        assertThat(parsed.itemType).isEqualTo("Bug")
        assertThat(parsed.title).isEqualTo("Login crashes")
        assertThat(parsed.projectId).isEqualTo(3)
        assertThat(parsed.projectName).isEqualTo("Checkout")
        assertThat(parsed.projectKey).isEqualTo("CHK")
        assertThat(parsed.status).isEqualTo("In Progress")
        assertThat(parsed.priority).isEqualTo("High")
        assertThat(parsed.environment).isEqualTo("PROD")
        assertThat(parsed.dueDate).isEqualTo("2026-02-01")
        assertThat(parsed.assignees).hasSize(1)
        assertThat(parsed.assignees[0].id).isEqualTo(11)
        assertThat(parsed.assignees[0].role).isEqualTo(Role.ADMIN)
        assertThat(parsed.attachmentCount).isEqualTo(4)
    }

    @Test
    fun `EventItemBrief applies defaults when optionals omitted`() {
        val adapter = moshi.adapter(EventItemBrief::class.java)
        val json = """
            {
              "id": 1,
              "item_type": "Task",
              "title": "Ship",
              "project_id": 2,
              "status": "New",
              "priority": "Low",
              "environment": "DEV"
            }
        """.trimIndent()

        val parsed = adapter.fromJson(json)!!
        assertThat(parsed.projectName).isNull()
        assertThat(parsed.projectKey).isNull()
        assertThat(parsed.dueDate).isNull()
        assertThat(parsed.assignees).isEmpty()
        assertThat(parsed.attachmentCount).isEqualTo(0)
    }

    @Test
    fun `EventItemBrief serializes key fields`() {
        val adapter = moshi.adapter(EventItemBrief::class.java)
        val dto = EventItemBrief(
            id = 9,
            itemType = "Requirement",
            title = "New flow",
            projectId = 5,
            status = "Approved",
            priority = "Medium",
            environment = "UAT",
            assignees = listOf(UserBrief(id = 2, name = "Lin", email = "lin@x.io", role = Role.MANAGER)),
            attachmentCount = 2,
        )

        val out = adapter.toJson(dto)
        assertThat(out).contains("\"id\":9")
        assertThat(out).contains("\"item_type\":\"Requirement\"")
        assertThat(out).contains("\"title\":\"New flow\"")
        assertThat(out).contains("\"project_id\":5")
        assertThat(out).contains("\"status\":\"Approved\"")
        assertThat(out).contains("\"priority\":\"Medium\"")
        assertThat(out).contains("\"environment\":\"UAT\"")
        assertThat(out).contains("\"role\":\"manager\"")
        assertThat(out).contains("\"attachment_count\":2")

        // round-trip back
        val again = adapter.fromJson(out)!!
        assertThat(again).isEqualTo(dto)
    }

    // ---------------------------------------------------------------------
    // BugUpdate  (a *Update DTO -> OmitNullJsonAdapterFactory elides nulls)
    // ---------------------------------------------------------------------

    @Test
    fun `BugUpdate parses all fields`() {
        val adapter = moshi.adapter(BugUpdate::class.java)
        val json = """
            {
              "project_id": 3,
              "title": "Re-titled",
              "description": "details",
              "reporter_id": 8,
              "assignee_ids": [1, 2, 3],
              "status": "Resolved",
              "priority": "Critical",
              "environment": "PROD",
              "due_date": "2026-03-15",
              "item_type": "Bug",
              "event_id": 42
            }
        """.trimIndent()

        val parsed = adapter.fromJson(json)!!
        assertThat(parsed.projectId).isEqualTo(3)
        assertThat(parsed.title).isEqualTo("Re-titled")
        assertThat(parsed.description).isEqualTo("details")
        assertThat(parsed.reporterId).isEqualTo(8)
        assertThat(parsed.assigneeIds).containsExactly(1, 2, 3).inOrder()
        assertThat(parsed.status).isEqualTo("Resolved")
        assertThat(parsed.priority).isEqualTo("Critical")
        assertThat(parsed.environment).isEqualTo("PROD")
        assertThat(parsed.dueDate).isEqualTo("2026-03-15")
        assertThat(parsed.itemType).isEqualTo("Bug")
        assertThat(parsed.eventId).isEqualTo(42)
    }

    @Test
    fun `BugUpdate parses empty object as all nulls`() {
        val adapter = moshi.adapter(BugUpdate::class.java)
        val parsed = adapter.fromJson("{}")!!
        assertThat(parsed.projectId).isNull()
        assertThat(parsed.title).isNull()
        assertThat(parsed.description).isNull()
        assertThat(parsed.reporterId).isNull()
        assertThat(parsed.assigneeIds).isNull()
        assertThat(parsed.status).isNull()
        assertThat(parsed.priority).isNull()
        assertThat(parsed.environment).isNull()
        assertThat(parsed.dueDate).isNull()
        assertThat(parsed.itemType).isNull()
        assertThat(parsed.eventId).isNull()
    }

    @Test
    fun `BugUpdate omits null fields and serializes set fields`() {
        val adapter = moshi.adapter(BugUpdate::class.java)
        val dto = BugUpdate(title = "only title", status = "Closed")
        val out = adapter.toJson(dto)

        assertThat(out).contains("\"title\":\"only title\"")
        assertThat(out).contains("\"status\":\"Closed\"")
        // OmitNullJsonAdapterFactory drops null fields for *Update DTOs.
        assertThat(out).doesNotContain("project_id")
        assertThat(out).doesNotContain("description")
        assertThat(out).doesNotContain("event_id")
    }

    // ---------------------------------------------------------------------
    // ActivityOut  (Instant field)
    // ---------------------------------------------------------------------

    @Test
    fun `ActivityOut parses all fields`() {
        val adapter = moshi.adapter(ActivityOut::class.java)
        val json = """
            {
              "id": 100,
              "bug_id": 55,
              "entity_type": "bug",
              "entity_id": 55,
              "actor_user_id": 9,
              "actor_name": "Ada",
              "action": "status_changed",
              "detail": "New -> Resolved",
              "created_at": "2026-01-01T00:00:00Z"
            }
        """.trimIndent()

        val parsed = adapter.fromJson(json)!!
        assertThat(parsed.id).isEqualTo(100)
        assertThat(parsed.bugId).isEqualTo(55)
        assertThat(parsed.entityType).isEqualTo("bug")
        assertThat(parsed.entityId).isEqualTo(55)
        assertThat(parsed.actorUserId).isEqualTo(9)
        assertThat(parsed.actorName).isEqualTo("Ada")
        assertThat(parsed.action).isEqualTo("status_changed")
        assertThat(parsed.detail).isEqualTo("New -> Resolved")
        assertThat(parsed.createdAt).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"))
    }

    @Test
    fun `ActivityOut applies defaults when optionals omitted`() {
        val adapter = moshi.adapter(ActivityOut::class.java)
        val json = """
            {
              "id": 1,
              "entity_type": "comment",
              "action": "created",
              "created_at": "2026-06-10T08:52:25Z"
            }
        """.trimIndent()

        val parsed = adapter.fromJson(json)!!
        assertThat(parsed.bugId).isNull()
        assertThat(parsed.entityId).isNull()
        assertThat(parsed.actorUserId).isNull()
        assertThat(parsed.actorName).isEqualTo("system")
        assertThat(parsed.detail).isEqualTo("")
        assertThat(parsed.createdAt).isEqualTo(Instant.parse("2026-06-10T08:52:25Z"))
    }

    @Test
    fun `ActivityOut serializes key fields including ISO instant`() {
        val adapter = moshi.adapter(ActivityOut::class.java)
        val dto = ActivityOut(
            id = 3,
            entityType = "bug",
            action = "assigned",
            createdAt = Instant.parse("2026-05-05T12:30:00Z"),
        )
        val out = adapter.toJson(dto)

        assertThat(out).contains("\"id\":3")
        assertThat(out).contains("\"entity_type\":\"bug\"")
        assertThat(out).contains("\"action\":\"assigned\"")
        assertThat(out).contains("\"actor_name\":\"system\"")
        assertThat(out).contains("\"created_at\":\"2026-05-05T12:30:00Z\"")

        val again = adapter.fromJson(out)!!
        assertThat(again).isEqualTo(dto)
    }

    // ---------------------------------------------------------------------
    // CommentOut  (Instant + nested AttachmentBrief list)
    // ---------------------------------------------------------------------

    @Test
    fun `CommentOut parses all fields with nested attachment`() {
        val adapter = moshi.adapter(CommentOut::class.java)
        val json = """
            {
              "id": 12,
              "bug_id": 34,
              "author_user_id": 5,
              "author_name": "Lin",
              "body": "Looks fixed",
              "created_at": "2026-01-02T03:04:05Z",
              "attachments": [
                {
                  "id": 1,
                  "filename": "log.txt",
                  "content_type": "text/plain",
                  "size_bytes": 2048,
                  "uploader_user_id": 5,
                  "uploader_name": "Lin",
                  "comment_id": 12,
                  "created_at": "2026-01-02T03:04:06Z"
                }
              ]
            }
        """.trimIndent()

        val parsed = adapter.fromJson(json)!!
        assertThat(parsed.id).isEqualTo(12)
        assertThat(parsed.bugId).isEqualTo(34)
        assertThat(parsed.authorUserId).isEqualTo(5)
        assertThat(parsed.authorName).isEqualTo("Lin")
        assertThat(parsed.body).isEqualTo("Looks fixed")
        assertThat(parsed.createdAt).isEqualTo(Instant.parse("2026-01-02T03:04:05Z"))
        assertThat(parsed.attachments).hasSize(1)
        val att = parsed.attachments[0]
        assertThat(att.id).isEqualTo(1)
        assertThat(att.filename).isEqualTo("log.txt")
        assertThat(att.contentType).isEqualTo("text/plain")
        assertThat(att.sizeBytes).isEqualTo(2048L)
        assertThat(att.uploaderName).isEqualTo("Lin")
        assertThat(att.commentId).isEqualTo(12)
        assertThat(att.createdAt).isEqualTo(Instant.parse("2026-01-02T03:04:06Z"))
    }

    @Test
    fun `CommentOut applies defaults when optionals omitted`() {
        val adapter = moshi.adapter(CommentOut::class.java)
        val json = """
            {
              "id": 1,
              "bug_id": 2,
              "author_name": "system",
              "body": "auto",
              "created_at": "2026-01-01T00:00:00Z"
            }
        """.trimIndent()

        val parsed = adapter.fromJson(json)!!
        assertThat(parsed.authorUserId).isNull()
        assertThat(parsed.attachments).isEmpty()
    }

    @Test
    fun `CommentOut serializes key fields`() {
        val adapter = moshi.adapter(CommentOut::class.java)
        val dto = CommentOut(
            id = 8,
            bugId = 9,
            authorName = "Ada",
            body = "hello",
            createdAt = Instant.parse("2026-04-04T04:04:04Z"),
            attachments = listOf(
                AttachmentBrief(
                    id = 2,
                    filename = "shot.png",
                    contentType = "image/png",
                    sizeBytes = 100L,
                    uploaderName = "Ada",
                    createdAt = Instant.parse("2026-04-04T04:04:05Z"),
                ),
            ),
        )
        val out = adapter.toJson(dto)

        assertThat(out).contains("\"id\":8")
        assertThat(out).contains("\"bug_id\":9")
        assertThat(out).contains("\"author_name\":\"Ada\"")
        assertThat(out).contains("\"body\":\"hello\"")
        assertThat(out).contains("\"created_at\":\"2026-04-04T04:04:04Z\"")
        assertThat(out).contains("\"filename\":\"shot.png\"")
        assertThat(out).contains("\"content_type\":\"image/png\"")
        assertThat(out).contains("\"size_bytes\":100")

        val again = adapter.fromJson(out)!!
        assertThat(again).isEqualTo(dto)
    }

    // ---------------------------------------------------------------------
    // CustomFieldOut
    // ---------------------------------------------------------------------

    @Test
    fun `CustomFieldOut parses all fields`() {
        val adapter = moshi.adapter(CustomFieldOut::class.java)
        val json = """
            {
              "id": 4,
              "project_id": 6,
              "name": "Severity",
              "field_type": "select",
              "options": ["S1", "S2", "S3"],
              "is_required": true,
              "position": 2
            }
        """.trimIndent()

        val parsed = adapter.fromJson(json)!!
        assertThat(parsed.id).isEqualTo(4)
        assertThat(parsed.projectId).isEqualTo(6)
        assertThat(parsed.name).isEqualTo("Severity")
        assertThat(parsed.fieldType).isEqualTo("select")
        assertThat(parsed.options).containsExactly("S1", "S2", "S3").inOrder()
        assertThat(parsed.isRequired).isTrue()
        assertThat(parsed.position).isEqualTo(2)
    }

    @Test
    fun `CustomFieldOut applies empty options default when omitted`() {
        val adapter = moshi.adapter(CustomFieldOut::class.java)
        val json = """
            {
              "id": 1,
              "project_id": 1,
              "name": "Notes",
              "field_type": "text",
              "is_required": false,
              "position": 0
            }
        """.trimIndent()

        val parsed = adapter.fromJson(json)!!
        assertThat(parsed.options).isEmpty()
        assertThat(parsed.isRequired).isFalse()
    }

    @Test
    fun `CustomFieldOut serializes key fields`() {
        val adapter = moshi.adapter(CustomFieldOut::class.java)
        val dto = CustomFieldOut(
            id = 10,
            projectId = 11,
            name = "Component",
            fieldType = "select",
            options = listOf("API", "UI"),
            isRequired = true,
            position = 1,
        )
        val out = adapter.toJson(dto)

        assertThat(out).contains("\"id\":10")
        assertThat(out).contains("\"project_id\":11")
        assertThat(out).contains("\"name\":\"Component\"")
        assertThat(out).contains("\"field_type\":\"select\"")
        assertThat(out).contains("\"is_required\":true")
        assertThat(out).contains("\"position\":1")
        assertThat(out).contains("API")
        assertThat(out).contains("UI")

        val again = adapter.fromJson(out)!!
        assertThat(again).isEqualTo(dto)
    }
}
