package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentRepoDao {
    @Query("SELECT * FROM recent_repos ORDER BY visitedAt DESC LIMIT 20")
    fun getRecentRepos(): Flow<List<RecentRepoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecent(repo: RecentRepoEntity)

    @Query("DELETE FROM recent_repos WHERE fullName = :fullName")
    suspend fun deleteRecent(fullName: String)

    @Query("DELETE FROM recent_repos")
    suspend fun clearAll()
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY savedAt DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE fullName = :fullName)")
    fun isBookmarked(fullName: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(repo: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE fullName = :fullName")
    suspend fun deleteBookmark(fullName: String)
}

@Dao
interface DownloadRecordDao {
    @Query("SELECT * FROM download_records ORDER BY completedAt DESC")
    fun getAllDownloads(): Flow<List<DownloadRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(record: DownloadRecordEntity): Long

    @Query("DELETE FROM download_records WHERE id = :id")
    suspend fun deleteDownloadById(id: Long)

    @Query("DELETE FROM download_records WHERE filePath = :filePath")
    suspend fun deleteDownloadByPath(filePath: String)

    @Query("DELETE FROM download_records")
    suspend fun clearAll()
}
