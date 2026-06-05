package com.bughunter.core.nav

internal sealed interface BhRoute {
    val path: String

    data object Splash : BhRoute { override val path: String = "splash" }
    data object Login : BhRoute { override val path: String = "login" }
    data object LoginTotp : BhRoute { override val path: String = "login/totp" }
    data object ForgotPassword : BhRoute { override val path: String = "forgot" }

    data class ResetPassword(val token: String) : BhRoute {
        override val path: String = "reset/$token"
        companion object {
            const val PATTERN: String = "reset/{token}"
            const val ARG_TOKEN: String = "token"
        }
    }

    data object Signup : BhRoute { override val path: String = "signup" }

    data class AcceptInvite(val token: String) : BhRoute {
        override val path: String = "invite/$token"
        companion object {
            const val PATTERN: String = "invite/{token}"
            const val ARG_TOKEN: String = "token"
        }
    }

    data object Dashboard : BhRoute { override val path: String = "home/dashboard" }
    data object Bugs : BhRoute { override val path: String = "home/bugs" }

    data class BugDetail(val id: Int) : BhRoute {
        override val path: String = "home/bugs/$id"
        companion object {
            const val PATTERN: String = "home/bugs/{bugId}"
            const val ARG_ID: String = "bugId"
        }
    }

    data object BugCreate : BhRoute { override val path: String = "home/bugs/new" }
    data object Events : BhRoute { override val path: String = "home/events" }

    data class EventDetail(val id: Int) : BhRoute {
        override val path: String = "home/events/$id"
        companion object {
            const val PATTERN: String = "home/events/{eventId}"
            const val ARG_ID: String = "eventId"
        }
    }

    data object Analytics : BhRoute { override val path: String = "home/analytics" }
    data object Audit : BhRoute { override val path: String = "home/audit" }
    data object Sessions : BhRoute { override val path: String = "home/sessions" }
    data object Invitations : BhRoute { override val path: String = "home/invitations" }

    data object Projects : BhRoute { override val path: String = "home/projects" }

    data class ProjectDetail(val id: Int) : BhRoute {
        override val path: String = "home/projects/$id"
        companion object {
            const val PATTERN: String = "home/projects/{projectId}"
            const val ARG_ID: String = "projectId"
        }
    }

    data object Organization : BhRoute { override val path: String = "home/org" }
    data object Branding : BhRoute { override val path: String = "home/org/branding" }
    data object Members : BhRoute { override val path: String = "home/org/members" }
    data object Webhooks : BhRoute { override val path: String = "home/org/webhooks" }

    data object Profile : BhRoute { override val path: String = "home/me/profile" }
    data object ChangePassword : BhRoute { override val path: String = "home/me/password" }
    data object ChangeEmail : BhRoute { override val path: String = "home/me/email" }
    data object TwoFactor : BhRoute { override val path: String = "home/me/2fa" }
    data object MySessions : BhRoute { override val path: String = "home/me/sessions" }
    data object DsarExport : BhRoute { override val path: String = "home/me/data" }
    data object Settings : BhRoute { override val path: String = "home/me/settings" }

    data object Chatbot : BhRoute { override val path: String = "home/chat" }
}

internal object BhRoutes {
    // Splash is intentionally NOT in this set. The auth gate uses this set to
    // detect when the user is already on a "no-redirect" screen; treating
    // Splash as an auth route stranded users on it after bootstrap completed.
    val authRoutes: Set<String> = setOf(
        BhRoute.Login.path,
        BhRoute.LoginTotp.path,
        BhRoute.ForgotPassword.path,
        BhRoute.Signup.path,
        BhRoute.ResetPassword.PATTERN,
        BhRoute.AcceptInvite.PATTERN,
    )

    fun isAuthRoute(routePath: String?): Boolean {
        if (routePath == null) return false
        return authRoutes.any { authPath ->
            routePath == authPath || routePath.startsWith("reset/") || routePath.startsWith("invite/")
        }
    }
}
