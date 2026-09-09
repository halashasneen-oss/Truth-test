package com.nuvexa.truthtest.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nuvexa.truthtest.data.model.Question
import java.io.InputStreamReader
import java.time.LocalDate

class QuestionRepository(private val context: Context) {
    private val gson = Gson()

    fun allQuestions(): List<Question> {
        val language = context.resources.configuration.locales[0].language
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

    fun random(category: String): Question? = allQuestions().filter { it.category == category }.randomOrNull()

    fun dailyQuestion(): Question? {
        val all = allQuestions()
        if (all.isEmpty()) return null
        val index = (LocalDate.now().toEpochDay() % all.size.toLong()).toInt()
        return all[index]
    }
}
