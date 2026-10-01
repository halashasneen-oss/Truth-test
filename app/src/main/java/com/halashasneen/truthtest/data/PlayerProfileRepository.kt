package com.halashasneen.truthtest.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.halashasneen.truthtest.core.AppStorageContract
import com.halashasneen.truthtest.data.model.PlayerProfile
import java.util.UUID

class PlayerProfileRepository(context: Context) {
    private val prefs = context.getSharedPreferences(
        AppStorageContract.PREFS_PROFILES,
        Context.MODE_PRIVATE
    )
    private val gson = Gson()
    private val listType = object : TypeToken<List<PlayerProfile>>() {}.type

    fun getAll(): List<PlayerProfile> {
        val raw = prefs.getString(AppStorageContract.KEY_PLAYER_PROFILES, null)
        val stored = if (raw.isNullOrBlank()) {
            emptyList()
        } else {
            runCatching { gson.fromJson<List<PlayerProfile>>(raw, listType).orEmpty() }
                .getOrDefault(emptyList())
        }
        return if (stored.isEmpty()) listOf(createDefault()) else stored
    }

    fun active(): PlayerProfile {
        val profiles = getAll()
        val activeId = prefs.getString(AppStorageContract.KEY_ACTIVE_PROFILE_ID, null)
        return profiles.firstOrNull { it.id == activeId } ?: profiles.first()
    }

    fun setActive(id: String) {
        if (getAll().any { it.id == id }) {
            prefs.edit().putString(AppStorageContract.KEY_ACTIVE_PROFILE_ID, id).apply()
        }
    }

    fun add(name: String, avatar: String): PlayerProfile? {
        val clean = name.trim().take(24)
        if (clean.isBlank()) return null
        val current = getAll()
        if (current.size >= MAX_PROFILES) return null
        val profile = PlayerProfile(
            id = UUID.randomUUID().toString(),
            name = clean,
            avatar = avatar.ifBlank { DEFAULT_AVATAR }
        )
        save(current + profile)
        return profile
    }

    fun update(id: String, name: String, avatar: String): Boolean {
        val clean = name.trim().take(24)
        if (clean.isBlank()) return false
        val current = getAll()
        if (current.none { it.id == id }) return false
        save(current.map {
            if (it.id == id) it.copy(name = clean, avatar = avatar.ifBlank { DEFAULT_AVATAR }) else it
        })
        return true
    }

    fun delete(id: String): Boolean {
        val current = getAll()
        if (current.size <= 1 || current.none { it.id == id }) return false
        val updated = current.filterNot { it.id == id }
        save(updated)
        if (prefs.getString(AppStorageContract.KEY_ACTIVE_PROFILE_ID, null) == id) {
            setActive(updated.first().id)
        }
        return true
    }

    fun recordResult(id: String, score: Int, xpAward: Int): PlayerProfile? {
        val cleanScore = score.coerceIn(0, 100)
        var updatedProfile: PlayerProfile? = null
        val updated = getAll().map { profile ->
            if (profile.id == id) {
                profile.copy(
                    xp = profile.xp + xpAward.coerceAtLeast(0),
                    games = profile.games + 1,
                    bestScore = maxOf(profile.bestScore, cleanScore),
                    totalScore = profile.totalScore + cleanScore
                ).also { updatedProfile = it }
            } else {
                profile
            }
        }
        if (updatedProfile != null) save(updated)
        return updatedProfile
    }

    fun addXp(id: String, xp: Int): PlayerProfile? {
        if (xp <= 0) return getAll().firstOrNull { it.id == id }
        var updatedProfile: PlayerProfile? = null
        val updated = getAll().map { profile ->
            if (profile.id == id) {
                profile.copy(xp = profile.xp + xp).also { updatedProfile = it }
            } else {
                profile
            }
        }
        if (updatedProfile != null) save(updated)
        return updatedProfile
    }

    fun totalXp(): Int = getAll().sumOf { it.xp }

    private fun createDefault(): PlayerProfile {
        val profile = PlayerProfile(
            id = DEFAULT_ID,
            name = "Player 1",
            avatar = DEFAULT_AVATAR
        )
        save(listOf(profile))
        prefs.edit().putString(AppStorageContract.KEY_ACTIVE_PROFILE_ID, profile.id).apply()
        return profile
    }

    private fun save(items: List<PlayerProfile>) {
        prefs.edit()
            .putString(AppStorageContract.KEY_PLAYER_PROFILES, gson.toJson(items))
            .apply()
    }

    companion object {
        const val MAX_PROFILES = 8
        const val DEFAULT_ID = "local-player-1"
        const val DEFAULT_AVATAR = "◎"
    }
}
