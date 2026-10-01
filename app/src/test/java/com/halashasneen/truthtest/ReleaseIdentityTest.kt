package com.halashasneen.truthtest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseIdentityTest {
    @Test
    fun applicationIdStaysCompatibleWithPublishedPlayListing() {
        assertEquals("com.halahasneen.truthtest", BuildConfig.APPLICATION_ID)
    }

    @Test
    fun truthTest2UsesAnUpgradeVersionCode() {
        assertTrue(BuildConfig.VERSION_CODE >= 2)
    }
}
