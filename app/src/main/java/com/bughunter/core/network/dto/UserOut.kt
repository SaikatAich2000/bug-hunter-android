package com.bughunter.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.Instant

@JsonClass(generateAdapter = true)
internal data class UserOut(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "email") val email: String,
    @Json(name = "role") val role: Role,
    @Json(name = "is_active") val isActive: Boolean,
    @Json(name = "created_at") val createdAt: Instant,
    @Json(name = "updated_at") val updatedAt: Instant,
)
