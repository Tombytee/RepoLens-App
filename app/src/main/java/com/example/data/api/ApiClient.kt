package com.example.data.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

data class RateLimitState(
    val limit: Int = 60,
    val remaining: Int = 60,
    val resetEpochSeconds: Long = 0L,
    val hasCustomToken: Boolean = false
)

object ApiClient {
    private const val BASE_URL = "https://api.github.com/"

    @Volatile
    private var customToken: String? = null

    private val _rateLimitState = MutableStateFlow(RateLimitState())
    val rateLimitState = _rateLimitState.asStateFlow()

    fun setCustomToken(token: String?) {
        customToken = token?.takeIf { it.isNotBlank() }
        _rateLimitState.value = _rateLimitState.value.copy(
            hasCustomToken = !customToken.isNullOrBlank()
        )
    }

    fun getCustomToken(): String? = customToken

    private val authAndRateLimitInterceptor = Interceptor { chain ->
        val original = chain.request()
        val requestBuilder = original.newBuilder()
            .header("Accept", "application/vnd.github.v3+json")
            .header("User-Agent", "RepoLens-Android-App")

        customToken?.let { token ->
            requestBuilder.header("Authorization", "Bearer $token")
        }

        val response: Response = chain.proceed(requestBuilder.build())

        // Extract rate-limit headers if present
        val limitHeader = response.header("x-ratelimit-limit")?.toIntOrNull()
        val remainingHeader = response.header("x-ratelimit-remaining")?.toIntOrNull()
        val resetHeader = response.header("x-ratelimit-reset")?.toLongOrNull()

        if (limitHeader != null && remainingHeader != null) {
            _rateLimitState.value = RateLimitState(
                limit = limitHeader,
                remaining = remainingHeader,
                resetEpochSeconds = resetHeader ?: 0L,
                hasCustomToken = !customToken.isNullOrBlank()
            )
        }

        response
    }

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authAndRateLimitInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    val service: GitHubApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GitHubApiService::class.java)
    }
}
