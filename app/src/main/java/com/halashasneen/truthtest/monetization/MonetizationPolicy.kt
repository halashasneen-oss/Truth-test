package com.halashasneen.truthtest.monetization

object MonetizationPolicy {
    const val REWARDED_AD_FREE_MS = 60L * 60L * 1_000L
    const val INTERSTITIAL_EVENT_THRESHOLD = 2
    const val INTERSTITIAL_MIN_INTERVAL_MS = 60L * 1_000L
    const val FULLSCREEN_CROSS_FORMAT_GAP_MS = 120L * 1_000L
    const val APP_OPEN_MIN_INTERVAL_MS = 30L * 60L * 1_000L

    fun adsSuppressed(
        adFreeUntil: Long,
        now: Long
    ): Boolean = adFreeUntil > now

    fun interstitialEligible(
        adFreeUntil: Long,
        eventCount: Int,
        lastShownAt: Long,
        now: Long,
        lastFullscreenAt: Long = 0L
    ): Boolean {
        if (adsSuppressed(adFreeUntil, now)) return false
        if (lastFullscreenAt > 0L && now - lastFullscreenAt < FULLSCREEN_CROSS_FORMAT_GAP_MS) return false
        if (eventCount < INTERSTITIAL_EVENT_THRESHOLD) return false
        return lastShownAt <= 0L || now - lastShownAt >= INTERSTITIAL_MIN_INTERVAL_MS
    }

    fun appOpenEligible(
        adFreeUntil: Long,
        lastAppOpenAt: Long,
        lastFullscreenAt: Long,
        now: Long
    ): Boolean {
        if (adsSuppressed(adFreeUntil, now)) return false
        if (lastAppOpenAt > 0L && now - lastAppOpenAt < APP_OPEN_MIN_INTERVAL_MS) return false
        if (lastFullscreenAt > 0L && now - lastFullscreenAt < FULLSCREEN_CROSS_FORMAT_GAP_MS) return false
        return true
    }

    fun remainingAdFreeMs(adFreeUntil: Long, now: Long): Long =
        (adFreeUntil - now).coerceAtLeast(0L)
}
