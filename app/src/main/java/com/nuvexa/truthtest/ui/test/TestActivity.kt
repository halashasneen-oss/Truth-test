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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.nuvexa.truthtest.R
import com.nuvexa.truthtest.audio.AudioRecorderEngine
import com.nuvexa.truthtest.audio.VoiceAnalyzer
import com.nuvexa.truthtest.data.HistoryRepository
import com.nuvexa.truthtest.data.QuestionRepository
import com.nuvexa.truthtest.data.model.Question
import com.nuvexa.truthtest.data.model.TestResult
import com.nuvexa.truthtest.databinding.ActivityTestBinding
import com.nuvexa.truthtest.share.ResultCardRenderer
import java.util.UUID

class TestActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTestBinding
    private val recorder = AudioRecorderEngine()
    private lateinit var questions: QuestionRepository
    private lateinit var history: HistoryRepository
    private var mode = MODE_SOLO
    private var currentQuestion: Question? = null
    private var isRecording = false
    private var currentPlayer = 1
    private var firstScore: Int? = null
    private var finalScore = 0
    private var secondScore: Int? = null
    private var startedAt = 0L
    private val handler = Handler(Looper.getMainLooper())
    private val timer = object : Runnable { override fun run() { if (!isRecording) return; val sec = ((SystemClock.elapsedRealtime() - startedAt) / 1000).toInt(); binding.timerText.text = "%02d:%02d".format(sec / 60, sec % 60); handler.postDelayed(this, 250) } }
    private val micPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (granted) startRecordingInternal() else Toast.makeText(this, R.string.permission_audio, Toast.LENGTH_LONG).show() }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        binding = ActivityTestBinding.inflate(layoutInflater)
        setContentView(binding.root)
        questions = QuestionRepository(this); history = HistoryRepository(this)
        mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_SOLO
        binding.screenTitle.setText(if (mode == MODE_DUEL) R.string.duel_mode else if (mode == MODE_CUSTOM) R.string.custom_question else R.string.solo_test)
        bindCategories(); bindActions()
        if (intent.getBooleanExtra(EXTRA_DAILY, false)) questions.dailyQuestion()?.let(::beginQuestion)
        else if (mode == MODE_CUSTOM) showCustom() else showCategories()
    }

    private fun bindCategories() {
        mapOf(binding.categoryEmbarrassing to "embarrassing", binding.categoryFunny to "funny", binding.categoryBold to "bold", binding.categoryRomantic to "romantic", binding.categoryFriendship to "friendship", binding.categoryFamily to "family").forEach { (button, category) -> button.setOnClickListener { questions.random(category)?.let(::beginQuestion) } }
    }

    private fun bindActions() {
        binding.customContinue.setOnClickListener { val text = binding.customQuestionInput.text?.toString()?.trim().orEmpty(); if (text.isNotBlank()) beginQuestion(Question("custom-${System.currentTimeMillis()}", "custom", text)) }
        binding.recordButton.setOnClickListener { if (isRecording) stopRecording() else ensureMicAndStart() }
        binding.newTestButton.setOnClickListener { resetFlow() }
        binding.homeButton.setOnClickListener { finish() }
        binding.shareButton.setOnClickListener { shareResult() }
    }

    private fun showCategories() { setPanels(category = true) }
    private fun showCustom() { setPanels(custom = true) }
    private fun beginQuestion(question: Question) { currentQuestion = question; currentPlayer = 1; firstScore = null; secondScore = null; binding.questionText.text = question.text; binding.playerLabel.text = if (mode == MODE_DUEL) getString(R.string.player_one) else ""; binding.waveform.reset(); binding.timerText.text = "00:00"; setPanels(record = true) }
    private fun setPanels(category: Boolean = false, custom: Boolean = false, record: Boolean = false, result: Boolean = false) { binding.categoryPanel.visibility = if (category) View.VISIBLE else View.GONE; binding.customPanel.visibility = if (custom) View.VISIBLE else View.GONE; binding.recordPanel.visibility = if (record) View.VISIBLE else View.GONE; binding.resultPanel.visibility = if (result) View.VISIBLE else View.GONE }

    private fun ensureMicAndStart() { if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startRecordingInternal() else micPermission.launch(Manifest.permission.RECORD_AUDIO) }
    private fun startRecordingInternal() { runCatching { isRecording = true; binding.recordButton.text = "■"; binding.waveform.reset(); startedAt = SystemClock.elapsedRealtime(); handler.post(timer); recorder.start { amp -> binding.waveform.post { binding.waveform.addAmplitude(amp) } } }.onFailure { isRecording = false; Toast.makeText(this, R.string.permission_audio, Toast.LENGTH_LONG).show() } }
    private fun stopRecording() {
        isRecording = false; handler.removeCallbacks(timer); binding.recordButton.text = "●"
        val samples = recorder.stop(); val analysis = VoiceAnalyzer.analyze(samples)
        if (!analysis.usable) { Toast.makeText(this, R.string.record_at_least, Toast.LENGTH_LONG).show(); binding.timerText.text = "00:00"; return }
        val q = currentQuestion ?: return
        if (mode == MODE_DUEL && currentPlayer == 1) { firstScore = analysis.score; save(q, analysis.score, "duel_p1"); currentPlayer = 2; binding.playerLabel.text = getString(R.string.player_two); binding.waveform.reset(); binding.timerText.text = "00:00"; Toast.makeText(this, R.string.player_two, Toast.LENGTH_SHORT).show(); return }
        finalScore = analysis.score; save(q, analysis.score, if (mode == MODE_DUEL) "duel_p2" else mode)
        if (mode == MODE_DUEL) { secondScore = analysis.score; showDuelResult(firstScore ?: 0, analysis.score) } else showResult(analysis.score)
    }

    private fun save(q: Question, score: Int, resultMode: String) { history.add(TestResult(UUID.randomUUID().toString(), q.text, q.category, score, System.currentTimeMillis(), resultMode)) }
    private fun showResult(score: Int) { setPanels(result = true); binding.duelComparison.visibility = View.GONE; renderResult(score) }
    private fun showDuelResult(one: Int, two: Int) { setPanels(result = true); finalScore = maxOf(one, two); binding.duelComparison.visibility = View.VISIBLE; val winner = if (one == two) "🤝" else if (one > two) "🏆 ${getString(R.string.player_one)}" else "🏆 ${getString(R.string.player_two)}"; binding.duelComparison.text = "$winner\n${getString(R.string.player_one)} $one%   VS   ${getString(R.string.player_two)} $two%"; renderResult(maxOf(one, two)) }
    private fun renderResult(score: Int) { binding.resultQuestion.text = currentQuestion?.text.orEmpty(); binding.resultScore.text = "$score%"; binding.resultProgress.setProgressCompat(score, true); binding.resultProgress.setIndicatorColor(getColor(if (score >= 80) R.color.success else if (score >= 55) R.color.warning else R.color.danger)); binding.resultLabel.setText(if (score >= 85) R.string.result_honest else if (score >= 65) R.string.result_hesitant else if (score >= 45) R.string.result_white_lie else R.string.result_actor) }
    private fun shareResult() { val q = currentQuestion ?: return; val uri = ResultCardRenderer.render(this, q.text, finalScore, secondScore); val share = Intent(Intent.ACTION_SEND).apply { type = "image/png"; putExtra(Intent.EXTRA_STREAM, uri); putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text, q.text, finalScore)); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }; startActivity(Intent.createChooser(share, getString(R.string.share_result))) }
    private fun resetFlow() { currentQuestion = null; currentPlayer = 1; firstScore = null; secondScore = null; finalScore = 0; binding.waveform.reset(); binding.timerText.text = "00:00"; if (mode == MODE_CUSTOM) showCustom() else showCategories() }
    override fun onDestroy() { handler.removeCallbacks(timer); recorder.release(); super.onDestroy() }

    companion object { const val EXTRA_MODE = "mode"; const val EXTRA_DAILY = "daily"; const val MODE_SOLO = "solo"; const val MODE_DUEL = "duel"; const val MODE_CUSTOM = "custom" }
}
