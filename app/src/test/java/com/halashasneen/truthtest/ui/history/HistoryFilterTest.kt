package com.halashasneen.truthtest.ui.history

import com.halashasneen.truthtest.data.HistoryRepository
import com.halashasneen.truthtest.data.QuestionRepository
import com.halashasneen.truthtest.data.model.TestResult
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryFilterTest {
    @Test
    fun filtersLegacySocialAndIntensityModes() {
        val duel = result("duel_p2", QuestionRepository.INTENSITY_BOLD)
        val group = result("group4_p4", QuestionRepository.INTENSITY_MEDIUM)
        val solo = result("solo", QuestionRepository.INTENSITY_LIGHT)
        val daily = result(HistoryRepository.MODE_DAILY, QuestionRepository.INTENSITY_MEDIUM)
        val couples = result("social_couples", QuestionRepository.INTENSITY_MEDIUM)
        val friends = result("social_friends", QuestionRepository.INTENSITY_LIGHT)
        val party = result("social_party", QuestionRepository.INTENSITY_BOLD)
        val challenge = result("social_challenge", QuestionRepository.INTENSITY_MEDIUM)

        assertTrue(HistoryFilter.DUEL.matches(duel))
        assertTrue(HistoryFilter.GROUP.matches(group))
        assertTrue(HistoryFilter.SOLO.matches(solo))
        assertTrue(HistoryFilter.DAILY.matches(daily))
        assertTrue(HistoryFilter.COUPLES.matches(couples))
        assertTrue(HistoryFilter.FRIENDS.matches(friends))
        assertTrue(HistoryFilter.PARTY.matches(party))
        assertTrue(HistoryFilter.CHALLENGE.matches(challenge))
        assertTrue(HistoryFilter.BOLD.matches(duel))
        assertTrue(HistoryFilter.MEDIUM.matches(group))
        assertTrue(HistoryFilter.LIGHT.matches(solo))
        assertFalse(HistoryFilter.GROUP.matches(duel))
        assertFalse(HistoryFilter.SOLO.matches(daily))
    }

    private fun result(mode: String, intensity: String) = TestResult(
        id = mode,
        question = "Q",
        category = "funny",
        score = 80,
        timestamp = 1L,
        mode = mode,
        intensity = intensity
    )
}
