package com.bughunter.core.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bughunter.core.ui.theme.LocalBrandTokens

/**
 * Skeleton loading primitives — an animated gradient sweep over
 * placeholder shapes, shown while real content loads. Replaces the bare
 * centered CircularProgressIndicator pattern: the page keeps its layout
 * (no jump when content lands) and reads as "loading content", not
 * "frozen app".
 *
 * Colours derive from the brand border token so the skeleton sits
 * naturally on both dark and light surfaces.
 */
@Composable
fun bhShimmerBrush(): Brush {
    val tokens = LocalBrandTokens.current
    val transition = rememberInfiniteTransition(label = "shimmer")
    val offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerOffset",
    )
    val base = tokens.border.copy(alpha = 0.45f)
    val highlight = tokens.border.copy(alpha = 0.16f)
    return Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(offset - 600f, 0f),
        end = Offset(offset, 0f),
    )
}

/** A single shimmering placeholder block. */
@Composable
fun BhShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
) {
    Spacer(
        modifier = modifier
            .clip(shape)
            .background(bhShimmerBrush()),
    )
}

/**
 * Skeleton stand-in for a list row card: leading badge block, two text
 * lines, trailing chip. Mirrors BugRow proportions so the swap to real
 * rows is seamless.
 */
@Composable
fun BhShimmerListItem(modifier: Modifier = Modifier) {
    BhCard(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BhShimmerBox(modifier = Modifier.size(36.dp), shape = CircleShape)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BhShimmerBox(modifier = Modifier.fillMaxWidth(0.7f).height(14.dp))
                BhShimmerBox(modifier = Modifier.fillMaxWidth(0.45f).height(11.dp))
            }
            BhShimmerBox(modifier = Modifier.width(56.dp).height(22.dp))
        }
    }
}

/** A column of [count] shimmering list rows. */
@Composable
fun BhShimmerList(
    count: Int = 6,
    modifier: Modifier = Modifier,
    spacing: Dp = 8.dp,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) {
        repeat(count) {
            BhShimmerListItem()
        }
    }
}
