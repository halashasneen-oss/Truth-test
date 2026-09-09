package com.nuvexa.truthtest.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nuvexa.truthtest.data.model.TestResult
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class HistoryRepository(context: Context) {
    private val prefs = context.getSharedPreferences("truth_test_history", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val type = object : TypeToken<List<TestResult>>() {}.type

    fun getAll(): List<TestResult> {
        val raw = prefs.getString("results", null) ?: return emptyList()
        return runCatching { gson.fromJson<List<TestResult>>(raw, type).orEmpty() }
            .getOrDefault(emptyList())
            .sortedByDescending { it.timestamp }
    }

    fun add(result: TestResult) {
        val updated = (listOf(result) + getAll()).distinctBy { it.id }.take(100)
        prefs.edit().putString("results", gson.toJson(updated)).apply()
    }

    fun currentStreak(nowMillis: Long = System.currentTimeMillis()): Int {
        val zone = ZoneId.systemDefault()
        val days = getAll()
            .map { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() }
            .distinct()
            .sortedDescending()
        if (days.isEmpty()) return 0

        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        var expected: LocalDate = when (days.first()) {
            today -> today
            today.minusDays(1) -> today.minusDays(1)
            else -> return 0
        }

        var streak = 0
        for (day in days) {
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

    fun clear() = prefs.edit().remove("results").apply()
}
