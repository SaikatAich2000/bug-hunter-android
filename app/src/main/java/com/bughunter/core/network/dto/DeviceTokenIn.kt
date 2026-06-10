package com.bughunter.core.network.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class DeviceTokenIn(
    val token: String,
    val platform: String = "android",
)

@JsonClass(generateAdapter = true)
internal data class NotificationPreferencesOut(
    val mentions: Boolean,
    val assignments: Boolean,
    val activity: Boolean,
)

@JsonClass(generateAdapter = true)
internal data class NotificationPreferencesIn(
    val mentions: Boolean? = null,
    val assignments: Boolean? = null,
    val activity: Boolean? = null,
)
