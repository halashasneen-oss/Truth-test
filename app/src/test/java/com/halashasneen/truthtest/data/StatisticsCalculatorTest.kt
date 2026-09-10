package com.halashasneen.truthtest.data

import com.halashasneen.truthtest.data.model.TestResult
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class StatisticsCalculatorTest {
    private val zone = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 9, 9)
    private val nowMillis = today.atStartOfDay(zone).plusHours(12).toInstant().toEpochMilli()

    @Test
    fun calculatesCoreStatisticsAndStreaks() {
        val results = listOf(
            result("1", 72, "funny", "solo", today),
            result("2", 88, "funny", "duel_p1", today.minusDays(1)),
            result("3", 91, "bold", "duel_p2", today.minusDays(1)),
            result("4", 49, "family", "solo", today.minusDays(2)),
            result("5", 60, "family", "solo", today.minusDays(5))
        )

        val summary = StatisticsCalculator.calculate(results, nowMillis, zone)

        assertEquals(5, summary.totalResults)
        assertEquals(1, summary.testsToday)
        assertEquals(72, summary.averageScore)
        assertEquals(91, summary.bestScore)
        assertEquals(1, summary.duelSessions)
        assertEquals(3, summary.currentStreak)
        assertEquals(3, summary.bestStreak)
        assertEquals("family", summary.favoriteCategory)
        assertEquals(2, summary.favoriteCategoryCount)
        assertEquals(listOf(60, 49, 88, 91, 72), summary.recentScores)
    }

    @Test
    fun emptyHistoryReturnsZeroedSummary() {
        val summary = StatisticsCalculator.calculate(emptyList(), nowMillis, zone)

        assertEquals(0, summary.totalResults)
        assertEquals(0, summary.averageScore)
        assertEquals(0, summary.bestScore)
        assertEquals(0, summary.currentStreak)
        assertEquals(0, summary.bestStreak)
        assertEquals(null, summary.favoriteCategory)
        assertEquals(emptyList<Int>(), summary.recentScores)
    }

    private fun result(
        id: String,
        score: Int,
        category: String,
        mode: String,
        day: LocalDate
    ) = TestResult(
        id = id,
        question = "Question $id",
        category = category,
        score = score,
        timestamp = day.atStartOfDay(zone).plusHours(id.toLong()).toInstant().toEpochMilli(),
        mode = mode
    )
}
