package com.bughunter.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.Instant

@JsonClass(generateAdapter = true)
internal data class BugDetail(
    @Json(name = "id") val id: Int,
    @Json(name = "project_id") val projectId: Int,
    @Json(name = "project_name") val projectName: String? = null,
    @Json(name = "project_key") val projectKey: String? = null,
    @Json(name = "item_type") val itemType: String = "Bug",
    @Json(name = "event_id") val eventId: Int? = null,
    @Json(name = "event_name") val eventName: String? = null,
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String = "",
    @Json(name = "reporter") val reporter: UserBrief? = null,
    @Json(name = "assignees") val assignees: List<UserBrief> = emptyList(),
    @Json(name = "status") val status: String,
    @Json(name = "priority") val priority: String,
    @Json(name = "environment") val environment: String,
    @Json(name = "due_date") val dueDate: String? = null,
    @Json(name = "created_at") val createdAt: Instant,
    @Json(name = "updated_at") val updatedAt: Instant,
    @Json(name = "attachment_count") val attachmentCount: Int = 0,
    @Json(name = "can_edit") val canEdit: Boolean = false,
    @Json(name = "comments") val comments: List<CommentOut> = emptyList(),
    @Json(name = "activities") val activities: List<ActivityOut> = emptyList(),
    @Json(name = "attachments") val attachments: List<AttachmentBrief> = emptyList(),
)
