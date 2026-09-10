package com.halashasneen.truthtest.share

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File
import java.nio.ByteOrder
import kotlin.math.min

/** Encodes the deterministic procedural soundtrack to AAC/M4A entirely on-device. */
object ResultAudioRenderer {
    private const val SAMPLE_RATE = 44_100
    private const val CHANNEL_COUNT = 1
    private const val BIT_RATE = 96_000
    private const val MAX_INPUT_SIZE = 16_384

    fun render(output: File, sound: ShareSound, durationMs: Long) {
        require(sound.enabled) { "Sound must be enabled before AAC rendering" }
        if (output.exists()) output.delete()
        output.parentFile?.mkdirs()

        val format = MediaFormat.createAudioFormat(
            MediaFormat.MIMETYPE_AUDIO_AAC,
            SAMPLE_RATE,
            CHANNEL_COUNT
        ).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, MAX_INPUT_SIZE)
        }

        val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        val muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val info = MediaCodec.BufferInfo()
        val totalSamples = durationMs * SAMPLE_RATE / 1_000L
        val durationSeconds = durationMs / 1_000.0
        var sampleCursor = 0L
        var inputDone = false
        var outputDone = false
        var muxerStarted = false
        var trackIndex = -1

        try {
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()

            while (!outputDone) {
                if (!inputDone) {
                    val inputIndex = encoder.dequeueInputBuffer(10_000)
                    if (inputIndex >= 0) {
                        val input = encoder.getInputBuffer(inputIndex)
                            ?: error("AAC encoder returned an empty input buffer")
                        input.clear()
                        input.order(ByteOrder.LITTLE_ENDIAN)
                        val remainingSamples = totalSamples - sampleCursor

                        if (remainingSamples <= 0L) {
                            val ptsUs = sampleCursor * 1_000_000L / SAMPLE_RATE
                            encoder.queueInputBuffer(
                                inputIndex,
                                0,
                                0,
                                ptsUs,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            inputDone = true
                        } else {
                            val sampleCount = min(input.remaining() / 2, remainingSamples.toInt())
                            val startSample = sampleCursor
                            repeat(sampleCount) {
                                val value = ProceduralSoundSynth.sample(
                                    style = sound,
                                    sampleIndex = sampleCursor,
                                    sampleRate = SAMPLE_RATE,
                                    durationSeconds = durationSeconds
                                )
                                input.putShort((value * Short.MAX_VALUE).toInt().toShort())
                                sampleCursor++
                            }
                            val ptsUs = startSample * 1_000_000L / SAMPLE_RATE
                            encoder.queueInputBuffer(inputIndex, 0, sampleCount * 2, ptsUs, 0)
                        }
                    }
                }

                var draining = true
                while (draining && !outputDone) {
                    val outputIndex = encoder.dequeueOutputBuffer(info, 0)
                    when {
                        outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> draining = false
                        outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            check(!muxerStarted) { "AAC output format changed twice" }
                            trackIndex = muxer.addTrack(encoder.outputFormat)
                            muxer.start()
                            muxerStarted = true
                        }
                        outputIndex >= 0 -> {
                            val encoded = encoder.getOutputBuffer(outputIndex)
                                ?: error("AAC encoder returned an empty output buffer")
                            if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                                info.size = 0
                            }
                            if (info.size > 0) {
                                check(muxerStarted) { "AAC muxer has not started" }
                                encoded.position(info.offset)
                                encoded.limit(info.offset + info.size)
                                muxer.writeSampleData(trackIndex, encoded, info)
                            }
                            outputDone = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                            encoder.releaseOutputBuffer(outputIndex, false)
                        }
                    }
                }
            }
        } finally {
            runCatching { encoder.stop() }
            runCatching { encoder.release() }
            if (muxerStarted) runCatching { muxer.stop() }
            runCatching { muxer.release() }
        }
    }
}
