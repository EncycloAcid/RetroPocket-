package com.retropocket.libretro

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

data class SessionConfig(
    val coreLibrary: String,
    val romUri: Uri,
    val romDisplayName: String,
    val systemDir: File,
    val saveDir: File,
    val copyRomToCache: Boolean = true
)

sealed class SessionState {
    data object Idle : SessionState()
    data object Loading : SessionState()
    data class Running(val width: Int, val height: Int, val fps: Float) : SessionState()
    data class Error(val message: String) : SessionState()
}

class LibretroSession(private val context: Context) : LibretroBridge.Callbacks {
    private val bridge = LibretroBridge()
    private val audio = AudioPlayer()
    val frames = FrameBuffer()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loopJob: Job? = null
    private val running = AtomicBoolean(false)
    private val paused = AtomicBoolean(false)

    @Volatile
    var state: SessionState = SessionState.Idle
        private set

    var onStateChanged: ((SessionState) -> Unit)? = null

    init {
        bridge.nativeSetCallback(this)
    }

    suspend fun start(config: SessionConfig): Boolean = withContext(Dispatchers.IO) {
        stopInternal()
        publish(SessionState.Loading)

        config.systemDir.mkdirs()
        config.saveDir.mkdirs()
        bridge.nativeSetDirectories(config.systemDir.absolutePath, config.saveDir.absolutePath)

        val corePath = CoreLocator.resolve(context, config.coreLibrary)
        if (corePath == null) {
            publish(SessionState.Error("Core not found: ${config.coreLibrary}"))
            return@withContext false
        }
        if (!bridge.nativeLoadCore(corePath)) {
            publish(SessionState.Error("Failed to load core"))
            return@withContext false
        }

        val romFile = materializeRom(config)
            ?: run {
                publish(SessionState.Error("Unable to open ROM"))
                return@withContext false
            }

        if (!bridge.nativeLoadGame(romFile.absolutePath, null)) {
            publish(SessionState.Error("Core rejected ROM"))
            return@withContext false
        }

        val av = bridge.nativeGetAvInfo()
        val w = av[0].toInt().coerceAtLeast(1)
        val h = av[1].toInt().coerceAtLeast(1)
        val fps = av[2].coerceAtLeast(30f)
        val sampleRate = av[3].toInt().coerceAtLeast(22050)
        audio.configure(sampleRate)

        running.set(true)
        paused.set(false)
        publish(SessionState.Running(w, h, fps))
        startLoop(fps)
        true
    }

    fun setButton(id: Int, pressed: Boolean) {
        bridge.nativeSetButton(id, pressed)
    }

    fun pause(value: Boolean) {
        paused.set(value)
        if (value) audio.pause() else audio.resume()
    }

    fun reset() {
        bridge.nativeReset()
    }

    fun saveState(slotFile: File): Boolean {
        val bytes = bridge.nativeSerialize() ?: return false
        slotFile.parentFile?.mkdirs()
        slotFile.writeBytes(bytes)
        return true
    }

    fun loadState(slotFile: File): Boolean {
        if (!slotFile.exists()) return false
        return bridge.nativeUnserialize(slotFile.readBytes())
    }

    fun stop() {
        scope.launch { stopInternal() }
    }

    fun destroy() {
        stop()
        bridge.nativeSetCallback(null)
        scope.cancel()
    }

    override fun onVideoFrame(rgb565: ShortArray, width: Int, height: Int) {
        frames.publish(rgb565, width, height)
    }

    override fun onAudioSamples(samples: ShortArray) {
        if (!paused.get()) audio.write(samples)
    }

    private fun startLoop(fps: Float) {
        loopJob?.cancel()
        val frameNanos = (1_000_000_000L / fps).toLong().coerceAtLeast(1L)
        loopJob = scope.launch {
            var next = System.nanoTime()
            while (isActive && running.get()) {
                if (!paused.get()) {
                    bridge.nativeRunFrame()
                }
                next += frameNanos
                val sleep = next - System.nanoTime()
                if (sleep > 0) {
                    val ms = sleep / 1_000_000
                    val ns = (sleep % 1_000_000).toInt()
                    try {
                        Thread.sleep(ms, ns)
                    } catch (_: InterruptedException) {
                        break
                    }
                } else {
                    next = System.nanoTime()
                }
            }
        }
    }

    private fun stopInternal() {
        running.set(false)
        loopJob?.cancel()
        loopJob = null
        audio.release()
        bridge.nativeUnload()
        publish(SessionState.Idle)
    }

    private fun materializeRom(config: SessionConfig): File? {
        return try {
            val cacheDir = File(context.cacheDir, "roms/${config.romDisplayName.hashCode()}")
                .also {
                    it.deleteRecursively()
                    it.mkdirs()
                }
            val dest = File(cacheDir, config.romDisplayName.ifBlank { "game.rom" })
            context.contentResolver.openInputStream(config.romUri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            } ?: return null

            // For .cue sheets, copy sibling track files from the same folder when possible
            if (dest.extension.equals("cue", ignoreCase = true)) {
                copyCueSiblings(config.romUri, cacheDir)
            }
            dest
        } catch (_: Exception) {
            null
        }
    }

    private fun copyCueSiblings(cueUri: Uri, cacheDir: File) {
        val cueDoc = DocumentFile.fromSingleUri(context, cueUri) ?: return
        val parent = cueDoc.parentFile ?: return
        parent.listFiles().forEach { child ->
            val name = child.name ?: return@forEach
            if (name.equals(cueDoc.name, ignoreCase = true)) return@forEach
            val ext = name.substringAfterLast('.', "").lowercase()
            if (ext !in setOf("bin", "img", "iso", "raw", "wav", "mp3", "ape")) return@forEach
            val out = File(cacheDir, name)
            try {
                context.contentResolver.openInputStream(child.uri)?.use { input ->
                    out.outputStream().use { output -> input.copyTo(output) }
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun publish(newState: SessionState) {
        state = newState
        onStateChanged?.invoke(newState)
    }
}
