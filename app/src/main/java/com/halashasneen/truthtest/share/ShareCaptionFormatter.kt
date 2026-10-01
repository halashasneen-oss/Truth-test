package com.halashasneen.truthtest.share

import android.content.Context
import com.halashasneen.truthtest.R

/** Playful copy only: a score is not evidence of honesty or deception. */
object ShareCaptionFormatter {
    const val PLAY_URL = "https://play.google.com/store/apps/details?id=com.halahasneen.truthtest"

    enum class Tone { HIGH, MEDIUM, LOW }

    fun tone(score: Int): Tone = when {
        score >= 80 -> Tone.HIGH
        score >= 50 -> Tone.MEDIUM
        else -> Tone.LOW
    }

    fun create(
        context: Context,
        question: String,
        score: Int,
        modeLabel: String,
        playerName: String? = null,
        summary: String? = null
    ): String {
        val description = context.getString(when (tone(score)) {
            Tone.HIGH -> R.string.hotfix_share_high
            Tone.MEDIUM -> R.string.hotfix_share_medium
            Tone.LOW -> R.string.hotfix_share_low
        })
        return buildString {
            append(context.getString(R.string.hotfix_share_intro, modeLabel, score.coerceIn(0, 100)))
            if (!playerName.isNullOrBlank()) append("\n").append(playerName.take(48))
            append("\n").append(context.getString(R.string.hotfix_share_question, question.take(400)))
            append("\n").append(description)
            if (!summary.isNullOrBlank()) append("\n\n").append(summary)
            append("\n\n").append(context.getString(R.string.hotfix_share_challenge))
            append("\n").append(PLAY_URL)
        }
    }
}
