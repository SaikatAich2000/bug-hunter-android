package com.bughunter.feature.bugs.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bughunter.core.network.DomainError
import com.bughunter.core.network.dto.BugDetail
import com.bughunter.core.network.dto.Role
import com.bughunter.core.ui.components.BhBadge
import com.bughunter.core.ui.components.BhErrorBanner
import com.bughunter.core.ui.components.BhIconButton
import com.bughunter.core.ui.components.BhRichHtml
import com.bughunter.core.ui.components.BhTopAppBar
import com.bughunter.core.ui.theme.LocalBrandTokens
import com.bughunter.core.ui.util.UiState
import com.bughunter.feature.bugs.list.envKind
import com.bughunter.feature.bugs.list.priorityKind
import com.bughunter.feature.bugs.list.statusKind

@Composable
internal fun BugDetailScreen(
    onBack: () -> Unit,
    onEdit: (Int) -> Unit,
    currentUserRole: Role? = null,
    viewModel: BugDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    BugDetailContent(
        state = state,
        currentUserRole = currentUserRole,
        onBack = onBack,
        onEdit = onEdit,
        onCommentDraftChange = viewModel::onCommentDraftChange,
        onPostComment = viewModel::postComment,
        onAttach = { /* attachment picker is wired by host */ },
        onDeleteAttachment = viewModel::deleteAttachment,
        onDeleteComment = viewModel::deleteComment,
        onRetry = viewModel::refresh,
    )
}

@Composable
internal fun BugDetailContent(
    state: UiState<BugDetailScreenModel>,
    currentUserRole: Role?,
    onBack: () -> Unit,
    onEdit: (Int) -> Unit,
    onCommentDraftChange: (String) -> Unit,
    onPostComment: () -> Unit,
    onAttach: () -> Unit,
    onDeleteAttachment: (Int) -> Unit,
    onDeleteComment: (Int) -> Unit,
    onRetry: () -> Unit,
) {
    Scaffold(
        topBar = {
            BhTopAppBar(
                title = "Bug",
                navigationIcon = {
                    BhIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = onBack,
                    )
                },
            )
        },
        floatingActionButton = {
            val data = (state as? UiState.Success)?.data
            if (data != null && data.bug.canEdit && !data.isReadOnly) {
                FloatingActionButton(
                    onClick = { onEdit(data.bug.id) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            when (state) {
                UiState.Loading -> CenteredLoader()
                UiState.Empty -> Text(
                    "No bug",
                    modifier = Modifier.padding(24.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                is UiState.Error -> ErrorPanel(error = state.error, onRetry = onRetry)
                is UiState.Success -> BugDetailBody(
                    model = state.data,
                    currentUserRole = currentUserRole,
                    onCommentDraftChange = onCommentDraftChange,
                    onPostComment = onPostComment,
                    onAttach = onAttach,
                    onDeleteAttachment = onDeleteAttachment,
                    onDeleteComment = onDeleteComment,
                )
            }
        }
    }
}

@Composable
private fun BugDetailBody(
    model: BugDetailScreenModel,
    currentUserRole: Role?,
    onCommentDraftChange: (String) -> Unit,
    onPostComment: () -> Unit,
    onAttach: () -> Unit,
    onDeleteAttachment: (Int) -> Unit,
    onDeleteComment: (Int) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val wide = maxWidth >= 900.dp
        val scroll = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            HeaderRow(bug = model.bug)
            if (model.isReadOnly) {
                ReadonlyBanner()
            }
            if (wide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Column(
                        modifier = Modifier.weight(1.6f),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        BugBodySection(
                            model = model,
                            currentUserRole = currentUserRole,
                            onCommentDraftChange = onCommentDraftChange,
                            onPostComment = onPostComment,
                            onAttach = onAttach,
                            onDeleteAttachment = onDeleteAttachment,
                            onDeleteComment = onDeleteComment,
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        MetaSidePanel(bug = model.bug)
                    }
                }
            } else {
                MetaSidePanel(bug = model.bug)
                BugBodySection(
                    model = model,
                    currentUserRole = currentUserRole,
                    onCommentDraftChange = onCommentDraftChange,
                    onPostComment = onPostComment,
                    onAttach = onAttach,
                    onDeleteAttachment = onDeleteAttachment,
                    onDeleteComment = onDeleteComment,
                )
            }
            ActivitySection(activities = model.bug.activities)
        }
    }
}

@Composable
private fun BugBodySection(
    model: BugDetailScreenModel,
    currentUserRole: Role?,
    onCommentDraftChange: (String) -> Unit,
    onPostComment: () -> Unit,
    onAttach: () -> Unit,
    onDeleteAttachment: (Int) -> Unit,
    onDeleteComment: (Int) -> Unit,
) {
    val bug = model.bug
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BhRichHtml(html = bug.description)
        // Surface action errors (failed post / delete) right above the
        // action area so users actually see why their tap did nothing.
        BhErrorBanner(error = model.actionError)
        AttachmentsSection(
            attachments = bug.attachments,
            currentUserRole = currentUserRole,
            onDownload = { /* wired by host */ },
            onDelete = onDeleteAttachment,
        )
        CommentsSection(
            comments = model.commentsNewestFirst,
            currentUserRole = currentUserRole,
            onDelete = onDeleteComment,
        )
        if (!model.isReadOnly) {
            CommentComposer(
                value = model.commentDraft,
                isPosting = model.isPostingComment,
                onValueChange = onCommentDraftChange,
                onPost = onPostComment,
                onAttach = onAttach,
            )
        }
    }
}

@Composable
private fun HeaderRow(bug: BugDetail) {
    val tokens = LocalBrandTokens.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val key = bug.projectKey?.takeIf { it.isNotBlank() }
        val idLabel = if (key != null) "$key-${bug.id}" else "#${bug.id}"
        Text(
            text = idLabel,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = bug.title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BhBadge(label = bug.status, kind = statusKind(bug.status))
            BhBadge(label = bug.priority, kind = priorityKind(bug.priority))
            BhBadge(label = bug.environment, kind = envKind(bug.environment))
        }
        Text(
            text = "${bug.itemType} • ${bug.projectName ?: "—"}",
            style = MaterialTheme.typography.bodySmall,
            color = tokens.textMuted,
        )
    }
}

@Composable
private fun ReadonlyBanner() {
    val tokens = LocalBrandTokens.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Text(
            text = "Read-only — only admins and managers can edit requirements.",
            style = MaterialTheme.typography.bodySmall,
            color = tokens.readonlyBannerBorder,
            modifier = Modifier.padding(12.dp),
        )
    }
}

@Composable
private fun CenteredLoader() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ErrorPanel(error: DomainError, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = when (error) {
                DomainError.Network -> "Network unavailable."
                DomainError.NotFound -> "Bug not found."
                DomainError.Unauthorized -> "Session expired."
                else -> "Failed to load bug."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "Retry",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
