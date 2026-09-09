package com.nuvexa.truthtest.ui.test

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.nuvexa.truthtest.data.HistoryRepository
import com.nuvexa.truthtest.data.QuestionRepository
import com.nuvexa.truthtest.data.model.Question
import com.nuvexa.truthtest.data.model.TestResult
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TestViewModel(application: Application) : AndroidViewModel(application) {
    private val questions = QuestionRepository(application)
    private val history = HistoryRepository(application)

    private val _state = MutableStateFlow(TestUiState())
    val state: StateFlow<TestUiState> = _state.asStateFlow()

    fun configure(mode: String, daily: Boolean) {
        if (_state.value.initialized) return
        val initialStage = if (mode == TestActivity.MODE_CUSTOM) TestStage.CUSTOM else TestStage.CATEGORY
        _state.value = TestUiState(initialized = true, mode = mode, stage = initialStage)
        if (daily) questions.dailyQuestion()?.let(::beginQuestion)
    }

    fun chooseCategory(category: String): Boolean {
        val question = questions.random(category) ?: return false
        beginQuestion(question)
        return true
    }

    fun useCustomQuestion(text: String): Boolean {
        val clean = text.trim()
        if (clean.isBlank()) return false
        beginQuestion(Question("custom-${System.currentTimeMillis()}", "custom", clean))
        return true
    }

    private fun beginQuestion(question: Question) {
        _state.value = _state.value.copy(
            stage = TestStage.RECORDING,
            question = question,
            player = 1,
            firstScore = null,
            secondScore = null,
            finalScore = 0
        )
    }

    /** Returns true when Duel Mode should hand the phone to player 2. */
    fun submitScore(score: Int): Boolean {
        val current = _state.value
        val question = current.question ?: return false
        if (current.mode == TestActivity.MODE_DUEL && current.player == 1) {
            save(question, score, "duel_p1")
            _state.value = current.copy(player = 2, firstScore = score)
            return true
        }

        save(question, score, if (current.mode == TestActivity.MODE_DUEL) "duel_p2" else current.mode)
        val second = if (current.mode == TestActivity.MODE_DUEL) score else null
        val finalScore = if (current.mode == TestActivity.MODE_DUEL) maxOf(current.firstScore ?: 0, score) else score
        _state.value = current.copy(
            stage = TestStage.RESULT,
            secondScore = second,
            finalScore = finalScore
        )
        return false
    }

    fun reset() {
        val current = _state.value
        _state.value = TestUiState(
            initialized = true,
            mode = current.mode,
            stage = if (current.mode == TestActivity.MODE_CUSTOM) TestStage.CUSTOM else TestStage.CATEGORY
        )
    }

    private fun save(question: Question, score: Int, mode: String) {
        history.add(
            TestResult(
                id = UUID.randomUUID().toString(),
                question = question.text,
                category = question.category,
                score = score,
                timestamp = System.currentTimeMillis(),
                mode = mode
            )
        )
    }
}
