package io.tl.mynhentai.data.local

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class SettingsHelper(private val appContext: Context) {

    private val prefs = appContext.getSharedPreferences("mynhentai_settings", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    var maxConcurrency: Int
        get() = prefs.getInt(KEY_CONCURRENCY, 10)
        set(value) = prefs.edit().putInt(KEY_CONCURRENCY, value).apply()

    var languageFilter: String
        get() = prefs.getString(KEY_LANGUAGE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()

    var languageFilterEnabled: Boolean
        get() = prefs.getBoolean(KEY_LANG_FILTER_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_LANG_FILTER_ENABLED, value).apply()

    val cacheDir: java.io.File
        get() = java.io.File(appContext.cacheDir, "coil_cache")

    private val searchHistoryFile: java.io.File
        get() = java.io.File(appContext.filesDir, "search_history.json")

    fun getSearchHistory(): List<String> {
        return try {
            if (!searchHistoryFile.exists()) return emptyList()
            val text = searchHistoryFile.readText()
            val data = json.decodeFromString<HistoryData>(text)
            data.items.take(10)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveSearchHistory(history: List<String>) {
        try {
            val text = json.encodeToString(HistoryData.serializer(), HistoryData(history.take(10)))
            searchHistoryFile.parentFile?.mkdirs()
            searchHistoryFile.writeText(text)
        } catch (_: Exception) { }
    }

    fun cacheSize(): Long {
        return if (cacheDir.exists()) {
            cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        } else 0L
    }

    fun clearCache() {
        cacheDir.deleteRecursively()
    }

    @Serializable
    data class HistoryData(val items: List<String>)

    companion object {
        private const val KEY_CONCURRENCY = "max_concurrency"
        private const val KEY_LANGUAGE = "language_filter"
        private const val KEY_LANG_FILTER_ENABLED = "language_filter_enabled"
    }
}
