package com.bughunter.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.Instant

@JsonClass(generateAdapter = true)
internal data class AttachmentBrief(
    @Json(name = "id") val id: Int,
    @Json(name = "filename") val filename: String,
    @Json(name = "content_type") val contentType: String,
    @Json(name = "size_bytes") val sizeBytes: Long,
    @Json(name = "uploader_user_id") val uploaderUserId: Int? = null,
    @Json(name = "uploader_name") val uploaderName: String,
    @Json(name = "comment_id") val commentId: Int? = null,
    @Json(name = "created_at") val createdAt: Instant,
)
