package com.bughunter.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class LoginIn(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String,
)
