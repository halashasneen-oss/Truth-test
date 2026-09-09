package com.nuvexa.truthtest.share

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File
import java.nio.ByteBuffer

/** Remuxes the already-rendered H.264 video and generated AAC track into one MP4. */
object MediaTrackMuxer {
    fun mux(videoFile: File, audioFile: File, outputFile: File) {
        if (outputFile.exists()) outputFile.delete()
        outputFile.parentFile?.mkdirs()

        val videoExtractor = MediaExtractor()
        val audioExtractor = MediaExtractor()
        val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var muxerStarted = false

        try {
            videoExtractor.setDataSource(videoFile.absolutePath)
            audioExtractor.setDataSource(audioFile.absolutePath)

            val videoInputTrack = findTrack(videoExtractor, "video/")
            val audioInputTrack = findTrack(audioExtractor, "audio/")
            check(videoInputTrack >= 0) { "Missing video track" }
            check(audioInputTrack >= 0) { "Missing audio track" }

            val videoFormat = videoExtractor.getTrackFormat(videoInputTrack)
            val audioFormat = audioExtractor.getTrackFormat(audioInputTrack)
            val videoOutputTrack = muxer.addTrack(videoFormat)
            val audioOutputTrack = muxer.addTrack(audioFormat)
            muxer.start()
            muxerStarted = true

            videoExtractor.selectTrack(videoInputTrack)
            audioExtractor.selectTrack(audioInputTrack)
            copyTrack(videoExtractor, muxer, videoOutputTrack, videoFormat)
            copyTrack(audioExtractor, muxer, audioOutputTrack, audioFormat)
        } finally {
            runCatching { videoExtractor.release() }
            runCatching { audioExtractor.release() }
            if (muxerStarted) runCatching { muxer.stop() }
            runCatching { muxer.release() }
        }
    }

    private fun findTrack(extractor: MediaExtractor, prefix: String): Int {
        for (index in 0 until extractor.trackCount) {
            val mime = extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME).orEmpty()
            if (mime.startsWith(prefix)) return index
        }
        return -1
    }

    private fun copyTrack(
        extractor: MediaExtractor,
        muxer: MediaMuxer,
        outputTrack: Int,
        format: MediaFormat
    ) {
        val requestedSize = if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
            format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
        } else 0
        val buffer = ByteBuffer.allocateDirect(maxOf(requestedSize, 1_048_576))
        val info = MediaCodec.BufferInfo()

        while (true) {
            buffer.clear()
            val size = extractor.readSampleData(buffer, 0)
            if (size < 0) break
            val ptsUs = extractor.sampleTime
            if (ptsUs < 0L) break

            info.offset = 0
            info.size = size
            info.presentationTimeUs = ptsUs
            info.flags = extractor.sampleFlags
            buffer.position(0)
            buffer.limit(size)
            muxer.writeSampleData(outputTrack, buffer, info)
            extractor.advance()
        }
    }
}
