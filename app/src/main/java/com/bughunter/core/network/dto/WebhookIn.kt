package com.bughunter.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class WebhookIn(
    @Json(name = "url") val url: String,
    @Json(name = "secret") val secret: String? = null,
    @Json(name = "active") val active: Boolean = true,
    @Json(name = "event_types") val eventTypes: List<String> = emptyList(),
)
