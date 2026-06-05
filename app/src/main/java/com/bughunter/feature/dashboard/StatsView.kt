package com.bughunter.feature.dashboard

import com.bughunter.core.network.dto.StatsOut
import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal data class ProjectStatRow(
    val projectId: Int?,
    val name: String,
    val key: String?,
    val color: String?,
    val count: Int,
)

internal data class AssigneeStatRow(
    val userId: Int?,
    val name: String,
    val email: String?,
    val count: Int,
)

internal data class TimelinePoint(
    val date: LocalDate?,
    val label: String,
    val count: Int,
)

internal data class StatsView(
    val totals: Totals,
    val byStatus: List<Pair<String, Int>>,
    val byPriority: List<Pair<String, Int>>,
    val byEnvironment: List<Pair<String, Int>>,
    val byType: Map<String, Int>,
    val byProject: List<ProjectStatRow>,
    val byAssignee: List<AssigneeStatRow>,
    val timeline: List<TimelinePoint>,
    val projectsCount: Int,
    val usersCount: Int,
) {
    internal data class Totals(
        val total: Int,
        val open: Int,
        val resolved: Int,
        val closed: Int,
        val resolveLater: Int,
    )

    companion object {
        fun from(stats: StatsOut): StatsView {
            val totals = Totals(
                total = stats.bugs,
                open = stats.open,
                resolved = stats.resolved,
                closed = stats.closed,
                resolveLater = stats.resolveLater,
            )
            return StatsView(
                totals = totals,
                byStatus = stats.byStatus.entries
                    .sortedByDescending { it.value }
                    .map { it.key to it.value },
                byPriority = PRIORITY_ORDER.mapNotNull { name ->
                    stats.byPriority[name]?.let { name to it }
                },
                byEnvironment = ENV_ORDER.mapNotNull { name ->
                    stats.byEnvironment[name]?.let { name to it }
                },
                byType = stats.byType,
                byProject = stats.byProject.map(::toProjectRow)
                    .sortedByDescending { it.count }
                    .take(MAX_PROJECT_ROWS),
                byAssignee = stats.byAssignee.map(::toAssigneeRow)
                    .sortedByDescending { it.count }
                    .take(MAX_ASSIGNEE_ROWS),
                timeline = stats.timeline.map(::toTimelinePoint).takeLast(TIMELINE_DAYS),
                projectsCount = stats.projects,
                usersCount = stats.users,
            )
        }

        private fun toProjectRow(raw: Map<String, Any?>): ProjectStatRow = ProjectStatRow(
            projectId = (raw["project_id"] ?: raw["id"]) as? Int,
            name = (raw["project_name"] ?: raw["name"] ?: "Unassigned").toString(),
            key = (raw["project_key"] ?: raw["key"]) as? String,
            color = raw["color"] as? String,
            count = (raw["count"] as? Number)?.toInt() ?: 0,
        )

        private fun toAssigneeRow(raw: Map<String, Any?>): AssigneeStatRow = AssigneeStatRow(
            userId = (raw["user_id"] ?: raw["id"]) as? Int,
            name = (raw["name"] ?: raw["display_name"] ?: raw["email"] ?: "Unassigned").toString(),
            email = raw["email"] as? String,
            count = (raw["count"] as? Number)?.toInt() ?: 0,
        )

        private fun toTimelinePoint(raw: Map<String, Any?>): TimelinePoint {
            val rawDate = (raw["date"] ?: raw["day"])?.toString()
            val parsed = rawDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            return TimelinePoint(
                date = parsed,
                label = parsed?.format(SHORT_DAY) ?: rawDate.orEmpty(),
                count = (raw["count"] as? Number)?.toInt() ?: 0,
            )
        }

        private val SHORT_DAY: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")
        private val PRIORITY_ORDER: List<String> = listOf("Low", "Medium", "High", "Critical")
        private val ENV_ORDER: List<String> = listOf("DEV", "UAT", "PROD")
        private const val MAX_PROJECT_ROWS: Int = 8
        private const val MAX_ASSIGNEE_ROWS: Int = 8
        private const val TIMELINE_DAYS: Int = 14
    }
}
