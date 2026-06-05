package com.bughunter.feature.dashboard

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.bughunter.core.network.DomainError
import com.bughunter.core.network.dto.BugOut
import com.bughunter.core.ui.theme.BugHunterTheme
import com.bughunter.core.ui.util.UiState
import org.junit.Rule
import org.junit.Test
import java.time.Instant

internal class DashboardScreenTest {

    @get:Rule val composeRule = createComposeRule()

    @Test
    fun showsKpiLabelsOnSuccess() {
        composeRule.setContent {
            BugHunterTheme {
                DashboardScreenTestHarness(state = UiState.Success(MODEL))
            }
        }
        composeRule.onAllNodesWithText("Total")[0].assertIsDisplayed()
        composeRule.onAllNodesWithText("Open")[0].assertIsDisplayed()
    }

    @Test
    fun showsErrorCopyOnNetworkError() {
        composeRule.setContent {
            BugHunterTheme {
                DashboardScreenTestHarness(state = UiState.Error(DomainError.Network))
            }
        }
        composeRule.onNodeWithText("Couldn't load the dashboard").assertIsDisplayed()
    }

    @Test
    fun showsRecentBugRows() {
        composeRule.setContent {
            BugHunterTheme {
                DashboardScreenTestHarness(state = UiState.Success(MODEL))
            }
        }
        composeRule.onNodeWithText("Sample bug 1").assertIsDisplayed()
    }

    private companion object {
        private val STATS: StatsView = StatsView(
            totals = StatsView.Totals(total = 10, open = 4, resolved = 5, closed = 1, resolveLater = 0),
            byStatus = emptyList(),
            byPriority = emptyList(),
            byEnvironment = emptyList(),
            byType = emptyMap(),
            byProject = emptyList(),
            byAssignee = emptyList(),
            timeline = emptyList(),
            projectsCount = 1,
            usersCount = 2,
        )

        private val BUG_SAMPLE: BugOut = BugOut(
            id = 1,
            projectId = 1,
            projectName = "Apollo",
            projectKey = "APO",
            itemType = "Bug",
            eventId = null,
            eventName = null,
            title = "Sample bug 1",
            description = "",
            reporter = null,
            assignees = emptyList(),
            status = "New",
            priority = "Medium",
            environment = "DEV",
            dueDate = null,
            createdAt = Instant.parse("2026-06-01T10:00:00Z"),
            updatedAt = Instant.parse("2026-06-01T10:00:00Z"),
            attachmentCount = 0,
            canEdit = true,
        )

        private val MODEL: DashboardScreenModel = DashboardScreenModel(
            stats = STATS,
            recentBugs = listOf(BUG_SAMPLE),
            activeTile = null,
            tab = DashboardTypeTab.ALL,
        )
    }
}
