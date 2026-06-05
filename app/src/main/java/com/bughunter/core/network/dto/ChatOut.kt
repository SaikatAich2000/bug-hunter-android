package com.bughunter.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class ChatOut(
    @Json(name = "blocks") val blocks: List<ChatBlock> = emptyList(),
)
