package com.bughunter.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class LoginTotpStepIn(
    @Json(name = "pending_token") val pendingToken: String,
    @Json(name = "code") val code: String,
)
