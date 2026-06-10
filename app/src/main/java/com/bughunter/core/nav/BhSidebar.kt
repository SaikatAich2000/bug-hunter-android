package com.bughunter.core.nav

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.ExitToApp
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bughunter.R
import com.bughunter.core.network.dto.MeOut
import com.bughunter.core.ui.components.BhAvatar
import com.bughunter.core.ui.components.BhGradientText
import com.bughunter.core.ui.theme.LocalAccentGradient
import com.bughunter.core.ui.theme.LocalBrandTokens

internal data class BhSidebarItem(
    val route: BhRoute,
    val label: String,
    val icon: ImageVector,
)

private val PrimaryDestinations: List<BhSidebarItem> = listOf(
    BhSidebarItem(BhRoute.Dashboard, "Dashboard", Icons.Outlined.Dashboard),
    BhSidebarItem(BhRoute.Bugs, "Work Items", Icons.Outlined.BugReport),
    BhSidebarItem(BhRoute.Events, "Events", Icons.Outlined.CalendarMonth),
    BhSidebarItem(BhRoute.Analytics, "Analytics", Icons.Outlined.Analytics),
    BhSidebarItem(BhRoute.Audit, "Audit Trail", Icons.Outlined.Shield),
)

private val OrgDestinations: List<BhSidebarItem> = listOf(
    BhSidebarItem(BhRoute.Projects, "Projects", Icons.Outlined.Folder),
    BhSidebarItem(BhRoute.Members, "Members", Icons.Outlined.Group),
)

private val BottomBarDestinations: List<BhSidebarItem> = listOf(
    BhSidebarItem(BhRoute.Dashboard, "Home", Icons.Outlined.Dashboard),
    BhSidebarItem(BhRoute.Bugs, "Work", Icons.Outlined.BugReport),
    BhSidebarItem(BhRoute.Projects, "Projects", Icons.Outlined.Folder),
    BhSidebarItem(BhRoute.Analytics, "Stats", Icons.Outlined.Analytics),
)

@Composable
internal fun BhSidebar(
    currentRoute: String?,
    me: MeOut?,
    onNavigate: (BhRoute) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gradient = LocalAccentGradient.current
    var showSignOutDialog by remember { mutableStateOf(false) }
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign out?") },
            text = { Text("You'll need to sign in again to access your work.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        onLogout()
                    },
                ) { Text("Sign out", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) { Text("Cancel") }
            },
        )
    }
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .width(280.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .windowInsetsPadding(WindowInsets.statusBars)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            BhSidebarBrandRow(gradient = gradient)
            if (me != null) BhSidebarOrgBanner(me = me)
            BhSidebarSectionHeader(label = "Primary")
            PrimaryDestinations.forEach { item ->
                BhSidebarNavItem(
                    item = item,
                    isActive = currentRoute == item.route.path,
                    onClick = { onNavigate(item.route) },
                )
            }
            BhSidebarSectionHeader(label = "Organization")
            OrgDestinations.forEach { item ->
                BhSidebarNavItem(
                    item = item,
                    isActive = currentRoute == item.route.path,
                    onClick = { onNavigate(item.route) },
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (me != null) {
                BhSidebarAccountCard(
                    me = me,
                    onLogout = { showSignOutDialog = true },
                )
            }
        }
    }
}

@Composable
private fun BhSidebarBrandRow(gradient: androidx.compose.ui.graphics.Brush) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = "Bug Hunter",
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(44.dp),
        )
        Column {
            BhGradientText(
                text = "Bug Hunter",
                brush = gradient,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = "v2.9",
                style = MaterialTheme.typography.labelSmall,
                color = LocalBrandTokens.current.textFaint,
            )
        }
    }
}

@Composable
private fun BhSidebarOrgBanner(me: MeOut) {
    val tokens = LocalBrandTokens.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = tokens.accentSoft,
        shape = RoundedCornerShape(10.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = me.organizationName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = me.organizationSlug,
                style = MaterialTheme.typography.bodySmall,
                color = tokens.textMuted,
            )
        }
    }
}

@Composable
private fun BhSidebarSectionHeader(label: String) {
    Text(
        text = label.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.08.sp),
        color = LocalBrandTokens.current.textMuted,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
    )
}

@Composable
private fun BhSidebarNavItem(
    item: BhSidebarItem,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    val tokens = LocalBrandTokens.current
    val bg = if (isActive) tokens.accentSoft else Color.Transparent
    val fg = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    Surface(
        onClick = onClick,
        color = bg,
        shape = RoundedCornerShape(9.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = item.icon, contentDescription = item.label, tint = fg)
            Text(
                text = item.label,
                color = fg,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun BhSidebarAccountCard(
    me: MeOut,
    onLogout: () -> Unit,
) {
    val tokens = LocalBrandTokens.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BhAvatar(
                    displayName = me.name,
                    userId = me.id.toString(),
                    email = me.email,
                    sizeDp = 36,
                )
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = me.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = me.role.name.lowercase().replaceFirstChar { it.titlecase() },
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.textMuted,
                    )
                    Text(
                        text = me.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.textFaint,
                        maxLines = 1,
                    )
                }
            }
            Surface(
                onClick = onLogout,
                color = Color.Transparent,
                shape = RoundedCornerShape(7.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ExitToApp,
                        contentDescription = "Logout",
                        tint = tokens.textMuted,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "Sign out",
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.textMuted,
                    )
                }
            }
        }
    }
}

@Composable
internal fun BhSidebarBottomBarItems(): List<BhSidebarItem> = BottomBarDestinations

// Reserve PaddingValues helper for consumers that want consistent inner padding.
internal val BhSidebarContentPadding = PaddingValues(horizontal = 14.dp, vertical = 18.dp)
