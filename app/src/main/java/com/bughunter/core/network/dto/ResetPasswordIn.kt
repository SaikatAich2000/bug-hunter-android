package com.bughunter.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class ResetPasswordIn(
    @Json(name = "token") val token: String,
    @Json(name = "new_password") val newPassword: String,
)
