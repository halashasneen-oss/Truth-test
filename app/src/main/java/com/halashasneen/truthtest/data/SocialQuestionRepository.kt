package com.halashasneen.truthtest.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.halashasneen.truthtest.data.model.QuestionPack
import com.halashasneen.truthtest.data.model.SocialMode
import com.halashasneen.truthtest.data.model.SocialQuestion
import java.io.InputStreamReader

class SocialQuestionRepository(private val context: Context) {
    private val gson = Gson()

    fun all(): List<SocialQuestion> {
        val language = context.resources.configuration.locales[0].language
        val asset = if (language == "ar") "social_questions_ar.json" else "social_questions_en.json"
        return runCatching {
            context.assets.open(asset).use { input ->
                InputStreamReader(input).use { reader ->
                    val type = object : TypeToken<List<SocialQuestion>>() {}.type
                    gson.fromJson<List<SocialQuestion>>(reader, type).orEmpty()
                }
            }
        }.getOrDefault(emptyList())
    }

    fun availablePacks(mode: SocialMode): List<QuestionPack> = when (mode) {
        SocialMode.COUPLES -> listOf(QuestionPack.COUPLES, QuestionPack.DEEP, QuestionPack.FUNNY, QuestionPack.BOLD)
        SocialMode.FRIENDS -> listOf(QuestionPack.FRIENDS, QuestionPack.FUNNY, QuestionPack.BOLD, QuestionPack.DEEP)
        SocialMode.PARTY -> listOf(QuestionPack.CLASSIC, QuestionPack.FUNNY, QuestionPack.BOLD)
        SocialMode.CHALLENGE -> listOf(QuestionPack.CLASSIC, QuestionPack.FUNNY, QuestionPack.BOLD, QuestionPack.DEEP)
    }

    fun next(pack: QuestionPack, usedIds: Set<String>): SocialQuestion? =
        QuestionSelector.next(
            all().filter { it.pack == pack.storageKey },
            usedIds
        )
}

object QuestionSelector {
    fun next(questions: List<SocialQuestion>, usedIds: Set<String>): SocialQuestion? {
        if (questions.isEmpty()) return null
        val fresh = questions.filterNot { it.id in usedIds }
        return (fresh.ifEmpty { questions }).randomOrNull()
    }
}
