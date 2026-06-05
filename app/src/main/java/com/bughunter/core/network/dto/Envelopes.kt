package com.bughunter.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class ValidationError(
    @Json(name = "loc") val loc: List<String> = emptyList(),
    @Json(name = "msg") val msg: String,
    @Json(name = "type") val type: String,
)

@JsonClass(generateAdapter = true)
internal data class HTTPValidationError(
    @Json(name = "detail") val detail: List<ValidationError> = emptyList(),
)
