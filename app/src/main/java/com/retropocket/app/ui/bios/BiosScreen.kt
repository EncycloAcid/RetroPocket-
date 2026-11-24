package com.retropocket.app.ui.bios

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.retropocket.app.ui.theme.Charcoal
import com.retropocket.app.ui.theme.Phosphor
import com.retropocket.data.bios.BiosStatus

@Composable
fun BiosScreen(
    statuses: List<BiosStatus>,
    lastImportOk: Boolean,
    onBack: () -> Unit,
    onImport: () -> Unit,
    onClear: () -> Unit
) {
    val ps1 = statuses.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Phosphor)
            }
            Text("BIOS Manager", style = MaterialTheme.typography.headlineMedium, color = Phosphor)
        }

        Spacer(Modifier.height(12.dp))
        Text(
            "Only systems that need firmware ask for BIOS. Files stay in app-private storage.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(24.dp))
        Text("PlayStation", style = MaterialTheme.typography.titleLarge, color = Phosphor)
        Spacer(Modifier.height(6.dp))
        Text(
            if (ps1?.present == true) "Ready · ${ps1.fileName}" else "Missing — import SCPH1001.bin (or similar) you own",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        val sha1 = ps1?.sha1
        if (sha1 != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                "SHA-1 ${sha1.take(12)}…",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onImport,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Phosphor, contentColor = Charcoal)
        ) {
            Text("Import BIOS file", style = MaterialTheme.typography.labelLarge)
        }

        AnimatedVisibility(
            visible = lastImportOk,
            enter = fadeIn() + scaleIn(initialScale = 0.92f)
        ) {
            Row(
                modifier = Modifier.padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Phosphor)
                Spacer(Modifier.height(0.dp))
                Text(
                    "  BIOS imported",
                    style = MaterialTheme.typography.titleMedium,
                    color = Phosphor
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onClear) {
            Text("Clear imported BIOS", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
