package com.bughunter.feature.bugs

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.bughunter.core.network.dto.BugDetail
import com.bughunter.core.ui.theme.BugHunterTheme
import com.bughunter.core.ui.util.UiState
import com.bughunter.feature.bugs.detail.BugDetailContent
import com.bughunter.feature.bugs.detail.BugDetailScreenModel
import org.junit.Rule
import org.junit.Test
import java.time.Instant

internal class BugDetailScreenTest {

    @get:Rule val composeRule = createComposeRule()

    @Test
    fun showsBugTitle() {
        composeRule.setContent {
            BugHunterTheme {
                BugDetailContent(
                    state = UiState.Success(BugDetailScreenModel(bug = sampleBug)),
                    currentUserRole = null,
                    onBack = {},
                    onEdit = {},
                    onCommentDraftChange = {},
                    onPostComment = {},
                    onAttach = {},
                    onDeleteAttachment = {},
                    onDeleteComment = {},
                    onRetry = {},
                )
            }
        }
        composeRule.onNodeWithText("Login page broken").assertIsDisplayed()
    }

    private val sampleBug = BugDetail(
        id = 42,
        projectId = 1,
        projectName = "Apollo",
        projectKey = "APO",
        itemType = "Bug",
        title = "Login page broken",
        description = "<p>Cannot sign in.</p>",
        status = "New",
        priority = "High",
        environment = "PROD",
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2026-01-02T00:00:00Z"),
    )
}
