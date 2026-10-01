package com.halashasneen.truthtest.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.halashasneen.truthtest.data.model.SocialQuestion
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialQuestionAssetTest {
    private val gson = Gson()
    private val packs = setOf("classic", "funny", "bold", "deep", "couples", "friends")

    @Test
    fun socialBanksContainSixtyLocalizedQuestionsEach() {
        val english = load("social_questions_en.json")
        val arabic = load("social_questions_ar.json")

        assertEquals(60, english.size)
        assertEquals(60, arabic.size)
        assertEquals(english.map { it.id }.toSet(), arabic.map { it.id }.toSet())
        validate(english)
        validate(arabic)
    }

    private fun validate(items: List<SocialQuestion>) {
        assertEquals(items.size, items.map { it.id }.toSet().size)
        assertEquals(packs, items.map { it.pack }.toSet())
        packs.forEach { pack ->
            assertEquals(10, items.count { it.pack == pack })
        }
        assertTrue(items.all { it.text.isNotBlank() })
        assertTrue(items.all { it.intensity in QuestionRepository.INTENSITIES })
    }

    private fun load(name: String): List<SocialQuestion> {
        val file = listOf(
            File("src/main/assets/$name"),
            File("app/src/main/assets/$name")
        ).firstOrNull { it.exists() } ?: error("Missing asset $name")
        val type = object : TypeToken<List<SocialQuestion>>() {}.type
        return gson.fromJson(file.readText(), type)
    }
}
