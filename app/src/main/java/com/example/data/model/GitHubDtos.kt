package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RepoOwnerDto(
    @Json(name = "login") val login: String,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    @Json(name = "html_url") val htmlUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class LicenseDto(
    @Json(name = "key") val key: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "spdx_id") val spdxId: String? = null
)

@JsonClass(generateAdapter = true)
data class RepoDto(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "full_name") val fullName: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "owner") val owner: RepoOwnerDto,
    @Json(name = "html_url") val htmlUrl: String,
    @Json(name = "stargazers_count") val stargazersCount: Int = 0,
    @Json(name = "forks_count") val forksCount: Int = 0,
    @Json(name = "open_issues_count") val openIssuesCount: Int = 0,
    @Json(name = "subscribers_count") val subscribersCount: Int? = null,
    @Json(name = "language") val language: String? = null,
    @Json(name = "default_branch") val defaultBranch: String = "main",
    @Json(name = "license") val license: LicenseDto? = null,
    @Json(name = "topics") val topics: List<String>? = emptyList(),
    @Json(name = "updated_at") val updatedAt: String? = null,
    @Json(name = "pushed_at") val pushedAt: String? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "archived") val archived: Boolean = false,
    @Json(name = "size") val sizeKb: Long = 0
)

@JsonClass(generateAdapter = true)
data class ReadmeDto(
    @Json(name = "name") val name: String,
    @Json(name = "path") val path: String,
    @Json(name = "sha") val sha: String,
    @Json(name = "size") val size: Long = 0,
    @Json(name = "content") val content: String? = null,
    @Json(name = "encoding") val encoding: String? = null,
    @Json(name = "download_url") val downloadUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class ReleaseAssetDto(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "size") val size: Long = 0,
    @Json(name = "download_count") val downloadCount: Int = 0,
    @Json(name = "browser_download_url") val browserDownloadUrl: String,
    @Json(name = "content_type") val contentType: String? = null,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class ReleaseDto(
    @Json(name = "id") val id: Long,
    @Json(name = "tag_name") val tagName: String,
    @Json(name = "name") val name: String? = null,
    @Json(name = "body") val body: String? = null,
    @Json(name = "published_at") val publishedAt: String? = null,
    @Json(name = "prerelease") val prerelease: Boolean = false,
    @Json(name = "draft") val draft: Boolean = false,
    @Json(name = "html_url") val htmlUrl: String? = null,
    @Json(name = "assets") val assets: List<ReleaseAssetDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ContentItemDto(
    @Json(name = "name") val name: String,
    @Json(name = "path") val path: String,
    @Json(name = "sha") val sha: String,
    @Json(name = "size") val size: Long = 0,
    @Json(name = "type") val type: String, // "file" or "dir"
    @Json(name = "download_url") val downloadUrl: String? = null,
    @Json(name = "content") val content: String? = null,
    @Json(name = "encoding") val encoding: String? = null
)

@JsonClass(generateAdapter = true)
data class CommitAuthorDto(
    @Json(name = "name") val name: String? = null,
    @Json(name = "date") val date: String? = null
)

@JsonClass(generateAdapter = true)
data class CommitDetailDto(
    @Json(name = "message") val message: String,
    @Json(name = "author") val author: CommitAuthorDto? = null
)

@JsonClass(generateAdapter = true)
data class CommitDto(
    @Json(name = "sha") val sha: String,
    @Json(name = "commit") val commit: CommitDetailDto,
    @Json(name = "author") val author: RepoOwnerDto? = null,
    @Json(name = "html_url") val htmlUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class ContributorDto(
    @Json(name = "login") val login: String,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    @Json(name = "contributions") val contributions: Int = 0,
    @Json(name = "html_url") val htmlUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class RateLimitCoreDto(
    @Json(name = "limit") val limit: Int = 60,
    @Json(name = "remaining") val remaining: Int = 60,
    @Json(name = "reset") val reset: Long = 0,
    @Json(name = "used") val used: Int = 0
)

@JsonClass(generateAdapter = true)
data class RateLimitResourcesDto(
    @Json(name = "core") val core: RateLimitCoreDto
)

@JsonClass(generateAdapter = true)
data class RateLimitResponseDto(
    @Json(name = "resources") val resources: RateLimitResourcesDto
)
