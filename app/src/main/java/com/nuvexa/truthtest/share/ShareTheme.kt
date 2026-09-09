package com.nuvexa.truthtest.share

import android.content.Context
import android.content.ContextWrapper
import com.nuvexa.truthtest.R

data class SharePalette(
    val background: Int,
    val primary: Int,
    val secondary: Int,
    val accent: Int
)

enum class ShareTheme(
    val storageKey: String,
    val labelRes: Int,
    val emoji: String,
    val palette: SharePalette
) {
    NEON_PURPLE(
        storageKey = "neon_purple",
        labelRes = R.string.theme_neon_purple,
        emoji = "🟣",
        palette = SharePalette(
            background = 0xFF0B0B12.toInt(),
            primary = 0xFF7C3AED.toInt(),
            secondary = 0xFFEC4899.toInt(),
            accent = 0xFF22D3EE.toInt()
        )
    ),
    CYBER_CYAN(
        storageKey = "cyber_cyan",
        labelRes = R.string.theme_cyber_cyan,
        emoji = "🩵",
        palette = SharePalette(
            background = 0xFF03171C.toInt(),
            primary = 0xFF0891B2.toInt(),
            secondary = 0xFF22D3EE.toInt(),
            accent = 0xFFA3E635.toInt()
        )
    ),
    ROMANTIC_PINK(
        storageKey = "romantic_pink",
        labelRes = R.string.theme_romantic_pink,
        emoji = "💗",
        palette = SharePalette(
            background = 0xFF1B0B16.toInt(),
            primary = 0xFFDB2777.toInt(),
            secondary = 0xFFFB7185.toInt(),
            accent = 0xFFFBCFE8.toInt()
        )
    ),
    GOLD_CHALLENGE(
        storageKey = "gold_challenge",
        labelRes = R.string.theme_gold_challenge,
        emoji = "🏆",
        palette = SharePalette(
            background = 0xFF171006.toInt(),
            primary = 0xFFD97706.toInt(),
            secondary = 0xFFFACC15.toInt(),
            accent = 0xFFFDE68A.toInt()
        )
    );

    companion object {
        fun fromStorage(value: String?): ShareTheme =
            entries.firstOrNull { it.storageKey == value } ?: NEON_PURPLE
    }
}

/**
 * Lightweight color overlay for share renderers.
 *
 * The existing PNG/MP4 renderers already read the app brand colors from Context.
 * Wrapping the context lets both exporters use the same selected palette without
 * duplicating their rendering pipelines or adding external dependencies.
 */
class ShareThemeContext private constructor(
    base: Context,
    private val shareTheme: ShareTheme
) : ContextWrapper(base) {

    override fun getApplicationContext(): Context = this

    override fun getColor(id: Int): Int = when (id) {
        R.color.bg_dark -> shareTheme.palette.background
        R.color.purple -> shareTheme.palette.primary
        R.color.pink -> shareTheme.palette.secondary
        R.color.cyan -> shareTheme.palette.accent
        else -> super.getColor(id)
    }

    companion object {
        fun wrap(base: Context, theme: ShareTheme): Context = ShareThemeContext(base, theme)
    }
}
