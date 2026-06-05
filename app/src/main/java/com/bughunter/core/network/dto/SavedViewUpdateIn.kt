package com.bughunter.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class SavedViewUpdateIn(
    @Json(name = "name") val name: String? = null,
    @Json(name = "filters") val filters: Map<String, Any?>? = null,
    @Json(name = "is_shared") val isShared: Boolean? = null,
)
