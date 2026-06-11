package com.bughunter.feature.webhooks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bughunter.core.network.dto.WebhookOut
import com.bughunter.core.ui.components.BhCard
import com.bughunter.core.ui.components.BhDangerButton
import com.bughunter.core.ui.components.BhEmptyState
import com.bughunter.core.ui.components.BhGhostButton
import com.bughunter.core.ui.components.BhPrimaryButton
import com.bughunter.core.ui.components.BhSecondaryButton
import com.bughunter.core.ui.components.BhTopAppBar
import com.bughunter.core.ui.theme.LocalBrandTokens
import com.bughunter.core.ui.util.UiState

@Composable
internal fun WebhooksScreen(
    onBack: () -> Unit,
    viewModel: WebhooksViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    WebhooksContent(
        state = state,
        onBack = onBack,
        onToggle = viewModel::toggleActive,
        onDelete = viewModel::delete,
        onTest = viewModel::test,
        onCreate = { u, s, e -> viewModel.create(u, s, e) { /* no-op */ } },
        onUpdate = { id, u, s, a, e -> viewModel.update(id, u, s, a, e) },
        onRefresh = viewModel::load,
    )
}

@Composable
internal fun WebhooksScreenTestHarness(
    state: UiState<WebhooksModel>,
    onBack: () -> Unit = {},
    onToggle: (WebhookOut) -> Unit = {},
    onDelete: (Int) -> Unit = {},
    onTest: (Int) -> Unit = {},
    onCreate: (String, String?, List<String>) -> Unit = { _, _, _ -> },
    onUpdate: (Int, String?, String?, Boolean?, List<String>?) -> Unit = { _, _, _, _, _ -> },
    onRefresh: () -> Unit = {},
) {
    WebhooksContent(state, onBack, onToggle, onDelete, onTest, onCreate, onUpdate, onRefresh)
}

@Composable
private fun WebhooksContent(
    state: UiState<WebhooksModel>,
    onBack: () -> Unit,
    onToggle: (WebhookOut) -> Unit,
    onDelete: (Int) -> Unit,
    onTest: (Int) -> Unit,
    onCreate: (String, String?, List<String>) -> Unit,
    onUpdate: (Int, String?, String?, Boolean?, List<String>?) -> Unit,
    onRefresh: () -> Unit,
) {
    var creating by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<WebhookOut?>(null) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        BhTopAppBar(
            title = "Webhooks",
            navigationIcon = { BhGhostButton(text = "Back", onClick = onBack) },
            actions = { BhGhostButton(text = "Refresh", onClick = onRefresh) },
        )
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                BhPrimaryButton(text = "+ Add webhook", onClick = { creating = true })
            }
            when (state) {
                UiState.Loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
                is UiState.Error -> Text(
                    "Couldn't load webhooks.",
                    color = MaterialTheme.colorScheme.error,
                )
                UiState.Empty -> BhEmptyState(
                    title = "No webhooks yet.",
                    helper = "Add a webhook to forward events to your endpoint.",
                )
                is UiState.Success -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(items = state.data.webhooks, key = { it.id }) { hook ->
                        WebhookRow(
                            hook = hook,
                            onToggle = { onToggle(hook) },
                            onTest = { onTest(hook.id) },
                            onEdit = { editing = hook },
                            onDelete = { onDelete(hook.id) },
                        )
                    }
                }
            }
        }
    }

    if (creating) {
        WebhookFormDialog(
            initial = null,
            onDismiss = { creating = false },
            onSubmit = { url, secret, _, events ->
                onCreate(url, secret, events)
                creating = false
            },
        )
    }
    val edit = editing
    if (edit != null) {
        WebhookFormDialog(
            initial = edit,
            onDismiss = { editing = null },
            onSubmit = { url, secret, active, events ->
                onUpdate(edit.id, url, secret.takeIf { it.isNotBlank() }, active, events)
                editing = null
            },
        )
    }
}

@Composable
private fun WebhookRow(
    hook: WebhookOut,
    onToggle: () -> Unit,
    onTest: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val tokens = LocalBrandTokens.current
    BhCard(padding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = hook.url,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Secret: ${hook.secretMasked.ifBlank { "(none)" }}",
                        style = MaterialTheme.typography.labelSmall,
                        color = tokens.textMuted,
                    )
                    if (hook.eventTypes.isNotEmpty()) {
                        Text(
                            text = hook.eventTypes.joinToString(", "),
                            style = MaterialTheme.typography.labelSmall,
                            color = tokens.textFaint,
                        )
                    }
                }
                Switch(checked = hook.active, onCheckedChange = { onToggle() })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BhSecondaryButton(text = "Test", onClick = onTest)
                BhGhostButton(text = "Edit", onClick = onEdit)
                BhDangerButton(text = "Delete", onClick = onDelete)
            }
        }
    }
}
