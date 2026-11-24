package com.retropocket.libretro

import android.content.Context
import java.io.File

object CoreLocator {
    /**
     * Resolves a packaged native library path for a Libretro core.
     * Cores are shipped as lib{name}.so under jniLibs.
     */
    fun resolve(context: Context, coreLibraryName: String): String? {
        val libName = if (coreLibraryName.startsWith("lib")) {
            coreLibraryName.removePrefix("lib").removeSuffix(".so")
        } else {
            coreLibraryName.removeSuffix(".so")
        }
        val fileName = "lib$libName.so"
        val nativeDir = context.applicationInfo.nativeLibraryDir
        val candidate = File(nativeDir, fileName)
        if (candidate.exists()) return candidate.absolutePath

        // Fallback: extract from apk libs if needed
        context.applicationInfo.splitSourceDirs?.forEach { _ -> }
        val alt = File(nativeDir, "$libName.so")
        if (alt.exists()) return alt.absolutePath
        return null
    }
}
