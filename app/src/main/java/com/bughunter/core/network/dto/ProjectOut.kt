package com.bughunter.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.Instant

@JsonClass(generateAdapter = true)
internal data class ProjectOut(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "key") val key: String = "",
    @Json(name = "description") val description: String = "",
    @Json(name = "color") val color: String = "#c9764f",
    @Json(name = "created_at") val createdAt: Instant,
    @Json(name = "updated_at") val updatedAt: Instant,
    @Json(name = "can_manage") val canManage: Boolean = false,
    @Json(name = "member_count") val memberCount: Int = 0,
)
