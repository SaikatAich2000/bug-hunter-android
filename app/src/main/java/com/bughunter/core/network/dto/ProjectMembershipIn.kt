package com.bughunter.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class ProjectMembershipIn(
    @Json(name = "user_id") val userId: Int,
    @Json(name = "role") val role: ProjectRole = ProjectRole.MEMBER,
)
