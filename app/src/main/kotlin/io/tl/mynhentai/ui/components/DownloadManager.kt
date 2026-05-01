package io.tl.mynhentai.ui.components

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DownloadManager(
    private val client: OkHttpClient,
    private val appContext: Context
) {

    private suspend fun downloadImage(url: String, dest: File) = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        response.body?.byteStream()?.use { input ->
            dest.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    suspend fun downloadAndSave(
        pages: List<Pair<Int, String>>,
        galleryId: Long,
        galleryTitle: String,
        targetDir: String
    ) = withContext(Dispatchers.IO) {
        val tempDir = File(appContext.cacheDir, "download_temp_$galleryId")
        tempDir.mkdirs()
        try {
            for ((num, url) in pages) {
                val ext = if (url.contains("jpg") || url.contains("jpeg")) ".jpg" else ".png"
                val pageFile = File(tempDir, "$num$ext")
                if (!pageFile.exists()) {
                    downloadImage(url, pageFile)
                }
            }
            val zipFile = File(targetDir, "${galleryTitle.replace("/", "_")}.zip")
            zipFile.parentFile?.mkdirs()
            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                tempDir.listFiles()?.sortedBy { it.nameWithoutExtension.toIntOrNull() }?.forEach { file ->
                    zos.putNextEntry(ZipEntry(file.name))
                    file.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            }
            tempDir.deleteRecursively()
            zipFile
        } catch (e: Exception) {
            tempDir.deleteRecursively()
            throw e
        }
    }

    suspend fun cacheForOffline(
        pages: List<Pair<Int, String>>,
        galleryId: Long,
        imageBaseUrl: (String) -> String
    ): Int = withContext(Dispatchers.IO) {
        val cacheDir = File(appContext.cacheDir, "offline/$galleryId")
        cacheDir.mkdirs()
        var count = 0
        for ((num, path) in pages) {
            val url = imageBaseUrl(path)
            val ext = if (url.contains("jpg") || url.contains("jpeg")) ".jpg" else ".png"
            val pageFile = File(cacheDir, "$num$ext")
            if (!pageFile.exists()) {
                try {
                    downloadImage(url, pageFile)
                    count++
                } catch (_: Exception) { }
            }
        }
        count
    }

    suspend fun getOfflinePages(galleryId: Long): List<File> = withContext(Dispatchers.IO) {
        val dir = File(appContext.cacheDir, "offline/$galleryId")
        if (dir.exists()) {
            dir.listFiles()?.sortedBy { it.nameWithoutExtension.toIntOrNull() }?.toList() ?: emptyList()
        } else emptyList()
    }

    companion object {
        val defaultDownloadPath: String
            get() {
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                return File(dir, "MyNHentai").absolutePath
            }
    }
}
