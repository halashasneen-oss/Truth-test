package com.halashasneen.truthtest.data.model

data class PlayerProfile(
    val id: String,
    val name: String,
    val avatar: String,
    val xp: Int = 0,
    val games: Int = 0,
    val bestScore: Int = 0,
    val totalScore: Int = 0
) {
    val averageScore: Int
        get() = if (games == 0) 0 else (totalScore.toDouble() / games).toInt().coerceIn(0, 100)
}
