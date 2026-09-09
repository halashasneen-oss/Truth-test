package com.nuvexa.truthtest.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nuvexa.truthtest.data.model.Question
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestionBankAssetTest {
    private val gson = Gson()
    private val categories = setOf("embarrassing", "funny", "bold", "romantic", "friendship", "family")

    @Test
    fun bankContainsThreeHundredLocalizedQuestionsWithBalancedIntensity() {
        val english = load("questions_en.json")
        val arabic = load("questions_ar.json")
        assertEquals(150, english.size)
        assertEquals(150, arabic.size)
        assertEquals(300, english.size + arabic.size)

        validate(english)
        validate(arabic)
        assertEquals(english.map { it.id }.toSet(), arabic.map { it.id }.toSet())
    }

    private fun validate(items: List<Question>) {
        assertEquals(items.size, items.map { it.id }.toSet().size)
        assertEquals(categories, items.map { it.category }.toSet())
        categories.forEach { category ->
            val questions = items.filter { it.category == category }
            assertEquals(25, questions.size)
            assertEquals(8, questions.count { it.intensity == QuestionRepository.INTENSITY_LIGHT })
            assertEquals(8, questions.count { it.intensity == QuestionRepository.INTENSITY_MEDIUM })
            assertEquals(9, questions.count { it.intensity == QuestionRepository.INTENSITY_BOLD })
            assertTrue(questions.all { it.text.isNotBlank() })
        }
    }

    private fun load(name: String): List<Question> {
        val file = listOf(
            File("src/main/assets/$name"),
            File("app/src/main/assets/$name")
        ).firstOrNull { it.exists() } ?: error("Missing asset $name")
        val type = object : TypeToken<List<Question>>() {}.type
        return gson.fromJson(file.readText(), type)
    }
}
