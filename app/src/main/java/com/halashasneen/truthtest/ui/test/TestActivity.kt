package com.halashasneen.truthtest.ui.test

import android.Manifest
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
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
import com.halashasneen.truthtest.data.PlayerRanking
import com.halashasneen.truthtest.data.QuestionRepository
import com.halashasneen.truthtest.databinding.ActivityTestBinding
import com.halashasneen.truthtest.share.ResultCardRenderer
import com.halashasneen.truthtest.share.ResultVideoShareRenderer
import com.halashasneen.truthtest.share.ShareSound
import com.halashasneen.truthtest.share.ShareTheme
import com.halashasneen.truthtest.share.ShareThemeContext
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

    private val micPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
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
            requestedPlayerCount = intent.getIntExtra(EXTRA_PLAYER_COUNT, 1)
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
        spec.button.backgroundTintList = ColorStateList.valueOf(getColor(spec.colorRes))
        spec.button.setTextColor(
            if (spec.colorRes == R.color.warning || spec.colorRes == R.color.cyan) 0xFF111118.toInt()
            else 0xFFFFFFFF.toInt()
        )
        spec.button.cornerRadius = (20f * resources.displayMetrics.density).toInt()
    }

    private fun bindActions() {
        binding.customContinue.setOnClickListener { viewModel.useCustomQuestion(binding.customQuestionInput.text?.toString().orEmpty()) }
        binding.recordButton.setOnClickListener { if (isRecording) stopRecording() else ensureMicAndStart() }
        binding.newTestButton.setOnClickListener { viewModel.reset() }
        binding.homeButton.setOnClickListener { finish() }
        binding.shareButton.setOnClickListener { showShareThemePicker(viewModel.state.value) }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.state.collect(::render) }
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
                else -> R.string.solo_test
            }
        )
        setPanels(
            state.stage == TestStage.CATEGORY,
            state.stage == TestStage.CUSTOM,
            state.stage == TestStage.RECORDING,
            state.stage == TestStage.RESULT
        )
        state.question?.let { binding.questionText.text = it.text }
        if (state.stage == TestStage.RECORDING) {
            binding.playerLabel.text = if (state.playerCount > 1) {
                getString(R.string.player_turn_format, state.player, state.playerCount)
            } else ""
            if (renderedPlayer != state.player) {
                renderedPlayer = state.player
                binding.waveform.reset()
                binding.pulseRing.reset()
                binding.timerText.text = "00:00"
            }
        }
        if (state.stage == TestStage.RESULT) {
            renderResult(state)
            if (stageChanged) animateResultEntrance()
        }
        lastStage = state.stage
    }

    private fun animateResultEntrance() {
        binding.resultPanel.alpha = 0f
        binding.resultPanel.translationY = 24f * resources.displayMetrics.density
        binding.resultPanel.animate().alpha(1f).translationY(0f).setDuration(420L).start()
        binding.resultScore.scaleX = 0.72f
        binding.resultScore.scaleY = 0.72f
        binding.resultScore.animate().scaleX(1f).scaleY(1f).setStartDelay(120L).setDuration(360L).start()
    }

    private fun setPanels(category: Boolean = false, custom: Boolean = false, record: Boolean = false, result: Boolean = false) {
        binding.categoryPanel.visibility = if (category) View.VISIBLE else View.GONE
        binding.customPanel.visibility = if (custom) View.VISIBLE else View.GONE
        binding.recordPanel.visibility = if (record) View.VISIBLE else View.GONE
        binding.resultPanel.visibility = if (result) View.VISIBLE else View.GONE
    }

    private fun ensureMicAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startRecordingInternal()
        else micPermission.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun startRecordingInternal() {
        runCatching {
            isRecording = true
            binding.recordButton.text = "■"
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
            Toast.makeText(this, R.string.permission_audio, Toast.LENGTH_LONG).show()
        }
    }

    private fun stopRecording() {
        isRecording = false
        handler.removeCallbacks(timer)
        binding.recordButton.text = "●"
        binding.pulseRing.reset()
        val analysis = VoiceAnalyzer.analyze(recorder.stop())
        if (!analysis.usable) {
            Toast.makeText(this, R.string.record_at_least, Toast.LENGTH_LONG).show()
            binding.timerText.text = "00:00"
            return
        }
        if (viewModel.submitScore(analysis.score)) {
            val nextPlayer = viewModel.state.value.player
            Toast.makeText(this, getString(R.string.next_player_ready_format, nextPlayer), Toast.LENGTH_SHORT).show()
        }
    }

    private fun renderResult(state: TestUiState) {
        val question = state.question ?: return
        val score = state.finalScore
        binding.resultQuestion.text = question.text
        binding.resultWaveform.setValues(binding.waveform.snapshot())
        binding.resultScore.text = "$score%"
        binding.resultProgress.setProgressCompat(score, true)
        binding.resultProgress.setIndicatorColor(getColor(if (score >= 80) R.color.success else if (score >= 55) R.color.warning else R.color.danger))
        binding.resultLabel.setText(if (score >= 85) R.string.result_honest else if (score >= 65) R.string.result_hesitant else if (score >= 45) R.string.result_white_lie else R.string.result_actor)

        when (state.mode) {
            MODE_DUEL -> renderDuelResult(state)
            MODE_GROUP -> renderGroupResult(state)
            else -> binding.duelComparison.visibility = View.GONE
        }
    }

    private fun renderDuelResult(state: TestUiState) {
        val one = state.firstScore ?: 0
        val two = state.secondScore ?: 0
        val winner = when {
            one == two -> "🤝"
            one > two -> "🏆 ${getString(R.string.player_one)}"
            else -> "🏆 ${getString(R.string.player_two)}"
        }
        binding.duelComparison.visibility = View.VISIBLE
        binding.duelComparison.text = "$winner\n${getString(R.string.player_one)} $one%   VS   ${getString(R.string.player_two)} $two%"
    }

    private fun renderGroupResult(state: TestUiState) {
        val standings = PlayerRanking.standings(state.playerScores)
        val winners = PlayerRanking.winners(state.playerScores)
        val headline = if (winners.size == 1) {
            getString(R.string.group_winner_format, playerName(winners.first()))
        } else {
            getString(R.string.group_tie_format, winners.joinToString(" • ") { playerName(it) })
        }
        binding.duelComparison.visibility = View.VISIBLE
        binding.duelComparison.text = buildString {
            append(headline)
            standings.forEach { standing ->
                append('\n')
                append(getString(R.string.group_ranking_line_format, standing.rank, playerName(standing.playerNumber), standing.score))
            }
        }
    }

    private fun playerName(number: Int): String = getString(R.string.player_number_format, number)

    private fun showShareThemePicker(state: TestUiState) {
        if (state.question == null) return
        val themes = ShareTheme.entries
        val saved = ShareTheme.fromStorage(
            getSharedPreferences(SHARE_PREFS, MODE_PRIVATE).getString(PREF_SHARE_THEME, null)
        )
        var selectedIndex = themes.indexOf(saved).coerceAtLeast(0)
        val labels = themes.map { "${it.emoji}  ${getString(it.labelRes)}" }.toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.choose_share_theme)
            .setSingleChoiceItems(labels, selectedIndex) { _, which -> selectedIndex = which }
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.continue_label) { _, _ ->
                val selectedTheme = themes[selectedIndex]
                getSharedPreferences(SHARE_PREFS, MODE_PRIVATE)
                    .edit()
                    .putString(PREF_SHARE_THEME, selectedTheme.storageKey)
                    .apply()
                showShareFormatPicker(state, selectedTheme)
            }
            .show()
    }

    private fun showShareFormatPicker(state: TestUiState, theme: ShareTheme) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.choose_share_format)
            .setItems(arrayOf(getString(R.string.share_video), getString(R.string.share_image))) { _, which ->
                if (which == 0) showShareSoundPicker(state, theme) else shareImage(state, theme)
            }
            .show()
    }

    private fun showShareSoundPicker(state: TestUiState, theme: ShareTheme) {
        val sounds = ShareSound.entries
        val saved = ShareSound.fromStorage(
            getSharedPreferences(SHARE_PREFS, MODE_PRIVATE).getString(PREF_SHARE_SOUND, null)
        )
        var selectedIndex = sounds.indexOf(saved).coerceAtLeast(0)
        val labels = sounds.map { "${it.emoji}  ${getString(it.labelRes)}" }.toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.choose_video_sound)
            .setSingleChoiceItems(labels, selectedIndex) { _, which -> selectedIndex = which }
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.continue_label) { _, _ ->
                val selectedSound = sounds[selectedIndex]
                getSharedPreferences(SHARE_PREFS, MODE_PRIVATE)
                    .edit()
                    .putString(PREF_SHARE_SOUND, selectedSound.storageKey)
                    .apply()
                shareVideo(state, theme, selectedSound)
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

    private fun shareVideo(state: TestUiState, theme: ShareTheme, sound: ShareSound) {
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
                Toast.makeText(this@TestActivity, R.string.video_failed, Toast.LENGTH_LONG).show()
            } finally {
                binding.shareButton.isEnabled = true
                binding.shareButton.setText(R.string.share_result)
            }
        }
    }

    private fun shareMessage(state: TestUiState, question: String): String =
        if (state.mode == MODE_GROUP) {
            getString(R.string.group_share_text, question, state.finalScore, state.playerCount)
        } else {
            getString(R.string.share_text, question, state.finalScore)
        }

    private fun launchShare(uri: Uri, mimeType: String, message: String) {
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, message)
            clipData = ClipData.newUri(contentResolver, getString(R.string.share_result), uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }, getString(R.string.share_result)))
    }

    override fun onDestroy() {
        handler.removeCallbacks(timer)
        recorder.release()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_MODE = "mode"
        const val EXTRA_DAILY = "daily"
        const val EXTRA_PLAYER_COUNT = "player_count"
        const val MODE_SOLO = "solo"
        const val MODE_DUEL = "duel"
        const val MODE_GROUP = "group"
        const val MODE_CUSTOM = "custom"
        private const val SHARE_PREFS = "truth_test_share"
        private const val PREF_SHARE_THEME = "share_theme"
        private const val PREF_SHARE_SOUND = "share_sound"
    }
}
