package com.bughunter.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.Instant

@JsonClass(generateAdapter = true)
internal data class TotpStatus(
    @Json(name = "enabled") val enabled: Boolean,
    @Json(name = "enrolled_at") val enrolledAt: Instant? = null,
    @Json(name = "unused_recovery_codes") val unusedRecoveryCodes: Int? = null,
)
