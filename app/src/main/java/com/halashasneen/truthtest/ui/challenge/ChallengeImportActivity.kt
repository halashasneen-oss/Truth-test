package com.halashasneen.truthtest.ui.challenge

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.halashasneen.truthtest.R
import com.halashasneen.truthtest.ui.SafeArea
import com.halashasneen.truthtest.data.model.ChallengeType
import com.halashasneen.truthtest.data.model.QuestionPack
import com.halashasneen.truthtest.data.model.SocialMode
import com.halashasneen.truthtest.databinding.ActivityChallengeImportBinding
import com.halashasneen.truthtest.share.ChallengePayload
import com.halashasneen.truthtest.share.ChallengePayloadCodec
import com.halashasneen.truthtest.ui.social.SocialSessionActivity

class ChallengeImportActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChallengeImportBinding
    private var payload: ChallengePayload? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChallengeImportBinding.inflate(layoutInflater)
        setContentView(binding.root)
        SafeArea.apply(this, binding.root)

        val raw = intent.getStringExtra(EXTRA_RAW_CHALLENGE) ?: intent.dataString
        payload = ChallengePayloadCodec.decode(raw)
        render()
        binding.cancelButton.setOnClickListener { finish() }
        binding.openButton.setOnClickListener { openChallenge() }
    }

    private fun render() {
        val value = payload
        if (value == null) {
            binding.statusIcon.text = "!"
            binding.titleText.setText(R.string.p4_qr_invalid)
            binding.detailsText.setText(R.string.p4_deep_link_note)
            binding.questionText.visibility = View.GONE
            binding.openButton.visibility = View.GONE
            return
        }

        val mode = SocialMode.fromStorage(value.mode)
        val pack = QuestionPack.fromStorage(value.pack)
        val challenge = ChallengeType.fromStorage(value.challengeType)
        binding.titleText.setText(R.string.p4_qr_valid)
        binding.detailsText.text = getString(
            R.string.p4_challenge_details,
            getString(modeLabel(mode)),
            getString(packLabel(pack)),
            getString(challengeLabel(challenge))
        )
        if (!value.questionText.isNullOrBlank()) {
            binding.questionText.visibility = View.VISIBLE
            binding.questionText.text = value.questionText
        } else {
            binding.questionText.visibility = View.GONE
        }
    }

    private fun openChallenge() {
        val value = payload ?: return
        startActivity(
            Intent(this, SocialSessionActivity::class.java).apply {
                putExtra(SocialSessionActivity.EXTRA_MODE, value.mode)
                putExtra(SocialSessionActivity.EXTRA_INITIAL_PACK, value.pack)
                putExtra(SocialSessionActivity.EXTRA_INITIAL_CHALLENGE, value.challengeType)
                putExtra(SocialSessionActivity.EXTRA_IMPORTED_QUESTION_ID, value.questionId)
                putExtra(SocialSessionActivity.EXTRA_IMPORTED_QUESTION_TEXT, value.questionText)
                putExtra(
                    SocialSessionActivity.EXTRA_IMPORTED_QUESTION_INTENSITY,
                    value.questionIntensity
                )
            }
        )
        finish()
    }

    private fun modeLabel(mode: SocialMode): Int = when (mode) {
        SocialMode.COUPLES -> R.string.p3_mode_couples
        SocialMode.FRIENDS -> R.string.p3_mode_friends
        SocialMode.PARTY -> R.string.p3_mode_party
        SocialMode.CHALLENGE -> R.string.p3_mode_challenge
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

    companion object {
        const val EXTRA_RAW_CHALLENGE = "raw_challenge"
    }
}
