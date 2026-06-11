package com.bughunter.core.push

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bughunter.core.data.local.PushPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class NotificationPermissionViewModel @Inject constructor(
    private val pushPrefs: PushPrefs,
) : ViewModel() {

    val promptShown: StateFlow<Boolean> = pushPrefs.permissionPromptShown
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun markPromptShown() {
        viewModelScope.launch { pushPrefs.setPermissionPromptShown(true) }
    }

    fun markPermanentlyDenied(value: Boolean) {
        viewModelScope.launch { pushPrefs.setPermissionPermanentlyDenied(value) }
    }
}

/**
 * One-time rationale dialog for POST_NOTIFICATIONS (Android 13+).
 *
 * Shown lazily — only after the user has logged in (so the request
 * happens in context, not on a cold start before they know what the app
 * does) and only if the OS doesn't already have a decision recorded.
 *
 * Below API 33 the permission is granted at install time; this composable
 * silently no-ops on those devices.
 *
 * The user can dismiss the rationale — that just delays the system
 * permission prompt to the next login. Saying "Not now" inside the
 * system prompt counts as a soft deny; subsequent prompts will only fire
 * a few times before Android auto-denies. We respect that — we never
 * re-ask after the user has explicitly closed the dialog twice.
 */
@Composable
internal fun NotificationPermissionPrompt(
    viewModel: NotificationPermissionViewModel = hiltViewModel(),
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = LocalContext.current
    val promptShown by viewModel.promptShown.collectAsState()
    var dialogVisible by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (!granted) {
            // If the user hit "Don't allow" and Android won't let us
            // re-prompt (shouldShowRequestPermissionRationale=false
            // after a deny means permanently denied), record that so
            // future opt-in re-attempts know to deep-link into Settings
            // instead of firing a system dialog that will silently
            // no-op.
            val act = context as? Activity
            val canStillAsk = act != null &&
                ActivityCompat.shouldShowRequestPermissionRationale(
                    act, Manifest.permission.POST_NOTIFICATIONS,
                )
            viewModel.markPermanentlyDenied(!canStillAsk)
        }
    }

    LaunchedEffect(promptShown) {
        if (promptShown) return@LaunchedEffect
        val state = ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS,
        )
        if (state == PackageManager.PERMISSION_GRANTED) {
            viewModel.markPromptShown()
            return@LaunchedEffect
        }
        dialogVisible = true
    }

    if (!dialogVisible) return

    AlertDialog(
        onDismissRequest = {
            dialogVisible = false
            viewModel.markPromptShown()
        },
        title = { Text("Stay in the loop") },
        text = {
            Column {
                Text(
                    "Get notified when a bug, task, or requirement is " +
                        "assigned to you, when someone @mentions you, " +
                        "or when an item you follow changes status.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "You can fine-tune which channels notify you in " +
                        "Settings later.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                dialogVisible = false
                scope.launch { viewModel.markPromptShown() }
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }) { Text("Turn on") }
        },
        dismissButton = {
            TextButton(onClick = {
                dialogVisible = false
                scope.launch { viewModel.markPromptShown() }
            }) { Text("Not now") }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Open the system Settings page for this app so the user can flip
 *  notification permission back on after a permanent deny. */
internal fun openAppNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    try {
        context.startActivity(intent)
    } catch (_: Throwable) {
        // Fallback for devices that don't expose the deep link — open
        // the generic app details page.
        val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(fallback)
    }
}
