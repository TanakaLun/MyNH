package io.tl.mynhentai.data.local

import android.content.Context
import android.net.Uri
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

@Serializable
data class BackupData(
    val version: Int = 2,
    val favorites: List<FavoriteEntity>? = null,
    val history: List<HistoryEntity>? = null,
    val settings: JsonObject? = null,
    val usageEvents: List<UsageEventEntity>? = null,
    val readingRecords: List<ReadingRecordEntity>? = null,
)

class BackupHelper(
    private val context: Context,
    private val dao: MangaDao,
    private val usageDao: UsageDao,
    private val settingsHelper: SettingsHelper,
) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    suspend fun export(
        uri: Uri,
        favorites: Boolean,
        history: Boolean,
        backupSettings: Boolean,
        statistics: Boolean,
    ): Unit = withContext(Dispatchers.IO) {
        val data = BackupData(
            favorites = if (favorites) dao.getFavoritesOnce() else null,
            history = if (history) dao.getHistoryOnce() else null,
            settings = if (backupSettings) settingsHelper.exportToJson() else null,
            usageEvents = if (statistics) usageDao.getAllEventsOnce() else null,
            readingRecords = if (statistics) usageDao.getAllReadingsOnce() else null,
        )
        val output = context.contentResolver.openOutputStream(uri, "wt")
            ?: throw IOException("Cannot open output stream")
        output.use {
            it.write(json.encodeToString(BackupData.serializer(), data).toByteArray())
        }
    }

    suspend fun parse(uri: Uri): BackupData = withContext(Dispatchers.IO) {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Cannot open input stream")
        val text = input.use { it.readBytes().decodeToString() }
        json.decodeFromString(BackupData.serializer(), text)
    }

    suspend fun importFrom(
        data: BackupData,
        favorites: Boolean,
        history: Boolean,
        backupSettings: Boolean,
        statistics: Boolean,
    ): Unit = withContext(Dispatchers.IO) {
        if (favorites) data.favorites?.let { dao.insertFavorites(it) }
        if (history) data.history?.let { dao.insertHistories(it) }
        if (backupSettings) data.settings?.let { settingsHelper.importFromJson(it) }
        if (statistics) {
            usageDao.clearEvents()
            usageDao.clearReadings()
            data.usageEvents?.takeIf { it.isNotEmpty() }?.let { usageDao.insertEvents(it) }
            data.readingRecords?.takeIf { it.isNotEmpty() }?.let { usageDao.insertReadings(it) }
        }
    }
}
