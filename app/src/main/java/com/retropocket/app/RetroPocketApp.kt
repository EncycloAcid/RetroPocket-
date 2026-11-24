package com.retropocket.app

import android.app.Application
import com.retropocket.data.repository.GameRepository

class RetroPocketApp : Application() {
    lateinit var repository: GameRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = GameRepository(this)
    }
}
