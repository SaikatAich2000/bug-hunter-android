package com.bughunter.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class EmailChangeRequestIn(
    @Json(name = "new_email") val newEmail: String,
    @Json(name = "current_password") val currentPassword: String,
)
