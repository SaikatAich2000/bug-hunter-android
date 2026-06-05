package com.bughunter.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class BugListResponse(
    @Json(name = "items") val items: List<BugOut> = emptyList(),
    @Json(name = "page") val page: Int,
    @Json(name = "page_size") val pageSize: Int,
    @Json(name = "total") val total: Int,
    @Json(name = "pages") val pages: Int,
)
