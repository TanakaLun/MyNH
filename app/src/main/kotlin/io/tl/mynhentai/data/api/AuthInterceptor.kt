package io.tl.mynhentai.data.api

import io.tl.mynhentai.data.local.AccountStore
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Adds `Authorization: Key <api_key>` to nhentai API calls once an account is bound.
 *
 * The header is skipped after the server rejected the key (so public browsing keeps working
 * anonymously) and it is never sent to the image/thumbnail CDN hosts.
 */
class AuthInterceptor(private val accountStore: AccountStore) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val host = request.url.host
        val key = if (host == API_HOST || host == "www.$API_HOST") {
            accountStore.apiKeyForRequests()
        } else {
            null
        }
        val newRequest = if (key.isNullOrBlank()) {
            request
        } else {
            request.newBuilder()
                .header("Authorization", "Key $key")
                .build()
        }
        return chain.proceed(newRequest)
    }

    private companion object {
        const val API_HOST = "nhentai.net"
    }
}
