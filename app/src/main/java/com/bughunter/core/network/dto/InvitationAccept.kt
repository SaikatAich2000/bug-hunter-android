package com.bughunter.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class InvitationAccept(
    @Json(name = "token") val token: String,
    @Json(name = "name") val name: String,
    @Json(name = "password") val password: String,
)
