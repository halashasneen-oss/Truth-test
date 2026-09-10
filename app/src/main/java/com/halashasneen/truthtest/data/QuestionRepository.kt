package com.halashasneen.truthtest.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.halashasneen.truthtest.data.model.Question
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
                    gson.fromJson<List<Question>>(reader, type).orEmpty().map(::normalize)
                }
            }
        }.getOrDefault(emptyList())
    }

    fun count(category: String, intensity: String = preferredIntensity()): Int =
        allQuestions().count { it.category == category && it.intensity == normalizeIntensity(intensity) }

    fun random(category: String, intensity: String = preferredIntensity()): Question? {
        val normalizedIntensity = normalizeIntensity(intensity)
        val categoryQuestions = allQuestions().filter { it.category == category }
        if (categoryQuestions.isEmpty()) return null
        val matching = categoryQuestions.filter { it.intensity == normalizedIntensity }
        val questions = matching.ifEmpty { categoryQuestions }
        val key = "last_${currentLanguage()}_${category}_$normalizedIntensity"
        val lastId = prefs.getString(key, null)
        val pool = if (questions.size > 1) questions.filterNot { it.id == lastId } else questions
        val chosen = pool.randomOrNull() ?: questions.first()
        prefs.edit().putString(key, chosen.id).apply()
        return chosen
    }

    fun dailyQuestion(intensity: String = preferredIntensity()): Question? {
        val all = allQuestions()
        if (all.isEmpty()) return null
        val preferred = all.filter { it.intensity == normalizeIntensity(intensity) }.ifEmpty { all }
        val index = (LocalDate.now().toEpochDay() % preferred.size.toLong()).toInt()
        return preferred[index]
    }

    fun preferredIntensity(): String = normalizeIntensity(
        prefs.getString(PREF_INTENSITY, INTENSITY_MEDIUM)
    )

    fun setPreferredIntensity(intensity: String) {
        prefs.edit().putString(PREF_INTENSITY, normalizeIntensity(intensity)).apply()
    }

    private fun normalize(question: Question): Question = question.copy(
        intensity = normalizeIntensity(question.intensity)
    )

    private fun normalizeIntensity(value: String?): String {
        val normalized = value?.lowercase()?.trim() ?: return INTENSITY_MEDIUM
        return if (normalized in INTENSITIES) normalized else INTENSITY_MEDIUM
    }

    private fun currentLanguage(): String = context.resources.configuration.locales[0].language

    companion object {
        const val INTENSITY_LIGHT = "light"
        const val INTENSITY_MEDIUM = "medium"
        const val INTENSITY_BOLD = "bold"
        private const val PREF_INTENSITY = "preferred_intensity"
        val INTENSITIES = setOf(INTENSITY_LIGHT, INTENSITY_MEDIUM, INTENSITY_BOLD)
    }
}
