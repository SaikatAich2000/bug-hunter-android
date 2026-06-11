package com.bughunter.feature.organizations.branding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.bughunter.core.ui.theme.LocalBrandTokens
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
internal fun HslColorPicker(
    label: String,
    hexValue: String,
    onHexChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalBrandTokens.current
    val initial = remember(hexValue) { hexToHsl(hexValue) }
    var hue by remember(hexValue) { mutableFloatStateOf(initial.first) }
    var sat by remember(hexValue) { mutableFloatStateOf(initial.second) }
    var light by remember(hexValue) { mutableFloatStateOf(initial.third) }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(hslToColor(hue, sat, light))
                    .border(1.dp, tokens.border, RoundedCornerShape(6.dp)),
            )
            Text(
                text = hexValue,
                style = MaterialTheme.typography.bodySmall,
                color = tokens.textMuted,
            )
        }
        HueBar(
            hue = hue,
            onHueChange = {
                hue = it
                onHexChange(hslToHex(hue, sat, light))
            },
        )
        Text(text = "Saturation", style = MaterialTheme.typography.labelSmall, color = tokens.textMuted)
        Slider(
            value = sat,
            onValueChange = {
                sat = it
                onHexChange(hslToHex(hue, sat, light))
            },
            valueRange = 0f..1f,
        )
        Text(text = "Lightness", style = MaterialTheme.typography.labelSmall, color = tokens.textMuted)
        Slider(
            value = light,
            onValueChange = {
                light = it
                onHexChange(hslToHex(hue, sat, light))
            },
            valueRange = 0f..1f,
        )
    }
}

@Composable
private fun HueBar(hue: Float, onHueChange: (Float) -> Unit) {
    val tokens = LocalBrandTokens.current
    val gradient = remember {
        Brush.horizontalGradient(
            colors = listOf(
                Color.Red,
                Color.Yellow,
                Color.Green,
                Color.Cyan,
                Color.Blue,
                Color.Magenta,
                Color.Red,
            ),
        )
    }
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(gradient)
            .border(1.dp, tokens.border, RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val pct = (offset.x / size.width).coerceIn(0f, 1f)
                    onHueChange(pct * 360f)
                }
            },
    ) {
        val x = (hue / 360f) * size.width
        drawCircle(
            color = Color.White,
            radius = 6f,
            center = Offset(x, size.height / 2f),
        )
    }
}

private fun hexToHsl(hex: String): Triple<Float, Float, Float> {
    val cleaned = hex.removePrefix("#").let {
        when (it.length) {
            6 -> it
            3 -> it.map { c -> "$c$c" }.joinToString("")
            else -> "6366f1"
        }
    }
    val r = cleaned.substring(0, 2).toIntOrNull(16)?.div(255f) ?: 0f
    val g = cleaned.substring(2, 4).toIntOrNull(16)?.div(255f) ?: 0f
    val b = cleaned.substring(4, 6).toIntOrNull(16)?.div(255f) ?: 0f
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val l = (max + min) / 2f
    val d = max - min
    val s = if (d == 0f) 0f else d / (1f - abs(2f * l - 1f))
    val h = when {
        d == 0f -> 0f
        max == r -> 60f * (((g - b) / d) % 6f)
        max == g -> 60f * (((b - r) / d) + 2f)
        else -> 60f * (((r - g) / d) + 4f)
    }
    val hue = if (h < 0) h + 360f else h
    return Triple(hue, s, l)
}

private fun hslToColor(h: Float, s: Float, l: Float): Color {
    val c = (1f - abs(2f * l - 1f)) * s
    val x = c * (1f - abs(((h / 60f) % 2f) - 1f))
    val m = l - c / 2f
    val (r1, g1, b1) = when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(r1 + m, g1 + m, b1 + m)
}

private fun hslToHex(h: Float, s: Float, l: Float): String {
    val color = hslToColor(h, s, l)
    val r = (color.red * 255f).roundToInt().coerceIn(0, 255)
    val g = (color.green * 255f).roundToInt().coerceIn(0, 255)
    val b = (color.blue * 255f).roundToInt().coerceIn(0, 255)
    return "#%02x%02x%02x".format(r, g, b)
}

// Unused helper kept private to silence unused-param warnings.
@Suppress("unused")
private fun trackSize() = Size.Zero
