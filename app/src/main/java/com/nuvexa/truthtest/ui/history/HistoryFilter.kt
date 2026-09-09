package com.nuvexa.truthtest.ui.history

import com.nuvexa.truthtest.data.QuestionRepository
import com.nuvexa.truthtest.data.model.TestResult

enum class HistoryFilter {
    ALL,
    SOLO,
    DUEL,
    GROUP,
    CUSTOM,
    LIGHT,
    MEDIUM,
    BOLD;

    fun matches(result: TestResult): Boolean = when (this) {
        ALL -> true
        SOLO -> result.mode == "solo"
        DUEL -> result.mode.startsWith("duel_")
        GROUP -> result.mode.startsWith("group")
        CUSTOM -> result.mode == "custom"
        LIGHT -> result.intensity == QuestionRepository.INTENSITY_LIGHT
        MEDIUM -> result.intensity == QuestionRepository.INTENSITY_MEDIUM
        BOLD -> result.intensity == QuestionRepository.INTENSITY_BOLD
    }
}
