package com.halashasneen.truthtest.monetization

import com.halashasneen.truthtest.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdsIntegrationContractTest {
    @Test fun debugIdsRemainOfficialTestUnits() {
        assertTrue(BuildConfig.DEBUG)
        assertEquals("ca-app-pub-3940256099942544/9214589741", BuildConfig.ADMOB_BANNER_ID)
        assertEquals("ca-app-pub-3940256099942544/1033173712", BuildConfig.ADMOB_INTERSTITIAL_ID)
        assertEquals("ca-app-pub-3940256099942544/5224354917", BuildConfig.ADMOB_REWARDED_ID)
    }

    @Test fun retryNeverFloodsWhenNoFillOrConfigurationErrorsOccur() {
        assertEquals(120_000L,
            AdRetryPolicy.retryDelayMs(1, AdRetryPolicy.Failure.NO_FILL))
        assertEquals(Long.MAX_VALUE,
            AdRetryPolicy.retryDelayMs(1, AdRetryPolicy.Failure.CONFIGURATION))
    }
}
