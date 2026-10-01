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
    fun interstitialRequiresTwoNaturalBreaksAndNinetySecondSpacing() {
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
    fun rewardDurationIsExactlyOneHour() {
        assertEquals(3_600_000L, MonetizationPolicy.REWARDED_AD_FREE_MS)
        assertEquals(60_000L, MonetizationPolicy.remainingAdFreeMs(120_000L, 60_000L))
        assertEquals(0L, MonetizationPolicy.remainingAdFreeMs(50_000L, 60_000L))
    }
}
