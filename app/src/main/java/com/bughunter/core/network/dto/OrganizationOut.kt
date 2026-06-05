package com.bughunter.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.Instant

@JsonClass(generateAdapter = true)
internal data class OrganizationOut(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "slug") val slug: String,
    @Json(name = "description") val description: String = "",
    @Json(name = "created_at") val createdAt: Instant,
)
