package io.tl.mynhentai.data.local

import android.content.Context

class SettingsHelper(private val appContext: Context) {

    private val prefs = appContext.getSharedPreferences("mynhentai_settings", Context.MODE_PRIVATE)

    var maxConcurrency: Int
        get() = prefs.getInt(KEY_CONCURRENCY, 10)
        set(value) = prefs.edit().putInt(KEY_CONCURRENCY, value).apply()

    var languageFilter: String
        get() = prefs.getString(KEY_LANGUAGE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()

    val cacheDir: java.io.File
        get() = java.io.File(appContext.cacheDir, "coil_cache")

    fun cacheSize(): Long {
        return if (cacheDir.exists()) {
            cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        } else 0L
    }

    fun clearCache() {
        cacheDir.deleteRecursively()
    }

    companion object {
        private const val KEY_CONCURRENCY = "max_concurrency"
        private const val KEY_LANGUAGE = "language_filter"
    }
}
