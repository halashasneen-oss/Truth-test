package com.halashasneen.truthtest.monetization

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MonetizationPolicyTest {
    @Test
    fun rewardedWindowIsTheOnlyAdSuppressionState() {
        val now = 1_000_000L
        assertTrue(MonetizationPolicy.adsSuppressed(now + 10_000L, now))
        assertFalse(MonetizationPolicy.adsSuppressed(now, now))
    }

    @Test
    fun interstitialRequiresTwoNaturalBreaksAndSixtySecondSpacing() {
        val now = 1_000_000L
        assertFalse(
            MonetizationPolicy.interstitialEligible(0L, 1, 0L, now)
        )
        assertTrue(
            MonetizationPolicy.interstitialEligible(0L, 2, 0L, now)
        )
        assertFalse(
            MonetizationPolicy.interstitialEligible(
                0L,
                2,
                now - MonetizationPolicy.INTERSTITIAL_MIN_INTERVAL_MS + 1L,
                now
            )
        )
        assertTrue(
            MonetizationPolicy.interstitialEligible(
                0L,
                2,
                now - MonetizationPolicy.INTERSTITIAL_MIN_INTERVAL_MS,
                now
            )
        )
    }

    @Test
    fun adFreeSessionSuppressesInterstitialRegardlessOfCounter() {
        val now = 1_000_000L
        assertFalse(MonetizationPolicy.interstitialEligible(now + 60_000L, 20, 0L, now))
    }

    @Test
    fun appOpenRespectsRewardSuppressionAndFullscreenSpacing() {
        val now = 10_000_000L
        assertTrue(MonetizationPolicy.appOpenEligible(0L, 0L, 0L, now))
        assertFalse(MonetizationPolicy.appOpenEligible(now + 1_000L, 0L, 0L, now))
        assertFalse(MonetizationPolicy.appOpenEligible(0L, now - 1_000L, 0L, now))
        assertFalse(MonetizationPolicy.appOpenEligible(0L, 0L, now - 1_000L, now))
        assertTrue(MonetizationPolicy.appOpenEligible(
            0L, now - MonetizationPolicy.APP_OPEN_MIN_INTERVAL_MS,
            now - MonetizationPolicy.FULLSCREEN_CROSS_FORMAT_GAP_MS, now
        ))
    }

    @Test
    fun anAppOpenOrRewardedAdPreventsImmediateInterstitial() {
        val now = 10_000_000L
        assertFalse(MonetizationPolicy.interstitialEligible(
            0L, 2, now - 90_000L, now, now - 60_000L
        ))
        assertTrue(MonetizationPolicy.interstitialEligible(
            0L, 2, now - 90_000L, now, now - 180_000L
        ))
    }

    @Test
    fun rewardDurationIsExactlyOneHour() {
        assertEquals(3_600_000L, MonetizationPolicy.REWARDED_AD_FREE_MS)
        assertEquals(60_000L, MonetizationPolicy.remainingAdFreeMs(120_000L, 60_000L))
        assertEquals(0L, MonetizationPolicy.remainingAdFreeMs(50_000L, 60_000L))
    }
}
