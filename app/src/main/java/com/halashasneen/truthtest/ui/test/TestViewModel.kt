package com.halashasneen.truthtest.ui.test

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.halashasneen.truthtest.audio.VoiceAnalysis
import com.halashasneen.truthtest.data.AchievementRepository
import com.halashasneen.truthtest.data.HistoryRepository
import com.halashasneen.truthtest.data.QuestionRepository
import com.halashasneen.truthtest.data.model.Question
import com.halashasneen.truthtest.data.model.TestResult
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TestViewModel(application: Application) : AndroidViewModel(application) {
    private val questions = QuestionRepository(application)
    private val history = HistoryRepository(application)
    private val achievements = AchievementRepository(application)
    private val _state = MutableStateFlow(TestUiState())
    val state: StateFlow<TestUiState> = _state.asStateFlow()

    fun configure(mode: String, daily: Boolean, requestedPlayerCount: Int = 1) {
        if (_state.value.initialized) return
        val initialStage = if (mode == TestActivity.MODE_CUSTOM) TestStage.CUSTOM else TestStage.CATEGORY
        val playerCount = when (mode) {
            TestActivity.MODE_DUEL -> 2
            TestActivity.MODE_GROUP -> requestedPlayerCount.coerceIn(3, 4)
            else -> 1
        }
        val intensity = questions.preferredIntensity()
        _state.value = TestUiState(
            initialized = true,
            mode = mode,
            daily = daily,
            stage = initialStage,
            selectedIntensity = intensity,
            playerCount = playerCount
        )
        if (daily) questions.dailyQuestion(intensity)?.let(::beginQuestion)
    }

    fun categoryCount(category: String): Int = questions.count(category, _state.value.selectedIntensity)

    fun setIntensity(intensity: String) {
        val clean = if (intensity in QuestionRepository.INTENSITIES) {
            intensity
        } else {
            QuestionRepository.INTENSITY_MEDIUM
        }
        questions.setPreferredIntensity(clean)
        _state.value = _state.value.copy(selectedIntensity = clean)
    }

    fun chooseCategory(category: String): Boolean {
        val question = questions.random(category, _state.value.selectedIntensity) ?: return false
        beginQuestion(question)
        return true
    }

    fun useCustomQuestion(text: String): Boolean {
        val clean = text.trim()
        if (clean.isBlank()) return false
        beginQuestion(Question("custom-" + System.currentTimeMillis(), "custom", clean, null))
        return true
    }

    fun beginAnalysis(analysis: VoiceAnalysis) {
        val current = _state.value
        if (current.stage != TestStage.RECORDING) return
        _state.value = current.copy(stage = TestStage.ANALYZING, analysis = analysis)
    }

    fun completeAnalysis(): Boolean {
        val current = _state.value
        val analysis = current.analysis ?: return false
        if (current.stage != TestStage.ANALYZING) return false
        return commitScore(analysis)
    }

    fun reset() {
        val current = _state.value
        if (current.daily) {
            val dailyQuestion = questions.dailyQuestion(current.selectedIntensity)
            if (dailyQuestion != null) {
                _state.value = TestUiState(
                    initialized = true,
                    mode = current.mode,
                    daily = true,
                    stage = TestStage.RECORDING,
                    question = dailyQuestion,
                    selectedIntensity = current.selectedIntensity,
                    playerCount = 1
                )
                return
            }
        }
        _state.value = TestUiState(
            initialized = true,
            mode = current.mode,
            daily = false,
            stage = if (current.mode == TestActivity.MODE_CUSTOM) TestStage.CUSTOM else TestStage.CATEGORY,
            selectedIntensity = current.selectedIntensity,
            playerCount = current.playerCount
        )
    }

    private fun beginQuestion(question: Question) {
        _state.value = _state.value.copy(
            stage = TestStage.RECORDING,
            question = question,
            player = 1,
            playerScores = emptyList(),
            playerAnalyses = emptyList(),
            analysis = null,
            finalScore = 0
        )
    }

    private fun commitScore(analysis: VoiceAnalysis): Boolean {
        val current = _state.value
        val question = current.question ?: return false
        val cleanScore = analysis.score.coerceIn(0, 100)
        val updatedScores = current.playerScores + cleanScore
        val updatedAnalyses = current.playerAnalyses + analysis
        save(question, cleanScore, historyMode(current))

        if (current.player < current.playerCount) {
            _state.value = current.copy(
                stage = TestStage.RECORDING,
                player = current.player + 1,
                playerScores = updatedScores,
                playerAnalyses = updatedAnalyses,
                analysis = null
            )
            return true
        }

        val bestIndex = updatedScores.indices.maxByOrNull { updatedScores[it] } ?: 0
        _state.value = current.copy(
            stage = TestStage.RESULT,
            playerScores = updatedScores,
            playerAnalyses = updatedAnalyses,
            analysis = updatedAnalyses.getOrNull(bestIndex),
            finalScore = updatedScores.getOrElse(bestIndex) { cleanScore }
        )
        return false
    }

    private fun historyMode(state: TestUiState): String = when {
        state.daily && state.mode == TestActivity.MODE_SOLO -> HistoryRepository.MODE_DAILY
        state.mode == TestActivity.MODE_DUEL -> "duel_p" + state.player
        state.mode == TestActivity.MODE_GROUP -> "group" + state.playerCount + "_p" + state.player
        else -> state.mode
    }

    private fun save(question: Question, score: Int, mode: String) {
        history.add(
            TestResult(
                UUID.randomUUID().toString(),
                question.text,
                question.category,
                score,
                System.currentTimeMillis(),
                mode,
                question.intensity
            )
        )
        achievements.sync(history.getAll())
    }
}
