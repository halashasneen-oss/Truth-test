package com.halashasneen.truthtest.data

import android.content.Context
import com.halashasneen.truthtest.data.model.TestResult

enum class AchievementKey(val storageKey: String) {
    FIRST("first"),
    TEN("ten"),
    FIFTY("fifty"),
    HUNDRED("hundred"),
    HIGH("high"),
    STREAK_3("streak_3"),
    STREAK_7("streak_7"),
    DUEL("duel"),
    GROUP("group"),
    CUSTOM("custom");

    companion object {
        fun fromStorage(value: String): AchievementKey? = entries.firstOrNull { it.storageKey == value }
    }
}

object AchievementEngine {
    fun earned(results: List<TestResult>): Set<AchievementKey> {
        val earned = linkedSetOf<AchievementKey>()
        val bestStreak = StatisticsCalculator.calculate(results).bestStreak
        if (results.isNotEmpty()) earned += AchievementKey.FIRST
        if (results.size >= 10) earned += AchievementKey.TEN
        if (results.size >= 50) earned += AchievementKey.FIFTY
        if (results.size >= 100) earned += AchievementKey.HUNDRED
        if (results.any { it.score >= 95 }) earned += AchievementKey.HIGH
        if (bestStreak >= 3) earned += AchievementKey.STREAK_3
        if (bestStreak >= 7) earned += AchievementKey.STREAK_7
        if (results.any { it.mode == "duel_p2" }) earned += AchievementKey.DUEL
        if (results.any { it.mode == "group3_p3" || it.mode == "group4_p4" }) earned += AchievementKey.GROUP
        if (results.any { it.mode == "custom" }) earned += AchievementKey.CUSTOM
        return earned
    }
}

/** Keeps earned achievements permanently, even if local history is later cleared. */
class AchievementRepository(context: Context) {
    private val prefs = context.getSharedPreferences("truth_test_achievements", Context.MODE_PRIVATE)

    fun sync(results: List<TestResult>): Set<AchievementKey> {
        val merged = prefs.getStringSet(KEY_UNLOCKED, emptySet()).orEmpty().toMutableSet()
        merged += AchievementEngine.earned(results).map { it.storageKey }
        prefs.edit().putStringSet(KEY_UNLOCKED, merged).apply()
        return merged.mapNotNull { AchievementKey.fromStorage(it) }.toSet()
    }

    fun unlocked(): Set<AchievementKey> =
        prefs.getStringSet(KEY_UNLOCKED, emptySet()).orEmpty()
            .mapNotNull { AchievementKey.fromStorage(it) }
            .toSet()

    private companion object {
        const val KEY_UNLOCKED = "unlocked"
    }
}
