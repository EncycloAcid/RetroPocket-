package com.retropocket.data.scan

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.retropocket.data.db.GameEntity
import com.retropocket.data.model.ConsoleSystem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RomScanner(private val context: Context) {

    suspend fun scanTree(treeUri: Uri): List<GameEntity> = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext emptyList()
        val found = mutableListOf<GameEntity>()
        walk(root, found)
        found.distinctBy { it.uri }
    }

    private fun walk(dir: DocumentFile, out: MutableList<GameEntity>) {
        val children = dir.listFiles()
        for (child in children) {
            if (child.isDirectory) {
                walk(child, out)
                continue
            }
            if (!child.isFile) continue
            val name = child.name ?: continue
            val ext = name.substringAfterLast('.', missingDelimiterValue = "").lowercase()
            if (ext.isEmpty()) continue
            val system = ConsoleSystem.fromExtension(ext) ?: continue
            // Prefer .cue over raw .bin for PS1 to avoid duplicate noise when both exist
            if (system == ConsoleSystem.PS1 && ext == "bin") {
                val cueSibling = name.dropLast(3) + "cue"
                val hasCue = children.any { it.name.equals(cueSibling, ignoreCase = true) }
                if (hasCue) continue
            }
            val title = name.substringBeforeLast('.').replace('_', ' ').trim()
            out += GameEntity(
                title = title.ifBlank { name },
                systemId = system.id,
                uri = child.uri.toString(),
                fileName = name
            )
        }
    }
}
