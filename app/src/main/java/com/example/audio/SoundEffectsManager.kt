package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * Lightweight, zero-dependency offline audio synthesizer using Android PCM AudioTrack.
 * Safely catches any hardware/emulator exceptions so headless Robolectric tests never fail.
 */
object SoundEffectsManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private const val SAMPLE_RATE = 22050

    fun playStartupChime(enabled: Boolean) {
        if (!enabled) return
        scope.launch {
            // Pleasant 3-note company intro arpeggio (C5 -> E5 -> G5 -> C6)
            playToneSequence(
                notesHz = listOf(523.25 to 140, 659.25 to 140, 783.99 to 160, 1046.50 to 280),
                volume = 0.25
            )
        }
    }

    fun playSuccessChime(enabled: Boolean) {
        if (!enabled) return
        scope.launch {
            playToneSequence(
                notesHz = listOf(659.25 to 85, 880.0 to 130),
                volume = 0.22
            )
        }
    }

    fun playWarningTone(enabled: Boolean) {
        if (!enabled) return
        scope.launch {
            playToneSequence(
                notesHz = listOf(311.13 to 150, 233.08 to 200),
                volume = 0.25
            )
        }
    }

    fun playTapTick(enabled: Boolean) {
        if (!enabled) return
        scope.launch {
            playToneSequence(
                notesHz = listOf(740.0 to 32),
                volume = 0.12
            )
        }
    }

    private fun playToneSequence(
        notesHz: List<Pair<Double, Int>>,
        volume: Double
    ) {
        try {
            val totalSamples = notesHz.sumOf { (SAMPLE_RATE * it.second) / 1000 }
            if (totalSamples <= 0) return

            val pcm = ShortArray(totalSamples)
            var offset = 0

            for ((freqHz, durationMs) in notesHz) {
                val count = (SAMPLE_RATE * durationMs) / 1000
                for (i in 0 until count) {
                    val progress = i.toDouble() / count.coerceAtLeast(1)
                    // Smooth attack/decay envelope to avoid clicks
                    val attack = if (progress < 0.08) progress / 0.08 else 1.0
                    val release = if (progress > 0.72) (1.0 - progress) / 0.28 else 1.0
                    val envelope = attack * release
                    val angle = 2.0 * PI * freqHz * i / SAMPLE_RATE
                    val sample = (sin(angle) * volume * envelope * Short.MAX_VALUE).toInt()
                    pcm[offset + i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
                offset += count
            }

            val minBufBytes = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            if (minBufBytes <= 0) return

            val bufferBytes = maxOf(minBufBytes, pcm.size * 2)
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(bufferBytes)
                .build()

            try {
                track.write(pcm, 0, pcm.size)
                track.play()
                val totalMs = notesHz.sumOf { it.second }.toLong() + 60L
                Thread.sleep(totalMs)
            } finally {
                runCatching { track.stop() }
                runCatching { track.release() }
            }
        } catch (_: Throwable) {
            // Safe no-op in headless/test environments without audio hardware
        }
    }
}
