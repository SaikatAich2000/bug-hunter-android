package com.bughunter.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.bughunter.core.ui.theme.BhNavItemShape
import com.bughunter.core.ui.theme.LocalBrandTokens

data class BhNavDestination(
    val key: String,
    val label: String,
    val icon: ImageVector,
)

@Composable
fun BhSidebarNav(
    destinations: List<BhNavDestination>,
    currentKey: String?,
    onSelect: (BhNavDestination) -> Unit,
    modifier: Modifier = Modifier,
    widthDp: Int = 280,
    orgBanner: @Composable (() -> Unit)? = null,
    accountCard: @Composable (() -> Unit)? = null,
    footer: @Composable (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .width(widthDp.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 14.dp, vertical = 18.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (orgBanner != null) orgBanner()
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            destinations.forEach { destination ->
                val selected = destination.key == currentKey
                BhSidebarItem(
                    destination = destination,
                    selected = selected,
                    onClick = { onSelect(destination) },
                )
            }
        }
        Box(modifier = Modifier.height(8.dp))
        if (accountCard != null) accountCard()
        if (footer != null) footer()
    }
}

@Composable
private fun BhSidebarItem(
    destination: BhNavDestination,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val tokens = LocalBrandTokens.current
    val bg: Color = if (selected) tokens.accentSoft else Color.Transparent
    val fg: Color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(BhNavItemShape)
            .background(bg, BhNavItemShape)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(width = 3.dp, height = 18.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(brush = tokens.accentGradient),
            )
        }
        Icon(
            imageVector = destination.icon,
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = destination.label,
            style = MaterialTheme.typography.labelLarge,
            color = fg,
        )
    }
}
