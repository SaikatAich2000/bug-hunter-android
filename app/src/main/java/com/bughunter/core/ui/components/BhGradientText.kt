package com.bughunter.core.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit

@Composable
fun BhGradientText(
    text: String,
    brush: Brush,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
) {
    val baseStyle = LocalTextStyle.current
    val effective = baseStyle.copy(
        brush = brush,
        fontSize = if (fontSize == TextUnit.Unspecified) baseStyle.fontSize else fontSize,
        fontWeight = fontWeight ?: baseStyle.fontWeight,
    )
    Text(
        text = text,
        modifier = modifier,
        style = effective,
        color = Color.Unspecified,
    )
}
