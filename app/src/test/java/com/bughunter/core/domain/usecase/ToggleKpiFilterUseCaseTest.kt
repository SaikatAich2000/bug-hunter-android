package com.bughunter.core.domain.usecase

import com.bughunter.core.data.repository.BugListFilters
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The dashboard KPI strip is a toggle: tapping a tile applies its status
 * filter; tapping the active tile again clears it; tapping "Total" always
 * clears. Every transition resets paging to page 1.
 */
class ToggleKpiFilterUseCaseTest {

    private val useCase = ToggleKpiFilterUseCase()

    @Test
    fun `tapping a tile applies its statuses and resets the page`() {
        val start = BugListFilters(statuses = emptyList(), page = 4)
        val out = useCase(start, ToggleKpiFilterUseCase.KpiTile.RESOLVED)
        assertThat(out.statuses).containsExactly("Resolved")
        assertThat(out.page).isEqualTo(1)
    }

    @Test
    fun `tapping the active tile again clears the filter`() {
        val active = BugListFilters(statuses = listOf("Closed"), page = 1)
        val out = useCase(active, ToggleKpiFilterUseCase.KpiTile.CLOSED)
        assertThat(out.statuses).isEmpty()
        assertThat(out.page).isEqualTo(1)
    }

    @Test
    fun `Total always clears the status filter`() {
        val active = BugListFilters(statuses = listOf("Resolved"), page = 2)
        val out = useCase(active, ToggleKpiFilterUseCase.KpiTile.TOTAL)
        assertThat(out.statuses).isEmpty()
        assertThat(out.page).isEqualTo(1)
    }

    @Test
    fun `switching from one tile to another replaces the statuses`() {
        val active = BugListFilters(statuses = ToggleKpiFilterUseCase.OPEN_STATUSES, page = 3)
        val out = useCase(active, ToggleKpiFilterUseCase.KpiTile.RESOLVE_LATER)
        assertThat(out.statuses).containsExactly("Resolve Later")
    }

    @Test
    fun `Open tile carries the full open-status set`() {
        val out = useCase(BugListFilters(), ToggleKpiFilterUseCase.KpiTile.OPEN)
        assertThat(out.statuses).isEqualTo(ToggleKpiFilterUseCase.OPEN_STATUSES)
        assertThat(out.statuses).contains("In Progress")
    }
}
