package com.bughunter.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class ChatIn(
    @Json(name = "question") val question: String,
    @Json(name = "context") val context: Map<String, Any?>? = null,
)
