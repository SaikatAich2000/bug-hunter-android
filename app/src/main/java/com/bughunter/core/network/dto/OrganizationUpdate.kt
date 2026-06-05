package com.bughunter.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class OrganizationUpdate(
    @Json(name = "name") val name: String? = null,
    @Json(name = "description") val description: String? = null,
)
