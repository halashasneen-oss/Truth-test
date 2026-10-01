package com.halashasneen.truthtest.data

import android.content.Context
import com.halashasneen.truthtest.core.AppStorageContract
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
    CUSTOM("custom"),
    COUPLES("couples"),
    FRIENDS("friends"),
    PARTY("party"),
    DAILY("daily"),
    CHALLENGE("challenge"),
    XP_500("xp_500"),
    SESSIONS_5("sessions_5");

    companion object {
        fun fromStorage(value: String): AchievementKey? =
            entries.firstOrNull { it.storageKey == value }
    }
}

object AchievementEngine {
    fun earned(
        results: List<TestResult>,
        totalXp: Int = 0,
        completedSessions: Int = 0
    ): Set<AchievementKey> {
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

        if (results.any { it.mode == "social_couples" }) earned += AchievementKey.COUPLES
        if (results.any { it.mode == "social_friends" }) earned += AchievementKey.FRIENDS
        if (results.any { it.mode == "social_party" }) earned += AchievementKey.PARTY
        if (results.any { it.mode == HistoryRepository.MODE_DAILY }) earned += AchievementKey.DAILY
        if (results.any { it.mode == "social_challenge" }) earned += AchievementKey.CHALLENGE
        if (totalXp >= 500) earned += AchievementKey.XP_500
        if (completedSessions >= 5) earned += AchievementKey.SESSIONS_5

        return earned
    }
}

class AchievementRepository(context: Context) {
    private val prefs = context.getSharedPreferences(
        AppStorageContract.PREFS_ACHIEVEMENTS,
        Context.MODE_PRIVATE
    )

    fun sync(
        results: List<TestResult>,
        totalXp: Int = 0,
        completedSessions: Int = 0
    ): Set<AchievementKey> {
        val merged = prefs.getStringSet(
            AppStorageContract.KEY_ACHIEVEMENTS_UNLOCKED,
            emptySet()
        ).orEmpty().toMutableSet()

        merged += AchievementEngine.earned(
            results = results,
            totalXp = totalXp,
            completedSessions = completedSessions
        ).map { it.storageKey }

        prefs.edit()
            .putStringSet(AppStorageContract.KEY_ACHIEVEMENTS_UNLOCKED, merged)
            .apply()

        return merged.mapNotNull { AchievementKey.fromStorage(it) }.toSet()
    }

    fun unlocked(): Set<AchievementKey> =
        prefs.getStringSet(AppStorageContract.KEY_ACHIEVEMENTS_UNLOCKED, emptySet())
            .orEmpty()
            .mapNotNull { AchievementKey.fromStorage(it) }
            .toSet()
}
