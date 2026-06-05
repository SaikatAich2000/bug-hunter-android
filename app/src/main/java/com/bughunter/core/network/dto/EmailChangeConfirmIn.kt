package com.bughunter.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class EmailChangeConfirmIn(
    @Json(name = "code") val code: String,
)
