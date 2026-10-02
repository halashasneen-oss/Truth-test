package com.halashasneen.truthtest.core

import com.halashasneen.truthtest.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class ReleaseContractTest {
    @Test
    fun publishedGooglePlayIdentityIsPreserved() {
        assertEquals("com.halahasneen.truthtest", BuildConfig.APPLICATION_ID)
    }

    @Test
    fun truthTestTwoStartsWithExpectedVersion() {
        assertEquals(8, BuildConfig.VERSION_CODE)
        assertEquals("2.0.6", BuildConfig.VERSION_NAME)
    }
}
