package com.nuvexa.truthtest.data

import com.nuvexa.truthtest.data.model.TestResult
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementEngineTest {
    private val zone = ZoneId.systemDefault()

    @Test
    fun unlocksMilestonesAndModes() {
        val start = LocalDate.of(2026, 9, 1)
        val results = (1..100).map { index ->
            val day = start.plusDays((index % 7).toLong())
            TestResult(
                id = index.toString(),
                question = "Q$index",
                category = if (index == 1) "custom" else "funny",
                score = if (index == 2) 97 else 70,
                timestamp = day.atTime(12, 0).atZone(zone).toInstant().toEpochMilli() + index,
                mode = when (index) {
                    1 -> "custom"
                    3 -> "duel_p2"
                    4 -> "group3_p3"
                    else -> "solo"
                }
            )
        }

        val earned = AchievementEngine.earned(results)

        assertTrue(AchievementKey.FIRST in earned)
        assertTrue(AchievementKey.TEN in earned)
        assertTrue(AchievementKey.FIFTY in earned)
        assertTrue(AchievementKey.HUNDRED in earned)
        assertTrue(AchievementKey.HIGH in earned)
        assertTrue(AchievementKey.STREAK_3 in earned)
        assertTrue(AchievementKey.STREAK_7 in earned)
        assertTrue(AchievementKey.DUEL in earned)
        assertTrue(AchievementKey.GROUP in earned)
        assertTrue(AchievementKey.CUSTOM in earned)
    }
}
