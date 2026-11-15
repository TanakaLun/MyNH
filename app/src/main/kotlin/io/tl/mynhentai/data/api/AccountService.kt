package io.tl.mynhentai.data.api

import io.tl.mynhentai.data.model.ApiFavoriteResponse
import io.tl.mynhentai.data.model.ApiResponse
import io.tl.mynhentai.data.model.ApiUser
import io.tl.mynhentai.data.model.BlacklistApiResponse
import io.tl.mynhentai.data.model.BlacklistListResponse
import io.tl.mynhentai.data.model.BlacklistUpdateRequest
import io.tl.mynhentai.data.model.DownloadResponse
import io.tl.mynhentai.data.model.MangaSummary
import io.tl.mynhentai.data.model.RandomFavoriteResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Account related endpoints of the nhentai v2 API.
 *
 * Everything here authenticates with an API key (`Authorization: Key ...`), which is added
 * by [AuthInterceptor]. Password based endpoints are intentionally not exposed: they require
 * a Cloudflare Turnstile solution and are marked first-party only by the API docs.
 */
interface AccountService {

    @GET("api/v2/user")
    suspend fun getMe(): ApiUser

    @GET("api/v2/favorites")
    suspend fun getFavorites(@Query("page") page: Int = 1): ApiResponse<MangaSummary>

    @GET("api/v2/favorites/random")
    suspend fun getRandomFavorite(): RandomFavoriteResponse

    @GET("api/v2/galleries/{id}/favorite")
    suspend fun getFavorite(@Path("id") id: Long): ApiFavoriteResponse

    @POST("api/v2/galleries/{id}/favorite")
    suspend fun addFavorite(@Path("id") id: Long): ApiFavoriteResponse

    @DELETE("api/v2/galleries/{id}/favorite")
    suspend fun removeFavorite(@Path("id") id: Long): ApiFavoriteResponse

    @GET("api/v2/blacklist")
    suspend fun getBlacklist(): BlacklistListResponse

    @POST("api/v2/blacklist")
    suspend fun updateBlacklist(@Body body: BlacklistUpdateRequest): BlacklistApiResponse

    @POST("api/v2/galleries/{id}/download")
    suspend fun getDownloadUrl(
        @Path("id") id: Long,
        @Query("format") format: String,
    ): DownloadResponse
}
