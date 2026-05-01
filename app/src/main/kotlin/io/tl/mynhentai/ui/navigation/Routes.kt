package io.tl.mynhentai.ui.navigation

import java.net.URLEncoder

object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val SEARCH_QUERY = "search?query={query}"
    const val DETAIL = "gallery/{id}"
    const val READER = "reader/{id}"
    const val LIBRARY = "favorites"
    const val SETTINGS = "settings"

    fun detail(id: Long) = "gallery/$id"
    fun reader(id: Long) = "reader/$id"
    fun search(query: String) = "search?query=${URLEncoder.encode(query, "UTF-8")}"
}
