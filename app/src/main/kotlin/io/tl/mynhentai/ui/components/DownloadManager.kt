package io.tl.mynhentai.ui.components

/**
 * Downloads are written through MediaStore into the public Download collection, so there is no
 * file-system path to hand out anymore. This is the human readable location shown in the UI.
 */
object DownloadManager {
    const val defaultDownloadPath: String = "Download/MyNHentai"
}
