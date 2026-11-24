package com.retropocket.libretro

/**
 * JNI façade over a dynamically loaded Libretro core.
 */
class LibretroBridge {
    interface Callbacks {
        fun onVideoFrame(rgb565: ShortArray, width: Int, height: Int)
        fun onAudioSamples(samples: ShortArray)
    }

    external fun nativeSetCallback(callback: Callbacks?)
    external fun nativeSetDirectories(systemDir: String, saveDir: String)
    external fun nativeLoadCore(corePath: String): Boolean
    external fun nativeLoadGame(path: String, romBytes: ByteArray?): Boolean
    external fun nativeRunFrame()
    external fun nativeReset()
    external fun nativeUnload()
    external fun nativeSetButton(id: Int, pressed: Boolean)
    external fun nativeGetAvInfo(): FloatArray
    external fun nativeSerialize(): ByteArray?
    external fun nativeUnserialize(data: ByteArray): Boolean

    companion object {
        init {
            System.loadLibrary("retropocket_jni")
        }

        // Libretro joypad ids
        const val BTN_B = 0
        const val BTN_Y = 1
        const val BTN_SELECT = 2
        const val BTN_START = 3
        const val BTN_UP = 4
        const val BTN_DOWN = 5
        const val BTN_LEFT = 6
        const val BTN_RIGHT = 7
        const val BTN_A = 8
        const val BTN_X = 9
        const val BTN_L = 10
        const val BTN_R = 11
        const val BTN_L2 = 12
        const val BTN_R2 = 13
        const val BTN_L3 = 14
        const val BTN_R3 = 15
    }
}
