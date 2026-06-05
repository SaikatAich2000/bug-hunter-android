package com.bughunter.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class BugUpdate(
    @Json(name = "project_id") val projectId: Int? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "reporter_id") val reporterId: Int? = null,
    @Json(name = "assignee_ids") val assigneeIds: List<Int>? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "priority") val priority: String? = null,
    @Json(name = "environment") val environment: String? = null,
    @Json(name = "due_date") val dueDate: String? = null,
    @Json(name = "item_type") val itemType: String? = null,
    @Json(name = "event_id") val eventId: Int? = null,
)
