package com.nuvexa.truthtest.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nuvexa.truthtest.data.model.TestResult

class HistoryRepository(context: Context) {
    private val prefs = context.getSharedPreferences("truth_test_history", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val type = object : TypeToken<List<TestResult>>() {}.type

    fun getAll(): List<TestResult> {
        val raw = prefs.getString("results", null) ?: return emptyList()
        return runCatching { gson.fromJson<List<TestResult>>(raw, type).orEmpty() }
            .getOrDefault(emptyList())
            .sortedByDescending { it.timestamp }
    }

    fun add(result: TestResult) {
        val updated = (listOf(result) + getAll()).distinctBy { it.id }.take(100)
        prefs.edit().putString("results", gson.toJson(updated)).apply()
    }

    fun clear() = prefs.edit().remove("results").apply()
}
