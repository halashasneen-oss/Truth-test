package com.nuvexa.truthtest.data

import com.nuvexa.truthtest.data.model.TestResult
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

data class StatisticsSummary(
    val totalResults: Int,
    val testsToday: Int,
    val averageScore: Int,
    val bestScore: Int,
    val duelSessions: Int,
    val currentStreak: Int,
    val bestStreak: Int,
    val favoriteCategory: String?,
    val favoriteCategoryCount: Int,
    val recentScores: List<Int>
)

/** Pure local statistics derived from the existing on-device history. */
object StatisticsCalculator {
    fun calculate(
        results: List<TestResult>,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): StatisticsSummary {
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val daysDescending = results
            .map { Instant.ofEpochMilli(it.timestamp).atZone(zoneId).toLocalDate() }
            .distinct()
            .sortedDescending()

        val categoryCounts = results.groupingBy { it.category }.eachCount()
        val favorite = categoryCounts.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .firstOrNull()

        return StatisticsSummary(
            totalResults = results.size,
            testsToday = results.count {
                Instant.ofEpochMilli(it.timestamp).atZone(zoneId).toLocalDate() == today
            },
            averageScore = if (results.isEmpty()) 0 else results.map { it.score }.average().roundToInt(),
            bestScore = results.maxOfOrNull { it.score } ?: 0,
            duelSessions = results.count { it.mode == "duel_p2" },
            currentStreak = currentStreak(daysDescending, today),
            bestStreak = bestStreak(daysDescending),
            favoriteCategory = favorite?.key,
            favoriteCategoryCount = favorite?.value ?: 0,
            recentScores = results.sortedBy { it.timestamp }.takeLast(7).map { it.score }
        )
    }

    private fun currentStreak(daysDescending: List<LocalDate>, today: LocalDate): Int {
        if (daysDescending.isEmpty()) return 0
        var expected = when (daysDescending.first()) {
            today -> today
            today.minusDays(1) -> today.minusDays(1)
            else -> return 0
        }

        var streak = 0
        for (day in daysDescending) {
            when {
                day == expected -> {
                    streak++
                    expected = expected.minusDays(1)
                }
                day.isBefore(expected) -> break
            }
        }
        return streak
    }

    private fun bestStreak(daysDescending: List<LocalDate>): Int {
        val days = daysDescending.sorted()
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
