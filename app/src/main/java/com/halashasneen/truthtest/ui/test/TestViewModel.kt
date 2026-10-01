package com.halashasneen.truthtest.ui.test

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.halashasneen.truthtest.audio.VoiceAnalysis
import com.halashasneen.truthtest.data.AchievementRepository
import com.halashasneen.truthtest.data.HistoryRepository
import com.halashasneen.truthtest.data.PlayerProfileRepository
import com.halashasneen.truthtest.data.QuestionRepository
import com.halashasneen.truthtest.data.SocialSessionRepository
import com.halashasneen.truthtest.data.XpEngine
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
    private val profiles = PlayerProfileRepository(application)
    private val sessions = SocialSessionRepository(application)

    private val _state = MutableStateFlow(TestUiState())
    val state: StateFlow<TestUiState> = _state.asStateFlow()

    fun configure(
        mode: String,
        daily: Boolean,
        requestedPlayerCount: Int = 1,
        requestedQuestionCount: Int = 1,
        forcedQuestionText: String? = null,
        forcedQuestionCategory: String? = null,
        forcedQuestionIntensity: String? = null,
        historyModeOverride: String? = null,
        sessionPlayerName: String? = null
    ) {
        if (_state.value.initialized) return

        val intensity = forcedQuestionIntensity
            ?.takeIf { it in QuestionRepository.INTENSITIES }
            ?: questions.preferredIntensity()

        if (!forcedQuestionText.isNullOrBlank()) {
            _state.value = TestUiState(
                initialized = true,
                mode = mode,
                daily = false,
                stage = TestStage.RECORDING,
                question = Question(
                    id = "session-" + System.currentTimeMillis(),
                    category = forcedQuestionCategory ?: "social",
                    text = forcedQuestionText,
                    intensity = intensity
                ),
                selectedIntensity = intensity,
                playerCount = 1,
                historyModeOverride = historyModeOverride,
                sessionPlayerName = sessionPlayerName,
                externalSession = true
            )
            return
        }

        val initialStage =
            if (mode == TestActivity.MODE_CUSTOM) TestStage.CUSTOM else TestStage.CATEGORY
        val playerCount = when (mode) {
            TestActivity.MODE_DUEL -> 2
            TestActivity.MODE_GROUP -> requestedPlayerCount.coerceIn(3, 4)
            else -> 1
        }

        _state.value = TestUiState(
            initialized = true,
            mode = mode,
            daily = daily,
            stage = initialStage,
            selectedIntensity = intensity,
            playerCount = playerCount,
            totalQuestions = if (mode == TestActivity.MODE_SOLO && !daily) requestedQuestionCount.coerceIn(1, 10) else 1,
            requestedQuestions = if (mode == TestActivity.MODE_SOLO && !daily) requestedQuestionCount.coerceIn(1, 10) else 1
        )

        if (daily) questions.dailyQuestion(intensity)?.let(::beginQuestion)
    }

    fun categoryCount(category: String): Int =
        questions.count(category, _state.value.selectedIntensity)

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

    /** Called only after the completed result has been shown and the user taps Next. */
    fun nextQuestion(): Boolean {
        val current = _state.value
        if (current.externalSession || current.daily || current.mode != TestActivity.MODE_SOLO ||
            current.stage != TestStage.RESULT || current.questionNumber >= current.totalQuestions
        ) return false
        val next = questions.nextUnseen(
            current.selectedCategory ?: current.question?.category ?: "funny",
            current.selectedIntensity,
            current.usedQuestionIds
        ) ?: run {
            _state.value = current.copy(totalQuestions = current.questionNumber)
            return false
        }
        _state.value = current.copy(
            stage = TestStage.RECORDING,
            question = next,
            questionNumber = current.questionNumber + 1,
            usedQuestionIds = current.usedQuestionIds + next.id,
            player = 1,
            playerScores = emptyList(),
            playerAnalyses = emptyList(),
            analysis = null,
            finalScore = 0
        )
        return true
    }

    /** Explicit early finish retains the results already saved once per completed question. */
    fun finishEarly() {
        val current = _state.value
        if (current.stage == TestStage.RESULT && current.mode == TestActivity.MODE_SOLO &&
            !current.daily && !current.externalSession && current.questionNumber > 0
        ) _state.value = current.copy(totalQuestions = current.questionNumber)
    }

    fun useCustomQuestion(text: String): Boolean {
        val clean = text.trim()
        if (clean.isBlank()) return false
        beginQuestion(
            Question(
                id = "custom-" + System.currentTimeMillis(),
                category = "custom",
                text = clean,
                intensity = null
            )
        )
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
        if (current.externalSession) return

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
            stage = if (current.mode == TestActivity.MODE_CUSTOM) {
                TestStage.CUSTOM
            } else {
                TestStage.CATEGORY
            },
            selectedIntensity = current.selectedIntensity,
            playerCount = current.playerCount,
            totalQuestions = current.requestedQuestions,
            requestedQuestions = current.requestedQuestions
        )
    }

    private fun beginQuestion(question: Question) {
        _state.value = _state.value.copy(
            stage = TestStage.RECORDING,
            question = question,
            selectedCategory = question.category,
            usedQuestionIds = _state.value.usedQuestionIds + question.id,
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

        val firstDailyToday = current.daily && !history.hasDailyResultToday()
        save(
            question = question,
            score = cleanScore,
            mode = historyMode(current),
            awardProgression = !current.externalSession && (!current.daily || firstDailyToday),
            daily = current.daily && firstDailyToday
        )

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
            finalScore = updatedScores.getOrElse(bestIndex) { cleanScore },
            completedQuestionScores = if (current.mode == TestActivity.MODE_SOLO && !current.daily && !current.externalSession) {
                current.completedQuestionScores + cleanScore
            } else current.completedQuestionScores
        )
        return false
    }

    private fun historyMode(state: TestUiState): String =
        state.historyModeOverride ?: when {
            state.daily && state.mode == TestActivity.MODE_SOLO -> HistoryRepository.MODE_DAILY
            state.mode == TestActivity.MODE_DUEL -> "duel_p" + state.player
            state.mode == TestActivity.MODE_GROUP -> "group" + state.playerCount + "_p" + state.player
            else -> state.mode
        }

    private fun syncAchievementsAndXp() {
        var known = achievements.unlocked()
        repeat(3) {
            val updated = achievements.sync(
                results = history.getAll(),
                totalXp = profiles.totalXp(),
                completedSessions = sessions.completedCount()
            )
            val newlyUnlocked = updated - known
            if (newlyUnlocked.isEmpty()) return
            profiles.addXp(
                profiles.active().id,
                XpEngine.achievementXp(newlyUnlocked.size)
            )
            known = updated
        }
    }

    private fun save(
        question: Question,
        score: Int,
        mode: String,
        awardProgression: Boolean,
        daily: Boolean
    ) {
        history.add(
            TestResult(
                id = UUID.randomUUID().toString(),
                question = question.text,
                category = question.category,
                score = score,
                timestamp = System.currentTimeMillis(),
                mode = mode,
                intensity = question.intensity
            )
        )

        if (awardProgression) {
            val active = profiles.active()
            profiles.recordResult(
                id = active.id,
                score = score,
                xpAward = XpEngine.resultXp(score, daily)
            )
        }

        if (!mode.startsWith("social_")) {
            syncAchievementsAndXp()
        }
    }
}
