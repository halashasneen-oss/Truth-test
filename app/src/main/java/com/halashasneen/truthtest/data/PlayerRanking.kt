package com.halashasneen.truthtest.data

data class PlayerStanding(
    val playerNumber: Int,
    val score: Int,
    val rank: Int
)

object PlayerRanking {
    fun standings(scores: List<Int>): List<PlayerStanding> {
        val sorted = scores
            .mapIndexed { index, score -> index + 1 to score.coerceIn(0, 100) }
            .sortedWith(compareByDescending<Pair<Int, Int>> { it.second }.thenBy { it.first })

        var previousScore: Int? = null
        var previousRank = 0
        return sorted.mapIndexed { index, (playerNumber, score) ->
            val rank = if (score == previousScore) previousRank else index + 1
            previousScore = score
            previousRank = rank
            PlayerStanding(playerNumber, score, rank)
        }
    }

    fun winners(scores: List<Int>): List<Int> {
        val top = scores.maxOrNull() ?: return emptyList()
        return scores.mapIndexedNotNull { index, score -> if (score == top) index + 1 else null }
    }
}
