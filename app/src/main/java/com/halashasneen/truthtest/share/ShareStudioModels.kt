package com.halashasneen.truthtest.share

import com.halashasneen.truthtest.R

enum class ShareFormat(
    val storageKey: String,
    val labelRes: Int,
    val width: Int,
    val height: Int
) {
    SQUARE("square", R.string.p4_format_square, 1080, 1080),
    FEED("feed", R.string.p4_format_feed, 1080, 1350),
    STORY("story", R.string.p4_format_story, 1080, 1920);

    companion object {
        fun fromStorage(value: String?): ShareFormat =
            entries.firstOrNull { it.storageKey == value } ?: FEED
    }
}

enum class ShareTemplate(
    val storageKey: String,
    val labelRes: Int,
    val emoji: String,
    val theme: ShareTheme
) {
    MINIMAL("minimal", R.string.p4_template_minimal, "◻", ShareTheme.MINIMAL),
    NEON("neon", R.string.p4_template_neon, "✦", ShareTheme.NEON_PURPLE),
    PARTY("party", R.string.p4_template_party, "◈", ShareTheme.CYBER_CYAN),
    COUPLES("couples", R.string.p4_template_couples, "♡", ShareTheme.ROMANTIC_PINK),
    CHALLENGE("challenge", R.string.p4_template_challenge, "↯", ShareTheme.GOLD_CHALLENGE),
    DARK_PREMIUM("dark_premium", R.string.p4_template_dark_premium, "◆", ShareTheme.DARK_PREMIUM);

    companion object {
        fun fromStorage(value: String?): ShareTemplate =
            entries.firstOrNull { it.storageKey == value } ?: NEON
    }
}

enum class ShareSourceType {
    RESULT,
    SESSION
}

data class StudioShareContent(
    val sourceType: ShareSourceType,
    val sourceId: String,
    val question: String,
    val score: Int,
    val modeLabel: String,
    val playerName: String? = null,
    val badge: String? = null,
    val secondaryText: String? = null,
    val cta: String? = null,
    val challengeUri: String? = null
)

data class ShareHistoryEntry(
    val sourceId: String,
    val sourceType: String,
    val format: String,
    val template: String,
    val timestamp: Long
)
