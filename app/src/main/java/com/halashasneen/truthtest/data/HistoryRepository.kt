package com.halashasneen.truthtest.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.halashasneen.truthtest.core.AppStorageContract
import com.halashasneen.truthtest.data.model.TestResult
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class HistoryRepository(context: Context) {
    private val prefs = context.getSharedPreferences(AppStorageContract.PREFS_HISTORY, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val type = object : TypeToken<List<TestResult>>() {}.type

    fun getAll(): List<TestResult> {
        val raw = prefs.getString(AppStorageContract.KEY_HISTORY_RESULTS, null) ?: return emptyList()
        return runCatching { gson.fromJson<List<TestResult>>(raw, type).orEmpty() }
            .getOrDefault(emptyList())
            .sortedByDescending { it.timestamp }
    }

    fun add(result: TestResult) {
        save((listOf(result) + getAll()).distinctBy { it.id }.take(MAX_RESULTS))
    }

    fun delete(id: String): Boolean {
        val current = getAll()
        val updated = current.filterNot { it.id == id }
        if (updated.size == current.size) return false
        save(updated)
        return true
    }

    fun hasResultToday(nowMillis: Long = System.currentTimeMillis()): Boolean =
        hasResultTodayMatching(nowMillis) { true }

    fun hasDailyResultToday(nowMillis: Long = System.currentTimeMillis()): Boolean =
        hasResultTodayMatching(nowMillis) { it.mode == MODE_DAILY }

    private fun hasResultTodayMatching(
        nowMillis: Long,
        predicate: (TestResult) -> Boolean
    ): Boolean {
        val zone = ZoneId.systemDefault()
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        return getAll().any {
            predicate(it) && Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() == today
        }
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

    fun clear() = prefs.edit().remove(AppStorageContract.KEY_HISTORY_RESULTS).apply()

    private fun save(items: List<TestResult>) {
        prefs.edit().putString(AppStorageContract.KEY_HISTORY_RESULTS, gson.toJson(items)).apply()
    }

    companion object {
        const val MODE_DAILY = "daily"
        private const val MAX_RESULTS = 500
    }
}
