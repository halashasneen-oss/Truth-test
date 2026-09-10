package com.halashasneen.truthtest.data.model

data class TestResult(
    val id: String,
    val question: String,
    val category: String,
    val score: Int,
    val timestamp: Long,
    val mode: String = "solo",
    val intensity: String? = null
)
