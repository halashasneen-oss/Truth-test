package com.halashasneen.truthtest.data

import com.halashasneen.truthtest.data.model.TestResult
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementEngineTest {
    private val zone = ZoneId.systemDefault()

    @Test
    fun unlocksLegacyMilestonesAndModes() {
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

    @Test
    fun unlocksPhaseThreeSocialProgress() {
        val modes = listOf(
            "social_couples",
            "social_friends",
            "social_party",
            "social_challenge",
            HistoryRepository.MODE_DAILY
        )
        val results = modes.mapIndexed { index, mode ->
            TestResult(
                id = "social-$index",
                question = "Q",
                category = "funny",
                score = 80,
                timestamp = 1_000L + index,
                mode = mode
            )
        }

        val earned = AchievementEngine.earned(
            results = results,
            totalXp = 600,
            completedSessions = 5
        )
        assertTrue(AchievementKey.COUPLES in earned)
        assertTrue(AchievementKey.FRIENDS in earned)
        assertTrue(AchievementKey.PARTY in earned)
        assertTrue(AchievementKey.DAILY in earned)
        assertTrue(AchievementKey.CHALLENGE in earned)
        assertTrue(AchievementKey.XP_500 in earned)
        assertTrue(AchievementKey.SESSIONS_5 in earned)
    }
}
