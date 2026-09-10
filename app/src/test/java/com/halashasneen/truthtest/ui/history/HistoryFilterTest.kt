package com.halashasneen.truthtest.ui.history

import com.halashasneen.truthtest.data.QuestionRepository
import com.halashasneen.truthtest.data.model.TestResult
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryFilterTest {
    @Test
    fun filtersModesAndIntensity() {
        val duel = result("duel_p2", QuestionRepository.INTENSITY_BOLD)
        val group = result("group4_p4", QuestionRepository.INTENSITY_MEDIUM)
        val solo = result("solo", QuestionRepository.INTENSITY_LIGHT)

        assertTrue(HistoryFilter.DUEL.matches(duel))
        assertTrue(HistoryFilter.GROUP.matches(group))
        assertTrue(HistoryFilter.SOLO.matches(solo))
        assertTrue(HistoryFilter.BOLD.matches(duel))
        assertTrue(HistoryFilter.MEDIUM.matches(group))
        assertTrue(HistoryFilter.LIGHT.matches(solo))
        assertFalse(HistoryFilter.GROUP.matches(duel))
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
