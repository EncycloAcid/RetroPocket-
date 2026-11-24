package com.retropocket.app.ui.play

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.retropocket.libretro.FrameBuffer
import com.retropocket.libretro.VideoFrame
import kotlin.math.min

/**
 * Lightweight RGB565 frame presenter. Avoids GLSurfaceView complexity while
 * still delivering smooth enough playback for MVP (cores typically 240–320px).
 */
class GlFrameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var frameBuffer: FrameBuffer? = null
    private var bitmap: Bitmap? = null
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val ints = IntArray(512 * 512)

    fun bind(buffer: FrameBuffer) {
        frameBuffer = buffer
    }

    fun presentLatest() {
        val frame = frameBuffer?.peek() ?: return
        drawFrame(frame)
        invalidate()
    }

    private fun drawFrame(frame: VideoFrame) {
        val w = frame.width
        val h = frame.height
        if (w <= 0 || h <= 0) return
        val needed = w * h
        val px = if (ints.size < needed) IntArray(needed) else ints
        val src = frame.pixels
        val count = min(src.size, needed)
        for (i in 0 until count) {
            val c = src[i].toInt() and 0xFFFF
            val r = ((c ushr 11) and 0x1F) * 255 / 31
            val g = ((c ushr 5) and 0x3F) * 255 / 63
            val b = (c and 0x1F) * 255 / 31
            px[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
        val bmp = bitmap?.takeIf { it.width == w && it.height == h }
            ?: Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { bitmap = it }
        bmp.setPixels(px, 0, w, 0, 0, w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bmp = bitmap ?: return
        val scale = min(width.toFloat() / bmp.width, height.toFloat() / bmp.height)
        val dw = bmp.width * scale
        val dh = bmp.height * scale
        val left = (width - dw) / 2f
        val top = (height - dh) / 2f
        canvas.drawBitmap(
            bmp,
            null,
            android.graphics.RectF(left, top, left + dw, top + dh),
            paint
        )
    }
}
