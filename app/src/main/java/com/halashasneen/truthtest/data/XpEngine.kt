package com.halashasneen.truthtest.data

object XpEngine {
    private const val XP_PER_LEVEL = 250

    fun resultXp(score: Int, daily: Boolean = false): Int {
        val clean = score.coerceIn(0, 100)
        return 10 + clean / 10 + (if (clean >= 90) 5 else 0) + (if (daily) 15 else 0)
    }

    fun sessionCompletionXp(winner: Boolean): Int = 20 + if (winner) 10 else 0

    fun achievementXp(unlockedCount: Int): Int =
        unlockedCount.coerceAtLeast(0) * 25

    fun levelForXp(xp: Int): Int = 1 + xp.coerceAtLeast(0) / XP_PER_LEVEL

    fun levelProgress(xp: Int): Int = xp.coerceAtLeast(0) % XP_PER_LEVEL

    fun xpToNextLevel(xp: Int): Int = XP_PER_LEVEL - levelProgress(xp)

    const val levelSize: Int = XP_PER_LEVEL
}
