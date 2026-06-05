package com.bughunter.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class SavedViewIn(
    @Json(name = "name") val name: String,
    @Json(name = "filters") val filters: Map<String, Any?> = emptyMap(),
    @Json(name = "is_shared") val isShared: Boolean = false,
)
