package com.bughunter.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class HealthOut(
    @Json(name = "status") val status: String,
    @Json(name = "version") val version: String,
    @Json(name = "asset_version") val assetVersion: String,
)
