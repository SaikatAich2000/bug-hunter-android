package com.bughunter.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class EventUpdate(
    @Json(name = "name") val name: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "scheduled_for") val scheduledFor: String? = null,
    @Json(name = "manager_ids") val managerIds: List<Int>? = null,
)
