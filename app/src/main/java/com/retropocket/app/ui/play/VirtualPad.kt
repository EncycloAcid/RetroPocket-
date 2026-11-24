package com.retropocket.app.ui.play

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retropocket.app.ui.theme.Bezel
import com.retropocket.app.ui.theme.Phosphor
import com.retropocket.libretro.LibretroBridge

@Composable
fun VirtualPad(
    visible: Boolean,
    onButton: (id: Int, pressed: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible) return
    val view = LocalView.current

    fun press(id: Int, down: Boolean) {
        if (down) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        onButton(id, down)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .alpha(0.92f)
            .padding(16.dp)
    ) {
        PadKey(
            label = "L",
            modifier = Modifier
                .align(Alignment.TopStart)
                .height(40.dp)
                .width(72.dp),
            shapeRound = false
        ) { press(LibretroBridge.BTN_L, it) }

        PadKey(
            label = "R",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .height(40.dp)
                .width(72.dp),
            shapeRound = false
        ) { press(LibretroBridge.BTN_R, it) }

        Column(
            modifier = Modifier.align(Alignment.BottomStart),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PadKey("▲", Modifier.size(52.dp)) { press(LibretroBridge.BTN_UP, it) }
            Row {
                PadKey("◀", Modifier.size(52.dp)) { press(LibretroBridge.BTN_LEFT, it) }
                Spacer(Modifier.width(52.dp))
                PadKey("▶", Modifier.size(52.dp)) { press(LibretroBridge.BTN_RIGHT, it) }
            }
            PadKey("▼", Modifier.size(52.dp)) { press(LibretroBridge.BTN_DOWN, it) }
        }

        Column(
            modifier = Modifier.align(Alignment.BottomEnd),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PadKey("X", Modifier.size(50.dp)) { press(LibretroBridge.BTN_X, it) }
            Row {
                PadKey("Y", Modifier.size(50.dp)) { press(LibretroBridge.BTN_Y, it) }
                Spacer(Modifier.width(50.dp))
                PadKey("A", Modifier.size(50.dp), Phosphor.copy(alpha = 0.55f)) {
                    press(LibretroBridge.BTN_A, it)
                }
            }
            PadKey("B", Modifier.size(50.dp), Phosphor.copy(alpha = 0.45f)) {
                press(LibretroBridge.BTN_B, it)
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp)
        ) {
            PadKey(
                "SELECT",
                Modifier
                    .height(36.dp)
                    .width(72.dp),
                shapeRound = false
            ) { press(LibretroBridge.BTN_SELECT, it) }
            Spacer(Modifier.width(18.dp))
            PadKey(
                "START",
                Modifier
                    .height(36.dp)
                    .width(72.dp),
                shapeRound = false
            ) { press(LibretroBridge.BTN_START, it) }
        }
    }
}

@Composable
private fun PadKey(
    label: String,
    modifier: Modifier = Modifier,
    color: Color = Bezel.copy(alpha = 0.85f),
    shapeRound: Boolean = true,
    onPress: (Boolean) -> Unit
) {
    val shape = if (shapeRound) CircleShape else RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(color)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onPress(true)
                        try {
                            awaitRelease()
                        } finally {
                            onPress(false)
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = if (label.length > 1) 11.sp else 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
