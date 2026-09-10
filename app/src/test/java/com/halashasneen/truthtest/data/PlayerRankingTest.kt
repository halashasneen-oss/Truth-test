package com.halashasneen.truthtest.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerRankingTest {
    @Test
    fun ranksPlayersHighestFirst() {
        assertEquals(
            listOf(
                PlayerStanding(2, 92, 1),
                PlayerStanding(3, 81, 2),
                PlayerStanding(1, 67, 3)
            ),
            PlayerRanking.standings(listOf(67, 92, 81))
        )
        assertEquals(listOf(2), PlayerRanking.winners(listOf(67, 92, 81)))
    }

    @Test
    fun preservesSharedRankForTies() {
        assertEquals(
            listOf(
                PlayerStanding(1, 90, 1),
                PlayerStanding(3, 90, 1),
                PlayerStanding(2, 70, 3),
                PlayerStanding(4, 50, 4)
            ),
            PlayerRanking.standings(listOf(90, 70, 90, 50))
        )
        assertEquals(listOf(1, 3), PlayerRanking.winners(listOf(90, 70, 90, 50)))
    }
}
