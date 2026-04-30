package io.tl.mynhentai.data.api

import io.tl.mynhentai.data.model.ApiResponse
import io.tl.mynhentai.data.model.CdnResponse
import io.tl.mynhentai.data.model.MangaDetail
import io.tl.mynhentai.data.model.MangaSummary
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface MangaService {

    @GET("api/v2/galleries/popular")
    suspend fun getPopular(@Query("page") page: Int = 1): ApiResponse<MangaSummary>

    @GET("api/v2/galleries")
    suspend fun getGalleries(
        @Query("page") page: Int = 1,
        @Query("sort") sort: String = "date"
    ): ApiResponse<MangaSummary>

    @GET("api/v2/galleries/{id}")
    suspend fun getGalleryDetail(@Path("id") id: Long): MangaDetail

    @GET("api/v2/galleries/random")
    suspend fun getRandom(): MangaSummary

    @GET("api/v2/search")
    suspend fun search(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("sort") sort: String = "date"
    ): ApiResponse<MangaSummary>

    @GET("api/v2/cdn")
    suspend fun getCdnServers(): CdnResponse
}
