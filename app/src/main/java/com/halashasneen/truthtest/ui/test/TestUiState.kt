package com.halashasneen.truthtest.ui.test

import com.halashasneen.truthtest.audio.VoiceAnalysis
import com.halashasneen.truthtest.data.QuestionRepository
import com.halashasneen.truthtest.data.model.Question

enum class TestStage { CATEGORY, CUSTOM, RECORDING, ANALYZING, RESULT }

data class TestUiState(
    val initialized: Boolean = false,
    val mode: String = TestActivity.MODE_SOLO,
    val daily: Boolean = false,
    val stage: TestStage = TestStage.CATEGORY,
    val question: Question? = null,
    val selectedIntensity: String = QuestionRepository.INTENSITY_MEDIUM,
    val player: Int = 1,
    val playerCount: Int = 1,
    val playerScores: List<Int> = emptyList(),
    val playerAnalyses: List<VoiceAnalysis> = emptyList(),
    val analysis: VoiceAnalysis? = null,
    val finalScore: Int = 0,
    val historyModeOverride: String? = null,
    val sessionPlayerName: String? = null,
    val externalSession: Boolean = false
) {
    val firstScore: Int? get() = playerScores.getOrNull(0)
    val secondScore: Int? get() = playerScores.getOrNull(1)
}
