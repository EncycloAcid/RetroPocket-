package com.retropocket.app.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.retropocket.app.ui.components.GameCover
import com.retropocket.app.ui.theme.Charcoal
import com.retropocket.app.ui.theme.Danger
import com.retropocket.app.ui.theme.Phosphor
import com.retropocket.data.db.GameEntity
import com.retropocket.data.model.ConsoleSystem

@Composable
fun DetailScreen(
    game: GameEntity?,
    biosReady: Boolean,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onOpenBios: () -> Unit
) {
    if (game == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Game not found", color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = onBack) { Text("Back") }
        }
        return
    }

    val system = ConsoleSystem.fromId(game.systemId)
    val needsBios = system?.requiresBios == true && !biosReady

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Phosphor)
            }
            Text("Game", style = MaterialTheme.typography.titleLarge, color = Phosphor)
        }

        Spacer(Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            GameCover(
                title = game.title,
                systemId = game.systemId,
                modifier = Modifier.width(150.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(game.title, style = MaterialTheme.typography.headlineMedium, color = Phosphor)
                Spacer(Modifier.height(8.dp))
                Text(
                    system?.displayName ?: game.systemId,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    game.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        if (needsBios) {
            Text(
                "PlayStation needs a BIOS file you own. Import it once, then play.",
                style = MaterialTheme.typography.bodyMedium,
                color = Danger
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onOpenBios,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Phosphor, contentColor = Charcoal)
            ) {
                Text("Import BIOS", style = MaterialTheme.typography.labelLarge)
            }
        } else {
            Button(
                onClick = onPlay,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Phosphor, contentColor = Charcoal)
            ) {
                Text("Play", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
