package io.tl.mynhentai.data.api

class CdnRepository(private val mangaService: MangaService) {

    private var thumbServers: List<String> = emptyList()
    private var imageServers: List<String> = emptyList()

    suspend fun refresh() {
        val response = mangaService.getCdnServers()
        thumbServers = response.thumbServers
        imageServers = response.imageServers
    }

    fun resolveThumbnailUrl(path: String): String {
        if (thumbServers.isEmpty()) return "https://t3.nhentai.net/$path"
        return "${thumbServers[pickServer(path, thumbServers.size)]}/$path"
    }

    fun resolveImageUrl(path: String): String {
        if (imageServers.isEmpty()) return "https://i.nhentai.net/$path"
        return "${imageServers[pickServer(path, imageServers.size)]}/$path"
    }

    private fun pickServer(path: String, serverCount: Int): Int {
        val hash = path.hashCode() and Int.MAX_VALUE
        return hash % serverCount
    }
}
