package com.bughunter.feature.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.bughunter.core.network.DomainError
import com.bughunter.core.ui.theme.BugHunterTheme
import com.bughunter.feature.auth.login.LoginScreenTestHarness
import com.bughunter.feature.auth.login.LoginUiState
import org.junit.Rule
import org.junit.Test

internal class LoginScreenTest {

    @get:Rule val composeRule = createComposeRule()

    @Test
    fun showsSignInTitle() {
        composeRule.setContent {
            BugHunterTheme {
                LoginScreenTestHarness(state = LoginUiState())
            }
        }
        // Title and CTA both contain "Sign in"; expecting at least one match.
        composeRule.onAllNodesWithText("Sign in", substring = false)[0].assertIsDisplayed()
    }

    @Test
    fun showsForgotPasswordLink() {
        composeRule.setContent {
            BugHunterTheme {
                LoginScreenTestHarness(state = LoginUiState())
            }
        }
        composeRule.onNodeWithText("Forgot password?").assertIsDisplayed()
    }

    @Test
    fun showsErrorWhenUnauthorized() {
        composeRule.setContent {
            BugHunterTheme {
                LoginScreenTestHarness(
                    state = LoginUiState(
                        email = "a@b.co",
                        password = "secret123",
                        error = DomainError.Unauthorized,
                    ),
                )
            }
        }
        composeRule.onNodeWithText("Invalid email or password").assertIsDisplayed()
    }
}
