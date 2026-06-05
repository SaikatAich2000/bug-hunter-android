package com.bughunter.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class UserIn(
    @Json(name = "name") val name: String,
    @Json(name = "email") val email: String,
    @Json(name = "role") val role: Role = Role.MEMBER,
    @Json(name = "password") val password: String,
    @Json(name = "is_active") val isActive: Boolean = true,
)
