package com.nuvexa.truthtest.report

import com.nuvexa.truthtest.data.QuestionRepository
import com.nuvexa.truthtest.data.model.TestResult
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.roundToInt

data class MonthlyReportSummary(
    val month: YearMonth,
    val results: List<TestResult>,
    val totalAnswers: Int,
    val averageScore: Int,
    val bestScore: Int,
    val duelSessions: Int,
    val groupSessions: Int,
    val customAnswers: Int,
    val favoriteCategory: String?,
    val favoriteCategoryCount: Int,
    val lightCount: Int,
    val mediumCount: Int,
    val boldCount: Int,
    val bestStreak: Int
)

object MonthlyReportCalculator {
    fun calculate(
        results: List<TestResult>,
        month: YearMonth,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): MonthlyReportSummary {
        val monthResults = results.filter {
            val date = Instant.ofEpochMilli(it.timestamp).atZone(zoneId).toLocalDate()
            YearMonth.from(date) == month
        }.sortedByDescending { it.timestamp }

        val categoryCounts = monthResults.groupingBy { it.category }.eachCount()
        val favorite = categoryCounts.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .firstOrNull()

        val days = monthResults
            .map { Instant.ofEpochMilli(it.timestamp).atZone(zoneId).toLocalDate() }
            .distinct()
            .sorted()

        return MonthlyReportSummary(
            month = month,
            results = monthResults,
            totalAnswers = monthResults.size,
            averageScore = if (monthResults.isEmpty()) 0 else monthResults.map { it.score }.average().roundToInt(),
            bestScore = monthResults.maxOfOrNull { it.score } ?: 0,
            duelSessions = monthResults.count { it.mode == "duel_p2" },
            groupSessions = monthResults.count { it.mode == "group3_p3" || it.mode == "group4_p4" },
            customAnswers = monthResults.count { it.mode == "custom" },
            favoriteCategory = favorite?.key,
            favoriteCategoryCount = favorite?.value ?: 0,
            lightCount = monthResults.count { it.intensity == QuestionRepository.INTENSITY_LIGHT },
            mediumCount = monthResults.count { it.intensity == QuestionRepository.INTENSITY_MEDIUM },
            boldCount = monthResults.count { it.intensity == QuestionRepository.INTENSITY_BOLD },
            bestStreak = bestStreak(days)
        )
    }

    private fun bestStreak(days: List<LocalDate>): Int {
        if (days.isEmpty()) return 0
        var best = 1
        var current = 1
        for (index in 1 until days.size) {
            current = if (days[index] == days[index - 1].plusDays(1)) current + 1 else 1
            if (current > best) best = current
        }
        return best
    }
}
