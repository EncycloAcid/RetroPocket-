package com.retropocket.libretro

import java.util.concurrent.atomic.AtomicReference

data class VideoFrame(
    val pixels: ShortArray,
    val width: Int,
    val height: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VideoFrame) return false
        return width == other.width && height == other.height && pixels.contentEquals(other.pixels)
    }

    override fun hashCode(): Int {
        var result = pixels.contentHashCode()
        result = 31 * result + width
        result = 31 * result + height
        return result
    }
}

class FrameBuffer {
    private val latest = AtomicReference<VideoFrame?>(null)

    fun publish(rgb565: ShortArray, width: Int, height: Int) {
        latest.set(VideoFrame(rgb565.copyOf(), width, height))
    }

    fun consume(): VideoFrame? = latest.getAndSet(null)

    fun peek(): VideoFrame? = latest.get()
}
