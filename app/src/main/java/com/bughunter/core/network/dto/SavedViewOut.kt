package com.bughunter.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.Instant

@JsonClass(generateAdapter = true)
internal data class SavedViewOut(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "owner_user_id") val ownerUserId: Int? = null,
    @Json(name = "owner_name") val ownerName: String? = null,
    @Json(name = "is_shared") val isShared: Boolean = false,
    @Json(name = "filters") val filters: Map<String, Any?> = emptyMap(),
    @Json(name = "created_at") val createdAt: Instant,
    @Json(name = "updated_at") val updatedAt: Instant? = null,
)
