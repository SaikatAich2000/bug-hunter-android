package com.bughunter.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.Instant

@JsonClass(generateAdapter = true)
internal data class InvitationPreview(
    @Json(name = "email") val email: String,
    @Json(name = "organization_name") val organizationName: String,
    @Json(name = "role") val role: Role,
    @Json(name = "expires_at") val expiresAt: Instant,
    @Json(name = "invited_by_name") val invitedByName: String,
)
