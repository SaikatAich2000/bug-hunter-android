package com.bughunter.feature.chatbot

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.bughunter.core.ui.theme.BhPillShape
import com.bughunter.core.ui.theme.LocalAccentGradient

@Composable
internal fun SleuthFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    unread: Int = 0,
    expanded: Boolean = false,
) {
    val gradient = LocalAccentGradient.current
    val transition = rememberInfiniteTransition(label = "sleuth-fab-pulse")
    val pulseScale by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart),
        label = "scale",
    )
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart),
        label = "alpha",
    )
    Box(
        modifier = modifier
            .size(56.dp)
            .semantics { contentDescription = if (expanded) "Close Sleuth" else "Open Sleuth" },
        contentAlignment = Alignment.Center,
    ) {
        if (!expanded) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .scale(pulseScale)
                    .alpha(pulseAlpha)
                    .border(width = 2.dp, color = MaterialTheme.colorScheme.primary, shape = CircleShape),
            )
        }
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(brush = gradient, shape = CircleShape)
                .clickable { onClick() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Chat,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp),
            )
        }
        if (unread > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .height(20.dp)
                    .widthIn(min = 20.dp)
                    .background(color = MaterialTheme.colorScheme.error, shape = BhPillShape)
                    .border(width = 2.dp, color = MaterialTheme.colorScheme.background, shape = BhPillShape)
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (unread > 9) "9+" else unread.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                )
            }
        }
    }
}
