package io.tl.mynhentai.data.api

class CdnRepository(private val mangaService: MangaService) {

    private var cdnServers: List<String> = emptyList()

    suspend fun refresh() {
        cdnServers = mangaService.getCdnServers().servers
    }

    fun resolveUrl(path: String): String {
        if (cdnServers.isEmpty()) return "https://i.nhentai.net/$path"
        return "https://${cdnServers.first()}/$path"
    }
}
