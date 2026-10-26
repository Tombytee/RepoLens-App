package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_repos")
data class RecentRepoEntity(
    @PrimaryKey val fullName: String,
    val owner: String,
    val repoName: String,
    val description: String?,
    val stars: Int,
    val language: String?,
    val avatarUrl: String?,
    val visitedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val fullName: String,
    val owner: String,
    val repoName: String,
    val description: String?,
    val stars: Int,
    val language: String?,
    val avatarUrl: String?,
    val savedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "download_records")
data class DownloadRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val repoFullName: String,
    val versionTag: String,
    val fileName: String,
    val fileSize: Long,
    val filePath: String,
    val isApk: Boolean,
    val downloadUrl: String,
    val completedAt: Long = System.currentTimeMillis()
)
