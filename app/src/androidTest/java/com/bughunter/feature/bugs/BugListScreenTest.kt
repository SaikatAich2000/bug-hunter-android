package com.bughunter.feature.bugs

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.bughunter.core.network.dto.BugOut
import com.bughunter.core.ui.theme.BugHunterTheme
import com.bughunter.core.ui.util.UiState
import com.bughunter.feature.bugs.list.BugListContent
import com.bughunter.feature.bugs.list.fakeBugList
import org.junit.Rule
import org.junit.Test
import java.time.Instant

internal class BugListScreenTest {

    @get:Rule val composeRule = createComposeRule()

    @Test
    fun rendersBugTitle() {
        composeRule.setContent {
            BugHunterTheme {
                BugListContent(
                    state = UiState.Success(
                        fakeBugList(
                            items = listOf(sampleBug),
                        ),
                    ),
                    onOpenBug = {},
                    onCreateBug = {},
                    onQueryChange = {},
                    onToggleProject = {},
                    onToggleStatus = {},
                    onTogglePriority = {},
                    onToggleEnvironment = {},
                    onToggleItemType = {},
                    onToggleAssignee = {},
                    onClearFilters = {},
                    onLoadMore = {},
                    onRetry = {},
                )
            }
        }
        composeRule.onNodeWithText("Login page broken").assertIsDisplayed()
    }

    @Test
    fun emptyStateRenders() {
        composeRule.setContent {
            BugHunterTheme {
                BugListContent(
                    state = UiState.Success(fakeBugList(items = emptyList())),
                    onOpenBug = {},
                    onCreateBug = {},
                    onQueryChange = {},
                    onToggleProject = {},
                    onToggleStatus = {},
                    onTogglePriority = {},
                    onToggleEnvironment = {},
                    onToggleItemType = {},
                    onToggleAssignee = {},
                    onClearFilters = {},
                    onLoadMore = {},
                    onRetry = {},
                )
            }
        }
        composeRule.onNodeWithText("No items match your filters").assertIsDisplayed()
    }

    private val sampleBug = BugOut(
        id = 42,
        projectId = 1,
        projectName = "Apollo",
        projectKey = "APO",
        itemType = "Bug",
        title = "Login page broken",
        status = "New",
        priority = "High",
        environment = "PROD",
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2026-01-02T00:00:00Z"),
    )
}
