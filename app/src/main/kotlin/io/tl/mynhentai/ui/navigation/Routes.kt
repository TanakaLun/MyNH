package io.tl.mynhentai.ui.navigation

object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val DETAIL = "gallery/{id}"
    const val READER = "reader/{id}"
    const val LIBRARY = "favorites"
    const val SETTINGS = "settings"

    fun detail(id: Long) = "gallery/$id"
    fun reader(id: Long) = "reader/$id"
}
