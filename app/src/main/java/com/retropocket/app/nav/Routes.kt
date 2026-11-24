package com.retropocket.app.nav

object Routes {
    const val Home = "home"
    const val Library = "library"
    const val Detail = "detail/{gameId}"
    const val Play = "play/{gameId}"
    const val Settings = "settings"
    const val Bios = "bios"

    fun detail(gameId: Long) = "detail/$gameId"
    fun play(gameId: Long) = "play/$gameId"
}
