package com.halashasneen.truthtest.data

import com.halashasneen.truthtest.data.model.Question

/** Choose an unseen question; keep category/intensity whenever the bank allows. */
object QuestionSequence {
    fun next(
        all: List<Question>,
        category: String,
        intensity: String,
        usedIds: Set<String>
    ): Question? {
        val unseen = all.filterNot { it.id in usedIds }
        val preferred = unseen.filter { it.category == category && it.intensity == intensity }
        val sameCategory = unseen.filter { it.category == category }
        return (preferred.ifEmpty { sameCategory }.ifEmpty { unseen }).randomOrNull()
    }

    fun average(scores: List<Int>): Int =
        if (scores.isEmpty()) 0 else scores.average().toInt().coerceIn(0, 100)
}
