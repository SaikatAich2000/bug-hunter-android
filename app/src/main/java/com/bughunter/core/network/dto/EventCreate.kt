package com.bughunter.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class EventCreate(
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String = "",
    @Json(name = "scheduled_for") val scheduledFor: String? = null,
    @Json(name = "manager_ids") val managerIds: List<Int> = emptyList(),
)
