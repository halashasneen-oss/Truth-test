package com.nuvexa.truthtest.audio

import org.jtransforms.fft.DoubleFFT_1D
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sqrt

object VoiceAnalyzer {
    private const val FRAME = 2048
    private const val HOP = 1024

    fun analyze(samples: ShortArray, sampleRate: Int = AudioRecorderEngine.SAMPLE_RATE): VoiceAnalysis {
        if (samples.size < sampleRate * 2) return empty(false)

        val normalized = DoubleArray(samples.size) { samples[it] / 32768.0 }
        val overallRms = sqrt(normalized.sumOf { it * it } / normalized.size)
        if (overallRms < 0.008) return empty(false, overallRms)

        val pitches = mutableListOf<Double>()
        val energies = mutableListOf<Double>()
        var offset = 0
        while (offset + FRAME <= normalized.size) {
            val frame = DoubleArray(FRAME) { i ->
                val window = 0.5 - 0.5 * cos(2.0 * PI * i / (FRAME - 1))
                normalized[offset + i] * window
            }
            val rms = sqrt(frame.sumOf { it * it } / FRAME)
            energies += rms
            if (rms > 0.006) estimatePitch(frame, sampleRate)?.let(pitches::add)
            offset += HOP
        }

        val meanPitch = pitches.averageOrZero()
        val jitter = if (pitches.size > 1 && meanPitch > 0) {
            pitches.zipWithNext().map { (a, b) -> abs(b - a) }.average() / meanPitch
        } else 0.25

        val voicedEnergy = energies.filter { it > 0.006 }
        val meanEnergy = voicedEnergy.averageOrZero()
        val shimmer = if (voicedEnergy.size > 1 && meanEnergy > 0) {
            voicedEnergy.zipWithNext().map { (a, b) -> abs(b - a) }.average() / meanEnergy
        } else 0.5

        val pauseRatio = if (energies.isEmpty()) 1.0 else energies.count { it < 0.006 }.toDouble() / energies.size
        val pitchCv = if (pitches.size > 2 && meanPitch > 0) {
            val variance = pitches.sumOf { (it - meanPitch) * (it - meanPitch) } / pitches.size
            sqrt(variance) / meanPitch
        } else 0.35

        // Entertainment-only consistency score. It intentionally does not claim to detect deception.
        val penalty =
            (jitter / 0.12).coerceIn(0.0, 1.0) * 20.0 +
            (shimmer / 0.55).coerceIn(0.0, 1.0) * 18.0 +
            (pauseRatio / 0.65).coerceIn(0.0, 1.0) * 14.0 +
            (pitchCv / 0.35).coerceIn(0.0, 1.0) * 14.0
        val energyBonus = ((overallRms - 0.015) / 0.08).coerceIn(0.0, 1.0) * 6.0
        val score = (94.0 - penalty + energyBonus).roundToInt().coerceIn(24, 99)

        return VoiceAnalysis(score, meanPitch, jitter, shimmer, pauseRatio, overallRms, true)
    }

    private fun estimatePitch(frame: DoubleArray, sampleRate: Int): Double? {
        val fftData = DoubleArray(FRAME * 2)
        for (i in frame.indices) fftData[i] = frame[i]
        DoubleFFT_1D(FRAME.toLong()).realForwardFull(fftData)
        val minBin = (85.0 * FRAME / sampleRate).toInt().coerceAtLeast(1)
        val maxBin = (350.0 * FRAME / sampleRate).toInt().coerceAtMost(FRAME / 2 - 1)
        var bestBin = -1
        var bestMagnitude = 0.0
        for (bin in minBin..maxBin) {
            val re = fftData[2 * bin]
            val im = fftData[2 * bin + 1]
            val magnitude = re * re + im * im
            if (magnitude > bestMagnitude) {
                bestMagnitude = magnitude
                bestBin = bin
            }
        }
        return if (bestBin > 0 && bestMagnitude > 1e-6) bestBin.toDouble() * sampleRate / FRAME else null
    }

    private fun List<Double>.averageOrZero() = if (isEmpty()) 0.0 else average()

    private fun empty(usable: Boolean, rms: Double = 0.0) =
        VoiceAnalysis(0, 0.0, 0.0, 0.0, 1.0, rms, usable)
}
