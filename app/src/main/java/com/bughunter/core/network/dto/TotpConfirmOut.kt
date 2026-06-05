package com.bughunter.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class TotpConfirmOut(
    @Json(name = "enabled") val enabled: Boolean,
    @Json(name = "recovery_codes") val recoveryCodes: List<String> = emptyList(),
)
