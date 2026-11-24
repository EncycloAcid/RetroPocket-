package com.retropocket.app.ui.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.retropocket.app.ui.components.GameCover
import com.retropocket.app.ui.components.SystemChip
import com.retropocket.app.ui.theme.CharcoalSoft
import com.retropocket.app.ui.theme.Phosphor
import com.retropocket.data.db.GameEntity
import com.retropocket.data.model.ConsoleSystem
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    games: List<GameEntity>,
    onBack: () -> Unit,
    onOpenGame: (Long) -> Unit,
    onAddGames: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selectedSystem by remember { mutableStateOf<String?>(null) }

    val filtered = remember(games, query, selectedSystem) {
        games.filter { game ->
            val matchesQuery = query.isBlank() ||
                game.title.contains(query, ignoreCase = true) ||
                game.fileName.contains(query, ignoreCase = true)
            val matchesSystem = selectedSystem == null || game.systemId == selectedSystem
            matchesQuery && matchesSystem
        }
    }

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
            Text(
                text = "Library",
                style = MaterialTheme.typography.headlineMedium,
                color = Phosphor,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onAddGames) {
                Text("Scan", color = Phosphor)
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search games") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Phosphor,
                unfocusedBorderColor = CharcoalSoft,
                focusedContainerColor = CharcoalSoft.copy(alpha = 0.4f),
                unfocusedContainerColor = CharcoalSoft.copy(alpha = 0.25f)
            )
        )

        Spacer(Modifier.height(12.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                SystemChip(
                    label = "All",
                    selected = selectedSystem == null,
                    onClick = { selectedSystem = null }
                )
            }
            items(ConsoleSystem.entries) { system ->
                SystemChip(
                    label = system.displayName,
                    selected = selectedSystem == system.id,
                    accent = Color(system.accentHint),
                    onClick = { selectedSystem = system.id }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        AnimatedVisibility(visible = filtered.isEmpty(), enter = fadeIn(), exit = fadeOut()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No games yet", style = MaterialTheme.typography.titleLarge, color = Phosphor)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Add a ROM folder to build your pocket library.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onAddGames) { Text("Add games", color = Phosphor) }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(140.dp),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(filtered, key = { _, g -> g.id }) { _, game ->
                GameCover(
                    title = game.title,
                    systemId = game.systemId,
                    modifier = Modifier.clickable { onOpenGame(game.id) }
                )
            }
        }
    }
}
