package com.bughunter.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class UserUpdate(
    @Json(name = "name") val name: String? = null,
    @Json(name = "email") val email: String? = null,
    @Json(name = "role") val role: Role? = null,
    @Json(name = "is_active") val isActive: Boolean? = null,
    @Json(name = "password") val password: String? = null,
)
