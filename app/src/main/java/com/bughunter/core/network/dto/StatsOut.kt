package com.bughunter.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class StatsOut(
    @Json(name = "bugs") val bugs: Int,
    @Json(name = "open") val open: Int,
    @Json(name = "resolved") val resolved: Int,
    @Json(name = "closed") val closed: Int,
    @Json(name = "resolve_later") val resolveLater: Int,
    @Json(name = "projects") val projects: Int = 0,
    @Json(name = "users") val users: Int = 0,
    @Json(name = "by_status") val byStatus: Map<String, Int> = emptyMap(),
    @Json(name = "by_priority") val byPriority: Map<String, Int> = emptyMap(),
    @Json(name = "by_environment") val byEnvironment: Map<String, Int> = emptyMap(),
    @Json(name = "by_type") val byType: Map<String, Int> = emptyMap(),
    @Json(name = "by_project") val byProject: List<Map<String, Any?>> = emptyList(),
    @Json(name = "by_assignee") val byAssignee: List<Map<String, Any?>> = emptyList(),
    @Json(name = "timeline") val timeline: List<Map<String, Any?>> = emptyList(),
)
