package io.tl.mynhentai.ui.navigation

import kotlinx.serialization.Serializable
import top.yukonga.miuix.kmp.nav.core.NavKey

/**
 * Typed, serializable navigation keys backed by miuix-nav. The four main tabs share the single
 * [Main] key (tab switching is handled by the AndroidX [androidx.navigation.NavHost] nested inside
 * the root entry); everything pushed on top runs on the miuix-nav back stack with predictive back.
 */
@Serializable
sealed interface AppRoute : NavKey {
    @Serializable
    data object Main : AppRoute

    @Serializable
    data class Search(val query: String = "") : AppRoute

    @Serializable
    data class SearchResults(val query: String) : AppRoute

    @Serializable
    data class Detail(val id: Long) : AppRoute

    @Serializable
    data class Reader(val id: Long) : AppRoute
}