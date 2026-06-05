package com.bughunter.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class CustomValueOut(
    @Json(name = "field_id") val fieldId: Int,
    @Json(name = "name") val name: String,
    @Json(name = "field_type") val fieldType: String,
    @Json(name = "value") val value: String? = null,
)
