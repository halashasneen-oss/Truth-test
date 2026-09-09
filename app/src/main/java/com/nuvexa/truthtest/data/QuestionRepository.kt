package com.nuvexa.truthtest.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nuvexa.truthtest.data.model.Question
import java.io.InputStreamReader
import java.time.LocalDate

class QuestionRepository(private val context: Context) {
    private val gson = Gson()
    private val prefs = context.getSharedPreferences("truth_test_questions", Context.MODE_PRIVATE)

    fun allQuestions(): List<Question> {
        val language = currentLanguage()
        val asset = if (language == "ar") "questions_ar.json" else "questions_en.json"
        return runCatching {
            context.assets.open(asset).use { input ->
                InputStreamReader(input).use { reader ->
                    val type = object : TypeToken<List<Question>>() {}.type
                    gson.fromJson<List<Question>>(reader, type).orEmpty()
                }
            }
        }.getOrDefault(emptyList())
    }

    fun random(category: String): Question? {
        val questions = allQuestions().filter { it.category == category }
        if (questions.isEmpty()) return null
        val key = "last_${currentLanguage()}_$category"
        val lastId = prefs.getString(key, null)
        val pool = if (questions.size > 1) questions.filterNot { it.id == lastId } else questions
        val chosen = pool.randomOrNull() ?: questions.first()
        prefs.edit().putString(key, chosen.id).apply()
        return chosen
    }

    fun dailyQuestion(): Question? {
        val all = allQuestions()
        if (all.isEmpty()) return null
        val index = (LocalDate.now().toEpochDay() % all.size.toLong()).toInt()
        return all[index]
    }

    private fun currentLanguage(): String = context.resources.configuration.locales[0].language
}
