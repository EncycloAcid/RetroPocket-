package com.retropocket.data.model

enum class ConsoleSystem(
    val id: String,
    val displayName: String,
    val extensions: Set<String>,
    val coreLibrary: String,
    val requiresBios: Boolean,
    val accentHint: Long
) {
    GB(
        id = "gb",
        displayName = "Game Boy",
        extensions = setOf("gb", "gbc"),
        coreLibrary = "gambatte_libretro_android",
        requiresBios = false,
        accentHint = 0xFF8FBC8F
    ),
    GBA(
        id = "gba",
        displayName = "Game Boy Advance",
        extensions = setOf("gba"),
        coreLibrary = "mgba_libretro_android",
        requiresBios = false,
        accentHint = 0xFF6B8CFF
    ),
    NES(
        id = "nes",
        displayName = "NES",
        extensions = setOf("nes", "fds", "unf", "unif"),
        coreLibrary = "fceumm_libretro_android",
        requiresBios = false,
        accentHint = 0xFFE85D4C
    ),
    SNES(
        id = "snes",
        displayName = "SNES",
        extensions = setOf("sfc", "smc", "fig", "swc"),
        coreLibrary = "snes9x_libretro_android",
        requiresBios = false,
        accentHint = 0xFF9B7EDE
    ),
    PS1(
        id = "ps1",
        displayName = "PlayStation",
        extensions = setOf("cue", "bin", "img", "pbp", "chd", "iso"),
        coreLibrary = "pcsx_rearmed_libretro_android",
        requiresBios = true,
        accentHint = 0xFF4ECDC4
    );

    companion object {
        fun fromExtension(ext: String): ConsoleSystem? {
            val normalized = ext.lowercase().removePrefix(".")
            return entries.firstOrNull { normalized in it.extensions }
        }

        fun fromId(id: String): ConsoleSystem? = entries.firstOrNull { it.id == id }
    }
}
