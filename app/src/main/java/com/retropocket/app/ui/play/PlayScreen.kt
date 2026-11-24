package com.retropocket.app.ui.play

import android.view.KeyEvent
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.retropocket.app.ui.theme.Charcoal
import com.retropocket.app.ui.theme.CharcoalElevated
import com.retropocket.app.ui.theme.Phosphor
import com.retropocket.data.db.GameEntity
import com.retropocket.data.model.ConsoleSystem
import com.retropocket.libretro.LibretroBridge
import com.retropocket.libretro.LibretroSession
import com.retropocket.libretro.SessionConfig
import com.retropocket.libretro.SessionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayScreen(
    game: GameEntity?,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val session = remember { LibretroSession(context) }
    var state by remember { mutableStateOf<SessionState>(SessionState.Idle) }
    var showPad by remember { mutableStateOf(true) }
    var paused by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    val frameView = remember { mutableStateOf<GlFrameView?>(null) }

    DisposableEffect(Unit) {
        session.onStateChanged = { state = it }
        onDispose {
            session.destroy()
        }
    }

    LaunchedEffect(game?.id) {
        val g = game ?: return@LaunchedEffect
        val system = ConsoleSystem.fromId(g.systemId) ?: return@LaunchedEffect
        val ok = session.start(
            SessionConfig(
                coreLibrary = system.coreLibrary,
                romUri = android.net.Uri.parse(g.uri),
                romDisplayName = g.fileName,
                systemDir = File(context.filesDir, "bios"),
                saveDir = File(context.filesDir, "saves/${system.id}").also { it.mkdirs() }
            )
        )
        if (!ok) statusMessage = (session.state as? SessionState.Error)?.message
    }

    LaunchedEffect(state) {
        while (isActive && state is SessionState.Running) {
            frameView.value?.presentLatest()
            delay(8L)
        }
    }

    BackHandler {
        if (showMenu) showMenu = false else {
            paused = true
            session.pause(true)
            showMenu = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                GlFrameView(ctx).also {
                    it.bind(session.frames)
                    frameView.value = it
                    it.isFocusable = true
                    it.isFocusableInTouchMode = true
                    it.requestFocus()
                    it.setOnKeyListener { _, keyCode, event ->
                        handleGameKey(keyCode, event, session)
                    }
                    // Detect physical pad presence roughly via first key
                    it.setOnGenericMotionListener { _, _ ->
                        showPad = false
                        false
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        AnimatedVisibility(
            visible = showPad && state is SessionState.Running && !paused,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            VirtualPad(
                visible = true,
                onButton = { id, pressed -> session.setButton(id, pressed) }
            )
        }

        when (state) {
            is SessionState.Loading, SessionState.Idle -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Phosphor
                )
            }
            is SessionState.Error -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text((state as SessionState.Error).message, color = Color.White)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onExit) { Text("Back") }
                }
            }
            else -> Unit
        }

        TextButton(
            onClick = {
                paused = true
                session.pause(true)
                showMenu = true
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        ) {
            Text("Menu", color = Phosphor)
        }
    }

    if (showMenu) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = {
                showMenu = false
                paused = false
                session.pause(false)
            },
            sheetState = sheetState,
            containerColor = CharcoalElevated
        ) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text("Paused", style = MaterialTheme.typography.headlineMedium, color = Phosphor)
                Spacer(Modifier.height(12.dp))
                MenuAction("Resume") {
                    showMenu = false
                    paused = false
                    session.pause(false)
                }
                MenuAction("Save state") {
                    val slot = File(context.filesDir, "states/${game?.id}_slot0.state")
                    val ok = session.saveState(slot)
                    statusMessage = if (ok) "State saved" else "Save failed"
                }
                MenuAction("Load state") {
                    val slot = File(context.filesDir, "states/${game?.id}_slot0.state")
                    val ok = session.loadState(slot)
                    statusMessage = if (ok) "State loaded" else "No save state"
                }
                MenuAction("Reset") { session.reset() }
                MenuAction(if (showPad) "Hide touch controls" else "Show touch controls") {
                    showPad = !showPad
                }
                MenuAction("Quit") {
                    session.stop()
                    onExit()
                }
                statusMessage?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun MenuAction(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Phosphor, contentColor = Charcoal)
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

private fun handleGameKey(keyCode: Int, event: KeyEvent, session: LibretroSession): Boolean {
    val pressed = event.action == KeyEvent.ACTION_DOWN
    val id = when (keyCode) {
        KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_W -> LibretroBridge.BTN_UP
        KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_S -> LibretroBridge.BTN_DOWN
        KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_A -> LibretroBridge.BTN_LEFT
        KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_D -> LibretroBridge.BTN_RIGHT
        KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_SPACE -> LibretroBridge.BTN_A
        KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_SHIFT_LEFT -> LibretroBridge.BTN_B
        KeyEvent.KEYCODE_BUTTON_X -> LibretroBridge.BTN_X
        KeyEvent.KEYCODE_BUTTON_Y -> LibretroBridge.BTN_Y
        KeyEvent.KEYCODE_BUTTON_L1 -> LibretroBridge.BTN_L
        KeyEvent.KEYCODE_BUTTON_R1 -> LibretroBridge.BTN_R
        KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_ENTER -> LibretroBridge.BTN_START
        KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_TAB -> LibretroBridge.BTN_SELECT
        else -> return false
    }
    if (event.action == KeyEvent.ACTION_DOWN || event.action == KeyEvent.ACTION_UP) {
        session.setButton(id, pressed)
        return true
    }
    return false
}
