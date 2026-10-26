package com.example.data.repository

import android.util.Base64
import com.example.data.api.ApiClient
import com.example.data.api.GitHubApiService
import com.example.data.local.BookmarkDao
import com.example.data.local.BookmarkEntity
import com.example.data.local.RecentRepoDao
import com.example.data.local.RecentRepoEntity
import com.example.data.model.CommitDto
import com.example.data.model.ContentItemDto
import com.example.data.model.ContributorDto
import com.example.data.model.RateLimitResponseDto
import com.example.data.model.ReleaseDto
import com.example.data.model.RepoDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.IOException

class GitHubRepository(
    private val api: GitHubApiService = ApiClient.service,
    private val recentRepoDao: RecentRepoDao,
    private val bookmarkDao: BookmarkDao
) {
    val recentRepos: Flow<List<RecentRepoEntity>> = recentRepoDao.getRecentRepos()
    val bookmarks: Flow<List<BookmarkEntity>> = bookmarkDao.getAllBookmarks()

    fun isBookmarked(fullName: String): Flow<Boolean> = bookmarkDao.isBookmarked(fullName)

    suspend fun toggleBookmark(repo: RepoDto) {
        withContext(Dispatchers.IO) {
            val entity = BookmarkEntity(
                fullName = repo.fullName,
                owner = repo.owner.login,
                repoName = repo.name,
                description = repo.description,
                stars = repo.stargazersCount,
                language = repo.language,
                avatarUrl = repo.owner.avatarUrl
            )
            // If already bookmarked, delete; else insert
            // We check first
            // Note: caller or DAO can toggle
            try {
                bookmarkDao.insertBookmark(entity)
            } catch (e: Exception) {
                bookmarkDao.deleteBookmark(repo.fullName)
            }
        }
    }

    suspend fun removeBookmark(fullName: String) {
        withContext(Dispatchers.IO) {
            bookmarkDao.deleteBookmark(fullName)
        }
    }

    suspend fun addBookmark(repo: RepoDto) {
        withContext(Dispatchers.IO) {
            bookmarkDao.insertBookmark(
                BookmarkEntity(
                    fullName = repo.fullName,
                    owner = repo.owner.login,
                    repoName = repo.name,
                    description = repo.description,
                    stars = repo.stargazersCount,
                    language = repo.language,
                    avatarUrl = repo.owner.avatarUrl
                )
            )
        }
    }

    suspend fun getRepo(owner: String, repo: String): Result<RepoDto> = withContext(Dispatchers.IO) {
        try {
            val response = api.getRepo(owner, repo)
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                // Save to recent repos
                recentRepoDao.insertRecent(
                    RecentRepoEntity(
                        fullName = data.fullName,
                        owner = data.owner.login,
                        repoName = data.name,
                        description = data.description,
                        stars = data.stargazersCount,
                        language = data.language,
                        avatarUrl = data.owner.avatarUrl
                    )
                )
                Result.success(data)
            } else {
                Result.failure(mapHttpError(response))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error: Please check your internet connection."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getReadme(owner: String, repo: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = api.getReadme(owner, repo)
            if (response.isSuccessful && response.body() != null) {
                val dto = response.body()!!
                val decodedText = if (dto.encoding.equals("base64", ignoreCase = true) && !dto.content.isNullOrBlank()) {
                    val cleanBase64 = dto.content.replace("\n", "").replace("\r", "").trim()
                    val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                    String(decodedBytes, Charsets.UTF_8)
                } else {
                    dto.content ?: ""
                }
                Result.success(decodedText)
            } else if (response.code() == 404) {
                Result.success("") // Empty readme, not an error
            } else {
                Result.failure(mapHttpError(response))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLanguages(owner: String, repo: String): Result<Map<String, Long>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getLanguages(owner, repo)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(mapHttpError(response))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getReleases(owner: String, repo: String): Result<List<ReleaseDto>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getReleases(owner, repo)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(mapHttpError(response))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getContents(owner: String, repo: String, path: String = "", ref: String? = null): Result<List<ContentItemDto>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getContents(owner, repo, path, ref)
            if (response.isSuccessful && response.body() != null) {
                // Sort directories first, then alphabetical
                val sorted = response.body()!!.sortedWith(
                    compareBy<ContentItemDto> { it.type != "dir" }.thenBy { it.name.lowercase() }
                )
                Result.success(sorted)
            } else {
                Result.failure(mapHttpError(response))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFileContent(owner: String, repo: String, path: String, ref: String? = null): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = api.getSingleFile(owner, repo, path, ref)
            if (response.isSuccessful && response.body() != null) {
                val fileDto = response.body()!!
                val decoded = if (fileDto.encoding.equals("base64", ignoreCase = true) && !fileDto.content.isNullOrBlank()) {
                    val cleanBase64 = fileDto.content.replace("\n", "").replace("\r", "").trim()
                    val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                    String(decodedBytes, Charsets.UTF_8)
                } else {
                    fileDto.content ?: ""
                }
                Result.success(decoded)
            } else {
                Result.failure(mapHttpError(response))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCommits(owner: String, repo: String): Result<List<CommitDto>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getCommits(owner, repo)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(mapHttpError(response))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getContributors(owner: String, repo: String): Result<List<ContributorDto>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getContributors(owner, repo)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(mapHttpError(response))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRateLimit(): Result<RateLimitResponseDto> = withContext(Dispatchers.IO) {
        try {
            val response = api.getRateLimit()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(mapHttpError(response))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchRepositories(query: String): Result<List<RepoDto>> = withContext(Dispatchers.IO) {
        try {
            val response = api.searchRepositories(query)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.items)
            } else {
                Result.failure(mapHttpError(response))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteRecentRepo(fullName: String) = withContext(Dispatchers.IO) {
        recentRepoDao.deleteRecent(fullName)
    }

    suspend fun clearAllRecent() = withContext(Dispatchers.IO) {
        recentRepoDao.clearAll()
    }

    private fun <T> mapHttpError(response: Response<T>): Exception {
        return when (response.code()) {
            404 -> Exception("Repository not found (404). Please verify the owner and repo name.")
            403 -> {
                val reset = response.headers()["x-ratelimit-reset"]?.toLongOrNull()
                val resetMsg = if (reset != null) {
                    val date = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                        .format(java.util.Date(reset * 1000))
                    " Rate limit will reset at $date."
                } else ""
                Exception("GitHub API rate limit exceeded (403).$resetMsg You can add an optional Personal Access Token in Settings to raise your limit to 5,000/hr.")
            }
            401 -> Exception("GitHub authentication failed (401). Please check your token in Settings.")
            else -> Exception("GitHub API error (${response.code()}): ${response.message()}")
        }
    }
}
