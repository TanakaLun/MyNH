package io.tl.mynhentai.data.api

class CdnRepository(private val mangaService: MangaService) {

    private var thumbServers: List<String> = emptyList()
    private var imageServers: List<String> = emptyList()

    suspend fun refresh() {
        val all = mangaService.getCdnServers().servers
        thumbServers = all.filter { it.startsWith("t") }
        imageServers = all.filter { it.startsWith("i") }
    }

    fun resolveThumbnailUrl(path: String): String {
        if (thumbServers.isEmpty()) return "https://t3.nhentai.net/$path"
        return "https://${thumbServers.first()}/$path"
    }

    fun resolveImageUrl(path: String): String {
        if (imageServers.isEmpty()) return "https://i.nhentai.net/$path"
        return "https://${imageServers.first()}/$path"
    }
}
