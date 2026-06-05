package com.bughunter.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bughunter.core.ui.theme.LocalAccentGradient

data class BhSplitMenuItem(
    val label: String,
    val onClick: () -> Unit,
)

@Composable
fun BhSplitButton(
    primaryText: String,
    onPrimaryClick: () -> Unit,
    menuItems: List<BhSplitMenuItem>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val gradient = LocalAccentGradient.current
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(36.dp)
                .background(
                    brush = gradient,
                    shape = RoundedCornerShape(9.dp),
                ),
        ) {
            Box(
                modifier = Modifier
                    .clickable(enabled = enabled) { onPrimaryClick() }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = primaryText,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                )
            }
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(20.dp)
                    .background(Color.White.copy(alpha = 0.16f)),
            )
            Box(
                modifier = Modifier
                    .clickable(enabled = enabled) { expanded = true }
                    .padding(PaddingValues(horizontal = 8.dp, vertical = 8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = "More options",
                    tint = Color.White,
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            menuItems.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.label) },
                    onClick = {
                        expanded = false
                        item.onClick()
                    },
                )
            }
        }
    }
}
