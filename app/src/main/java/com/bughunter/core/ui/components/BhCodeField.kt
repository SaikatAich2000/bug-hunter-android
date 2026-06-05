package com.bughunter.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bughunter.core.ui.theme.BhInputShape
import com.bughunter.core.ui.theme.BhMonoFontFamily

@Composable
fun BhCodeField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    length: Int = 6,
    enabled: Boolean = true,
    isError: Boolean = false,
) {
    val focusRequesters = remember(length) { List(length) { FocusRequester() } }
    val digits: List<Char> = (0 until length).map { idx ->
        value.getOrNull(idx) ?: ' '
    }
    LaunchedEffect(value) {
        val nextIdx = value.length.coerceIn(0, length - 1)
        if (value.length in 1..length) {
            focusRequesters[nextIdx].requestFocus()
        }
    }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (idx in 0 until length) {
            val cellChar = digits[idx]
            val cellText = if (cellChar == ' ') "" else cellChar.toString()
            OutlinedTextField(
                value = cellText,
                onValueChange = { incoming ->
                    val cleaned = incoming.filter { it.isDigit() }
                    val newValue = when {
                        cleaned.isEmpty() -> value.take(idx)
                        cleaned.length == 1 -> value.take(idx) + cleaned + value.drop(idx + 1)
                        else -> {
                            val digit = cleaned.last()
                            value.take(idx) + digit + value.drop(idx + 1)
                        }
                    }.take(length)
                    onValueChange(newValue)
                },
                enabled = enabled,
                isError = isError,
                singleLine = true,
                maxLines = 1,
                shape = BhInputShape,
                textStyle = TextStyle(
                    fontFamily = BhMonoFontFamily,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier
                    .size(width = 48.dp, height = 56.dp)
                    .focusRequester(focusRequesters[idx]),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    errorBorderColor = MaterialTheme.colorScheme.error,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        }
    }
}
