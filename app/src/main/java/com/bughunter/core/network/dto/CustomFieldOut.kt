package com.bughunter.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class CustomFieldOut(
    @Json(name = "id") val id: Int,
    @Json(name = "project_id") val projectId: Int,
    @Json(name = "name") val name: String,
    @Json(name = "field_type") val fieldType: String,
    @Json(name = "options") val options: List<String> = emptyList(),
    @Json(name = "is_required") val isRequired: Boolean,
    @Json(name = "position") val position: Int,
)
