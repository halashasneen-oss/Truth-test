package com.halashasneen.truthtest.monetization

object MonetizationPolicy {
    const val REWARDED_AD_FREE_MS = 60L * 60L * 1_000L
    const val INTERSTITIAL_EVENT_THRESHOLD = 3
    const val INTERSTITIAL_MIN_INTERVAL_MS = 4L * 60L * 1_000L

    fun adsSuppressed(
        adFreeUntil: Long,
        now: Long
    ): Boolean = adFreeUntil > now

    fun interstitialEligible(
        adFreeUntil: Long,
        eventCount: Int,
        lastShownAt: Long,
        now: Long
    ): Boolean {
        if (adsSuppressed(adFreeUntil, now)) return false
        if (eventCount < INTERSTITIAL_EVENT_THRESHOLD) return false
        return lastShownAt <= 0L || now - lastShownAt >= INTERSTITIAL_MIN_INTERVAL_MS
    }

    fun remainingAdFreeMs(adFreeUntil: Long, now: Long): Long =
        (adFreeUntil - now).coerceAtLeast(0L)
}
