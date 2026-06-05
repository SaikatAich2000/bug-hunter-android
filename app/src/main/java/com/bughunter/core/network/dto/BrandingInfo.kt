package com.bughunter.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class BrandingInfo(
    @Json(name = "logo_data_url") val logoDataUrl: String? = null,
    @Json(name = "accent_color") val accentColor: String? = null,
)
