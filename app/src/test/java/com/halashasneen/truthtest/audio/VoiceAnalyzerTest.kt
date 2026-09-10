package com.halashasneen.truthtest.audio

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class VoiceAnalyzerTest {
    @Test fun silenceIsRejected() {
        val result = VoiceAnalyzer.analyze(ShortArray(AudioRecorderEngine.SAMPLE_RATE * 3))
        assertFalse(result.usable)
    }

    @Test fun voicedSignalProducesBoundedEntertainmentScore() {
        val rate = AudioRecorderEngine.SAMPLE_RATE
        val samples = ShortArray(rate * 3) { i ->
            val t = i.toDouble() / rate
            (sin(2.0 * PI * 180.0 * t) * 9000.0).toInt().toShort()
        }
        val result = VoiceAnalyzer.analyze(samples)
        assertTrue(result.usable)
        assertTrue(result.score in 24..99)
        assertTrue(result.averagePitchHz in 85.0..350.0)
    }

    @Test fun quietVoicedSignalIsAcceptedAfterAdaptiveGain() {
        val rate = AudioRecorderEngine.SAMPLE_RATE
        val samples = ShortArray(rate * 4) { i ->
            val t = i.toDouble() / rate
            // RMS is below the old 0.008 hard gate, matching quiet phone capture levels.
            (sin(2.0 * PI * 165.0 * t) * 220.0).toInt().toShort()
        }
        val result = VoiceAnalyzer.analyze(samples)
        assertTrue(result.usable)
        assertTrue(result.score in 24..99)
        assertTrue(result.averagePitchHz in 85.0..350.0)
    }

    @Test fun tinyNoiseDoesNotPassAdaptiveGainGate() {
        val rate = AudioRecorderEngine.SAMPLE_RATE
        val samples = ShortArray(rate * 3) { i -> if (i % 2 == 0) 8 else -8 }
        assertFalse(VoiceAnalyzer.analyze(samples).usable)
    }
}
