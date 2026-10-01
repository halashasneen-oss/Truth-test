package com.halashasneen.truthtest.monetization

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdRetryPolicyTest {
    @Test fun mapsGoogleMobileAdsErrorsWithoutConfusingNoFillAndNetwork() {
        assertEquals(AdRetryPolicy.Failure.INTERNAL, AdRetryPolicy.classify(0))
        assertEquals(AdRetryPolicy.Failure.CONFIGURATION, AdRetryPolicy.classify(1))
        assertEquals(AdRetryPolicy.Failure.NETWORK, AdRetryPolicy.classify(2))
        assertEquals(AdRetryPolicy.Failure.NO_FILL, AdRetryPolicy.classify(3))
        assertEquals(AdRetryPolicy.Failure.OTHER, AdRetryPolicy.classify(99))
    }

    @Test fun retriesAreBoundedAndConfigurationErrorsNeverSpin() {
        assertEquals(Long.MAX_VALUE,
            AdRetryPolicy.retryDelayMs(1, AdRetryPolicy.Failure.CONFIGURATION))
        assertEquals(30_000L,
            AdRetryPolicy.retryDelayMs(1, AdRetryPolicy.Failure.NETWORK))
        assertEquals(120_000L,
            AdRetryPolicy.retryDelayMs(1, AdRetryPolicy.Failure.NO_FILL))
        assertEquals(360_000L,
            AdRetryPolicy.retryDelayMs(20, AdRetryPolicy.Failure.NO_FILL))
        assertTrue(
            AdRetryPolicy.retryDelayMs(2, AdRetryPolicy.Failure.INTERNAL) >
                AdRetryPolicy.retryDelayMs(1, AdRetryPolicy.Failure.INTERNAL)
        )
    }
}
