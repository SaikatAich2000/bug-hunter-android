package com.bughunter.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class InvitationCreate(
    @Json(name = "email") val email: String,
    @Json(name = "role") val role: Role = Role.MEMBER,
    @Json(name = "project_ids") val projectIds: List<Int> = emptyList(),
    @Json(name = "as_lead") val asLead: Boolean = false,
)
