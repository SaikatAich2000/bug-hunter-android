package com.bughunter.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class MeOut(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "email") val email: String,
    @Json(name = "role") val role: Role,
    @Json(name = "is_active") val isActive: Boolean,
    @Json(name = "org_id") val orgId: Int,
    @Json(name = "organization_name") val organizationName: String,
    @Json(name = "organization_slug") val organizationSlug: String,
    @Json(name = "totp_enabled") val totpEnabled: Boolean = false,
    @Json(name = "branding") val branding: BrandingInfo? = null,
)
