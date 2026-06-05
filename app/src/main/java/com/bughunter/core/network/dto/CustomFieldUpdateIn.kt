package com.bughunter.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class CustomFieldUpdateIn(
    @Json(name = "name") val name: String? = null,
    @Json(name = "field_type") val fieldType: String? = null,
    @Json(name = "options") val options: List<String>? = null,
    @Json(name = "is_required") val isRequired: Boolean? = null,
    @Json(name = "position") val position: Int? = null,
)
