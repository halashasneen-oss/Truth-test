package com.nuvexa.truthtest.audio

data class VoiceAnalysis(
    val score: Int,
    val averagePitchHz: Double,
    val jitter: Double,
    val shimmer: Double,
    val pauseRatio: Double,
    val rms: Double,
    val usable: Boolean
)
