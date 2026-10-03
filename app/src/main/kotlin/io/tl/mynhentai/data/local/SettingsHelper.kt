package io.tl.mynhentai.data.local

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

class SettingsHelper(private val appContext: Context) {

    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    var maxConcurrency: Int
        get() = prefs.getInt(KEY_CONCURRENCY, 10)
        set(value) {
            prefs.edit().putInt(KEY_CONCURRENCY, value).apply()
        }

    var languageFilter: String
        get() = prefs.getString(KEY_LANGUAGE, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_LANGUAGE, value).apply()
        }

    var languageFilterEnabled: Boolean
        get() = prefs.getBoolean(KEY_LANG_FILTER_ENABLED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_LANG_FILTER_ENABLED, value).apply()
        }

    private val _monetEnabledFlow = MutableStateFlow(prefs.getBoolean(KEY_MONET_ENABLED, false))
    val monetEnabledFlow: StateFlow<Boolean> = _monetEnabledFlow.asStateFlow()

    var monetEnabled: Boolean
        get() = _monetEnabledFlow.value
        set(value) {
            prefs.edit().putBoolean(KEY_MONET_ENABLED, value).apply()
            _monetEnabledFlow.value = value
        }

    private val _backAnimStyleFlow = MutableStateFlow(prefs.getString(KEY_BACK_ANIM_STYLE, "slide") ?: "slide")
    val backAnimStyleFlow: StateFlow<String> = _backAnimStyleFlow.asStateFlow()

    var backAnimStyle: String
        get() = _backAnimStyleFlow.value
        set(value) {
            prefs.edit().putString(KEY_BACK_ANIM_STYLE, value).apply()
            _backAnimStyleFlow.value = value
        }

    private val _enableBlurFlow = MutableStateFlow(prefs.getBoolean(KEY_BLUR_ENABLED, true))
    val enableBlurFlow: StateFlow<Boolean> = _enableBlurFlow.asStateFlow()

    var enableBlur: Boolean
        get() = _enableBlurFlow.value
        set(value) {
            prefs.edit().putBoolean(KEY_BLUR_ENABLED, value).apply()
            _enableBlurFlow.value = value
        }

    private val _useFloatingNavbarFlow = MutableStateFlow(prefs.getBoolean(KEY_USE_FLOATING_NAVBAR, false))
    val useFloatingNavbarFlow: StateFlow<Boolean> = _useFloatingNavbarFlow.asStateFlow()

    var useFloatingNavbar: Boolean
        get() = _useFloatingNavbarFlow.value
        set(value) {
            prefs.edit().putBoolean(KEY_USE_FLOATING_NAVBAR, value).apply()
            _useFloatingNavbarFlow.value = value
        }

    private val _floatingNavbarStyleFlow = MutableStateFlow(prefs.getInt(KEY_FLOATING_NAVBAR_STYLE, 0))
    val floatingNavbarStyleFlow: StateFlow<Int> = _floatingNavbarStyleFlow.asStateFlow()

    var floatingNavbarStyle: Int
        get() = _floatingNavbarStyleFlow.value
        set(value) {
            prefs.edit().putInt(KEY_FLOATING_NAVBAR_STYLE, value).apply()
            _floatingNavbarStyleFlow.value = value
        }

    private val _floatingNavbarPositionFlow = MutableStateFlow(prefs.getInt(KEY_FLOATING_NAVBAR_POSITION, 0))
    val floatingNavbarPositionFlow: StateFlow<Int> = _floatingNavbarPositionFlow.asStateFlow()

    var floatingNavbarPosition: Int
        get() = _floatingNavbarPositionFlow.value
        set(value) {
            prefs.edit().putInt(KEY_FLOATING_NAVBAR_POSITION, value).apply()
            _floatingNavbarPositionFlow.value = value
        }

    fun exportToJson(): JsonObject = buildJsonObject {
        prefs.all.forEach { (key, value) ->
            when (value) {
                is Boolean -> put(key, value)
                is Int -> put(key, value)
                is Long -> put(key, value)
                is Float -> put(key, value)
                is String -> put(key, value)
                is Set<*> -> put(key, JsonArray(value.filterNotNull().map { JsonPrimitive(it.toString()) }))
                else -> put(key, JsonPrimitive(value.toString()))
            }
        }
    }

    fun importFromJson(obj: JsonObject) {
        val editor = prefs.edit()
        obj.forEach { (key, element) ->
            val prim = element as? JsonPrimitive ?: return@forEach
            when {
                prim.isString -> editor.putString(key, prim.content)
                prim.booleanOrNull != null -> editor.putBoolean(key, prim.boolean)
                prim.intOrNull != null -> editor.putInt(key, prim.int)
                prim.longOrNull != null -> editor.putLong(key, prim.long)
                prim.doubleOrNull != null -> editor.putFloat(key, prim.double.toFloat())
                else -> editor.putString(key, prim.content)
            }
        }
        editor.apply()
        reloadFlows()
    }

    private fun reloadFlows() {
        _monetEnabledFlow.value = prefs.getBoolean(KEY_MONET_ENABLED, false)
        _backAnimStyleFlow.value = prefs.getString(KEY_BACK_ANIM_STYLE, "slide") ?: "slide"
        _enableBlurFlow.value = prefs.getBoolean(KEY_BLUR_ENABLED, true)
        _useFloatingNavbarFlow.value = prefs.getBoolean(KEY_USE_FLOATING_NAVBAR, false)
        _floatingNavbarStyleFlow.value = prefs.getInt(KEY_FLOATING_NAVBAR_STYLE, 0)
        _floatingNavbarPositionFlow.value = prefs.getInt(KEY_FLOATING_NAVBAR_POSITION, 0)
    }

    val coilCacheDir: java.io.File
        get() = java.io.File(appContext.cacheDir, "coil_cache")

    val offlineCacheDir: java.io.File
        get() = java.io.File(appContext.cacheDir, "offline")

    private val searchHistoryFile: java.io.File
        get() = java.io.File(appContext.filesDir, "search_history.json")

    private fun readSearchHistory(): List<String> {
        return try {
            if (!searchHistoryFile.exists()) return emptyList()
            val text = searchHistoryFile.readText()
            json.decodeFromString<HistoryData>(text).items
        } catch (e: Exception) {
            emptyList()
        }
    }

    private val _searchHistoryFlow = MutableStateFlow(readSearchHistory())
    val searchHistoryFlow: StateFlow<List<String>> = _searchHistoryFlow.asStateFlow()

    private fun writeSearchHistory(history: List<String>) {
        try {
            searchHistoryFile.parentFile?.mkdirs()
            searchHistoryFile.writeText(json.encodeToString(HistoryData.serializer(), HistoryData(history)))
        } catch (_: Exception) { }
    }

    fun addSearchHistoryItem(query: String) {
        val history = _searchHistoryFlow.value.toMutableList()
        history.remove(query)
        history.add(0, query)
        _searchHistoryFlow.value = history
        writeSearchHistory(history)
    }

    fun removeSearchHistoryItem(query: String) {
        val history = _searchHistoryFlow.value.filter { it != query }
        _searchHistoryFlow.value = history
        writeSearchHistory(history)
    }

    fun clearSearchHistory() {
        _searchHistoryFlow.value = emptyList()
        writeSearchHistory(emptyList())
    }

    fun coilCacheSize(): Long {
        return if (coilCacheDir.exists()) {
            coilCacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        } else 0L
    }

    fun offlineCacheSize(): Long {
        return if (offlineCacheDir.exists()) {
            offlineCacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        } else 0L
    }

    fun totalCacheSize(): Long = coilCacheSize() + offlineCacheSize()

    fun clearCoilCache() {
        coilCacheDir.deleteRecursively()
    }

    fun clearOfflineCache() {
        offlineCacheDir.deleteRecursively()
    }

    fun clearAllCache() {
        clearCoilCache()
        clearOfflineCache()
    }

    @Serializable
    data class HistoryData(val items: List<String>)

    companion object {
        const val PREFS_NAME = "mynhentai_settings"
        const val KEY_CONCURRENCY = "max_concurrency"
        private const val KEY_LANGUAGE = "language_filter"
        private const val KEY_LANG_FILTER_ENABLED = "language_filter_enabled"
        private const val KEY_MONET_ENABLED = "monet_enabled"
        private const val KEY_BACK_ANIM_STYLE = "back_anim_style"
        private const val KEY_BLUR_ENABLED = "blur_enabled"
        private const val KEY_USE_FLOATING_NAVBAR = "use_floating_navbar"
        private const val KEY_FLOATING_NAVBAR_STYLE = "floating_navbar_style"
        private const val KEY_FLOATING_NAVBAR_POSITION = "floating_navbar_position"
    }
}
