package com.bughunter.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.Instant

@JsonClass(generateAdapter = true)
internal data class InvitationOut(
    @Json(name = "id") val id: Int,
    @Json(name = "org_id") val orgId: Int,
    @Json(name = "email") val email: String,
    @Json(name = "role") val role: Role,
    @Json(name = "invited_by_user_id") val invitedByUserId: Int? = null,
    @Json(name = "invited_by_name") val invitedByName: String,
    @Json(name = "initial_project_ids") val initialProjectIds: String = "",
    @Json(name = "expires_at") val expiresAt: Instant,
    @Json(name = "accepted_at") val acceptedAt: Instant? = null,
    @Json(name = "revoked_at") val revokedAt: Instant? = null,
    @Json(name = "created_at") val createdAt: Instant,
)
