package com.halashasneen.truthtest.ui.test

import android.Manifest
import android.animation.ValueAnimator
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.halashasneen.truthtest.R
import com.halashasneen.truthtest.audio.AudioRecorderEngine
import com.halashasneen.truthtest.audio.VoiceAnalyzer
import com.halashasneen.truthtest.core.AppStorageContract
import com.halashasneen.truthtest.data.PlayerRanking
import com.halashasneen.truthtest.data.QuestionRepository
import com.halashasneen.truthtest.databinding.ActivityTestBinding
import com.halashasneen.truthtest.share.ResultCardRenderer
import com.halashasneen.truthtest.share.ResultVideoShareRenderer
import com.halashasneen.truthtest.share.ShareSound
import com.halashasneen.truthtest.share.ShareTheme
import com.halashasneen.truthtest.share.ShareThemeContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TestActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTestBinding
    private val viewModel: TestViewModel by viewModels()
    private val recorder = AudioRecorderEngine()
    private var isRecording = false
    private var startedAt = 0L
    private var renderedPlayer = 0
    private var lastStage: TestStage? = null
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var categorySpecs: List<CategorySpec>

    private data class CategorySpec(
        val button: MaterialButton,
        val key: String,
        val labelRes: Int,
        val colorRes: Int
    )

    private val timer = object : Runnable {
        override fun run() {
            if (!isRecording) return
            val seconds = ((SystemClock.elapsedRealtime() - startedAt) / 1000).toInt()
            binding.timerText.text = "%02d:%02d".format(seconds / 60, seconds % 60)
            handler.postDelayed(this, 250)
        }
    }

    private val micPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startRecordingInternal()
            else Toast.makeText(this, R.string.permission_audio, Toast.LENGTH_LONG).show()
        }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        binding = ActivityTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.configure(
            mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_SOLO,
            daily = intent.getBooleanExtra(EXTRA_DAILY, false),
            requestedPlayerCount = intent.getIntExtra(EXTRA_PLAYER_COUNT, 1),
            forcedQuestionText = intent.getStringExtra(EXTRA_FORCED_QUESTION_TEXT),
            forcedQuestionCategory = intent.getStringExtra(EXTRA_FORCED_QUESTION_CATEGORY),
            forcedQuestionIntensity = intent.getStringExtra(EXTRA_FORCED_QUESTION_INTENSITY),
            historyModeOverride = intent.getStringExtra(EXTRA_HISTORY_MODE_OVERRIDE),
            sessionPlayerName = intent.getStringExtra(EXTRA_SESSION_PLAYER_NAME)
        )

        bindIntensity()
        bindCategories()
        bindActions()
        observeState()
    }

    private fun bindIntensity() {
        when (viewModel.state.value.selectedIntensity) {
            QuestionRepository.INTENSITY_LIGHT -> binding.intensityLight.isChecked = true
            QuestionRepository.INTENSITY_BOLD -> binding.intensityBold.isChecked = true
            else -> binding.intensityMedium.isChecked = true
        }

        binding.intensityGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val intensity = when (checkedIds.firstOrNull()) {
                R.id.intensityLight -> QuestionRepository.INTENSITY_LIGHT
                R.id.intensityBold -> QuestionRepository.INTENSITY_BOLD
                else -> QuestionRepository.INTENSITY_MEDIUM
            }
            if (intensity != viewModel.state.value.selectedIntensity) {
                viewModel.setIntensity(intensity)
                refreshCategoryCounts()
            }
        }
    }

    private fun bindCategories() {
        categorySpecs = listOf(
            CategorySpec(binding.categoryEmbarrassing, "embarrassing", R.string.embarrassing, R.color.orange),
            CategorySpec(binding.categoryFunny, "funny", R.string.funny, R.color.warning),
            CategorySpec(binding.categoryBold, "bold", R.string.bold, R.color.danger),
            CategorySpec(binding.categoryRomantic, "romantic", R.string.romantic, R.color.pink),
            CategorySpec(binding.categoryFriendship, "friendship", R.string.friendship, R.color.cyan),
            CategorySpec(binding.categoryFamily, "family", R.string.family, R.color.success)
        )

        categorySpecs.forEach { spec ->
            decorateCategory(spec)
            spec.button.setOnClickListener { viewModel.chooseCategory(spec.key) }
        }
    }

    private fun refreshCategoryCounts() {
        if (!::categorySpecs.isInitialized) return
        categorySpecs.forEach(::decorateCategory)
    }

    private fun decorateCategory(spec: CategorySpec) {
        spec.button.text = getString(
            R.string.category_count_format,
            getString(spec.labelRes),
            viewModel.categoryCount(spec.key)
        )
        spec.button.backgroundTintList = ColorStateList.valueOf(getColor(R.color.p2_surface))
        spec.button.strokeColor = ColorStateList.valueOf(getColor(spec.colorRes))
        spec.button.strokeWidth = dp(1)
        spec.button.setTextColor(getColor(R.color.p2_text_primary))
        spec.button.cornerRadius = dp(18)
    }

    private fun bindActions() {
        binding.testBackButton.setOnClickListener { finish() }
        binding.customContinue.setOnClickListener {
            viewModel.useCustomQuestion(binding.customQuestionInput.text?.toString().orEmpty())
        }
        binding.recordButton.setOnClickListener {
            if (isRecording) stopRecording() else ensureMicAndStart()
        }
        binding.newTestButton.setOnClickListener { viewModel.reset() }
        binding.homeButton.setOnClickListener {
            val state = viewModel.state.value
            if (state.externalSession && state.stage == TestStage.RESULT) {
                returnSessionResult(state)
            } else {
                finish()
            }
        }
        binding.shareButton.setOnClickListener { showShareThemePicker(viewModel.state.value) }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }
    }

    private fun render(state: TestUiState) {
        if (!state.initialized) return
        val stageChanged = lastStage != state.stage

        binding.screenTitle.setText(
            when (state.mode) {
                MODE_DUEL -> R.string.duel_mode
                MODE_GROUP -> R.string.group_mode
                MODE_CUSTOM -> R.string.custom_question
                MODE_SESSION -> R.string.p3_voice_session_title
                else -> R.string.solo_test
            }
        )

        setPanels(
            category = state.stage == TestStage.CATEGORY,
            custom = state.stage == TestStage.CUSTOM,
            record = state.stage == TestStage.RECORDING,
            analysis = state.stage == TestStage.ANALYZING,
            result = state.stage == TestStage.RESULT
        )

        state.question?.let {
            binding.questionText.text = it.text
            binding.questionMeta.text = questionMeta(it.category, it.intensity)
        }

        when (state.stage) {
            TestStage.RECORDING -> renderRecording(state, stageChanged)
            TestStage.ANALYZING -> if (stageChanged) animateAnalysisSteps()
            TestStage.RESULT -> {
                renderResult(state)
                if (stageChanged) animateResultEntrance(state.finalScore)
            }
            else -> Unit
        }

        if (stageChanged) animateCurrentPanel(state.stage)
        lastStage = state.stage
    }

    private fun renderRecording(state: TestUiState, stageChanged: Boolean) {
        binding.playerLabel.text = when {
            !state.sessionPlayerName.isNullOrBlank() -> state.sessionPlayerName
            state.playerCount > 1 -> getString(R.string.player_turn_format, state.player, state.playerCount)
            else -> ""
        }

        if (renderedPlayer != state.player || stageChanged) {
            renderedPlayer = state.player
            binding.waveform.reset()
            binding.pulseRing.reset()
            binding.timerText.text = "00:00"
            binding.recordHint.setText(R.string.record_idle)
            binding.recordButton.contentDescription = getString(R.string.tap_to_record)
        }
    }

    private fun animateCurrentPanel(stage: TestStage) {
        val view = when (stage) {
            TestStage.CATEGORY -> binding.categoryPanel
            TestStage.CUSTOM -> binding.customPanel
            TestStage.RECORDING -> binding.recordPanel
            TestStage.ANALYZING -> binding.analysisPanel
            TestStage.RESULT -> binding.resultPanel
        }
        view.alpha = 0f
        view.translationY = dp(12).toFloat()
        view.animate().alpha(1f).translationY(0f).setDuration(220L).start()
    }

    private fun animateAnalysisSteps() {
        val rows = listOf(
            binding.analysisStepStability to R.string.analysis_stability,
            binding.analysisStepPitch to R.string.analysis_pitch,
            binding.analysisStepPauses to R.string.analysis_pauses,
            binding.analysisStepEnergy to R.string.analysis_energy,
            binding.analysisStepFlow to R.string.analysis_flow
        )
        rows.forEachIndexed { index, pair ->
            val view = pair.first
            view.text = "✓  " + getString(pair.second)
            view.alpha = 0f
            view.translationX = dp(10).toFloat()
            view.animate()
                .alpha(1f)
                .translationX(0f)
                .setStartDelay(index * 85L)
                .setDuration(180L)
                .start()
        }
    }

    private fun animateResultEntrance(score: Int) {
        performResultHaptic()
        binding.resultScore.scaleX = 0.82f
        binding.resultScore.scaleY = 0.82f
        binding.resultScore.animate().scaleX(1f).scaleY(1f).setDuration(280L).start()

        ValueAnimator.ofInt(0, score).apply {
            duration = 620L
            addUpdateListener { animator ->
                val value = animator.animatedValue as Int
                binding.resultScore.text = value.toString() + "%"
                binding.resultProgress.progress = value
            }
            start()
        }
    }

    private fun setPanels(
        category: Boolean = false,
        custom: Boolean = false,
        record: Boolean = false,
        analysis: Boolean = false,
        result: Boolean = false
    ) {
        binding.categoryPanel.visibility = if (category) View.VISIBLE else View.GONE
        binding.customPanel.visibility = if (custom) View.VISIBLE else View.GONE
        binding.recordPanel.visibility = if (record) View.VISIBLE else View.GONE
        binding.analysisPanel.visibility = if (analysis) View.VISIBLE else View.GONE
        binding.resultPanel.visibility = if (result) View.VISIBLE else View.GONE
    }

    private fun ensureMicAndStart() {
        if (
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            startRecordingInternal()
        } else {
            micPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startRecordingInternal() {
        if (
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            isRecording = false
            Toast.makeText(this, R.string.permission_audio, Toast.LENGTH_LONG).show()
            return
        }

        runCatching {
            isRecording = true
            binding.recordButton.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            binding.recordButton.text = "■"
            binding.recordButton.contentDescription = getString(R.string.stop_recording)
            binding.recordHint.setText(R.string.record_live)
            binding.waveform.reset()
            binding.pulseRing.reset()
            startedAt = SystemClock.elapsedRealtime()
            handler.post(timer)
            recorder.start { amplitude ->
                binding.waveform.post {
                    binding.waveform.addAmplitude(amplitude)
                    binding.pulseRing.setAmplitude(amplitude)
                }
            }
        }.onFailure {
            isRecording = false
            binding.recordHint.setText(R.string.record_idle)
            Toast.makeText(this, R.string.permission_audio, Toast.LENGTH_LONG).show()
        }
    }

    private fun stopRecording() {
        isRecording = false
        binding.recordButton.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        handler.removeCallbacks(timer)
        binding.recordButton.text = "●"
        binding.recordButton.contentDescription = getString(R.string.tap_to_record)
        binding.recordHint.setText(R.string.record_idle)
        binding.pulseRing.reset()

        val analysis = VoiceAnalyzer.analyze(recorder.stop())
        if (!analysis.usable) {
            Toast.makeText(this, R.string.record_at_least, Toast.LENGTH_LONG).show()
            binding.timerText.text = "00:00"
            return
        }

        viewModel.beginAnalysis(analysis)
        lifecycleScope.launch {
            delay(MIN_ANALYSIS_REVEAL_MS)
            if (viewModel.state.value.stage == TestStage.ANALYZING) {
                if (viewModel.completeAnalysis()) {
                    val nextPlayer = viewModel.state.value.player
                    Toast.makeText(
                        this@TestActivity,
                        getString(R.string.next_player_ready_format, nextPlayer),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun renderResult(state: TestUiState) {
        val question = state.question ?: return
        binding.newTestButton.visibility = if (state.externalSession) View.GONE else View.VISIBLE
        binding.homeButton.setText(
            if (state.externalSession) R.string.p3_continue_after_result else R.string.back_home
        )
        val score = state.finalScore
        val analysis = state.analysis
        val breakdown = if (analysis != null) {
            TruthPresentation.breakdown(analysis)
        } else {
            TruthBreakdown(score, score, score, score, score)
        }

        binding.resultQuestion.text = question.text
        binding.resultWaveform.setValues(binding.waveform.snapshot())
        binding.resultScore.text = score.toString() + "%"
        binding.resultProgress.progress = score
        binding.resultProgress.setIndicatorColor(
            getColor(
                when {
                    score >= 80 -> R.color.p2_success
                    score >= 55 -> R.color.p2_warning
                    else -> R.color.p2_danger
                }
            )
        )
        binding.resultLabel.setText(
            when {
                score >= 85 -> R.string.result_very_convincing
                score >= 65 -> R.string.result_hard_to_read
                score >= 45 -> R.string.result_few_wobbles
                else -> R.string.result_poker_loading
            }
        )
        binding.resultBadge.setText(badgeLabel(TruthPresentation.badge(score)))

        bindMetric(binding.stabilityBar, binding.stabilityValue, breakdown.stability)
        bindMetric(binding.confidenceBar, binding.confidenceValue, breakdown.confidencePattern)
        bindMetric(binding.hesitationBar, binding.hesitationValue, breakdown.hesitationControl)
        bindMetric(binding.energyBar, binding.energyValue, breakdown.energy)
        bindMetric(binding.flowBar, binding.flowValue, breakdown.responseFlow)

        when (state.mode) {
            MODE_DUEL -> renderDuelResult(state)
            MODE_GROUP -> renderGroupResult(state)
            else -> binding.duelComparison.visibility = View.GONE
        }
    }

    private fun bindMetric(
        bar: com.google.android.material.progressindicator.LinearProgressIndicator,
        valueView: android.widget.TextView,
        value: Int
    ) {
        bar.setProgressCompat(value, true)
        valueView.text = getString(R.string.metric_value_format, value)
    }

    private fun badgeLabel(badge: ResultBadge): Int = when (badge) {
        ResultBadge.TRUTH_ROOKIE -> R.string.badge_truth_rookie
        ResultBadge.SMOOTH_TALKER -> R.string.badge_smooth_talker
        ResultBadge.POKER_FACE -> R.string.badge_poker_face
        ResultBadge.UNSHAKABLE -> R.string.badge_unshakable
        ResultBadge.TRUTH_MASTER -> R.string.badge_truth_master
    }

    private fun renderDuelResult(state: TestUiState) {
        val one = state.firstScore ?: 0
        val two = state.secondScore ?: 0
        val winner = when {
            one == two -> "🤝"
            one > two -> "🏆 " + getString(R.string.player_one)
            else -> "🏆 " + getString(R.string.player_two)
        }
        binding.duelComparison.visibility = View.VISIBLE
        binding.duelComparison.text =
            winner + "\n" +
            getString(R.string.player_one) + " " + one + "%   VS   " +
            getString(R.string.player_two) + " " + two + "%"
    }

    private fun renderGroupResult(state: TestUiState) {
        val standings = PlayerRanking.standings(state.playerScores)
        val winners = PlayerRanking.winners(state.playerScores)
        val headline = if (winners.size == 1) {
            getString(R.string.group_winner_format, playerName(winners.first()))
        } else {
            getString(
                R.string.group_tie_format,
                winners.joinToString(" • ") { playerName(it) }
            )
        }
        binding.duelComparison.visibility = View.VISIBLE
        binding.duelComparison.text = buildString {
            append(headline)
            standings.forEach { standing ->
                append('\n')
                append(
                    getString(
                        R.string.group_ranking_line_format,
                        standing.rank,
                        playerName(standing.playerNumber),
                        standing.score
                    )
                )
            }
        }
    }

    private fun playerName(number: Int): String =
        getString(R.string.player_number_format, number)

    private fun questionMeta(category: String, intensity: String?): String {
        val categoryLabel = when (category) {
            "embarrassing" -> getString(R.string.embarrassing)
            "funny" -> getString(R.string.funny)
            "bold" -> getString(R.string.bold)
            "romantic" -> getString(R.string.romantic)
            "friendship" -> getString(R.string.friendship)
            "family" -> getString(R.string.family)
            "custom" -> getString(R.string.custom_question)
            "classic" -> getString(R.string.p3_pack_classic)
            "deep" -> getString(R.string.p3_pack_deep)
            "couples" -> getString(R.string.p3_pack_couples)
            "friends" -> getString(R.string.p3_pack_friends)
            else -> category
        }
        val intensityLabel = when (intensity) {
            QuestionRepository.INTENSITY_LIGHT -> getString(R.string.intensity_light)
            QuestionRepository.INTENSITY_BOLD -> getString(R.string.intensity_bold)
            QuestionRepository.INTENSITY_MEDIUM -> getString(R.string.intensity_medium)
            else -> ""
        }
        return if (intensityLabel.isBlank()) categoryLabel else categoryLabel + " • " + intensityLabel
    }

    private fun returnSessionResult(state: TestUiState) {
        setResult(
            RESULT_OK,
            Intent().putExtra(EXTRA_SESSION_SCORE, state.finalScore)
        )
        finish()
    }

    private fun showShareThemePicker(state: TestUiState) {
        if (state.question == null) return
        val themes = ShareTheme.entries
        val saved = ShareTheme.fromStorage(
            getSharedPreferences(
                AppStorageContract.PREFS_SHARE,
                MODE_PRIVATE
            ).getString(AppStorageContract.KEY_SHARE_THEME, null)
        )
        var selectedIndex = themes.indexOf(saved).coerceAtLeast(0)
        val labels = themes.map { it.emoji + "  " + getString(it.labelRes) }.toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.choose_share_theme)
            .setSingleChoiceItems(labels, selectedIndex) { _, which -> selectedIndex = which }
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.continue_label) { _, _ ->
                val selectedTheme = themes[selectedIndex]
                getSharedPreferences(AppStorageContract.PREFS_SHARE, MODE_PRIVATE)
                    .edit()
                    .putString(AppStorageContract.KEY_SHARE_THEME, selectedTheme.storageKey)
                    .apply()
                showShareFormatPicker(state, selectedTheme)
            }
            .show()
    }

    private fun showShareFormatPicker(state: TestUiState, theme: ShareTheme) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.choose_share_format)
            .setItems(
                arrayOf(getString(R.string.share_video), getString(R.string.share_image))
            ) { _, which ->
                if (which == 0) showShareSoundPicker(state, theme)
                else shareImage(state, theme)
            }
            .show()
    }

    private fun showShareSoundPicker(state: TestUiState, theme: ShareTheme) {
        val sounds = ShareSound.entries
        val saved = ShareSound.fromStorage(
            getSharedPreferences(
                AppStorageContract.PREFS_SHARE,
                MODE_PRIVATE
            ).getString(AppStorageContract.KEY_SHARE_SOUND, null)
        )
        var selectedIndex = sounds.indexOf(saved).coerceAtLeast(0)
        val labels = sounds.map { it.emoji + "  " + getString(it.labelRes) }.toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.choose_video_sound)
            .setSingleChoiceItems(labels, selectedIndex) { _, which -> selectedIndex = which }
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.continue_label) { _, _ ->
                val selectedSound = sounds[selectedIndex]
                getSharedPreferences(AppStorageContract.PREFS_SHARE, MODE_PRIVATE)
                    .edit()
                    .putString(AppStorageContract.KEY_SHARE_SOUND, selectedSound.storageKey)
                    .apply()
                renderVideo(state, theme, selectedSound)
            }
            .show()
    }

    private fun shareImage(state: TestUiState, theme: ShareTheme) {
        val question = state.question ?: return
        val themedContext = ShareThemeContext.wrap(this, theme)
        val uri = ResultCardRenderer.render(
            context = themedContext,
            question = question.text,
            score = state.finalScore,
            firstScore = if (state.mode == MODE_DUEL) state.firstScore else null,
            secondScore = if (state.mode == MODE_DUEL) state.secondScore else null,
            groupScores = if (state.mode == MODE_GROUP) state.playerScores else emptyList(),
            waveform = binding.waveform.snapshot()
        )
        launchShare(uri, "image/png", shareMessage(state, question.text))
    }

    private fun renderVideo(state: TestUiState, theme: ShareTheme, sound: ShareSound) {
        val question = state.question ?: return
        val waveform = binding.waveform.snapshot()
        val themedContext = ShareThemeContext.wrap(this, theme)
        binding.shareButton.isEnabled = false
        binding.shareButton.setText(R.string.creating_video)

        lifecycleScope.launch {
            try {
                val uri = ResultVideoShareRenderer.render(
                    context = themedContext,
                    question = question.text,
                    score = state.finalScore,
                    firstScore = if (state.mode == MODE_DUEL) state.firstScore else null,
                    secondScore = if (state.mode == MODE_DUEL) state.secondScore else null,
                    groupScores = if (state.mode == MODE_GROUP) state.playerScores else emptyList(),
                    waveform = waveform,
                    sound = sound
                )
                launchShare(uri, "video/mp4", shareMessage(state, question.text))
            } catch (_: Throwable) {
                Toast.makeText(
                    this@TestActivity,
                    R.string.video_failed,
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                binding.shareButton.isEnabled = true
                binding.shareButton.setText(R.string.share_my_result)
            }
        }
    }

    private fun shareMessage(state: TestUiState, question: String): String =
        if (state.mode == MODE_GROUP) {
            getString(
                R.string.group_share_text,
                question,
                state.finalScore,
                state.playerCount
            )
        } else {
            getString(R.string.share_text, question, state.finalScore)
        }

    private fun launchShare(uri: Uri, mimeType: String, message: String) {
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = mimeType
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, message)
                    clipData = ClipData.newUri(
                        contentResolver,
                        getString(R.string.share_result),
                        uri
                    )
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                getString(R.string.share_result)
            )
        )
    }

    private fun performResultHaptic() {
        binding.resultPanel.performHapticFeedback(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.CONFIRM
            } else {
                HapticFeedbackConstants.VIRTUAL_KEY
            }
        )
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        handler.removeCallbacks(timer)
        recorder.release()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_MODE = "mode"
        const val EXTRA_DAILY = "daily"
        const val EXTRA_PLAYER_COUNT = "player_count"
        const val EXTRA_FORCED_QUESTION_TEXT = "forced_question_text"
        const val EXTRA_FORCED_QUESTION_CATEGORY = "forced_question_category"
        const val EXTRA_FORCED_QUESTION_INTENSITY = "forced_question_intensity"
        const val EXTRA_HISTORY_MODE_OVERRIDE = "history_mode_override"
        const val EXTRA_SESSION_PLAYER_NAME = "session_player_name"
        const val EXTRA_SESSION_SCORE = "session_score"

        const val MODE_SOLO = "solo"
        const val MODE_DUEL = "duel"
        const val MODE_GROUP = "group"
        const val MODE_CUSTOM = "custom"
        const val MODE_SESSION = "session"
        private const val MIN_ANALYSIS_REVEAL_MS = 650L
    }
}
