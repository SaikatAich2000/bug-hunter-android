package com.bughunter.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class MetaOut(
    @Json(name = "statuses") val statuses: List<String> = emptyList(),
    @Json(name = "statuses_by_type") val statusesByType: Map<String, List<String>> = emptyMap(),
    @Json(name = "priorities") val priorities: List<String> = emptyList(),
    @Json(name = "environments") val environments: List<String> = emptyList(),
    @Json(name = "allow_public_signup") val allowPublicSignup: Boolean = false,
)
