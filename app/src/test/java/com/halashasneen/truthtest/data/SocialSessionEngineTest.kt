package com.halashasneen.truthtest.data

import com.halashasneen.truthtest.data.model.SocialSession
import com.halashasneen.truthtest.data.model.SocialTurn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialSessionEngineTest {
    @Test
    fun ranksPlayersByAverageThenBestScore() {
        val session = SocialSession(
            id = "s",
            mode = "friends",
            pack = "friends",
            playerIds = listOf("a", "b", "c"),
            playerNames = listOf("A", "B", "C"),
            targetRounds = 2,
            turns = listOf(
                turn("a", "A", 80),
                turn("b", "B", 90),
                turn("c", "C", 70),
                turn("a", "A", 90),
                turn("b", "B", 70),
                turn("c", "C", 80)
            ),
            startedAt = 1L
        )

        val standings = SocialSessionEngine.standings(session)
        assertEquals("a", standings.first().playerId)
        assertEquals(85, standings.first().averageScore)
        assertEquals(setOf("a"), SocialSessionEngine.winnerIds(session))
        assertTrue(session.isComplete)
    }

    @Test
    fun sessionTracksNextPlayerAndRoundForResume() {
        val session = SocialSession(
            id = "s",
            mode = "party",
            pack = "funny",
            playerIds = listOf("a", "b", "c"),
            playerNames = listOf("A", "B", "C"),
            targetRounds = 3,
            turns = listOf(
                turn("a", "A", 80),
                turn("b", "B", 70),
                turn("c", "C", 75),
                turn("a", "A", 82)
            ),
            startedAt = 1L
        )

        assertEquals(1, session.nextPlayerIndex)
        assertEquals(2, session.currentRound)
        assertEquals(9, session.totalTurns)
    }

    private fun turn(id: String, name: String, score: Int) = SocialTurn(
        questionId = "q-$id-$score",
        questionText = "Q",
        playerId = id,
        playerName = name,
        score = score,
        timestamp = score.toLong()
    )
}
