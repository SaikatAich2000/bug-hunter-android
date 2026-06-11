package com.bughunter.feature.chatbot

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
internal fun SleuthOverlay(
    modifier: Modifier = Modifier,
    viewModel: SleuthViewModel = hiltViewModel(),
    onDownloadFile: (String) -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    SleuthOverlayContent(
        state = state,
        modifier = modifier,
        onFabClick = viewModel::togglePanel,
        onClose = viewModel::closePanel,
        onClear = viewModel::clearHistory,
        onTabSelected = viewModel::selectTab,
        onInputChange = viewModel::onInputChange,
        onSend = { viewModel.send() },
        onSuggestionTap = viewModel::onSuggestionTap,
        onConfirm = viewModel::onConfirm,
        onRowTap = viewModel::onTableRowTap,
        onDownload = onDownloadFile,
        onAutoOpenChange = viewModel::toggleAutoOpen,
        onShowTypingChange = viewModel::toggleShowTyping,
    )
}

@Composable
internal fun SleuthOverlayContent(
    state: SleuthUiState,
    modifier: Modifier = Modifier,
    onFabClick: () -> Unit,
    onClose: () -> Unit,
    onClear: () -> Unit,
    onTabSelected: (SleuthTab) -> Unit,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onSuggestionTap: (String) -> Unit,
    onConfirm: (Int, Int, Boolean) -> Unit,
    onRowTap: (Int) -> Unit,
    onDownload: (String) -> Unit,
    onAutoOpenChange: (Boolean) -> Unit,
    onShowTypingChange: (Boolean) -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        // Panel springs up from the FAB it expands out of: slide-up +
        // fade + slight scale reads as "the FAB grew into this panel".
        AnimatedVisibility(
            visible = state.isPanelOpen,
            enter = fadeIn() +
                slideInVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                    initialOffsetY = { it / 6 },
                ) +
                scaleIn(initialScale = 0.94f),
            exit = fadeOut() +
                slideOutVertically(targetOffsetY = { it / 8 }) +
                scaleOut(targetScale = 0.96f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 90.dp),
        ) {
            SleuthPanel(
                state = state,
                onClose = onClose,
                onClear = onClear,
                onTabSelected = onTabSelected,
                onInputChange = onInputChange,
                onSend = onSend,
                onSuggestionTap = onSuggestionTap,
                onConfirm = onConfirm,
                onRowTap = onRowTap,
                onDownload = onDownload,
                onAutoOpenChange = onAutoOpenChange,
                onShowTypingChange = onShowTypingChange,
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 22.dp, bottom = 22.dp),
        ) {
            SleuthFab(
                onClick = onFabClick,
                unread = state.unread,
                expanded = state.isPanelOpen,
            )
        }
    }
}
