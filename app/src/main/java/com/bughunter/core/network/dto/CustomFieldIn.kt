package com.bughunter.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class CustomFieldIn(
    @Json(name = "name") val name: String,
    @Json(name = "field_type") val fieldType: String = "text",
    @Json(name = "options") val options: List<String> = emptyList(),
    @Json(name = "is_required") val isRequired: Boolean = false,
    @Json(name = "position") val position: Int = 0,
)
