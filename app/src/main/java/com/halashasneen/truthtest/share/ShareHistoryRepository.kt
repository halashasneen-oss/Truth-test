package com.halashasneen.truthtest.share

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.halashasneen.truthtest.core.AppStorageContract

class ShareHistoryRepository(context: Context) {
    private val prefs = context.getSharedPreferences(
        AppStorageContract.PREFS_SHARE,
        Context.MODE_PRIVATE
    )
    private val gson = Gson()
    private val type = object : TypeToken<List<ShareHistoryEntry>>() {}.type

    fun all(): List<ShareHistoryEntry> {
        val raw = prefs.getString(AppStorageContract.KEY_SHARE_HISTORY, null)
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            gson.fromJson<List<ShareHistoryEntry>>(raw, type).orEmpty()
        }.getOrDefault(emptyList()).sortedByDescending { it.timestamp }
    }

    fun add(entry: ShareHistoryEntry) {
        val updated = (listOf(entry) + all())
            .distinctBy { it.sourceId + ":" + it.format + ":" + it.template + ":" + it.timestamp }
            .take(MAX_ENTRIES)
        prefs.edit()
            .putString(AppStorageContract.KEY_SHARE_HISTORY, gson.toJson(updated))
            .apply()
    }

    companion object {
        private const val MAX_ENTRIES = 30
    }
}
