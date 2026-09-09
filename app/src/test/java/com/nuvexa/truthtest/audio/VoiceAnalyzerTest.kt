package com.nuvexa.truthtest.audio

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
}
