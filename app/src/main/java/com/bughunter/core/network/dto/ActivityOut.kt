package com.bughunter.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.Instant

@JsonClass(generateAdapter = true)
internal data class ActivityOut(
    @Json(name = "id") val id: Int,
    @Json(name = "bug_id") val bugId: Int? = null,
    @Json(name = "entity_type") val entityType: String,
    @Json(name = "entity_id") val entityId: Int? = null,
    @Json(name = "actor_user_id") val actorUserId: Int? = null,
    @Json(name = "actor_name") val actorName: String = "system",
    @Json(name = "action") val action: String,
    @Json(name = "detail") val detail: String = "",
    @Json(name = "created_at") val createdAt: Instant,
)
