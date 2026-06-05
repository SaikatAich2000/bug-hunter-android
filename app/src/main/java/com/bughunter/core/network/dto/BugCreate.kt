package com.bughunter.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class BugCreate(
    @Json(name = "project_id") val projectId: Int,
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String = "",
    @Json(name = "reporter_id") val reporterId: Int? = null,
    @Json(name = "assignee_ids") val assigneeIds: List<Int> = emptyList(),
    @Json(name = "status") val status: String = "New",
    @Json(name = "priority") val priority: String = "Medium",
    @Json(name = "environment") val environment: String = "DEV",
    @Json(name = "due_date") val dueDate: String? = null,
    @Json(name = "item_type") val itemType: String = "Bug",
    @Json(name = "event_id") val eventId: Int? = null,
)
