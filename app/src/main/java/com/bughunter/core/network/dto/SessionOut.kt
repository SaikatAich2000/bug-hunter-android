package com.bughunter.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.Instant

@JsonClass(generateAdapter = true)
internal data class SessionOut(
    @Json(name = "id") val id: Int,
    @Json(name = "user_id") val userId: Int,
    @Json(name = "user_name") val userName: String? = null,
    @Json(name = "user_email") val userEmail: String? = null,
    @Json(name = "user_role") val userRole: String? = null,
    @Json(name = "ip_address") val ipAddress: String,
    @Json(name = "user_agent") val userAgent: String,
    @Json(name = "created_at") val createdAt: Instant,
    @Json(name = "last_seen_at") val lastSeenAt: Instant,
    @Json(name = "expires_at") val expiresAt: Instant,
    @Json(name = "is_current") val isCurrent: Boolean = false,
)
