package com.halashasneen.truthtest.ui.social

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.halashasneen.truthtest.R
import com.halashasneen.truthtest.data.AchievementRepository
import com.halashasneen.truthtest.data.HistoryRepository
import com.halashasneen.truthtest.data.PlayerProfileRepository
import com.halashasneen.truthtest.data.SocialQuestionRepository
import com.halashasneen.truthtest.data.SocialSessionEngine
import com.halashasneen.truthtest.data.SocialSessionRepository
import com.halashasneen.truthtest.data.XpEngine
import com.halashasneen.truthtest.data.model.ChallengeType
import com.halashasneen.truthtest.data.model.QuestionPack
import com.halashasneen.truthtest.data.model.SocialMode
import com.halashasneen.truthtest.data.model.SocialSession
import com.halashasneen.truthtest.data.model.SocialTurn
import com.halashasneen.truthtest.databinding.ActivitySocialSessionBinding
import com.halashasneen.truthtest.databinding.DialogProfileBinding
import com.halashasneen.truthtest.ui.test.TestActivity
import java.util.UUID

class SocialSessionActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySocialSessionBinding
    private val profiles by lazy { PlayerProfileRepository(this) }
    private val sessions by lazy { SocialSessionRepository(this) }
    private val questions by lazy { SocialQuestionRepository(this) }
    private val history by lazy { HistoryRepository(this) }
    private val achievements by lazy { AchievementRepository(this) }

    private var selectedMode = SocialMode.PARTY
    private var selectedPack = QuestionPack.CLASSIC
    private var selectedChallenge = ChallengeType.HIGHEST_SCORE
    private var selectedRounds = 3
    private val selectedProfileIds = linkedSetOf<String>()
    private var currentSession: SocialSession? = null

    private val voiceTurn =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val score = result.data?.getIntExtra(TestActivity.EXTRA_SESSION_SCORE, -1) ?: -1
                if (score >= 0) acceptTurn(score)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySocialSessionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        selectedMode = SocialMode.fromStorage(intent.getStringExtra(EXTRA_MODE))
        bindActions()

        val active = sessions.active()
        val shouldResume = intent.getBooleanExtra(EXTRA_RESUME, false) || savedInstanceState != null
        if (shouldResume && active != null) {
            selectedMode = SocialMode.fromStorage(active.mode)
            currentSession = active
            renderModeHeader()
            renderSession(active)
        } else {
            initializeSetup()
        }
    }

    private fun bindActions() {
        binding.backButton.setOnClickListener { finish() }
        binding.choosePlayersButton.setOnClickListener { choosePlayers() }
        binding.addPlayerButton.setOnClickListener { addProfile() }
        binding.packButton.setOnClickListener { choosePack() }
        binding.roundsButton.setOnClickListener { chooseRounds() }
        binding.challengeButton.setOnClickListener { chooseChallenge() }
        binding.startSessionButton.setOnClickListener { validateAndStart() }
        binding.startVoiceButton.setOnClickListener { launchVoiceTurn() }
        binding.saveExitButton.setOnClickListener {
            Toast.makeText(this, R.string.p3_session_saved, Toast.LENGTH_SHORT).show()
            finish()
        }
        binding.playAgainButton.setOnClickListener { prepareReplay() }
        binding.homeButton.setOnClickListener { finish() }
    }

    private fun initializeSetup() {
        selectedPack = questions.availablePacks(selectedMode).first()
        selectedChallenge = ChallengeType.HIGHEST_SCORE
        selectedRounds = defaultRounds(selectedMode)
        selectedProfileIds.clear()

        val (minPlayers, _) = playerLimits(selectedMode)
        profiles.getAll().take(minPlayers).forEach { selectedProfileIds += it.id }

        renderModeHeader()
        showPanel(setup = true)
        updateSetup()
    }

    private fun renderModeHeader() {
        binding.modeTitle.setText(modeTitle(selectedMode))
        binding.modeBody.setText(modeBody(selectedMode))
    }

    private fun updateSetup() {
        val selected = profiles.getAll().filter { it.id in selectedProfileIds }
        binding.playersSummary.text = if (selected.isEmpty()) {
            getString(R.string.p3_selected_players_format, 0)
        } else {
            selected.joinToString("  •  ") { it.avatar + " " + it.name }
        }
        binding.packButton.text =
            getString(R.string.p3_choose_pack) + "  •  " + getString(packLabel(selectedPack))
        binding.roundsButton.text = getString(R.string.p3_rounds_format, selectedRounds)
        binding.challengeButton.visibility =
            if (selectedMode == SocialMode.CHALLENGE) View.VISIBLE else View.GONE
        binding.roundsButton.visibility =
            if (selectedMode == SocialMode.CHALLENGE) View.GONE else View.VISIBLE
        binding.challengeButton.text =
            getString(R.string.p3_choose_challenge) + "  •  " + getString(challengeLabel(selectedChallenge))
    }

    private fun choosePlayers() {
        val available = profiles.getAll()
        if (available.isEmpty()) {
            addProfile()
            return
        }
        val working = selectedProfileIds.toMutableSet()
        val labels = available.map { it.avatar + "  " + it.name }.toTypedArray()
        val checked = BooleanArray(available.size) { available[it].id in working }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.p3_choose_players)
            .setMultiChoiceItems(labels, checked) { _, which, isChecked ->
                if (isChecked) working += available[which].id else working -= available[which].id
            }
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val (minPlayers, maxPlayers) = playerLimits(selectedMode)
                when {
                    working.size < minPlayers ->
                        Toast.makeText(
                            this,
                            getString(R.string.p3_min_players_format, minPlayers),
                            Toast.LENGTH_SHORT
                        ).show()
                    working.size > maxPlayers ->
                        Toast.makeText(
                            this,
                            getString(R.string.p3_max_players_format, maxPlayers),
                            Toast.LENGTH_SHORT
                        ).show()
                    else -> {
                        selectedProfileIds.clear()
                        selectedProfileIds.addAll(working)
                        updateSetup()
                    }
                }
            }
            .show()
    }

    private fun choosePack() {
        val packs = questions.availablePacks(selectedMode)
        val labels = packs.map { getString(packLabel(it)) }.toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.p3_choose_pack)
            .setSingleChoiceItems(labels, packs.indexOf(selectedPack).coerceAtLeast(0)) { dialog, which ->
                selectedPack = packs[which]
                updateSetup()
                dialog.dismiss()
            }
            .show()
    }

    private fun chooseRounds() {
        val values = when (selectedMode) {
            SocialMode.PARTY -> intArrayOf(1, 3, 5)
            else -> intArrayOf(3, 5, 7)
        }
        val labels = values.map { getString(R.string.p3_rounds_format, it) }.toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.p3_choose_rounds)
            .setSingleChoiceItems(labels, values.indexOf(selectedRounds).coerceAtLeast(0)) { dialog, which ->
                selectedRounds = values[which]
                updateSetup()
                dialog.dismiss()
            }
            .show()
    }

    private fun chooseChallenge() {
        val values = ChallengeType.entries
        val labels = values.map { getString(challengeLabel(it)) }.toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.p3_choose_challenge)
            .setSingleChoiceItems(labels, values.indexOf(selectedChallenge)) { dialog, which ->
                selectedChallenge = values[which]
                selectedRounds = selectedChallenge.rounds
                updateSetup()
                dialog.dismiss()
            }
            .show()
    }

    private fun validateAndStart() {
        val (minPlayers, maxPlayers) = playerLimits(selectedMode)
        when {
            selectedProfileIds.size < minPlayers -> {
                Toast.makeText(
                    this,
                    getString(R.string.p3_min_players_format, minPlayers),
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
            selectedProfileIds.size > maxPlayers -> {
                Toast.makeText(
                    this,
                    getString(R.string.p3_max_players_format, maxPlayers),
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
        }

        val existing = sessions.active()
        if (existing != null) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.p3_resume_session)
                .setMessage(R.string.p3_replace_session_body)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.p3_replace_session) { _, _ -> startNewSession() }
                .show()
        } else {
            startNewSession()
        }
    }

    private fun startNewSession() {
        val chosen = profiles.getAll().filter { it.id in selectedProfileIds }
        val rounds = if (selectedMode == SocialMode.CHALLENGE) {
            selectedChallenge.rounds
        } else {
            selectedRounds
        }
        val session = SocialSession(
            id = UUID.randomUUID().toString(),
            mode = selectedMode.storageKey,
            pack = selectedPack.storageKey,
            challengeType = if (selectedMode == SocialMode.CHALLENGE) {
                selectedChallenge.storageKey
            } else {
                null
            },
            playerIds = chosen.map { it.id },
            playerNames = chosen.map { it.name },
            targetRounds = rounds,
            startedAt = System.currentTimeMillis()
        )
        sessions.saveActive(session)
        currentSession = session
        renderSession(session)
    }

    private fun renderSession(source: SocialSession) {
        if (source.isComplete) {
            finishSession(source)
            return
        }

        var session = source
        if (session.pendingQuestionText.isNullOrBlank()) {
            val next = questions.next(
                QuestionPack.fromStorage(session.pack),
                session.turns.map { it.questionId }.toSet()
            )
            if (next == null) {
                Toast.makeText(this, R.string.p3_no_questions, Toast.LENGTH_LONG).show()
                return
            }
            session = session.copy(
                pendingQuestionId = next.id,
                pendingQuestionText = next.text,
                pendingQuestionIntensity = next.intensity
            )
            sessions.saveActive(session)
        }

        currentSession = session
        showPanel(session = true)

        val playerIndex = session.nextPlayerIndex
        val playerName = session.playerNames.getOrElse(playerIndex) {
            getString(R.string.player_number_format, playerIndex + 1)
        }

        binding.roundText.text =
            getString(R.string.p3_round_format, session.currentRound, session.targetRounds)
        binding.turnText.text =
            getString(R.string.p3_turn_progress_format, session.turns.size + 1, session.totalTurns)
        binding.playerTurn.text = getString(R.string.p3_your_turn_format, playerName)
        binding.sessionQuestion.text = session.pendingQuestionText.orEmpty()
        binding.sessionProgress.progress =
            ((session.turns.size.toFloat() / session.totalTurns.coerceAtLeast(1)) * 100f).toInt()
    }

    private fun launchVoiceTurn() {
        val session = currentSession ?: return
        val playerIndex = session.nextPlayerIndex
        val playerName = session.playerNames.getOrNull(playerIndex) ?: return
        val question = session.pendingQuestionText ?: return

        voiceTurn.launch(
            Intent(this, TestActivity::class.java).apply {
                putExtra(TestActivity.EXTRA_MODE, TestActivity.MODE_SESSION)
                putExtra(TestActivity.EXTRA_FORCED_QUESTION_TEXT, question)
                putExtra(TestActivity.EXTRA_FORCED_QUESTION_CATEGORY, session.pack)
                putExtra(
                    TestActivity.EXTRA_FORCED_QUESTION_INTENSITY,
                    session.pendingQuestionIntensity
                )
                putExtra(
                    TestActivity.EXTRA_HISTORY_MODE_OVERRIDE,
                    "social_" + session.mode
                )
                putExtra(TestActivity.EXTRA_SESSION_PLAYER_NAME, playerName)
            }
        )
    }

    private fun acceptTurn(score: Int) {
        val session = sessions.active() ?: currentSession ?: return
        val playerIndex = session.nextPlayerIndex
        val playerId = session.playerIds.getOrNull(playerIndex) ?: return
        val playerName = session.playerNames.getOrNull(playerIndex) ?: return
        val questionId = session.pendingQuestionId ?: return
        val questionText = session.pendingQuestionText ?: return

        profiles.recordResult(
            id = playerId,
            score = score,
            xpAward = XpEngine.resultXp(score)
        )

        val updated = session.copy(
            turns = session.turns + SocialTurn(
                questionId = questionId,
                questionText = questionText,
                playerId = playerId,
                playerName = playerName,
                score = score.coerceIn(0, 100),
                timestamp = System.currentTimeMillis()
            ),
            pendingQuestionId = null,
            pendingQuestionText = null,
            pendingQuestionIntensity = null
        )

        currentSession = updated
        if (updated.isComplete) {
            finishSession(updated)
        } else {
            sessions.saveActive(updated)
            renderSession(updated)
        }
    }

    private fun finishSession(session: SocialSession) {
        val alreadyCompleted = session.completedAt != null
        val finished = if (alreadyCompleted) session else sessions.complete(session)
        val standings = SocialSessionEngine.standings(finished)
        val winnerIds = SocialSessionEngine.winnerIds(finished)

        if (!alreadyCompleted) {
            finished.playerIds.forEach { id ->
                profiles.addXp(
                    id,
                    XpEngine.sessionCompletionXp(id in winnerIds)
                )
            }
        }

        var knownAchievements = achievements.unlocked()
        val unlockedThisSession = linkedSetOf<com.halashasneen.truthtest.data.AchievementKey>()
        repeat(3) {
            val updatedAchievements = achievements.sync(
                results = history.getAll(),
                totalXp = profiles.totalXp(),
                completedSessions = sessions.completedCount()
            )
            val newlyUnlocked = updatedAchievements - knownAchievements
            if (newlyUnlocked.isEmpty()) return@repeat
            unlockedThisSession += newlyUnlocked
            profiles.addXp(
                profiles.active().id,
                XpEngine.achievementXp(newlyUnlocked.size)
            )
            knownAchievements = updatedAchievements
        }
        val newlyUnlocked = unlockedThisSession.firstOrNull()

        currentSession = finished
        showPanel(summary = true)

        val winners = standings.filter { it.rank == 1 }.map { it.playerName }
        binding.winnerText.text = if (winners.size == 1) {
            getString(R.string.p3_winner_format, winners.first())
        } else {
            getString(R.string.p3_tie_format, winners.joinToString(" • "))
        }

        binding.rankingText.text = buildString {
            append(getString(R.string.p3_ranking_title))
            standings.forEach { standing ->
                append("\n")
                append(
                    getString(
                        R.string.p3_ranking_line,
                        standing.rank,
                        standing.playerName,
                        standing.averageScore,
                        standing.bestScore
                    )
                )
            }
        }

        val top = SocialSessionEngine.highestTurn(finished)
        binding.bestAnswerText.text = if (top == null) {
            getString(R.string.p3_best_answer_title)
        } else {
            getString(
                R.string.p3_best_answer_format,
                top.playerName,
                top.score,
                top.questionText
            )
        }

        val resultXp = finished.turns.sumOf { XpEngine.resultXp(it.score) }
        val completionXp = finished.playerIds.sumOf {
            XpEngine.sessionCompletionXp(it in winnerIds)
        }
        val achievementXp = XpEngine.achievementXp(unlockedThisSession.size)
        binding.xpEarnedText.text =
            getString(
                R.string.p3_xp_earned_format,
                resultXp + completionXp + achievementXp
            )

        if (newlyUnlocked != null) {
            binding.newBadgeText.visibility = View.VISIBLE
            binding.newBadgeText.text =
                getString(R.string.p3_new_badge_format, getString(achievementLabel(newlyUnlocked)))
        } else {
            binding.newBadgeText.visibility = View.GONE
        }
    }

    private fun prepareReplay() {
        val finished = currentSession ?: return
        selectedMode = SocialMode.fromStorage(finished.mode)
        selectedPack = QuestionPack.fromStorage(finished.pack)
        selectedChallenge = ChallengeType.fromStorage(finished.challengeType)
        selectedRounds = finished.targetRounds
        selectedProfileIds.clear()
        selectedProfileIds.addAll(finished.playerIds)
        currentSession = null
        sessions.clearActive()
        renderModeHeader()
        showPanel(setup = true)
        updateSetup()
    }

    private fun addProfile() {
        if (profiles.getAll().size >= PlayerProfileRepository.MAX_PROFILES) {
            Toast.makeText(this, R.string.p3_profile_limit, Toast.LENGTH_SHORT).show()
            return
        }
        val dialogBinding = DialogProfileBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.p3_add_profile)
            .setView(dialogBinding.root)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                val avatar = when (dialogBinding.avatarGroup.checkedChipId) {
                    R.id.avatarTwo -> "◆"
                    R.id.avatarThree -> "★"
                    R.id.avatarFour -> "◈"
                    R.id.avatarFive -> "⚡"
                    else -> "◎"
                }
                val profile = profiles.add(
                    dialogBinding.profileNameInput.text?.toString().orEmpty(),
                    avatar
                )
                if (profile != null) {
                    selectedProfileIds += profile.id
                    dialog.dismiss()
                    updateSetup()
                }
            }
        }
        dialog.show()
    }

    private fun showPanel(
        setup: Boolean = false,
        session: Boolean = false,
        summary: Boolean = false
    ) {
        binding.setupPanel.visibility = if (setup) View.VISIBLE else View.GONE
        binding.sessionPanel.visibility = if (session) View.VISIBLE else View.GONE
        binding.summaryPanel.visibility = if (summary) View.VISIBLE else View.GONE
    }

    private fun playerLimits(mode: SocialMode): Pair<Int, Int> = when (mode) {
        SocialMode.COUPLES -> 2 to 2
        SocialMode.FRIENDS -> 2 to 4
        SocialMode.PARTY -> 3 to 4
        SocialMode.CHALLENGE -> 2 to 4
    }

    private fun defaultRounds(mode: SocialMode): Int = when (mode) {
        SocialMode.PARTY -> 3
        SocialMode.CHALLENGE -> selectedChallenge.rounds
        else -> 3
    }

    private fun modeTitle(mode: SocialMode): Int = when (mode) {
        SocialMode.COUPLES -> R.string.p3_mode_couples
        SocialMode.FRIENDS -> R.string.p3_mode_friends
        SocialMode.PARTY -> R.string.p3_mode_party
        SocialMode.CHALLENGE -> R.string.p3_mode_challenge
    }

    private fun modeBody(mode: SocialMode): Int = when (mode) {
        SocialMode.COUPLES -> R.string.p3_mode_couples_body
        SocialMode.FRIENDS -> R.string.p3_mode_friends_body
        SocialMode.PARTY -> R.string.p3_mode_party_body
        SocialMode.CHALLENGE -> R.string.p3_mode_challenge_body
    }

    private fun packLabel(pack: QuestionPack): Int = when (pack) {
        QuestionPack.CLASSIC -> R.string.p3_pack_classic
        QuestionPack.FUNNY -> R.string.p3_pack_funny
        QuestionPack.BOLD -> R.string.p3_pack_bold
        QuestionPack.DEEP -> R.string.p3_pack_deep
        QuestionPack.COUPLES -> R.string.p3_pack_couples
        QuestionPack.FRIENDS -> R.string.p3_pack_friends
    }

    private fun challengeLabel(type: ChallengeType): Int = when (type) {
        ChallengeType.HIGHEST_SCORE -> R.string.p3_challenge_highest
        ChallengeType.BEST_OF_THREE -> R.string.p3_challenge_best_three
        ChallengeType.STREAK -> R.string.p3_challenge_streak
        ChallengeType.QUICK_ROUND -> R.string.p3_challenge_quick
    }

    private fun achievementLabel(key: com.halashasneen.truthtest.data.AchievementKey): Int =
        when (key) {
            com.halashasneen.truthtest.data.AchievementKey.FIRST -> R.string.achievement_first
            com.halashasneen.truthtest.data.AchievementKey.TEN -> R.string.achievement_ten
            com.halashasneen.truthtest.data.AchievementKey.FIFTY -> R.string.achievement_fifty
            com.halashasneen.truthtest.data.AchievementKey.HUNDRED -> R.string.achievement_hundred
            com.halashasneen.truthtest.data.AchievementKey.HIGH -> R.string.achievement_high
            com.halashasneen.truthtest.data.AchievementKey.STREAK_3 -> R.string.achievement_streak
            com.halashasneen.truthtest.data.AchievementKey.STREAK_7 -> R.string.achievement_streak_seven
            com.halashasneen.truthtest.data.AchievementKey.DUEL -> R.string.achievement_duel
            com.halashasneen.truthtest.data.AchievementKey.GROUP -> R.string.achievement_group
            com.halashasneen.truthtest.data.AchievementKey.CUSTOM -> R.string.achievement_custom
            com.halashasneen.truthtest.data.AchievementKey.COUPLES -> R.string.achievement_couples
            com.halashasneen.truthtest.data.AchievementKey.FRIENDS -> R.string.achievement_friends
            com.halashasneen.truthtest.data.AchievementKey.PARTY -> R.string.achievement_party
            com.halashasneen.truthtest.data.AchievementKey.DAILY -> R.string.achievement_daily
            com.halashasneen.truthtest.data.AchievementKey.CHALLENGE -> R.string.achievement_challenge
            com.halashasneen.truthtest.data.AchievementKey.XP_500 -> R.string.achievement_xp500
            com.halashasneen.truthtest.data.AchievementKey.SESSIONS_5 -> R.string.achievement_sessions5
        }

    companion object {
        const val EXTRA_MODE = "social_mode"
        const val EXTRA_RESUME = "resume_social"
    }
}
