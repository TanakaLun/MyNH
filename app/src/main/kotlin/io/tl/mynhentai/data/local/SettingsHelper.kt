package io.tl.mynhentai.data.local

import android.content.Context

class SettingsHelper(context: Context) {

    private val prefs = context.getSharedPreferences("mynhentai_settings", Context.MODE_PRIVATE)

    var maxConcurrency: Int
        get() = prefs.getInt(KEY_CONCURRENCY, 10)
        set(value) = prefs.edit().putInt(KEY_CONCURRENCY, value).apply()

    val cacheDir: java.io.File
        get() = java.io.File(context.cacheDir, "coil_cache")

    fun clearCache() {
        cacheDir.deleteRecursively()
    }

    companion object {
        private const val KEY_CONCURRENCY = "max_concurrency"
    }
}
