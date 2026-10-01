package com.halashasneen.truthtest.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.halashasneen.truthtest.core.AppStorageContract
import com.halashasneen.truthtest.data.model.SocialSession

class SocialSessionRepository(context: Context) {
    private val prefs = context.getSharedPreferences(
        AppStorageContract.PREFS_SOCIAL,
        Context.MODE_PRIVATE
    )
    private val gson = Gson()
    private val listType = object : TypeToken<List<SocialSession>>() {}.type

    fun active(): SocialSession? = runCatching {
        prefs.getString(AppStorageContract.KEY_ACTIVE_SOCIAL_SESSION, null)
            ?.let { gson.fromJson(it, SocialSession::class.java) }
    }.getOrNull()

    fun saveActive(session: SocialSession) {
        prefs.edit()
            .putString(AppStorageContract.KEY_ACTIVE_SOCIAL_SESSION, gson.toJson(session))
            .apply()
    }

    fun clearActive() {
        prefs.edit().remove(AppStorageContract.KEY_ACTIVE_SOCIAL_SESSION).apply()
    }

    fun completed(): List<SocialSession> = runCatching {
        gson.fromJson<List<SocialSession>>(
            prefs.getString(AppStorageContract.KEY_COMPLETED_SOCIAL_SESSIONS, null),
            listType
        ).orEmpty()
    }.getOrDefault(emptyList()).sortedByDescending { it.completedAt ?: it.startedAt }

    fun complete(session: SocialSession): SocialSession {
        val finished = session.copy(completedAt = System.currentTimeMillis())
        val updated = (listOf(finished) + completed())
            .distinctBy { it.id }
            .take(MAX_COMPLETED)
        prefs.edit()
            .putString(AppStorageContract.KEY_COMPLETED_SOCIAL_SESSIONS, gson.toJson(updated))
            .remove(AppStorageContract.KEY_ACTIVE_SOCIAL_SESSION)
            .apply()
        return finished
    }

    fun completedCount(): Int = completed().size

    companion object {
        private const val MAX_COMPLETED = 50
    }
}
