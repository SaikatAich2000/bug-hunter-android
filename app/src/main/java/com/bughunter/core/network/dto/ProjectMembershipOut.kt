package com.bughunter.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.Instant

@JsonClass(generateAdapter = true)
internal data class ProjectMembershipOut(
    @Json(name = "id") val id: Int,
    @Json(name = "user_id") val userId: Int,
    @Json(name = "user_name") val userName: String,
    @Json(name = "user_email") val userEmail: String,
    @Json(name = "user_role") val userRole: Role,
    @Json(name = "project_role") val projectRole: ProjectRole,
    @Json(name = "created_at") val createdAt: Instant,
)
