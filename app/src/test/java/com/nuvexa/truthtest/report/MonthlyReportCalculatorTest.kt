package com.nuvexa.truthtest.report

import com.nuvexa.truthtest.data.QuestionRepository
import com.nuvexa.truthtest.data.model.TestResult
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class MonthlyReportCalculatorTest {
    private val zone = ZoneId.of("UTC")

    @Test
    fun calculatesOnlySelectedMonthAndChallengeSessions() {
        val month = YearMonth.of(2026, 9)
        val results = listOf(
            result("1", 80, "solo", QuestionRepository.INTENSITY_LIGHT, 1),
            result("2", 90, "duel_p1", QuestionRepository.INTENSITY_MEDIUM, 2),
            result("3", 70, "duel_p2", QuestionRepository.INTENSITY_MEDIUM, 2),
            result("4", 95, "group3_p1", QuestionRepository.INTENSITY_BOLD, 3),
            result("5", 85, "group3_p2", QuestionRepository.INTENSITY_BOLD, 3),
            result("6", 75, "group3_p3", QuestionRepository.INTENSITY_BOLD, 3),
            result("7", 88, "custom", null, 4),
            TestResult("old", "Old", "family", 100, timestamp(2026, 8, 20), "solo", QuestionRepository.INTENSITY_LIGHT)
        )

        val summary = MonthlyReportCalculator.calculate(results, month, zone)

        assertEquals(7, summary.totalAnswers)
        assertEquals(95, summary.bestScore)
        assertEquals(83, summary.averageScore)
        assertEquals(1, summary.duelSessions)
        assertEquals(1, summary.groupSessions)
        assertEquals(1, summary.customAnswers)
        assertEquals(1, summary.lightCount)
        assertEquals(2, summary.mediumCount)
        assertEquals(3, summary.boldCount)
        assertEquals(4, summary.bestStreak)
    }

    private fun result(id: String, score: Int, mode: String, intensity: String?, day: Int) = TestResult(
        id = id,
        question = "Question $id",
        category = if (id == "7") "custom" else "funny",
        score = score,
        timestamp = timestamp(2026, 9, day),
        mode = mode,
        intensity = intensity
    )

    private fun timestamp(year: Int, month: Int, day: Int): Long = LocalDateTime
        .of(year, month, day, 12, 0)
        .atZone(zone)
        .toInstant()
        .toEpochMilli()
}
