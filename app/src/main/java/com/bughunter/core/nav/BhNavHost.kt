package com.bughunter.core.nav

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.bughunter.BuildConfig
import com.bughunter.feature.analytics.AnalyticsScreen
import com.bughunter.feature.audit.AuditScreen
import com.bughunter.feature.auth.acceptInvite.AcceptInviteScreen
import com.bughunter.feature.auth.login.LoginScreen
import com.bughunter.feature.auth.reset.ForgotPasswordScreen
import com.bughunter.feature.auth.reset.ResetPasswordScreen
import com.bughunter.feature.auth.signup.SignupScreen
import com.bughunter.feature.auth.totp.LoginTotpScreen
import com.bughunter.feature.bugs.create.BugCreateScreen
import com.bughunter.feature.bugs.detail.BugDetailScreen
import com.bughunter.feature.bugs.list.BugListScreen
import com.bughunter.feature.dashboard.DashboardScreen
import com.bughunter.feature.dsar.DsarExportScreen
import com.bughunter.feature.events.detail.EventDetailScreen
import com.bughunter.feature.events.list.EventsListScreen
import com.bughunter.feature.organizations.branding.BrandingScreen
import com.bughunter.feature.organizations.invitations.InvitationsScreen
import com.bughunter.feature.organizations.members.MembersScreen
import com.bughunter.feature.organizations.settings.OrganizationScreen
import com.bughunter.feature.projects.detail.ProjectDetailScreen
import com.bughunter.feature.projects.list.ProjectsScreen
import com.bughunter.feature.sessions.SessionsScreen
import com.bughunter.feature.settings.SettingsScreen
import com.bughunter.feature.settings.changeEmail.ChangeEmailScreen
import com.bughunter.feature.settings.changePassword.ChangePasswordScreen
import com.bughunter.feature.settings.profile.ProfileScreen
import com.bughunter.feature.settings.twoFactor.TwoFactorScreen
import com.bughunter.feature.webhooks.WebhooksScreen

@Composable
internal fun BhNavHost(
    navController: NavHostController,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Screen transitions: forward navigation slides the new screen in
    // from the right over a fade; back navigation mirrors it. Durations
    // stay short (220ms) so navigation feels snappy, not showy.
    NavHost(
        navController = navController,
        startDestination = BhRoute.Splash.path,
        modifier = modifier,
        enterTransition = {
            fadeIn(animationSpec = tween(220)) +
                slideInHorizontally(animationSpec = tween(220), initialOffsetX = { it / 8 })
        },
        exitTransition = {
            fadeOut(animationSpec = tween(180))
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(220))
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(180)) +
                slideOutHorizontally(animationSpec = tween(220), targetOffsetX = { it / 8 })
        },
    ) {
        composable(BhRoute.Splash.path) { BhSplashScreen() }

        composable(BhRoute.Login.path) {
            LoginScreen(
                onNavigateForgotPassword = { navController.navigate(BhRoute.ForgotPassword.path) },
                onNavigateSignup = { navController.navigate(BhRoute.Signup.path) },
            )
        }

        composable(BhRoute.LoginTotp.path) {
            LoginTotpScreen(
                onBackToLogin = {
                    navController.navigate(BhRoute.Login.path) {
                        popUpTo(BhRoute.Login.path) { inclusive = true }
                    }
                },
            )
        }

        composable(BhRoute.ForgotPassword.path) {
            ForgotPasswordScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = BhRoute.ResetPassword.PATTERN,
            arguments = listOf(navArgument(BhRoute.ResetPassword.ARG_TOKEN) { type = NavType.StringType }),
            deepLinks = listOf(navDeepLink { uriPattern = "https://bh.example.com/reset/{token}" }),
        ) { entry ->
            val token = entry.arguments?.getString(BhRoute.ResetPassword.ARG_TOKEN).orEmpty()
            ResetPasswordScreen(
                token = token,
                onBack = { navController.popBackStack() },
                onSuccess = {
                    navController.navigate(BhRoute.Login.path) {
                        popUpTo(BhRoute.Login.path) { inclusive = true }
                    }
                },
            )
        }

        composable(BhRoute.Signup.path) {
            SignupScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = BhRoute.AcceptInvite.PATTERN,
            arguments = listOf(navArgument(BhRoute.AcceptInvite.ARG_TOKEN) { type = NavType.StringType }),
            deepLinks = listOf(navDeepLink { uriPattern = "https://bh.example.com/invite/{token}" }),
        ) { entry ->
            val token = entry.arguments?.getString(BhRoute.AcceptInvite.ARG_TOKEN).orEmpty()
            AcceptInviteScreen(
                token = token,
                onBackToLogin = {
                    navController.navigate(BhRoute.Login.path) {
                        popUpTo(BhRoute.Login.path) { inclusive = true }
                    }
                },
            )
        }

        composable(BhRoute.Dashboard.path) {
            DashboardScreen(
                onBugClick = { id -> navController.navigate("home/bugs/$id") },
            )
        }

        composable(BhRoute.Bugs.path) {
            BugListScreen(
                onOpenBug = { id -> navController.navigate("home/bugs/$id") },
                onCreateBug = { navController.navigate(BhRoute.BugCreate.path) },
            )
        }

        composable(BhRoute.BugCreate.path) {
            BugCreateScreen(
                onBack = { navController.popBackStack() },
                onCreated = { id ->
                    navController.navigate("home/bugs/$id") {
                        popUpTo(BhRoute.Bugs.path)
                    }
                },
            )
        }

        composable(
            route = BhRoute.BugDetail.PATTERN,
            arguments = listOf(navArgument("bugId") { type = NavType.IntType }),
            deepLinks = listOf(
                navDeepLink { uriPattern = "https://bh.example.com/bugs/{bugId}" },
                navDeepLink { uriPattern = "app://bughunter/bug/{bugId}" },
            ),
        ) {
            BugDetailScreen(
                onBack = { navController.popBackStack() },
                onEdit = { /* edit-in-place; no separate edit route in v2.8.0 */ },
            )
        }

        composable(BhRoute.Events.path) {
            EventsListScreen(
                onEventClick = { id -> navController.navigate("home/events/$id") },
            )
        }

        composable(
            route = BhRoute.EventDetail.PATTERN,
            arguments = listOf(navArgument("eventId") { type = NavType.IntType }),
            deepLinks = listOf(
                navDeepLink { uriPattern = "https://bh.example.com/events/{eventId}" },
                navDeepLink { uriPattern = "app://bughunter/event/{eventId}" },
            ),
        ) {
            EventDetailScreen(
                onBack = { navController.popBackStack() },
                onBugClick = { id -> navController.navigate("home/bugs/$id") },
            )
        }

        composable(BhRoute.Analytics.path) { AnalyticsScreen() }
        composable(BhRoute.Audit.path) { AuditScreen() }
        composable(BhRoute.Sessions.path) { SessionsScreen() }

        composable(BhRoute.Invitations.path) {
            InvitationsScreen(onBack = { navController.popBackStack() })
        }

        composable(BhRoute.Projects.path) {
            ProjectsScreen(
                onProjectClick = { id -> navController.navigate("home/projects/$id") },
            )
        }

        composable(
            route = BhRoute.ProjectDetail.PATTERN,
            arguments = listOf(navArgument("projectId") { type = NavType.IntType }),
        ) { entry ->
            val projectId = entry.arguments?.getInt("projectId") ?: 0
            ProjectDetailScreen(
                projectId = projectId,
                onBack = { navController.popBackStack() },
                onOpenSettings = { /* project-settings sub-screen reached via in-screen pager in v2.8.0 */ },
                onOpenMembers = { navController.navigate(BhRoute.Members.path) },
            )
        }

        composable(BhRoute.Organization.path) {
            OrganizationScreen(onBack = { navController.popBackStack() })
        }
        composable(BhRoute.Branding.path) {
            BrandingScreen(onBack = { navController.popBackStack() })
        }
        composable(BhRoute.Members.path) {
            MembersScreen(
                onInviteClicked = { navController.navigate(BhRoute.Invitations.path) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(BhRoute.Webhooks.path) {
            WebhooksScreen(onBack = { navController.popBackStack() })
        }

        composable(BhRoute.Profile.path) {
            ProfileScreen(
                onChangePassword = { navController.navigate(BhRoute.ChangePassword.path) },
                onChangeEmail = { navController.navigate(BhRoute.ChangeEmail.path) },
                onTwoFactor = { navController.navigate(BhRoute.TwoFactor.path) },
                onMySessions = { navController.navigate(BhRoute.MySessions.path) },
            )
        }
        composable(BhRoute.ChangePassword.path) {
            ChangePasswordScreen(onDone = { navController.popBackStack() })
        }
        composable(BhRoute.ChangeEmail.path) {
            ChangeEmailScreen(onDone = { navController.popBackStack() })
        }
        composable(BhRoute.TwoFactor.path) { TwoFactorScreen() }
        composable(BhRoute.MySessions.path) {
            // Dedicated my-sessions screen is folded into SessionsScreen for v2.8.0.
            // It already exposes the current user's session row with a "this device" badge.
            SessionsScreen()
        }
        composable(BhRoute.DsarExport.path) {
            DsarExportScreen(onAccountDeleted = onLogout)
        }
        composable(BhRoute.Settings.path) {
            SettingsScreen(
                isDebug = BuildConfig.DEBUG,
                onProfile = { navController.navigate(BhRoute.Profile.path) },
                onChangePassword = { navController.navigate(BhRoute.ChangePassword.path) },
                onTwoFactor = { navController.navigate(BhRoute.TwoFactor.path) },
                onMySessions = { navController.navigate(BhRoute.MySessions.path) },
                onLogout = onLogout,
            )
        }

        composable(BhRoute.Chatbot.path) {
            // Sleuth is hosted as an overlay by BhAppShell on every authenticated route;
            // this composable exists so a deep link still resolves cleanly.
            BhPlaceholderScreen("Sleuth (open via the floating button)")
        }
    }
}

@Composable
private fun BhSplashScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun BhPlaceholderScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
    }
}
