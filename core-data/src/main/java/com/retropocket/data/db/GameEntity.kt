package com.retropocket.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "games",
    indices = [Index(value = ["uri"], unique = true), Index(value = ["systemId"])]
)
data class GameEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val systemId: String,
    val uri: String,
    val fileName: String,
    val lastPlayedAt: Long? = null,
    val favorite: Boolean = false
)
