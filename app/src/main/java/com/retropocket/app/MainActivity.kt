package com.retropocket.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.retropocket.app.nav.Routes
import com.retropocket.app.ui.bios.BiosScreen
import com.retropocket.app.ui.detail.DetailScreen
import com.retropocket.app.ui.home.HomeScreen
import com.retropocket.app.ui.library.LibraryScreen
import com.retropocket.app.ui.play.PlayScreen
import com.retropocket.app.ui.settings.SettingsScreen
import com.retropocket.app.ui.theme.RetroPocketTheme
import com.retropocket.data.model.ConsoleSystem
import com.retropocket.data.repository.GameRepository
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            RetroPocketTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val app = application as RetroPocketApp
                    RetroPocketNav(app.repository, onImmersive = { immersive ->
                        val controller = WindowInsetsControllerCompat(window, window.decorView)
                        if (immersive) {
                            controller.hide(WindowInsetsCompat.Type.systemBars())
                            controller.systemBarsBehavior =
                                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        } else {
                            controller.show(WindowInsetsCompat.Type.systemBars())
                            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        }
                    })
                }
            }
        }
    }
}

@Composable
private fun RetroPocketNav(
    repository: GameRepository,
    onImmersive: (Boolean) -> Unit
) {
    val nav = rememberNavController()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val games by repository.observeGames().collectAsState(initial = emptyList())
    val biosStatuses by repository.bios.statuses.collectAsState()
    var lastBiosImportOk by remember { mutableStateOf(false) }
    var gameCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(games) { gameCount = games.size }

    val treePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
        }
        scope.launch {
            repository.scanAndReplace(uri)
            nav.navigate(Routes.Library) {
                launchSingleTop = true
            }
        }
    }

    val biosPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = repository.bios.importBios(uri)
            lastBiosImportOk = result.isSuccess
        }
    }

    NavHost(navController = nav, startDestination = Routes.Home) {
        composable(Routes.Home) {
            onImmersive(false)
            HomeScreen(
                hasGames = games.isNotEmpty(),
                onAddGames = { treePicker.launch(null) },
                onOpenLibrary = { nav.navigate(Routes.Library) },
                onOpenSettings = { nav.navigate(Routes.Settings) }
            )
        }
        composable(Routes.Library) {
            onImmersive(false)
            LibraryScreen(
                games = games,
                onBack = { nav.popBackStack() },
                onOpenGame = { id -> nav.navigate(Routes.detail(id)) },
                onAddGames = { treePicker.launch(null) }
            )
        }
        composable(
            route = Routes.Detail,
            arguments = listOf(navArgument("gameId") { type = NavType.LongType })
        ) { entry ->
            onImmersive(false)
            val id = entry.arguments?.getLong("gameId") ?: return@composable
            val game by repository.observeGame(id).collectAsState(initial = null)
            val system = game?.let { ConsoleSystem.fromId(it.systemId) }
            val biosReady = system?.let { repository.bios.hasRequiredBios(it) } ?: true
            DetailScreen(
                game = game,
                biosReady = biosReady,
                onBack = { nav.popBackStack() },
                onPlay = {
                    scope.launch { repository.markPlayed(id) }
                    nav.navigate(Routes.play(id))
                },
                onOpenBios = { nav.navigate(Routes.Bios) }
            )
        }
        composable(
            route = Routes.Play,
            arguments = listOf(navArgument("gameId") { type = NavType.LongType })
        ) { entry ->
            onImmersive(true)
            val id = entry.arguments?.getLong("gameId") ?: return@composable
            val game by repository.observeGame(id).collectAsState(initial = null)
            PlayScreen(
                game = game,
                onExit = {
                    onImmersive(false)
                    nav.popBackStack()
                }
            )
        }
        composable(Routes.Settings) {
            onImmersive(false)
            SettingsScreen(
                onBack = { nav.popBackStack() },
                onOpenBios = { nav.navigate(Routes.Bios) },
                onRescan = {
                    scope.launch { repository.rescanSavedTree() }
                },
                gameCount = gameCount
            )
        }
        composable(Routes.Bios) {
            onImmersive(false)
            BiosScreen(
                statuses = biosStatuses,
                lastImportOk = lastBiosImportOk,
                onBack = { nav.popBackStack() },
                onImport = {
                    lastBiosImportOk = false
                    biosPicker.launch(arrayOf("*/*"))
                },
                onClear = {
                    scope.launch {
                        repository.bios.clearAll()
                        lastBiosImportOk = false
                    }
                }
            )
        }
    }
}
