package com.halashasneen.truthtest.share

import android.content.Context
import java.io.File

object ShareCacheManager {
    private const val MAX_AGE_MS = 24L * 60L * 60L * 1_000L

    fun cleanup(context: Context, now: Long = System.currentTimeMillis()) {
        val dir = File(context.cacheDir, "shares")
        if (!dir.exists()) return
        dir.listFiles().orEmpty().forEach { file ->
            if (file.isFile && now - file.lastModified() > MAX_AGE_MS) {
                runCatching { file.delete() }
            }
        }
    }
}
