package io.tl.mynhentai.ui.reader.turn

/**
 * Page transition for the horizontal reader.
 *
 * The order is part of the persisted setting (stored by name, so reordering is safe, but keep
 * [Scroll] first for readability). [Scroll] means the original vertical continuous reader.
 */
enum class PageAnimation {
    Scroll,
    Slide,
    Cover,
    Parallax,
    Fade,
    None,
    Simulation,
    GoogleCurl,
    IosCurl,
    ;

    val isPaged: Boolean get() = this != Scroll

    companion object {
        fun fromName(value: String?): PageAnimation =
            entries.firstOrNull { it.name == value } ?: Scroll
    }
}
