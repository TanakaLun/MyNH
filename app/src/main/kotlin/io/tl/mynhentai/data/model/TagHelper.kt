package io.tl.mynhentai.data.model

object TagHelper {
    private val languageMap = mapOf(
        12227L to "English",
        6346L to "日本語",
        29963L to "中文"
    )

    fun getLanguage(tagIds: List<Int>): String? {
        return tagIds.firstNotNullOfOrNull { languageMap[it.toLong()] }
    }

    fun isLanguageTag(tagId: Long): Boolean = tagId in languageMap
}
