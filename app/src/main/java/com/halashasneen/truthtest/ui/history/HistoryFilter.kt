package com.halashasneen.truthtest.ui.history

import com.halashasneen.truthtest.data.HistoryRepository
import com.halashasneen.truthtest.data.QuestionRepository
import com.halashasneen.truthtest.data.model.TestResult

enum class HistoryFilter {
    ALL,
    SOLO,
    DAILY,
    DUEL,
    GROUP,
    CUSTOM,
    LIGHT,
    MEDIUM,
    BOLD;

    fun matches(result: TestResult): Boolean = when (this) {
        ALL -> true
        SOLO -> result.mode == "solo"
        DAILY -> result.mode == HistoryRepository.MODE_DAILY
        DUEL -> result.mode.startsWith("duel_")
        GROUP -> result.mode.startsWith("group")
        CUSTOM -> result.mode == "custom"
        LIGHT -> result.intensity == QuestionRepository.INTENSITY_LIGHT
        MEDIUM -> result.intensity == QuestionRepository.INTENSITY_MEDIUM
        BOLD -> result.intensity == QuestionRepository.INTENSITY_BOLD
    }
}
