package com.example.data.api

import com.example.data.model.CommitDto
import com.example.data.model.ContentItemDto
import com.example.data.model.ContributorDto
import com.example.data.model.RateLimitResponseDto
import com.example.data.model.ReadmeDto
import com.example.data.model.ReleaseDto
import com.example.data.model.RepoDto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class SearchRepoResponseDto(
    @Json(name = "total_count") val totalCount: Int = 0,
    @Json(name = "items") val items: List<RepoDto> = emptyList()
)

interface GitHubApiService {

    @GET("repos/{owner}/{repo}")
    suspend fun getRepo(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<RepoDto>

    @GET("repos/{owner}/{repo}/readme")
    suspend fun getReadme(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<ReadmeDto>

    @GET("repos/{owner}/{repo}/languages")
    suspend fun getLanguages(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<Map<String, Long>>

    @GET("repos/{owner}/{repo}/releases")
    suspend fun getReleases(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 30
    ): Response<List<ReleaseDto>>

    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getContents(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path(value = "path", encoded = true) path: String = "",
        @Query("ref") ref: String? = null
    ): Response<List<ContentItemDto>>

    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getSingleFile(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path(value = "path", encoded = true) path: String,
        @Query("ref") ref: String? = null
    ): Response<ContentItemDto>

    @GET("repos/{owner}/{repo}/commits")
    suspend fun getCommits(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 25
    ): Response<List<CommitDto>>

    @GET("repos/{owner}/{repo}/contributors")
    suspend fun getContributors(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 20
    ): Response<List<ContributorDto>>

    @GET("rate_limit")
    suspend fun getRateLimit(): Response<RateLimitResponseDto>

    @GET("search/repositories")
    suspend fun searchRepositories(
        @Query("q") query: String,
        @Query("per_page") perPage: Int = 15,
        @Query("sort") sort: String = "stars"
    ): Response<SearchRepoResponseDto>
}
