package com.halashasneen.truthtest.data

import com.halashasneen.truthtest.data.model.SocialSession

data class SocialStanding(
    val playerId: String,
    val playerName: String,
    val averageScore: Int,
    val bestScore: Int,
    val rank: Int
)

object SocialSessionEngine {
    fun standings(session: SocialSession): List<SocialStanding> {
        val rows = session.playerIds.mapIndexed { index, id ->
            val turns = session.turns.filter { it.playerId == id }
            val average = if (turns.isEmpty()) 0 else turns.sumOf { it.score } / turns.size
            val best = turns.maxOfOrNull { it.score } ?: 0
            Triple(index, average.coerceIn(0, 100), best.coerceIn(0, 100))
        }.sortedWith(
            compareByDescending<Triple<Int, Int, Int>> { it.second }
                .thenByDescending { it.third }
        )

        var previousAverage: Int? = null
        var previousRank = 0
        return rows.mapIndexed { position, (profileIndex, average, best) ->
            val rank = if (average == previousAverage) previousRank else position + 1
            previousAverage = average
            previousRank = rank
            SocialStanding(
                playerId = session.playerIds[profileIndex],
                playerName = session.playerNames.getOrElse(profileIndex) { "Player ${profileIndex + 1}" },
                averageScore = average,
                bestScore = best,
                rank = rank
            )
        }
    }

    fun winnerIds(session: SocialSession): Set<String> {
        val standings = standings(session)
        val topRank = standings.minOfOrNull { it.rank } ?: return emptySet()
        return standings.filter { it.rank == topRank }.map { it.playerId }.toSet()
    }

    fun highestTurn(session: SocialSession) = session.turns.maxByOrNull { it.score }
}
