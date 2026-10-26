package com.mapherdf.maplenilset.bili.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.util.concurrent.TimeUnit

class BiliHttpClientFactory private constructor(
    private val cookieStore: BiliCookieStore,
) {

    fun create(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .cookieJar(cookieStore)
            .addInterceptor(BiliHeaderInterceptor())
            .build()

    private class BiliHeaderInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request().newBuilder()
                .header("User-Agent", BiliHttpHeaders.USER_AGENT)
                .header("Referer", BiliHttpHeaders.REFERER)
                .header("Origin", BiliHttpHeaders.ORIGIN)
                .build()
            return chain.proceed(request)
        }
    }

    companion object {
        private const val CONNECT_TIMEOUT_SECONDS = 10L
        private const val READ_TIMEOUT_SECONDS = 30L

        fun create(cookieStore: BiliCookieStore): BiliHttpClientFactory =
            BiliHttpClientFactory(cookieStore)
    }
}
