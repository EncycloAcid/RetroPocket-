package com.retropocket.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM games ORDER BY title COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE systemId = :systemId ORDER BY title COLLATE NOCASE ASC")
    fun observeBySystem(systemId: String): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE id = :id")
    fun observeById(id: Long): Flow<GameEntity?>

    @Query("SELECT * FROM games WHERE id = :id")
    suspend fun getById(id: Long): GameEntity?

    @Query(
        """
        SELECT * FROM games
        WHERE title LIKE '%' || :query || '%' OR fileName LIKE '%' || :query || '%'
        ORDER BY title COLLATE NOCASE ASC
        """
    )
    fun search(query: String): Flow<List<GameEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(games: List<GameEntity>)

    @Query("DELETE FROM games")
    suspend fun clearAll()

    @Query("UPDATE games SET lastPlayedAt = :timestamp WHERE id = :id")
    suspend fun markPlayed(id: Long, timestamp: Long)

    @Query("UPDATE games SET favorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("SELECT COUNT(*) FROM games")
    suspend fun count(): Int
}
