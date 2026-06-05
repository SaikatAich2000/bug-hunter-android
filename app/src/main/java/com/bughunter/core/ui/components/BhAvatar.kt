package com.bughunter.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.bughunter.core.ui.util.displayInitials

@Composable
fun BhAvatar(
    displayName: String?,
    userId: String,
    modifier: Modifier = Modifier,
    avatarUrl: String? = null,
    email: String? = null,
    sizeDp: Int = 36,
) {
    val initials = displayInitials(displayName, email)
    val bgColor = colorFromHash(userId)
    val initialsFontSize = (sizeDp * 0.4f).sp
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(bgColor, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (!avatarUrl.isNullOrEmpty()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(sizeDp.dp)
                    .clip(CircleShape),
            )
        } else {
            Text(
                text = initials,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = initialsFontSize,
                ),
            )
        }
    }
}

// Deterministic colour from a stable id so avatars never reflow across renders.
internal fun colorFromHash(seed: String): Color {
    if (seed.isEmpty()) return Color(0xFF6366F1)
    var h = 0
    for (c in seed) h = (h * 31 + c.code) and 0x7FFFFFFF
    val palette = listOf(
        Color(0xFF6366F1),
        Color(0xFF818CF8),
        Color(0xFF38BDF8),
        Color(0xFF10B981),
        Color(0xFFF59E0B),
        Color(0xFFF43F5E),
        Color(0xFFA78BFA),
        Color(0xFF0EA5E9),
        Color(0xFF8B5CF6),
        Color(0xFFEC4899),
    )
    return palette[h % palette.size]
}
