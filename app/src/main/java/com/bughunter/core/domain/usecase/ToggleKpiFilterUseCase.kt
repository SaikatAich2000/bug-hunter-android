package com.bughunter.core.domain.usecase

import com.bughunter.core.data.repository.BugListFilters
import javax.inject.Inject

internal class ToggleKpiFilterUseCase @Inject constructor() {

    enum class KpiTile(val statuses: List<String>) {
        TOTAL(emptyList()),
        OPEN(OPEN_STATUSES),
        RESOLVED(listOf("Resolved")),
        CLOSED(listOf("Closed")),
        RESOLVE_LATER(listOf("Resolve Later")),
    }

    operator fun invoke(current: BugListFilters, tile: KpiTile): BugListFilters {
        val target = tile.statuses
        return if (target.isEmpty()) {
            current.copy(statuses = emptyList(), page = 1)
        } else if (current.statuses.toSet() == target.toSet()) {
            current.copy(statuses = emptyList(), page = 1)
        } else {
            current.copy(statuses = target, page = 1)
        }
    }

    companion object {
        // Anything that is not Resolved/Closed/Resolve Later/Not a Bug counts as Open for the strip.
        internal val OPEN_STATUSES: List<String> = listOf(
            "New", "In Progress", "Reopened", "In Review", "Approved", "Implemented",
            "Done", "Blocked",
        )
    }
}
