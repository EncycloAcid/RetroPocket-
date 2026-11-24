package com.retropocket.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [GameEntity::class], version = 1, exportSchema = false)
abstract class RetroPocketDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao

    companion object {
        @Volatile private var instance: RetroPocketDatabase? = null

        fun get(context: Context): RetroPocketDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    RetroPocketDatabase::class.java,
                    "retropocket.db"
                ).build().also { instance = it }
            }
        }
    }
}
