package com.bughunter.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class TotpBeginOut(
    @Json(name = "secret") val secret: String,
    @Json(name = "otpauth_uri") val otpauthUri: String,
)
