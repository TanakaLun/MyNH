package io.tl.mynhentai.ui.components

import android.os.Environment
import java.io.File

object DownloadManager {
    val defaultDownloadPath: String
        get() {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            return File(dir, "MyNHentai").absolutePath
        }
}