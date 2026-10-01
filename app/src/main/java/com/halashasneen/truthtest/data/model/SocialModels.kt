package com.halashasneen.truthtest.data.model

enum class SocialMode(val storageKey: String) {
    COUPLES("couples"),
    FRIENDS("friends"),
    PARTY("party"),
    CHALLENGE("challenge");

    companion object {
        fun fromStorage(value: String?): SocialMode =
            entries.firstOrNull { it.storageKey == value } ?: PARTY
    }
}

enum class QuestionPack(val storageKey: String) {
    CLASSIC("classic"),
    FUNNY("funny"),
    BOLD("bold"),
    DEEP("deep"),
    COUPLES("couples"),
    FRIENDS("friends");

    companion object {
        fun fromStorage(value: String?): QuestionPack =
            entries.firstOrNull { it.storageKey == value } ?: CLASSIC
    }
}

enum class ChallengeType(val storageKey: String, val rounds: Int) {
    HIGHEST_SCORE("highest_score", 1),
    BEST_OF_THREE("best_of_three", 3),
    STREAK("streak", 3),
    QUICK_ROUND("quick_round", 1);

    companion object {
        fun fromStorage(value: String?): ChallengeType =
            entries.firstOrNull { it.storageKey == value } ?: HIGHEST_SCORE
    }
}

data class SocialQuestion(
    val id: String,
    val pack: String,
    val text: String,
    val intensity: String = "medium"
)

data class SocialTurn(
    val questionId: String,
    val questionText: String,
    val playerId: String,
    val playerName: String,
    val score: Int,
    val timestamp: Long
)

data class SocialSession(
    val id: String,
    val mode: String,
    val pack: String,
    val challengeType: String? = null,
    val playerIds: List<String>,
    val playerNames: List<String>,
    val targetRounds: Int,
    val turns: List<SocialTurn> = emptyList(),
    val pendingQuestionId: String? = null,
    val pendingQuestionText: String? = null,
    val pendingQuestionIntensity: String? = null,
    val startedAt: Long,
    val completedAt: Long? = null
) {
    val totalTurns: Int get() = playerIds.size * targetRounds
    val isComplete: Boolean get() = playerIds.isNotEmpty() && turns.size >= totalTurns
    val nextPlayerIndex: Int get() = if (playerIds.isEmpty()) 0 else turns.size % playerIds.size
    val currentRound: Int
        get() = if (playerIds.isEmpty()) 1 else (turns.size / playerIds.size + 1).coerceAtMost(targetRounds)
}
