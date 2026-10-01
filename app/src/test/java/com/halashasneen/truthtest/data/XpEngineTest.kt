package com.halashasneen.truthtest.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class XpEngineTest {
    @Test
    fun xpRewardsAreBoundedAndDailyBonusIsAppliedOnceByCaller() {
        assertEquals(10, XpEngine.resultXp(0))
        assertTrue(XpEngine.resultXp(95) > XpEngine.resultXp(60))
        assertEquals(
            XpEngine.resultXp(80) + 15,
            XpEngine.resultXp(80, daily = true)
        )
    }

    @Test
    fun achievementsGrantProgressionXp() {
        assertEquals(0, XpEngine.achievementXp(0))
        assertEquals(25, XpEngine.achievementXp(1))
        assertEquals(75, XpEngine.achievementXp(3))
    }

    @Test
    fun levelsAdvanceEveryTwoHundredFiftyXp() {
        assertEquals(1, XpEngine.levelForXp(0))
        assertEquals(1, XpEngine.levelForXp(249))
        assertEquals(2, XpEngine.levelForXp(250))
        assertEquals(3, XpEngine.levelForXp(500))
        assertEquals(20, XpEngine.levelProgress(270))
        assertEquals(230, XpEngine.xpToNextLevel(270))
    }
}
