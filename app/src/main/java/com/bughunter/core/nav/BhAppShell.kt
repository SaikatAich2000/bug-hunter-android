package com.bughunter.core.nav

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bughunter.R
import com.bughunter.core.data.local.AppPrefs
import com.bughunter.core.push.NotificationPermissionPrompt
import com.bughunter.core.ui.components.BhGradientText
import com.bughunter.core.ui.theme.LocalAccentGradient
import com.bughunter.feature.auth.AuthState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private val MediumWidthThreshold: Dp = 600.dp
private val ExpandedWidthThreshold: Dp = 840.dp

private enum class BhWidthClass { Compact, Medium, Expanded }

// Routes that should show the global top app bar (logo + page title + theme/settings).
// Sub-screens with their own back-button top bar (BugDetail, ProjectDetail, Members,
// Invitations, Branding, Organization, Webhooks, BugCreate) are intentionally
// omitted so we don't render two stacked top bars.
private val GlobalTopBarRoutes: Set<String> = setOf(
    BhRoute.Dashboard.path,
    BhRoute.Bugs.path,
    BhRoute.Events.path,
    BhRoute.Analytics.path,
    BhRoute.Audit.path,
    BhRoute.Sessions.path,
    BhRoute.Projects.path,
    BhRoute.Settings.path,
    BhRoute.Profile.path,
    BhRoute.ChangePassword.path,
    BhRoute.ChangeEmail.path,
    BhRoute.TwoFactor.path,
    BhRoute.MySessions.path,
    BhRoute.DsarExport.path,
)

@Composable
internal fun BhAppShell(
    currentThemeMode: AppPrefs.ThemeMode,
    onThemeModeChange: (AppPrefs.ThemeMode) -> Unit,
    viewModel: BhAppShellViewModel = hiltViewModel(),
) {
    val navController: NavHostController = rememberNavController()
    val authState by viewModel.authState.collectAsState()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute: String? = currentBackStack?.destination?.route

    // Forward intents from MainActivity (push tap, app-link, etc.) into
    // the nav controller so deep links resolve correctly on warm start.
    // handleDeepLink walks the registered deepLinks set and navigates if
    // it finds a match; unmatched intents are no-ops. We only honour
    // push-driven intents once the user is authenticated — pushing into
    // a bug screen while still on Login would loop the auth gate.
    LaunchedEffect(navController, authState) {
        if (authState !is AuthState.Authenticated) return@LaunchedEffect
        PushIntentBus.intents.collectLatest { intent ->
            runCatching { navController.handleDeepLink(intent) }
        }
    }

    LaunchedEffect(authState, currentRoute) {
        val onSplash = currentRoute == BhRoute.Splash.path
        when (authState) {
            is AuthState.Unauthenticated -> {
                // Move off Splash or any non-auth screen to Login.
                if (onSplash || (currentRoute != null && !BhRoutes.isAuthRoute(currentRoute))) {
                    navController.navigate(BhRoute.Login.path) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            is AuthState.AwaitingTotp -> {
                if (currentRoute != BhRoute.LoginTotp.path) {
                    navController.navigate(BhRoute.LoginTotp.path)
                }
            }
            is AuthState.Authenticated -> {
                // Move off Splash, null, or any auth screen to Dashboard.
                if (currentRoute == null || onSplash || BhRoutes.isAuthRoute(currentRoute)) {
                    navController.navigate(BhRoute.Dashboard.path) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            else -> Unit
        }
    }

    val isAuthRoute = BhRoutes.isAuthRoute(currentRoute)
    val me = (authState as? AuthState.Authenticated)?.me

    // Permission rationale dialog — fires on first launch BEFORE the
    // user signs in, so the system permission prompt is the first thing
    // they see (the standard Android UX). The prompt has internal once-
    // per-install latching via PushPrefs, so re-launches won't re-show
    // it. Below Android 13 the composable silently no-ops; the
    // permission is granted at install time anyway.
    NotificationPermissionPrompt()

    val navHostContent: @Composable (PaddingValues) -> Unit = { padding ->
        BhNavHost(
            navController = navController,
            onLogout = viewModel::onLogout,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val widthClass = when {
            maxWidth >= ExpandedWidthThreshold -> BhWidthClass.Expanded
            maxWidth >= MediumWidthThreshold -> BhWidthClass.Medium
            else -> BhWidthClass.Compact
        }
        when {
            isAuthRoute || me == null -> {
                Scaffold { padding -> navHostContent(padding) }
            }
            widthClass == BhWidthClass.Expanded -> {
                val showGlobalTopBar = currentRoute in GlobalTopBarRoutes
                PermanentNavigationDrawer(
                    drawerContent = {
                        BhSidebar(
                            currentRoute = currentRoute,
                            me = me,
                            onNavigate = { route -> navController.navigate(route.path) },
                            onLogout = viewModel::onLogout,
                        )
                    },
                ) {
                    Scaffold(
                        topBar = {
                            if (showGlobalTopBar) {
                                BhAppTopBar(
                                    title = titleForRoute(currentRoute),
                                    showMenu = false,
                                    onMenuClick = {},
                                    currentThemeMode = currentThemeMode,
                                    onThemeModeChange = onThemeModeChange,
                                    onSettingsClick = { navController.navigate(BhRoute.Settings.path) },
                                )
                            }
                        },
                    ) { padding -> navHostContent(padding) }
                }
            }
            widthClass == BhWidthClass.Medium -> {
                val showGlobalTopBar = currentRoute in GlobalTopBarRoutes
                Row(modifier = Modifier.fillMaxSize()) {
                    BhNavigationRail(
                        currentRoute = currentRoute,
                        onNavigate = { route -> navController.navigate(route.path) },
                    )
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            if (showGlobalTopBar) {
                                BhAppTopBar(
                                    title = titleForRoute(currentRoute),
                                    showMenu = false,
                                    onMenuClick = {},
                                    currentThemeMode = currentThemeMode,
                                    onThemeModeChange = onThemeModeChange,
                                    onSettingsClick = { navController.navigate(BhRoute.Settings.path) },
                                )
                            }
                        },
                    ) { padding ->
                        navHostContent(padding)
                    }
                }
            }
            else -> {
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                val showGlobalTopBar = currentRoute in GlobalTopBarRoutes
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.7f),
                    drawerContent = {
                        BhSidebar(
                            currentRoute = currentRoute,
                            me = me,
                            onNavigate = { route ->
                                scope.launch { drawerState.close() }
                                navController.navigate(route.path)
                            },
                            onLogout = viewModel::onLogout,
                        )
                    },
                ) {
                    Scaffold(
                        topBar = {
                            if (showGlobalTopBar) {
                                BhAppTopBar(
                                    title = titleForRoute(currentRoute),
                                    showMenu = true,
                                    onMenuClick = { scope.launch { drawerState.open() } },
                                    currentThemeMode = currentThemeMode,
                                    onThemeModeChange = onThemeModeChange,
                                    onSettingsClick = { navController.navigate(BhRoute.Settings.path) },
                                )
                            }
                        },
                        bottomBar = {
                            BhBottomBar(
                                currentRoute = currentRoute,
                                onNavigate = { route -> navController.navigate(route.path) },
                            )
                        },
                    ) { padding ->
                        navHostContent(padding)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BhAppTopBar(
    title: String,
    showMenu: Boolean,
    onMenuClick: () -> Unit,
    currentThemeMode: AppPrefs.ThemeMode,
    onThemeModeChange: (AppPrefs.ThemeMode) -> Unit,
    onSettingsClick: () -> Unit,
) {
    val gradient = LocalAccentGradient.current
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "Bug Hunter",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(36.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                if (title.isNotEmpty()) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                    )
                } else {
                    BhGradientText(
                        text = "Bug Hunter",
                        brush = gradient,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        },
        navigationIcon = {
            if (showMenu) {
                IconButton(onClick = onMenuClick) {
                    Icon(
                        imageVector = Icons.Filled.Menu,
                        contentDescription = "Open menu",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        },
        actions = {
            ThemeMenuAction(
                currentThemeMode = currentThemeMode,
                onThemeModeChange = onThemeModeChange,
            )
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}

@Composable
private fun ThemeMenuAction(
    currentThemeMode: AppPrefs.ThemeMode,
    onThemeModeChange: (AppPrefs.ThemeMode) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = when (currentThemeMode) {
                    AppPrefs.ThemeMode.DARK -> Icons.Filled.DarkMode
                    else -> Icons.Filled.LightMode
                },
                contentDescription = "Theme",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("System default") },
                onClick = {
                    onThemeModeChange(AppPrefs.ThemeMode.SYSTEM)
                    expanded = false
                },
                trailingIcon = { if (currentThemeMode == AppPrefs.ThemeMode.SYSTEM) Text("✓") },
            )
            DropdownMenuItem(
                text = { Text("Light") },
                onClick = {
                    onThemeModeChange(AppPrefs.ThemeMode.LIGHT)
                    expanded = false
                },
                trailingIcon = { if (currentThemeMode == AppPrefs.ThemeMode.LIGHT) Text("✓") },
            )
            DropdownMenuItem(
                text = { Text("Dark") },
                onClick = {
                    onThemeModeChange(AppPrefs.ThemeMode.DARK)
                    expanded = false
                },
                trailingIcon = { if (currentThemeMode == AppPrefs.ThemeMode.DARK) Text("✓") },
            )
        }
    }
}

private fun titleForRoute(route: String?): String = when {
    route == null -> ""
    route == BhRoute.Dashboard.path -> "Dashboard"
    route == BhRoute.Bugs.path -> "Work Items"
    route == BhRoute.BugCreate.path -> "New Item"
    route.startsWith("home/bugs/") -> "Item"
    route == BhRoute.Events.path -> "Events"
    route.startsWith("home/events/") -> "Event"
    route == BhRoute.Analytics.path -> "Analytics"
    route == BhRoute.Audit.path -> "Audit Trail"
    route == BhRoute.Sessions.path -> "Sessions"
    route == BhRoute.Projects.path -> "Projects"
    route.startsWith("home/projects/") -> "Project"
    route == BhRoute.Members.path -> "Members"
    route == BhRoute.Invitations.path -> "Invitations"
    route == BhRoute.Organization.path -> "Organization"
    route == BhRoute.Branding.path -> "Branding"
    route == BhRoute.Webhooks.path -> "Webhooks"
    route == BhRoute.Profile.path -> "Profile"
    route == BhRoute.ChangePassword.path -> "Password"
    route == BhRoute.ChangeEmail.path -> "Email"
    route == BhRoute.TwoFactor.path -> "Two-factor"
    route == BhRoute.MySessions.path -> "My Sessions"
    route == BhRoute.DsarExport.path -> "Data export"
    route == BhRoute.Settings.path -> "Settings"
    else -> ""
}

@Composable
private fun BhBottomBar(
    currentRoute: String?,
    onNavigate: (BhRoute) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        bhSidebarBottomBarItems().forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route.path,
                onClick = { onNavigate(item.route) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label, maxLines = 1) },
            )
        }
    }
}

@Composable
private fun BhNavigationRail(
    currentRoute: String?,
    onNavigate: (BhRoute) -> Unit,
) {
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        bhSidebarBottomBarItems().forEach { item ->
            NavigationRailItem(
                selected = currentRoute == item.route.path,
                onClick = { onNavigate(item.route) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
            )
        }
    }
}
