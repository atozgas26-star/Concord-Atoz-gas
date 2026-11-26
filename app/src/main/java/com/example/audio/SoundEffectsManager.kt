package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

enum class StartupSoundStyle(val label: String, val subtitle: String) {
    ROYAL_FANFARE("Royal Fanfare", "Warm cinematic chord rise"),
    IPHONE_GLASS("iPhone Glass", "Crisp modern crystal chime"),
    IGNITION_PULSE("Ignition Pulse", "Deep industrial energy swell"),
    CLASSIC_CHIME("Classic Chime", "Bright ascending bell melody")
}

private data class ChordStep(
    val frequenciesHz: List<Double>,
    val durationMs: Int,
    val harmonicRichness: Double = 0.28,
    val decayRate: Double = 2.6
)

/**
 * Lightweight, zero-dependency offline harmonic audio synthesizer using Android PCM AudioTrack.
 * Synthesizes layered chords with smooth attack/decay envelopes and overtone harmonics,
 * and safely catches any hardware/emulator exceptions so headless Robolectric tests never fail.
 */
object SoundEffectsManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private const val SAMPLE_RATE = 22050
    @Volatile
    private var activeStartupJob: Job? = null
    @Volatile
    private var activeStartupTrack: AudioTrack? = null

    fun stopStartupSound() {
        activeStartupJob?.cancel()
        activeStartupJob = null
        val track = activeStartupTrack
        activeStartupTrack = null
        if (track != null) {
            runCatching { track.stop() }
            runCatching { track.release() }
        }
    }

    fun playStartupChime(
        enabled: Boolean,
        style: StartupSoundStyle = StartupSoundStyle.ROYAL_FANFARE
    ) {
        stopStartupSound()
        if (!enabled) return
        activeStartupJob = scope.launch {
            val sequence = when (style) {
                StartupSoundStyle.ROYAL_FANFARE -> listOf(
                    // Warm orchestral D-major opening fanfare (~4.2s cinematic arc)
                    ChordStep(listOf(293.66, 440.00), 420, harmonicRichness = 0.34, decayRate = 2.0),
                    ChordStep(listOf(369.99, 587.33), 420, harmonicRichness = 0.34, decayRate = 2.0),
                    ChordStep(listOf(440.00, 659.25), 460, harmonicRichness = 0.36, decayRate = 1.9),
                    ChordStep(listOf(293.66, 440.00, 587.33, 739.99), 780, harmonicRichness = 0.40, decayRate = 1.6),
                    ChordStep(listOf(329.63, 493.88, 659.25, 880.00), 540, harmonicRichness = 0.35, decayRate = 1.8),
                    ChordStep(listOf(293.66, 587.33, 739.99, 880.00, 1174.66), 1580, harmonicRichness = 0.42, decayRate = 1.35)
                )
                StartupSoundStyle.IPHONE_GLASS -> listOf(
                    // Modern crisp iOS-inspired glass marimba cascade (~3.4s)
                    ChordStep(listOf(659.25, 1318.51), 220, harmonicRichness = 0.22, decayRate = 4.2),
                    ChordStep(listOf(830.61, 1661.22), 220, harmonicRichness = 0.22, decayRate = 4.2),
                    ChordStep(listOf(987.77, 1975.53), 260, harmonicRichness = 0.24, decayRate = 3.8),
                    ChordStep(listOf(659.25, 987.77, 1318.51), 540, harmonicRichness = 0.26, decayRate = 2.6),
                    ChordStep(listOf(830.61, 1244.51, 1661.22), 540, harmonicRichness = 0.26, decayRate = 2.6),
                    ChordStep(listOf(659.25, 987.77, 1318.51, 1975.53), 1500, harmonicRichness = 0.28, decayRate = 1.7)
                )
                StartupSoundStyle.IGNITION_PULSE -> listOf(
                    // Deep industrial bass ignition rising into crisp crimson-gold signature chord (~3.8s)
                    ChordStep(listOf(110.00, 220.00), 520, harmonicRichness = 0.45, decayRate = 1.4),
                    ChordStep(listOf(146.83, 293.66, 440.00), 520, harmonicRichness = 0.42, decayRate = 1.6),
                    ChordStep(listOf(164.81, 329.63, 493.88), 520, harmonicRichness = 0.40, decayRate = 1.7),
                    ChordStep(listOf(220.00, 440.00, 554.37, 659.25), 680, harmonicRichness = 0.36, decayRate = 1.8),
                    ChordStep(listOf(220.00, 440.00, 659.25, 880.00, 1108.73), 1560, harmonicRichness = 0.38, decayRate = 1.3)
                )
                StartupSoundStyle.CLASSIC_CHIME -> listOf(
                    // Refined C-major ascending crystal bell arpeggio (~3.0s)
                    ChordStep(listOf(523.25), 260, harmonicRichness = 0.24, decayRate = 3.2),
                    ChordStep(listOf(659.25), 260, harmonicRichness = 0.24, decayRate = 3.2),
                    ChordStep(listOf(783.99), 280, harmonicRichness = 0.26, decayRate = 3.0),
                    ChordStep(listOf(987.77), 320, harmonicRichness = 0.26, decayRate = 2.8),
                    ChordStep(listOf(523.25, 659.25, 783.99, 1046.50), 1650, harmonicRichness = 0.32, decayRate = 1.5)
                )
            }
            playChordSequence(
                steps = sequence,
                volume = 0.30,
                isStartupTrack = true,
                shouldContinue = { isActive }
            )
        }
    }

    fun playSuccessChime(enabled: Boolean) {
        if (!enabled) return
        scope.launch {
            playChordSequence(
                steps = listOf(
                    ChordStep(listOf(659.25, 1318.51), 90, harmonicRichness = 0.22, decayRate = 4.0),
                    ChordStep(listOf(880.0, 1318.51), 160, harmonicRichness = 0.25, decayRate = 3.2)
                ),
                volume = 0.22
            )
        }
    }

    fun playWarningTone(enabled: Boolean) {
        if (!enabled) return
        scope.launch {
            playChordSequence(
                steps = listOf(
                    ChordStep(listOf(311.13), 150, harmonicRichness = 0.28, decayRate = 2.5),
                    ChordStep(listOf(233.08), 210, harmonicRichness = 0.28, decayRate = 2.2)
                ),
                volume = 0.25
            )
        }
    }

    fun playTapTick(enabled: Boolean) {
        if (!enabled) return
        scope.launch {
            playChordSequence(
                steps = listOf(
                    ChordStep(listOf(780.0), 28, harmonicRichness = 0.15, decayRate = 5.0)
                ),
                volume = 0.12
            )
        }
    }

    /**
     * Distinct crisp double-ring bell reminder sound for urgent notifications,
     * extra cylinder vehicle transfers, store approval requests, and important chat alerts.
     */
    fun playNotificationBell(enabled: Boolean) {
        if (!enabled) return
        scope.launch {
            playChordSequence(
                steps = listOf(
                    ChordStep(listOf(1046.50, 1567.98, 2093.00), 170, harmonicRichness = 0.30, decayRate = 3.4),
                    ChordStep(listOf(1318.51, 1975.53, 2637.02), 340, harmonicRichness = 0.32, decayRate = 2.4)
                ),
                volume = 0.28
            )
        }
    }

    fun playChatMessageTone(enabled: Boolean) {
        if (!enabled) return
        scope.launch {
            playChordSequence(
                steps = listOf(
                    ChordStep(listOf(783.99, 1174.66), 75, harmonicRichness = 0.20, decayRate = 4.5),
                    ChordStep(listOf(1046.50, 1567.98), 115, harmonicRichness = 0.22, decayRate = 3.8)
                ),
                volume = 0.20
            )
        }
    }

    private fun playChordSequence(
        steps: List<ChordStep>,
        volume: Double,
        isStartupTrack: Boolean = false,
        shouldContinue: () -> Boolean = { true }
    ) {
        try {
            val totalSamples = steps.sumOf { (SAMPLE_RATE * it.durationMs) / 1000 }
            if (totalSamples <= 0) return

            val pcm = ShortArray(totalSamples)
            var offset = 0

            for (step in steps) {
                val count = (SAMPLE_RATE * step.durationMs) / 1000
                val voiceCount = step.frequenciesHz.size.coerceAtLeast(1)
                for (i in 0 until count) {
                    val progress = i.toDouble() / count.coerceAtLeast(1)
                    // Smooth attack + natural exponential acoustic bell/pad decay + soft tail
                    val attack = if (progress < 0.04) progress / 0.04 else 1.0
                    val naturalDecay = exp(-step.decayRate * progress)
                    val tailFade = if (progress > 0.85) (1.0 - progress) / 0.15 else 1.0
                    val envelope = attack * (0.28 + 0.72 * naturalDecay) * tailFade

                    var mixed = 0.0
                    for (freqHz in step.frequenciesHz) {
                        val t = i.toDouble() / SAMPLE_RATE
                        val fundamental = sin(2.0 * PI * freqHz * t)
                        val secondHarmonic = sin(2.0 * PI * (freqHz * 2.0) * t) * step.harmonicRichness
                        val thirdHarmonic = sin(2.0 * PI * (freqHz * 3.0) * t) * (step.harmonicRichness * 0.35)
                        mixed += (fundamental + secondHarmonic + thirdHarmonic) / (1.0 + step.harmonicRichness * 1.35)
                    }
                    val normalized = mixed / voiceCount
                    val sample = (normalized * volume * envelope * Short.MAX_VALUE).toInt()
                    pcm[offset + i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
                offset += count
            }

            if (!shouldContinue()) return

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
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
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

            if (isStartupTrack) {
                activeStartupTrack = track
            }

            try {
                track.write(pcm, 0, pcm.size)
                if (!shouldContinue()) return
                track.play()
                val totalMs = steps.sumOf { it.durationMs }.toLong() + 80L
                val sliceMs = 50L
                var elapsed = 0L
                while (elapsed < totalMs && shouldContinue()) {
                    Thread.sleep(minOf(sliceMs, totalMs - elapsed))
                    elapsed += sliceMs
                }
            } finally {
                if (isStartupTrack && activeStartupTrack === track) {
                    activeStartupTrack = null
                }
                runCatching { track.stop() }
                runCatching { track.release() }
            }
        } catch (_: Throwable) {
            // Safe no-op in headless/test environments without audio hardware
        }
    }
}
