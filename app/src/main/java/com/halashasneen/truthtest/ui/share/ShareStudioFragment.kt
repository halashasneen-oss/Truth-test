package com.halashasneen.truthtest.ui.share

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.halashasneen.truthtest.R
import com.halashasneen.truthtest.core.AppStorageContract
import com.halashasneen.truthtest.data.HistoryRepository
import com.halashasneen.truthtest.data.PlayerProfileRepository
import com.halashasneen.truthtest.data.SocialSessionEngine
import com.halashasneen.truthtest.data.SocialSessionRepository
import com.halashasneen.truthtest.data.model.ChallengeType
import com.halashasneen.truthtest.data.model.QuestionPack
import com.halashasneen.truthtest.data.model.SocialMode
import com.halashasneen.truthtest.data.model.SocialSession
import com.halashasneen.truthtest.data.model.TestResult
import com.halashasneen.truthtest.databinding.FragmentShareStudioBinding
import com.halashasneen.truthtest.share.ChallengePayload
import com.halashasneen.truthtest.share.ChallengePayloadCodec
import com.halashasneen.truthtest.share.ChallengeQrCardRenderer
import com.halashasneen.truthtest.share.ResultVideoShareRenderer
import com.halashasneen.truthtest.share.ShareFormat
import com.halashasneen.truthtest.share.ShareHistoryEntry
import com.halashasneen.truthtest.share.ShareHistoryRepository
import com.halashasneen.truthtest.share.ShareSound
import com.halashasneen.truthtest.share.ShareSourceType
import com.halashasneen.truthtest.share.ShareTemplate
import com.halashasneen.truthtest.share.ShareThemeContext
import com.halashasneen.truthtest.share.StudioCardRenderer
import com.halashasneen.truthtest.share.StudioShareContent
import com.halashasneen.truthtest.ui.challenge.ChallengeImportActivity
import com.halashasneen.truthtest.ui.test.ResultBadge
import com.halashasneen.truthtest.ui.test.TruthPresentation
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ShareStudioFragment : Fragment(R.layout.fragment_share_studio) {
    private var _binding: FragmentShareStudioBinding? = null
    private val binding get() = _binding!!

    private val history by lazy { HistoryRepository(requireContext()) }
    private val sessions by lazy { SocialSessionRepository(requireContext()) }
    private val profiles by lazy { PlayerProfileRepository(requireContext()) }
    private val shareHistory by lazy { ShareHistoryRepository(requireContext()) }

    private var sources: List<Source> = emptyList()
    private var selectedSource: Source? = null
    private var selectedTemplate = ShareTemplate.NEON
    private var selectedFormat = ShareFormat.FEED
    private var selectedSound = ShareSound.NEON_BEAT
    private var selectedCtaRes = R.string.p4_cta_beat
    private var previewUri: Uri? = null
    private var previewJob: Job? = null
    private var renderGeneration = 0

    private val scanner = registerForActivityResult(ScanContract()) { result ->
        val raw = result.contents
        if (raw.isNullOrBlank()) return@registerForActivityResult
        startActivity(
            Intent(requireContext(), ChallengeImportActivity::class.java)
                .putExtra(ChallengeImportActivity.EXTRA_RAW_CHALLENGE, raw)
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentShareStudioBinding.bind(view)

        val prefs = requireContext().getSharedPreferences(
            AppStorageContract.PREFS_SHARE,
            Context.MODE_PRIVATE
        )
        selectedTemplate = ShareTemplate.fromStorage(
            prefs.getString(AppStorageContract.KEY_SHARE_TEMPLATE, null)
        )
        selectedFormat = ShareFormat.fromStorage(
            prefs.getString(AppStorageContract.KEY_SHARE_FORMAT, null)
        )
        selectedSound = ShareSound.fromStorage(
            prefs.getString(AppStorageContract.KEY_SHARE_SOUND, null)
        )

        binding.showModeSwitch.isChecked = true
        bindActions()
        refreshSources()
        updateControls()
        updateShareHistory()
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            refreshSources(keepSelection = true)
            updateShareHistory()
        }
    }

    private fun bindActions() {
        binding.sourceButton.setOnClickListener { chooseSource() }
        binding.templateButton.setOnClickListener { chooseTemplate() }
        binding.formatButton.setOnClickListener { chooseFormat() }
        binding.ctaButton.setOnClickListener { chooseCta() }
        binding.soundButton.setOnClickListener { chooseSound() }
        binding.showProfileSwitch.setOnCheckedChangeListener { _, _ -> requestPreview() }
        binding.showModeSwitch.setOnCheckedChangeListener { _, _ -> requestPreview() }
        binding.includeQrSwitch.setOnCheckedChangeListener { _, _ -> requestPreview() }
        binding.shareImageButton.setOnClickListener { shareImage() }
        binding.shareVideoButton.setOnClickListener { shareVideo() }
        binding.createQrButton.setOnClickListener { shareChallengeQr() }
        binding.scanQrButton.setOnClickListener { scanChallenge() }
    }

    private fun refreshSources(keepSelection: Boolean = false) {
        val oldKey = if (keepSelection) selectedSource?.key else null
        val resultSources = history.getAll().take(20).map { Source.fromResult(it, resultModeLabel(it)) }
        val sessionSources = sessions.completed().take(12).map { Source.fromSession(it, sessionModeLabel(it)) }
        sources = (resultSources + sessionSources).sortedByDescending { it.timestamp }
        selectedSource = sources.firstOrNull { it.key == oldKey } ?: sources.firstOrNull()

        val enabled = selectedSource != null
        listOf(
            binding.shareImageButton,
            binding.shareVideoButton,
            binding.createQrButton,
            binding.templateButton,
            binding.formatButton,
            binding.ctaButton,
            binding.soundButton
        ).forEach { it.isEnabled = enabled }

        binding.sourceMeta.text = selectedSource?.label ?: getString(R.string.p4_no_share_sources)
        requestPreview()
    }

    private fun chooseSource() {
        if (sources.isEmpty()) return
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.p4_choose_source)
            .setItems(sources.map { it.label }.toTypedArray()) { _, which ->
                selectedSource = sources[which]
                binding.sourceMeta.text = sources[which].label
                requestPreview()
            }
            .show()
    }

    private fun chooseTemplate() {
        val values = ShareTemplate.entries
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.p4_template)
            .setSingleChoiceItems(
                values.map { it.emoji + "  " + getString(it.labelRes) }.toTypedArray(),
                values.indexOf(selectedTemplate)
            ) { dialog, which ->
                selectedTemplate = values[which]
                persistOptions()
                updateControls()
                requestPreview()
                dialog.dismiss()
            }
            .show()
    }

    private fun chooseFormat() {
        val values = ShareFormat.entries
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.p4_format)
            .setSingleChoiceItems(
                values.map { getString(it.labelRes) }.toTypedArray(),
                values.indexOf(selectedFormat)
            ) { dialog, which ->
                selectedFormat = values[which]
                persistOptions()
                updateControls()
                requestPreview()
                dialog.dismiss()
            }
            .show()
    }

    private fun chooseCta() {
        val values = intArrayOf(
            R.string.p4_cta_beat,
            R.string.p4_cta_friends,
            R.string.p4_cta_turn
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.p4_cta)
            .setSingleChoiceItems(
                values.map { getString(it) }.toTypedArray(),
                values.indexOf(selectedCtaRes).coerceAtLeast(0)
            ) { dialog, which ->
                selectedCtaRes = values[which]
                updateControls()
                requestPreview()
                dialog.dismiss()
            }
            .show()
    }

    private fun chooseSound() {
        val values = ShareSound.entries
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.choose_video_sound)
            .setSingleChoiceItems(
                values.map { it.emoji + "  " + getString(it.labelRes) }.toTypedArray(),
                values.indexOf(selectedSound)
            ) { dialog, which ->
                selectedSound = values[which]
                persistOptions()
                updateControls()
                dialog.dismiss()
            }
            .show()
    }

    private fun updateControls() {
        binding.templateButton.text =
            getString(R.string.p4_template) + "  •  " + getString(selectedTemplate.labelRes)
        binding.formatButton.text =
            getString(R.string.p4_format) + "  •  " + getString(selectedFormat.labelRes)
        binding.ctaButton.text =
            getString(R.string.p4_cta) + "  •  " + getString(selectedCtaRes)
        binding.soundButton.text =
            getString(R.string.choose_video_sound) + "  •  " + getString(selectedSound.labelRes)
    }

    private fun persistOptions() {
        requireContext().getSharedPreferences(
            AppStorageContract.PREFS_SHARE,
            Context.MODE_PRIVATE
        ).edit()
            .putString(AppStorageContract.KEY_SHARE_TEMPLATE, selectedTemplate.storageKey)
            .putString(AppStorageContract.KEY_SHARE_FORMAT, selectedFormat.storageKey)
            .putString(AppStorageContract.KEY_SHARE_SOUND, selectedSound.storageKey)
            .apply()
    }

    private fun requestPreview() {
        val source = selectedSource ?: run {
            binding.previewImage.setImageDrawable(null)
            binding.previewProgress.visibility = View.GONE
            return
        }
        val generation = ++renderGeneration
        previewJob?.cancel()
        binding.previewProgress.visibility = View.VISIBLE
        previewJob = viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                val content = source.content(
                    context = requireContext(),
                    activePlayerName = profiles.active().name,
                    cta = getString(selectedCtaRes),
                    challengeUri = challengeUri(source)
                )
                StudioCardRenderer.render(
                    context = requireContext(),
                    content = content,
                    template = selectedTemplate,
                    format = selectedFormat,
                    showProfile = binding.showProfileSwitch.isChecked,
                    showMode = binding.showModeSwitch.isChecked,
                    includeQr = binding.includeQrSwitch.isChecked
                )
            }.onSuccess { uri ->
                if (_binding != null && generation == renderGeneration) {
                    previewUri = uri
                    binding.previewImage.setImageURI(null)
                    binding.previewImage.setImageURI(uri)
                }
            }.onFailure {
                if (_binding != null && generation == renderGeneration) {
                    Toast.makeText(requireContext(), R.string.p4_share_failed, Toast.LENGTH_SHORT).show()
                }
            }
            if (_binding != null && generation == renderGeneration) {
                binding.previewProgress.visibility = View.GONE
            }
        }
    }

    private fun shareImage() {
        val source = selectedSource ?: return
        binding.shareImageButton.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val content = source.content(
                    requireContext(),
                    profiles.active().name,
                    getString(selectedCtaRes),
                    challengeUri(source)
                )
                val uri = StudioCardRenderer.render(
                    context = requireContext(),
                    content = content,
                    template = selectedTemplate,
                    format = selectedFormat,
                    showProfile = binding.showProfileSwitch.isChecked,
                    showMode = binding.showModeSwitch.isChecked,
                    includeQr = binding.includeQrSwitch.isChecked
                )
                recordExport(source)
                launchShare(uri, "image/jpeg", getString(selectedCtaRes))
            } catch (_: Throwable) {
                Toast.makeText(requireContext(), R.string.p4_share_failed, Toast.LENGTH_LONG).show()
            } finally {
                if (_binding != null) binding.shareImageButton.isEnabled = true
            }
        }
    }

    private fun shareVideo() {
        val source = selectedSource ?: return
        binding.shareVideoButton.isEnabled = false
        binding.shareVideoButton.setText(R.string.creating_video)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val themed = ShareThemeContext.wrap(requireContext(), selectedTemplate.theme)
                val uri = ResultVideoShareRenderer.render(
                    context = themed,
                    question = source.question,
                    score = source.score,
                    groupScores = source.groupScores,
                    waveform = emptyList(),
                    sound = selectedSound,
                    cta = getString(selectedCtaRes)
                )
                recordExport(source, ShareFormat.STORY)
                launchShare(uri, "video/mp4", getString(selectedCtaRes))
            } catch (_: Throwable) {
                Toast.makeText(requireContext(), R.string.video_failed, Toast.LENGTH_LONG).show()
            } finally {
                if (_binding != null) {
                    binding.shareVideoButton.isEnabled = true
                    binding.shareVideoButton.setText(R.string.p4_share_video)
                }
            }
        }
    }

    private fun shareChallengeQr() {
        val source = selectedSource ?: return
        val challenge = challengeUri(source)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val uri = ChallengeQrCardRenderer.render(
                    context = requireContext(),
                    challengeUri = challenge,
                    title = source.modeLabel,
                    question = source.question
                )
                launchShare(uri, "image/jpeg", getString(R.string.p4_qr_body))
            } catch (_: Throwable) {
                Toast.makeText(requireContext(), R.string.p4_share_failed, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun scanChallenge() {
        val options = ScanOptions()
            .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            .setPrompt(getString(R.string.p4_scanner_title))
            .setBeepEnabled(false)
            .setOrientationLocked(true)
        scanner.launch(options)
    }

    private fun challengeUri(source: Source): String {
        val payload = ChallengePayload(
            mode = source.challengeMode.storageKey,
            pack = source.challengePack.storageKey,
            challengeType = source.challengeType.storageKey,
            questionId = source.questionId.take(96),
            questionText = source.question.take(220),
            questionIntensity = source.intensity
        )
        return ChallengePayloadCodec.encode(payload)
    }

    private fun recordExport(source: Source, format: ShareFormat = selectedFormat) {
        shareHistory.add(
            ShareHistoryEntry(
                sourceId = source.key,
                sourceType = source.type.name,
                format = format.storageKey,
                template = selectedTemplate.storageKey,
                timestamp = System.currentTimeMillis()
            )
        )
        updateShareHistory()
    }

    private fun updateShareHistory() {
        binding.shareHistoryText.text = getString(
            R.string.p4_share_history_count,
            shareHistory.all().size
        )
    }

    private fun launchShare(uri: Uri, mimeType: String, text: String) {
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = mimeType
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, text)
                    clipData = ClipData.newUri(
                        requireContext().contentResolver,
                        getString(R.string.share_result),
                        uri
                    )
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                getString(R.string.share_result)
            )
        )
    }

    private fun resultModeLabel(item: TestResult): String = when {
        item.mode == HistoryRepository.MODE_DAILY -> getString(R.string.experience_daily)
        item.mode == "social_couples" -> getString(R.string.p3_mode_couples)
        item.mode == "social_friends" -> getString(R.string.p3_mode_friends)
        item.mode == "social_party" -> getString(R.string.p3_mode_party)
        item.mode == "social_challenge" -> getString(R.string.p3_mode_challenge)
        item.mode.startsWith("duel_") -> getString(R.string.duel_mode)
        item.mode.startsWith("group") -> getString(R.string.group_mode)
        item.mode == "custom" -> getString(R.string.custom_question)
        else -> getString(R.string.solo_test)
    }

    private fun sessionModeLabel(session: SocialSession): String = getString(
        when (SocialMode.fromStorage(session.mode)) {
            SocialMode.COUPLES -> R.string.p3_mode_couples
            SocialMode.FRIENDS -> R.string.p3_mode_friends
            SocialMode.PARTY -> R.string.p3_mode_party
            SocialMode.CHALLENGE -> R.string.p3_mode_challenge
        }
    )

    override fun onDestroyView() {
        previewJob?.cancel()
        binding.previewImage.setImageDrawable(null)
        _binding = null
        super.onDestroyView()
    }

    private data class Source(
        val key: String,
        val type: ShareSourceType,
        val timestamp: Long,
        val label: String,
        val questionId: String,
        val question: String,
        val score: Int,
        val intensity: String?,
        val modeLabel: String,
        val playerName: String?,
        val secondaryText: String?,
        val groupScores: List<Int>,
        val challengeMode: SocialMode,
        val challengePack: QuestionPack,
        val challengeType: ChallengeType
    ) {
        fun content(
            context: Context,
            activePlayerName: String,
            cta: String,
            challengeUri: String
        ): StudioShareContent {
            val badge = when (TruthPresentation.badge(score)) {
                ResultBadge.TRUTH_ROOKIE -> context.getString(R.string.badge_truth_rookie)
                ResultBadge.SMOOTH_TALKER -> context.getString(R.string.badge_smooth_talker)
                ResultBadge.POKER_FACE -> context.getString(R.string.badge_poker_face)
                ResultBadge.UNSHAKABLE -> context.getString(R.string.badge_unshakable)
                ResultBadge.TRUTH_MASTER -> context.getString(R.string.badge_truth_master)
            }
            return StudioShareContent(
                sourceType = type,
                sourceId = key,
                question = question,
                score = score,
                modeLabel = modeLabel,
                playerName = playerName ?: activePlayerName.takeIf { type == ShareSourceType.RESULT },
                badge = badge,
                secondaryText = secondaryText,
                cta = cta,
                challengeUri = challengeUri
            )
        }

        companion object {
            fun fromResult(result: TestResult, modeLabel: String): Source {
                val pack = when (result.category) {
                    "funny" -> QuestionPack.FUNNY
                    "bold" -> QuestionPack.BOLD
                    "romantic" -> QuestionPack.COUPLES
                    "friendship" -> QuestionPack.FRIENDS
                    else -> QuestionPack.CLASSIC
                }
                return Source(
                    key = "r:" + result.id,
                    type = ShareSourceType.RESULT,
                    timestamp = result.timestamp,
                    label = modeLabel + " • " + result.score + "% • " + result.question.take(42),
                    questionId = result.id,
                    question = result.question,
                    score = result.score,
                    intensity = result.intensity,
                    modeLabel = modeLabel,
                    playerName = null,
                    secondaryText = null,
                    groupScores = emptyList(),
                    challengeMode = SocialMode.CHALLENGE,
                    challengePack = pack,
                    challengeType = ChallengeType.HIGHEST_SCORE
                )
            }

            fun fromSession(session: SocialSession, modeLabel: String): Source {
                val top = SocialSessionEngine.highestTurn(session)
                val standings = SocialSessionEngine.standings(session)
                val secondary = standings.joinToString("  •  ") {
                    "#" + it.rank + " " + it.playerName + " " + it.averageScore + "%"
                }
                return Source(
                    key = "s:" + session.id,
                    type = ShareSourceType.SESSION,
                    timestamp = session.completedAt ?: session.startedAt,
                    label = modeLabel + " • " + session.playerIds.size + " players • " + session.turns.size + " turns",
                    questionId = top?.questionId ?: session.id,
                    question = top?.questionText ?: modeLabel,
                    score = top?.score ?: standings.firstOrNull()?.averageScore ?: 0,
                    intensity = null,
                    modeLabel = modeLabel,
                    playerName = top?.playerName,
                    secondaryText = secondary,
                    groupScores = standings.map { it.averageScore },
                    challengeMode = SocialMode.fromStorage(session.mode),
                    challengePack = QuestionPack.fromStorage(session.pack),
                    challengeType = ChallengeType.fromStorage(session.challengeType)
                )
            }
        }
    }
}
