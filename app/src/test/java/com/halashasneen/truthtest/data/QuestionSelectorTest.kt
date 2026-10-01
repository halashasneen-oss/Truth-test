package com.halashasneen.truthtest.data

import com.halashasneen.truthtest.data.model.SocialQuestion
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

class QuestionSelectorTest {
    @Test
    fun avoidsUsedQuestionsWhileFreshQuestionsRemain() {
        val questions = listOf(
            SocialQuestion("a", "funny", "A"),
            SocialQuestion("b", "funny", "B"),
            SocialQuestion("c", "funny", "C")
        )
        repeat(20) {
            val selected = QuestionSelector.next(questions, setOf("a", "b"))
            assertNotNull(selected)
            assertFalse(selected!!.id in setOf("a", "b"))
        }
    }
}
