package com.nuvexa.truthtest.ui.test

import com.nuvexa.truthtest.data.model.Question

enum class TestStage { CATEGORY, CUSTOM, RECORDING, RESULT }

data class TestUiState(
    val initialized: Boolean = false,
    val mode: String = TestActivity.MODE_SOLO,
    val stage: TestStage = TestStage.CATEGORY,
    val question: Question? = null,
    val player: Int = 1,
    val firstScore: Int? = null,
    val secondScore: Int? = null,
    val finalScore: Int = 0
)
