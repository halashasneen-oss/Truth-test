package com.halashasneen.truthtest.share

import com.halashasneen.truthtest.R
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Soundtrack choices for exported result videos.
 * All audio is generated procedurally on-device; recorded microphone audio is never reused.
 */
enum class ShareSound(
    val storageKey: String,
    val labelRes: Int,
    val emoji: String
) {
    NEON_BEAT("neon_beat", R.string.sound_neon_beat, "🎵"),
    MINIMAL_PULSE("minimal_pulse", R.string.sound_minimal_pulse, "✨"),
    OFF("off", R.string.sound_off, "🔇");

    val enabled: Boolean
        get() = this != OFF

    companion object {
        fun fromStorage(value: String?): ShareSound =
            entries.firstOrNull { it.storageKey == value } ?: NEON_BEAT
    }
}

/** Pure deterministic synth used by the AAC exporter and JVM tests. */
object ProceduralSoundSynth {
    fun sample(
        style: ShareSound,
        sampleIndex: Long,
        sampleRate: Int = 44_100,
        durationSeconds: Double = 7.0
    ): Float {
        if (style == ShareSound.OFF) return 0f
        val t = sampleIndex.toDouble() / sampleRate.toDouble()
        if (t < 0.0 || t >= durationSeconds) return 0f

        val fadeIn = (t / 0.18).coerceIn(0.0, 1.0)
        val fadeOut = ((durationSeconds - t) / 0.38).coerceIn(0.0, 1.0)
        val envelope = fadeIn * fadeOut

        val raw = when (style) {
            ShareSound.NEON_BEAT -> neonBeat(t)
            ShareSound.MINIMAL_PULSE -> minimalPulse(t)
            ShareSound.OFF -> 0.0
        }
        return (raw * envelope).coerceIn(-0.82, 0.82).toFloat()
    }

    private fun neonBeat(t: Double): Double {
        val beatSeconds = 60.0 / 112.0
        val beatPhase = t % beatSeconds
        val kickEnvelope = exp(-beatPhase * 15.0)
        val kick = sin(2.0 * PI * 72.0 * t) * kickEnvelope * 0.34

        val halfBeat = beatSeconds / 2.0
        val hatPhase = t % halfBeat
        val hat = sin(2.0 * PI * 2_450.0 * t) * exp(-hatPhase * 42.0) * 0.025

        val roots = doubleArrayOf(220.00, 174.61, 196.00, 164.81)
        val root = roots[((t / 1.75).toInt()).coerceIn(0, roots.lastIndex)]
        val pad = (
            sin(2.0 * PI * root * t) * 0.055 +
                sin(2.0 * PI * root * 1.5 * t) * 0.032 +
                sin(2.0 * PI * root * 2.0 * t) * 0.022
            )

        val reveal = toneBurst(t, 4.45, 659.25, 0.58, 0.16) +
            toneBurst(t, 4.68, 880.00, 0.42, 0.11)
        val finish = toneBurst(t, 6.05, 783.99, 0.52, 0.12) +
            toneBurst(t, 6.22, 987.77, 0.46, 0.10)

        return kick + hat + pad + reveal + finish
    }

    private fun minimalPulse(t: Double): Double {
        val pulseStarts = doubleArrayOf(0.45, 1.85, 3.15, 4.55, 5.95)
        var pulses = 0.0
        for (start in pulseStarts) {
            pulses += toneBurst(t, start, 392.00, 0.34, 0.09)
            pulses += toneBurst(t, start + 0.08, 523.25, 0.28, 0.065)
        }
        val airyBed = sin(2.0 * PI * 146.83 * t) * 0.018 +
            sin(2.0 * PI * 220.00 * t) * 0.013
        return pulses + airyBed
    }

    private fun toneBurst(
        t: Double,
        start: Double,
        frequency: Double,
        decay: Double,
        gain: Double
    ): Double {
        if (t < start) return 0.0
        val local = t - start
        if (local > 0.9) return 0.0
        return sin(2.0 * PI * frequency * local) * exp(-local / decay) * gain
    }
}
