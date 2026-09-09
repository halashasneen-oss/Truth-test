package com.nuvexa.truthtest.data.model

data class Question(
    val id: String,
    val category: String,
    val text: String,
    val intensity: String? = null
)
