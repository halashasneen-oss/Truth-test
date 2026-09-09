package com.nuvexa.truthtest.share

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * High-level share renderer that preserves the proven silent video pipeline and
 * optionally adds a locally generated AAC soundtrack afterward.
 */
object ResultVideoShareRenderer {
    suspend fun render(
        context: Context,
        question: String,
        score: Int,
        firstScore: Int? = null,
        secondScore: Int? = null,
        waveform: List<Float> = emptyList(),
        sound: ShareSound = ShareSound.NEON_BEAT
    ): Uri {
        val silentUri = ResultVideoRenderer.render(
            context = context,
            question = question,
            score = score,
            firstScore = firstScore,
            secondScore = secondScore,
            waveform = waveform
        )
        if (!sound.enabled) return silentUri

        val appContext = context.applicationContext
        val dir = File(appContext.cacheDir, "shares").apply { mkdirs() }
        val stamp = System.currentTimeMillis()
        val silentCopy = File(dir, ".silent_$stamp.mp4")
        val audioFile = File(dir, ".sound_$stamp.m4a")
        val finalFile = File(dir, "truth_test_sound_$stamp.mp4")

        return try {
            val copied = appContext.contentResolver.openInputStream(silentUri)?.use { input ->
                silentCopy.outputStream().use { output -> input.copyTo(output) }
                true
            } ?: false
            if (!copied) return silentUri

            ResultAudioRenderer.render(audioFile, sound, ResultVideoTimeline.DURATION_MS)
            MediaTrackMuxer.mux(silentCopy, audioFile, finalFile)
            FileProvider.getUriForFile(
                appContext,
                "${appContext.packageName}.files",
                finalFile
            )
        } catch (_: Throwable) {
            runCatching { finalFile.delete() }
            silentUri
        } finally {
            runCatching { silentCopy.delete() }
            runCatching { audioFile.delete() }
        }
    }
}
