package com.bughunter.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.Instant

@JsonClass(generateAdapter = true)
internal data class CommentOut(
    @Json(name = "id") val id: Int,
    @Json(name = "bug_id") val bugId: Int,
    @Json(name = "author_user_id") val authorUserId: Int? = null,
    @Json(name = "author_name") val authorName: String,
    @Json(name = "body") val body: String,
    @Json(name = "created_at") val createdAt: Instant,
    @Json(name = "attachments") val attachments: List<AttachmentBrief> = emptyList(),
)
