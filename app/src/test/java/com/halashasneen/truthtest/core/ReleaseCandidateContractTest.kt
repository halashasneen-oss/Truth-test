package com.halashasneen.truthtest.core

import com.halashasneen.truthtest.BuildConfig
import com.halashasneen.truthtest.monetization.MonetizationPolicy
import org.junit.Assert.assertEquals
import org.junit.Test

class ReleaseCandidateContractTest {
    @Test
    fun debugBuildCannotAccidentallyUseProductionAds() {
        assertEquals(
            "ca-app-pub-3940256099942544/9214589741",
            BuildConfig.ADMOB_BANNER_ID
        )
        assertEquals(
            "ca-app-pub-3940256099942544/1033173712",
            BuildConfig.ADMOB_INTERSTITIAL_ID
        )
        assertEquals(
            "ca-app-pub-3940256099942544/5224354917",
            BuildConfig.ADMOB_REWARDED_ID
        )
    }

    @Test
    fun finalRewardAndInterstitialPolicyIsFrozen() {
        assertEquals(3_600_000L, MonetizationPolicy.REWARDED_AD_FREE_MS)
        assertEquals(3, MonetizationPolicy.INTERSTITIAL_EVENT_THRESHOLD)
        assertEquals(240_000L, MonetizationPolicy.INTERSTITIAL_MIN_INTERVAL_MS)
        assertEquals(4, AppStorageContract.CURRENT_DATA_SCHEMA_VERSION)
    }
}
