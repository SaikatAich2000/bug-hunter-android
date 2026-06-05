package com.bughunter.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.Instant

@JsonClass(generateAdapter = true)
internal data class EventDetailOut(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String,
    @Json(name = "scheduled_for") val scheduledFor: String? = null,
    @Json(name = "managers") val managers: List<UserBrief> = emptyList(),
    @Json(name = "item_count") val itemCount: Int = 0,
    @Json(name = "created_at") val createdAt: Instant,
    @Json(name = "updated_at") val updatedAt: Instant,
    @Json(name = "can_edit") val canEdit: Boolean = false,
    @Json(name = "can_delete") val canDelete: Boolean = false,
    @Json(name = "items") val items: List<EventItemBrief> = emptyList(),
)
