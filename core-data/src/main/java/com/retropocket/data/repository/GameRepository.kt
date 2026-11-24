package com.retropocket.data.repository

import android.content.Context
import android.net.Uri
import com.retropocket.data.bios.BiosManager
import com.retropocket.data.db.GameEntity
import com.retropocket.data.db.RetroPocketDatabase
import com.retropocket.data.prefs.UserPrefs
import com.retropocket.data.scan.RomScanner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class GameRepository(context: Context) {
    private val appContext = context.applicationContext
    private val db = RetroPocketDatabase.get(appContext)
    private val dao = db.gameDao()
    private val scanner = RomScanner(appContext)
    val prefs = UserPrefs(appContext)
    val bios = BiosManager(appContext)

    fun observeGames(): Flow<List<GameEntity>> = dao.observeAll()

    fun observeGamesBySystem(systemId: String): Flow<List<GameEntity>> =
        dao.observeBySystem(systemId)

    fun observeGame(id: Long): Flow<GameEntity?> = dao.observeById(id)

    fun search(query: String): Flow<List<GameEntity>> = dao.search(query)

    suspend fun getGame(id: Long): GameEntity? = dao.getById(id)

    suspend fun markPlayed(id: Long) = dao.markPlayed(id, System.currentTimeMillis())

    suspend fun setFavorite(id: Long, favorite: Boolean) = dao.setFavorite(id, favorite)

    suspend fun scanAndReplace(treeUri: Uri): Int {
        val games = scanner.scanTree(treeUri)
        dao.clearAll()
        if (games.isNotEmpty()) dao.upsertAll(games)
        prefs.setRomTreeUri(treeUri.toString())
        return games.size
    }

    suspend fun rescanSavedTree(): Int? {
        val uriString = prefs.romTreeUri.first() ?: return null
        return scanAndReplace(Uri.parse(uriString))
    }

    suspend fun gameCount(): Int = dao.count()
}
