package com.bughunter.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.Instant

@JsonClass(generateAdapter = true)
internal data class WebhookOut(
    @Json(name = "id") val id: Int,
    @Json(name = "url") val url: String,
    @Json(name = "secret_masked") val secretMasked: String = "",
    @Json(name = "active") val active: Boolean = true,
    @Json(name = "event_types") val eventTypes: List<String> = emptyList(),
    @Json(name = "created_at") val createdAt: Instant,
    @Json(name = "updated_at") val updatedAt: Instant? = null,
    @Json(name = "last_delivery_at") val lastDeliveryAt: Instant? = null,
    @Json(name = "last_status_code") val lastStatusCode: Int? = null,
)
