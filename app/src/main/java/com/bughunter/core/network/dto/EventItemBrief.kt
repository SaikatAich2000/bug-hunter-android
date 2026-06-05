package com.bughunter.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class EventItemBrief(
    @Json(name = "id") val id: Int,
    @Json(name = "item_type") val itemType: String,
    @Json(name = "title") val title: String,
    @Json(name = "project_id") val projectId: Int,
    @Json(name = "project_name") val projectName: String? = null,
    @Json(name = "project_key") val projectKey: String? = null,
    @Json(name = "status") val status: String,
    @Json(name = "priority") val priority: String,
    @Json(name = "environment") val environment: String,
    @Json(name = "due_date") val dueDate: String? = null,
    @Json(name = "assignees") val assignees: List<UserBrief> = emptyList(),
    @Json(name = "attachment_count") val attachmentCount: Int = 0,
)
