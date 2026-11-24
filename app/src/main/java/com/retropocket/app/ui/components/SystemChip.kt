package com.retropocket.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.retropocket.app.ui.theme.Charcoal
import com.retropocket.app.ui.theme.CharcoalSoft
import com.retropocket.app.ui.theme.Phosphor

@Composable
fun SystemChip(
    label: String,
    selected: Boolean,
    accent: Color = Phosphor,
    onClick: () -> Unit
) {
    val bg = if (selected) accent else CharcoalSoft
    val fg = if (selected) Charcoal else MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = fg,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}
