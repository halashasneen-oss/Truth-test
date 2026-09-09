package com.nuvexa.truthtest.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlin.concurrent.thread
import kotlin.math.sqrt

class AudioRecorderEngine(
    private val sampleRate: Int = SAMPLE_RATE
) {
    private var recorder: AudioRecord? = null
    private var recordingThread: Thread? = null
    @Volatile private var recording = false
    private val samples = mutableListOf<Short>()

    fun start(onAmplitude: (Float) -> Unit) {
        if (recording) return
        val minBuffer = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = maxOf(minBuffer, 2048)
        recorder = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize * 2
        )
        samples.clear()
        recorder?.startRecording()
        recording = true
        recordingThread = thread(name = "truth-test-recorder") {
            val buffer = ShortArray(bufferSize)
            while (recording) {
                val read = recorder?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                    synchronized(samples) {
                        for (i in 0 until read) samples.add(buffer[i])
                    }
                    var sum = 0.0
                    for (i in 0 until read) {
                        val normalized = buffer[i] / 32768.0
                        sum += normalized * normalized
                    }
                    val rms = sqrt(sum / read).toFloat().coerceIn(0f, 1f)
                    onAmplitude(rms)
                }
            }
        }
    }

    fun stop(): ShortArray {
        recording = false
        runCatching { recorder?.stop() }
        recordingThread?.join(250)
        recorder?.release()
        recorder = null
        recordingThread = null
        return synchronized(samples) { samples.toShortArray() }
    }

    fun release() {
        if (recording) stop()
        synchronized(samples) { samples.clear() }
    }

    companion object {
        const val SAMPLE_RATE = 16_000
    }
}
