package com.bughunter.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class BulkDeleteIn(
    @Json(name = "bug_ids") val bugIds: List<Int>,
)
