package com.nuvexa.truthtest.share

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
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
 * Color-only resource overlay used by the existing PNG and MP4 renderers.
 * Context.getColor() itself is final on Android, so the wrapper overrides
 * getResources() and lets the final Context method resolve through these
 * themed resources instead.
 */
class ShareThemeContext private constructor(
    base: Context,
    shareTheme: ShareTheme
) : ContextWrapper(base) {
    private val themedResources = ShareThemeResources(base.resources, shareTheme)

    override fun getApplicationContext(): Context = this

    override fun getResources(): Resources = themedResources

    companion object {
        fun wrap(base: Context, theme: ShareTheme): Context = ShareThemeContext(base, theme)
    }
}

@Suppress("DEPRECATION")
private class ShareThemeResources(
    private val baseResources: Resources,
    shareTheme: ShareTheme
) : Resources(
    baseResources.assets,
    baseResources.displayMetrics,
    baseResources.configuration
) {
    private val palette = shareTheme.palette

    override fun getColor(id: Int, theme: Theme?): Int = themedColor(id) ?: baseResources.getColor(id, theme)

    @Deprecated("Kept for platform compatibility")
    override fun getColor(id: Int): Int = themedColor(id) ?: baseResources.getColor(id)

    private fun themedColor(id: Int): Int? = when (id) {
        R.color.bg_dark -> palette.background
        R.color.purple -> palette.primary
        R.color.pink -> palette.secondary
        R.color.cyan -> palette.accent
        else -> null
    }
}
