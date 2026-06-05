package com.bughunter.feature.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.bughunter.core.ui.theme.BugHunterTheme
import com.bughunter.feature.auth.totp.LoginTotpTestHarness
import com.bughunter.feature.auth.totp.LoginTotpUiState
import org.junit.Rule
import org.junit.Test

internal class LoginTotpScreenTest {

    @get:Rule val composeRule = createComposeRule()

    @Test
    fun showsTitle() {
        composeRule.setContent {
            BugHunterTheme {
                LoginTotpTestHarness(state = LoginTotpUiState(email = "user@example.com"))
            }
        }
        composeRule.onNodeWithText("Two-factor verification").assertIsDisplayed()
    }

    @Test
    fun showsEmailHint() {
        composeRule.setContent {
            BugHunterTheme {
                LoginTotpTestHarness(state = LoginTotpUiState(email = "user@example.com"))
            }
        }
        composeRule.onNodeWithText("user@example.com", substring = true).assertIsDisplayed()
    }

    @Test
    fun showsBackLink() {
        composeRule.setContent {
            BugHunterTheme {
                LoginTotpTestHarness(state = LoginTotpUiState())
            }
        }
        composeRule.onNodeWithText("Use a different account").assertIsDisplayed()
    }
}
