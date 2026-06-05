package com.bughunter.core.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

internal data class BrandTokens(
    val isDark: Boolean,
    val statusColors: Map<String, Color>,
    val priorityColors: Map<String, Color>,
    val envColors: Map<String, Color>,
    val inviteStatusColors: Map<String, Pair<Color, Color>>,
    val accentGradient: Brush,
    val accentGradientHover: Brush,
    val dangerGradient: Brush,
    val kpiSurfaceGradient: Brush,
    val accentSoft: Color,
    val accentGlow: Color,
    val border: Color,
    val borderStrong: Color,
    val borderSoft: Color,
    val surfaceGlass: Color,
    val modalScrim: Color,
    val sidebarScrim: Color,
    val loaderScrim: Color,
    val textMuted: Color,
    val textFaint: Color,
    val scrollbarThumb: Color,
    val readonlyBannerBg: Color,
    val readonlyBannerBorder: Color,
    val sleuthOnline: Color,
) {
    fun statusColor(key: String): Color =
        statusColors[key.lowercase()] ?: statusColors["new"] ?: Color.Unspecified

    fun priorityColor(key: String): Color =
        priorityColors[key.lowercase()] ?: priorityColors["medium"] ?: Color.Unspecified

    fun envColor(key: String): Color =
        envColors[key.lowercase()] ?: envColors["dev"] ?: Color.Unspecified
}

internal val LocalBrandTokens = staticCompositionLocalOf<BrandTokens> {
    error("BrandTokens not provided. Wrap in BugHunterTheme {}.")
}

internal val LocalAccentGradient = staticCompositionLocalOf<Brush> {
    error("LocalAccentGradient not provided. Wrap in BugHunterTheme {}.")
}

internal fun buildBrandTokens(isDark: Boolean): BrandTokens {
    val accentGrad = if (isDark) {
        Brush.linearGradient(
            colorStops = arrayOf(
                0f to BhAccent_6366f1,
                0.5f to BhAccent_818cf8,
                1f to BhAccent2_38bdf8,
            ),
            start = Offset(0f, 0f),
            end = Offset.Infinite,
        )
    } else {
        Brush.linearGradient(
            colorStops = arrayOf(
                0f to BhAccentStrong_4f46e5,
                0.5f to BhAccent_6366f1,
                1f to BhAccent2Light_0284c7,
            ),
            start = Offset(0f, 0f),
            end = Offset.Infinite,
        )
    }
    val accentGradHover = if (isDark) {
        Brush.linearGradient(
            colorStops = arrayOf(
                0f to BhAccentStrong_4f46e5,
                0.5f to BhAccent_6366f1,
                1f to BhAccent2Strong_0ea5e9,
            ),
        )
    } else {
        Brush.linearGradient(
            colorStops = arrayOf(
                0f to BhAccentStronger_4338ca,
                0.5f to BhAccentStrong_4f46e5,
                1f to BhAccent2Stronger_0369a1,
            ),
        )
    }
    val dangerGrad = if (isDark) {
        Brush.linearGradient(0f to BhDangerStrong_e11d48, 1f to BhDanger_f43f5e)
    } else {
        Brush.linearGradient(0f to BhDangerStrongLight_b91c1c, 1f to BhDangerLight_dc2626)
    }
    val kpiBg = if (isDark) {
        Brush.linearGradient(0f to BhBgElev2_1a2138, 1f to BhBgElev_131829)
    } else {
        Brush.linearGradient(0f to BhBgElevLight_ffffff, 1f to BhBgElevLight_ffffff)
    }

    val statusMap: Map<String, Color> = if (isDark) {
        mapOf(
            "new" to BhStatusNew_38bdf8,
            "in progress" to BhStatusProgress_f59e0b,
            "in_progress" to BhStatusProgress_f59e0b,
            "resolved" to BhStatusResolved_10b981,
            "closed" to BhStatusClosed_94a3b8,
            "reopened" to BhStatusReopened_a78bfa,
            "not a bug" to BhStatusNotBug_64748b,
            "not_a_bug" to BhStatusNotBug_64748b,
            "resolve later" to BhStatusLater_f59e0b,
            "resolve_later" to BhStatusLater_f59e0b,
        )
    } else {
        mapOf(
            "new" to BhStatusNewLight_0284c7,
            "in progress" to BhStatusProgressLight_d97706,
            "in_progress" to BhStatusProgressLight_d97706,
            "resolved" to BhStatusResolvedLight_059669,
            "closed" to BhStatusClosedLight_64748b,
            "reopened" to BhStatusReopenedLight_7c3aed,
            "not a bug" to BhStatusNotBugLight_94a3b8,
            "not_a_bug" to BhStatusNotBugLight_94a3b8,
            "resolve later" to BhStatusProgressLight_d97706,
            "resolve_later" to BhStatusProgressLight_d97706,
        )
    }
    val priorityMap: Map<String, Color> = if (isDark) {
        mapOf(
            "low" to BhPriorityLow_94a3b8,
            "medium" to BhPriorityMedium_38bdf8,
            "high" to BhPriorityHigh_f59e0b,
            "critical" to BhPriorityCritical_f43f5e,
        )
    } else {
        mapOf(
            "low" to BhPriorityLowLight_64748b,
            "medium" to BhPriorityMediumLight_0284c7,
            "high" to BhPriorityHighLight_d97706,
            "critical" to BhPriorityCriticalLight_dc2626,
        )
    }
    val envMap: Map<String, Color> = if (isDark) {
        mapOf(
            "dev" to BhEnvDev_10b981,
            "uat" to BhEnvUat_f59e0b,
            "prod" to BhEnvProd_f43f5e,
        )
    } else {
        mapOf(
            "dev" to BhEnvDevLight_059669,
            "uat" to BhEnvUatLight_d97706,
            "prod" to BhEnvProdLight_dc2626,
        )
    }
    val inviteMap: Map<String, Pair<Color, Color>> = mapOf(
        "pending" to (BhInvitePendingBg to BhInvitePendingFg),
        "accepted" to (BhInviteAcceptedBg to BhInviteAcceptedFg),
        "revoked" to (BhInviteRevokedBg to BhInviteRevokedFg),
        "expired" to (BhInviteExpiredBg to BhInviteExpiredFg),
    )

    return BrandTokens(
        isDark = isDark,
        statusColors = statusMap,
        priorityColors = priorityMap,
        envColors = envMap,
        inviteStatusColors = inviteMap,
        accentGradient = accentGrad,
        accentGradientHover = accentGradHover,
        dangerGradient = dangerGrad,
        kpiSurfaceGradient = kpiBg,
        accentSoft = if (isDark) BhAccentSoftDark else BhAccentSoftLight,
        accentGlow = if (isDark) BhAccentGlowDark else BhAccentGlowLight,
        border = if (isDark) BhBorder_232c47 else BhBorderLight_e2e8f0,
        borderStrong = if (isDark) BhBorderStrong_344063 else BhBorderStrongLight_c8d2e0,
        borderSoft = if (isDark) BhBorderSoftDark else BhBorderSoftLight,
        surfaceGlass = if (isDark) BhSurfaceGlassDark else BhSurfaceGlassLight,
        modalScrim = BhModalScrim,
        sidebarScrim = BhSidebarScrim,
        loaderScrim = BhLoaderScrim,
        textMuted = if (isDark) BhTextMuted_9aa6c4 else BhTextMutedLight_475569,
        textFaint = if (isDark) BhTextFaint_5e6a85 else BhTextFaintLight_94a3b8,
        scrollbarThumb = if (isDark) BhScrollbarDark else BhScrollbarLight,
        readonlyBannerBg = BhReadonlyBannerBg,
        readonlyBannerBorder = BhReadonlyBannerBorder,
        sleuthOnline = BhSleuthOnline_4ade80,
    )
}
