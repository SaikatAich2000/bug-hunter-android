package com.bughunter.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class ProjectIn(
    @Json(name = "name") val name: String,
    @Json(name = "key") val key: String? = null,
    @Json(name = "description") val description: String = "",
    @Json(name = "color") val color: String = "#c9764f",
)
