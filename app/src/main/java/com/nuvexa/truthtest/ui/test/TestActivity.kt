package com.nuvexa.truthtest.ui.test

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
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
import com.nuvexa.truthtest.R
import com.nuvexa.truthtest.audio.AudioRecorderEngine
import com.nuvexa.truthtest.audio.VoiceAnalyzer
import com.nuvexa.truthtest.databinding.ActivityTestBinding
import com.nuvexa.truthtest.share.ResultCardRenderer
import kotlinx.coroutines.launch

class TestActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTestBinding
    private val viewModel: TestViewModel by viewModels()
    private val recorder = AudioRecorderEngine()
    private var isRecording = false
    private var startedAt = 0L
    private var renderedPlayer = 0

    private val handler = Handler(Looper.getMainLooper())
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
        bindCategories()
        bindActions()
        observeState()
        viewModel.configure(
            mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_SOLO,
            daily = intent.getBooleanExtra(EXTRA_DAILY, false)
        )
    }

    private fun bindCategories() {
        mapOf(
            binding.categoryEmbarrassing to "embarrassing",
            binding.categoryFunny to "funny",
            binding.categoryBold to "bold",
            binding.categoryRomantic to "romantic",
            binding.categoryFriendship to "friendship",
            binding.categoryFamily to "family"
        ).forEach { (button, category) ->
            button.setOnClickListener { viewModel.chooseCategory(category) }
        }
    }

    private fun bindActions() {
        binding.customContinue.setOnClickListener {
            viewModel.useCustomQuestion(binding.customQuestionInput.text?.toString().orEmpty())
        }
        binding.recordButton.setOnClickListener {
            if (isRecording) stopRecording() else ensureMicAndStart()
        }
        binding.newTestButton.setOnClickListener { viewModel.reset() }
        binding.homeButton.setOnClickListener { finish() }
        binding.shareButton.setOnClickListener { shareResult(viewModel.state.value) }
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
        binding.screenTitle.setText(
            when (state.mode) {
                MODE_DUEL -> R.string.duel_mode
                MODE_CUSTOM -> R.string.custom_question
                else -> R.string.solo_test
            }
        )
        setPanels(
            category = state.stage == TestStage.CATEGORY,
            custom = state.stage == TestStage.CUSTOM,
            record = state.stage == TestStage.RECORDING,
            result = state.stage == TestStage.RESULT
        )

        state.question?.let { binding.questionText.text = it.text }
        if (state.stage == TestStage.RECORDING) {
            binding.playerLabel.text = if (state.mode == MODE_DUEL) {
                getString(if (state.player == 1) R.string.player_one else R.string.player_two)
            } else ""
            if (renderedPlayer != state.player) {
                renderedPlayer = state.player
                binding.waveform.reset()
                binding.timerText.text = "00:00"
            }
        }
        if (state.stage == TestStage.RESULT) renderResult(state)
    }

    private fun setPanels(
        category: Boolean = false,
        custom: Boolean = false,
        record: Boolean = false,
        result: Boolean = false
    ) {
        binding.categoryPanel.visibility = if (category) View.VISIBLE else View.GONE
        binding.customPanel.visibility = if (custom) View.VISIBLE else View.GONE
        binding.recordPanel.visibility = if (record) View.VISIBLE else View.GONE
        binding.resultPanel.visibility = if (result) View.VISIBLE else View.GONE
    }

    private fun ensureMicAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startRecordingInternal()
        } else micPermission.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun startRecordingInternal() {
        runCatching {
            isRecording = true
            binding.recordButton.text = "■"
            binding.waveform.reset()
            startedAt = SystemClock.elapsedRealtime()
            handler.post(timer)
            recorder.start { amplitude -> binding.waveform.post { binding.waveform.addAmplitude(amplitude) } }
        }.onFailure {
            isRecording = false
            Toast.makeText(this, R.string.permission_audio, Toast.LENGTH_LONG).show()
        }
    }

    private fun stopRecording() {
        isRecording = false
        handler.removeCallbacks(timer)
        binding.recordButton.text = "●"
        val samples = recorder.stop()
        val analysis = VoiceAnalyzer.analyze(samples)
        if (!analysis.usable) {
            Toast.makeText(this, R.string.record_at_least, Toast.LENGTH_LONG).show()
            binding.timerText.text = "00:00"
            return
        }
        val handToPlayerTwo = viewModel.submitScore(analysis.score)
        if (handToPlayerTwo) Toast.makeText(this, R.string.player_two, Toast.LENGTH_SHORT).show()
    }

    private fun renderResult(state: TestUiState) {
        val question = state.question ?: return
        val score = state.finalScore
        binding.resultQuestion.text = question.text
        binding.resultScore.text = "$score%"
        binding.resultProgress.setProgressCompat(score, true)
        binding.resultProgress.setIndicatorColor(
            getColor(if (score >= 80) R.color.success else if (score >= 55) R.color.warning else R.color.danger)
        )
        binding.resultLabel.setText(
            if (score >= 85) R.string.result_honest
            else if (score >= 65) R.string.result_hesitant
            else if (score >= 45) R.string.result_white_lie
            else R.string.result_actor
        )

        if (state.mode == MODE_DUEL) {
            val one = state.firstScore ?: 0
            val two = state.secondScore ?: 0
            val winner = when {
                one == two -> "🤝"
                one > two -> "🏆 ${getString(R.string.player_one)}"
                else -> "🏆 ${getString(R.string.player_two)}"
            }
            binding.duelComparison.visibility = View.VISIBLE
            binding.duelComparison.text = "$winner\n${getString(R.string.player_one)} $one%   VS   ${getString(R.string.player_two)} $two%"
        } else {
            binding.duelComparison.visibility = View.GONE
        }
    }

    private fun shareResult(state: TestUiState) {
        val question = state.question ?: return
        val uri = ResultCardRenderer.render(this, question.text, state.finalScore, state.secondScore)
        val share = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text, question.text, state.finalScore))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(share, getString(R.string.share_result)))
    }

    override fun onDestroy() {
        handler.removeCallbacks(timer)
        recorder.release()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_MODE = "mode"
        const val EXTRA_DAILY = "daily"
        const val MODE_SOLO = "solo"
        const val MODE_DUEL = "duel"
        const val MODE_CUSTOM = "custom"
    }
}
