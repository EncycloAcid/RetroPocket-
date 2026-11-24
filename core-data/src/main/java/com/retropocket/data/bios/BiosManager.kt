package com.retropocket.data.bios

import android.content.Context
import android.net.Uri
import com.retropocket.data.model.ConsoleSystem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

data class BiosStatus(
    val system: ConsoleSystem,
    val present: Boolean,
    val fileName: String? = null,
    val sha1: String? = null
)

/**
 * Stores user-imported BIOS in app-private storage. Never ships copyrighted BIOS.
 */
class BiosManager(context: Context) {
    private val appContext = context.applicationContext
    private val biosDir = File(appContext.filesDir, "bios").also { it.mkdirs() }

    private val _statuses = MutableStateFlow(scanStatuses())
    val statuses: StateFlow<List<BiosStatus>> = _statuses.asStateFlow()

    fun biosDirectory(): File = biosDir

    fun hasRequiredBios(system: ConsoleSystem): Boolean {
        if (!system.requiresBios) return true
        return ps1Candidates().any { File(biosDir, it).exists() }
    }

    fun refresh() {
        _statuses.value = scanStatuses()
    }

    suspend fun importBios(uri: Uri, preferredName: String? = null): Result<BiosStatus> =
        withContext(Dispatchers.IO) {
            runCatching {
                val name = preferredName
                    ?: queryDisplayName(uri)
                    ?: "bios_${System.currentTimeMillis()}.bin"
                val dest = File(biosDir, name)
                appContext.contentResolver.openInputStream(uri)?.use { input ->
                    dest.outputStream().use { output -> input.copyTo(output) }
                } ?: error("Unable to open BIOS file")
                val sha1 = sha1Of(dest)
                refresh()
                BiosStatus(ConsoleSystem.PS1, true, dest.name, sha1)
            }
        }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        biosDir.listFiles()?.forEach { it.delete() }
        refresh()
    }

    private fun scanStatuses(): List<BiosStatus> {
        val ps1File = ps1Candidates().map { File(biosDir, it) }.firstOrNull { it.exists() }
        return listOf(
            BiosStatus(
                system = ConsoleSystem.PS1,
                present = ps1File != null,
                fileName = ps1File?.name,
                sha1 = ps1File?.let { sha1Of(it) }
            )
        )
    }

    private fun ps1Candidates(): List<String> = listOf(
        "scph1001.bin",
        "SCPH1001.BIN",
        "scph5501.bin",
        "SCPH5501.BIN",
        "scph7001.bin",
        "SCPH7001.BIN",
        "psxonpsp660.bin",
        "PSXONPSP660.BIN"
    )

    private fun queryDisplayName(uri: Uri): String? {
        val cursor = appContext.contentResolver.query(uri, null, null, null, null) ?: return null
        cursor.use {
            val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && it.moveToFirst()) return it.getString(index)
        }
        return null
    }

    private fun sha1Of(file: File): String {
        val digest = MessageDigest.getInstance("SHA-1")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
