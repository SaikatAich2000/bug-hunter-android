package com.bughunter.feature.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.bughunter.core.network.dto.InvitationPreview
import com.bughunter.core.network.dto.Role
import com.bughunter.core.ui.theme.BugHunterTheme
import com.bughunter.feature.auth.acceptInvite.AcceptInviteTestHarness
import com.bughunter.feature.auth.acceptInvite.AcceptInviteUiState
import org.junit.Rule
import org.junit.Test
import java.time.Instant

internal class AcceptInviteScreenTest {

    @get:Rule val composeRule = createComposeRule()

    private val preview = InvitationPreview(
        email = "alice@example.com",
        organizationName = "Acme",
        role = Role.MEMBER,
        expiresAt = Instant.parse("2099-01-01T00:00:00Z"),
        invitedByName = "Bob",
    )

    @Test
    fun showsLoadingState() {
        composeRule.setContent {
            BugHunterTheme {
                AcceptInviteTestHarness(
                    state = AcceptInviteUiState(token = "t", isLoadingPreview = true),
                )
            }
        }
        composeRule.onNodeWithText("Join your team").assertIsDisplayed()
    }

    @Test
    fun showsPreviewBanner() {
        composeRule.setContent {
            BugHunterTheme {
                AcceptInviteTestHarness(
                    state = AcceptInviteUiState(
                        token = "t",
                        isLoadingPreview = false,
                        preview = preview,
                    ),
                )
            }
        }
        composeRule.onNodeWithText("Acme", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Bob", substring = true).assertIsDisplayed()
    }

    @Test
    fun showsErrorBanner() {
        composeRule.setContent {
            BugHunterTheme {
                AcceptInviteTestHarness(
                    state = AcceptInviteUiState(
                        token = "t",
                        isLoadingPreview = false,
                        previewError = com.bughunter.core.network.DomainError.NotFound,
                    ),
                )
            }
        }
        composeRule.onNodeWithText("Go to sign in").assertIsDisplayed()
    }
}
