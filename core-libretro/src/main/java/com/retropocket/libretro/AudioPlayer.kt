package com.retropocket.libretro

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.atomic.AtomicBoolean

class AudioPlayer {
    private var track: AudioTrack? = null
    private val started = AtomicBoolean(false)

    fun configure(sampleRate: Int) {
        release()
        val rate = sampleRate.coerceIn(8000, 96000)
        val minBuf = AudioTrack.getMinBufferSize(
            rate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(rate / 10)
        track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(rate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(minBuf * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        track?.play()
        started.set(true)
    }

    fun write(samples: ShortArray) {
        if (!started.get()) return
        track?.write(samples, 0, samples.size)
    }

    fun pause() {
        track?.pause()
    }

    fun resume() {
        track?.play()
    }

    fun release() {
        started.set(false)
        try {
            track?.stop()
        } catch (_: Exception) {
        }
        track?.release()
        track = null
    }
}
