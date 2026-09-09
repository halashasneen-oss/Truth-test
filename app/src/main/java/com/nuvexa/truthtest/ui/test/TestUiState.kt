package com.nuvexa.truthtest.ui.test

import com.nuvexa.truthtest.data.QuestionRepository
import com.nuvexa.truthtest.data.model.Question

enum class TestStage { CATEGORY, CUSTOM, RECORDING, RESULT }

data class TestUiState(
    val initialized: Boolean = false,
    val mode: String = TestActivity.MODE_SOLO,
    val stage: TestStage = TestStage.CATEGORY,
    val question: Question? = null,
    val selectedIntensity: String = QuestionRepository.INTENSITY_MEDIUM,
    val player: Int = 1,
    val playerCount: Int = 1,
    val playerScores: List<Int> = emptyList(),
    val finalScore: Int = 0
) {
    val firstScore: Int? get() = playerScores.getOrNull(0)
    val secondScore: Int? get() = playerScores.getOrNull(1)
}
