package com.halashasneen.truthtest.data

import com.halashasneen.truthtest.data.model.Question
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class QuestionSequenceTest {
    private val bank = listOf(
        Question("a", "funny", "One", "light"),
        Question("b", "funny", "Two", "light"),
        Question("c", "funny", "Three", "medium"),
        Question("d", "bold", "Four", "light")
    )
    @Test fun doesNotRepeatPreviousQuestions() {
        val next = QuestionSequence.next(bank, "funny", "light", setOf("a"))
        assertEquals("b", next?.id)
    }
    @Test fun fallsBackToUnseenInCategoryThenOtherCategories() {
        assertEquals("c", QuestionSequence.next(bank, "funny", "light", setOf("a", "b"))?.id)
        assertEquals("d", QuestionSequence.next(bank, "funny", "light", setOf("a", "b", "c"))?.id)
    }
    @Test fun returnsNullWhenAllUsed() {
        assertNull(QuestionSequence.next(bank, "funny", "light", bank.map { it.id }.toSet()))
    }
    @Test fun averageAndEmptySummaryAreStable() {
        assertEquals(0, QuestionSequence.average(emptyList()))
        assertEquals(75, QuestionSequence.average(listOf(50, 100)))
    }
}
