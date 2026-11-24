package com.retropocket.app.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.retropocket.app.ui.theme.Bezel
import com.retropocket.app.ui.theme.BezelEdge
import com.retropocket.app.ui.theme.Charcoal
import com.retropocket.app.ui.theme.Phosphor
import com.retropocket.app.ui.theme.PhosphorDim

@Composable
fun HomeScreen(
    hasGames: Boolean,
    onAddGames: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val brandAlpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "brandAlpha"
    )
    val brandOffset by animateFloatAsState(
        targetValue = if (appeared) 0f else 28f,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "brandOffset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0A1014), Charcoal, Color(0xFF0C1512))
                )
            )
    ) {
        HandheldSilhouette(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 0.55f * brandAlpha }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 28.dp, vertical = 24.dp)
                .graphicsLayer {
                    alpha = brandAlpha
                    translationY = brandOffset
                },
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onOpenSettings, modifier = Modifier.align(Alignment.End)) {
                Text("Settings", color = Phosphor.copy(alpha = 0.85f))
            }

            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = "RetroPocket",
                    style = MaterialTheme.typography.displayMedium,
                    color = Phosphor
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Your pocket arcade.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = if (hasGames) onOpenLibrary else onAddGames,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Phosphor,
                        contentColor = Charcoal
                    )
                ) {
                    Text(
                        text = if (hasGames) "Open library" else "Add games",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                if (hasGames) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onAddGames) {
                        Text("Change ROM folder", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Pick a folder with GB, GBA, NES, SNES, or PS1 games. BIOS stays yours.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun HandheldSilhouette(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val bodyW = w * 0.72f
        val bodyH = h * 0.78f
        val left = (w - bodyW) / 2f
        val top = h * 0.12f

        drawRoundRect(
            color = Bezel.copy(alpha = 0.9f),
            topLeft = Offset(left, top),
            size = Size(bodyW, bodyH),
            cornerRadius = CornerRadius(48f, 48f)
        )
        drawRoundRect(
            brush = Brush.linearGradient(
                listOf(BezelEdge.copy(alpha = 0.35f), Color.Transparent)
            ),
            topLeft = Offset(left, top),
            size = Size(bodyW, bodyH),
            cornerRadius = CornerRadius(48f, 48f)
        )

        val screenW = bodyW * 0.78f
        val screenH = bodyH * 0.42f
        val screenLeft = left + (bodyW - screenW) / 2f
        val screenTop = top + bodyH * 0.1f
        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(PhosphorDim.copy(alpha = 0.55f), Color(0xFF102018))
            ),
            topLeft = Offset(screenLeft, screenTop),
            size = Size(screenW, screenH),
            cornerRadius = CornerRadius(18f, 18f)
        )

        // D-pad ghost
        val padCx = left + bodyW * 0.28f
        val padCy = top + bodyH * 0.68f
        val arm = bodyW * 0.06f
        drawRoundRect(
            color = Phosphor.copy(alpha = 0.18f),
            topLeft = Offset(padCx - arm * 0.35f, padCy - arm * 1.2f),
            size = Size(arm * 0.7f, arm * 2.4f),
            cornerRadius = CornerRadius(8f, 8f)
        )
        drawRoundRect(
            color = Phosphor.copy(alpha = 0.18f),
            topLeft = Offset(padCx - arm * 1.2f, padCy - arm * 0.35f),
            size = Size(arm * 2.4f, arm * 0.7f),
            cornerRadius = CornerRadius(8f, 8f)
        )

        // Face buttons ghost
        val btnCx = left + bodyW * 0.72f
        val btnCy = padCy
        val r = arm * 0.45f
        listOf(
            Offset(btnCx, btnCy - r * 2.1f),
            Offset(btnCx + r * 2.1f, btnCy),
            Offset(btnCx, btnCy + r * 2.1f),
            Offset(btnCx - r * 2.1f, btnCy)
        ).forEach {
            drawCircle(color = Phosphor.copy(alpha = 0.2f), radius = r, center = it)
        }
    }
}
