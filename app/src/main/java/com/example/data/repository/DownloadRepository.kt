package com.example.data.repository

import android.content.Context
import android.os.Environment
import com.example.data.api.ApiClient
import com.example.data.local.DownloadRecordDao
import com.example.data.local.DownloadRecordEntity
import com.example.util.FileFormatters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

enum class DownloadStatus {
    DOWNLOADING,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class ActiveDownload(
    val id: String, // unique key, e.g. "repo-version-filename"
    val fileName: String,
    val repoFullName: String,
    val versionTag: String,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val progressPercent: Int = 0,
    val isApk: Boolean = false,
    val status: DownloadStatus = DownloadStatus.DOWNLOADING,
    val errorMessage: String? = null,
    val localFilePath: String? = null
)

class DownloadRepository(
    private val context: Context,
    private val downloadRecordDao: DownloadRecordDao
) {
    private val repositoryScope = CoroutineScope(Dispatchers.IO + Job())
    private val activeJobs = mutableMapOf<String, Job>()

    private val _activeDownloads = MutableStateFlow<Map<String, ActiveDownload>>(emptyMap())
    val activeDownloads = _activeDownloads.asStateFlow()

    val completedDownloads: Flow<List<DownloadRecordEntity>> = downloadRecordDao.getAllDownloads()

    fun startDownload(
        downloadUrl: String,
        fileName: String,
        repoFullName: String,
        versionTag: String,
        expectedSize: Long = 0L
    ): String {
        val downloadId = "${repoFullName}_${versionTag}_${fileName}".replace("/", "_")
        val isApk = FileFormatters.isApk(fileName)

        // If already downloading, ignore
        if (activeJobs[downloadId]?.isActive == true) {
            return downloadId
        }

        val initialDownload = ActiveDownload(
            id = downloadId,
            fileName = fileName,
            repoFullName = repoFullName,
            versionTag = versionTag,
            downloadedBytes = 0L,
            totalBytes = expectedSize,
            progressPercent = 0,
            isApk = isApk,
            status = DownloadStatus.DOWNLOADING
        )

        updateActiveDownload(initialDownload)

        val job = repositoryScope.launch {
            downloadFile(downloadId, downloadUrl, fileName, repoFullName, versionTag, expectedSize, isApk)
        }
        activeJobs[downloadId] = job

        return downloadId
    }

    private suspend fun downloadFile(
        id: String,
        url: String,
        fileName: String,
        repoFullName: String,
        versionTag: String,
        expectedSize: Long,
        isApk: Boolean
    ) {
        val targetDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: context.filesDir
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        val destinationFile = File(targetDir, fileName)
        var downloadedBytes = 0L

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "RepoLens-Android-Downloader")
                .build()

            val response = ApiClient.okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                throw IOException("Server returned HTTP ${response.code}: ${response.message}")
            }

            val body = response.body ?: throw IOException("Empty response body from download source")
            val totalBytes = if (body.contentLength() > 0) body.contentLength() else expectedSize

            val inputStream = body.byteStream()
            val outputStream = FileOutputStream(destinationFile)

            val buffer = ByteArray(16 * 1024)
            var bytesRead: Int
            var lastEmittedTime = System.currentTimeMillis()

            try {
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead

                    val now = System.currentTimeMillis()
                    if (now - lastEmittedTime > 250 || downloadedBytes == totalBytes) {
                        lastEmittedTime = now
                        val percent = if (totalBytes > 0) {
                            ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
                        } else 0

                        updateActiveDownload(
                            ActiveDownload(
                                id = id,
                                fileName = fileName,
                                repoFullName = repoFullName,
                                versionTag = versionTag,
                                downloadedBytes = downloadedBytes,
                                totalBytes = totalBytes,
                                progressPercent = percent,
                                isApk = isApk,
                                status = DownloadStatus.DOWNLOADING,
                                localFilePath = destinationFile.absolutePath
                            )
                        )
                    }
                }
                outputStream.flush()
            } finally {
                outputStream.close()
                inputStream.close()
            }

            // Successfully downloaded
            updateActiveDownload(
                ActiveDownload(
                    id = id,
                    fileName = fileName,
                    repoFullName = repoFullName,
                    versionTag = versionTag,
                    downloadedBytes = downloadedBytes,
                    totalBytes = downloadedBytes,
                    progressPercent = 100,
                    isApk = isApk,
                    status = DownloadStatus.COMPLETED,
                    localFilePath = destinationFile.absolutePath
                )
            )

            // Save completed record to Room database
            downloadRecordDao.insertDownload(
                DownloadRecordEntity(
                    repoFullName = repoFullName,
                    versionTag = versionTag,
                    fileName = fileName,
                    fileSize = downloadedBytes,
                    filePath = destinationFile.absolutePath,
                    isApk = isApk,
                    downloadUrl = url,
                    completedAt = System.currentTimeMillis()
                )
            )

        } catch (e: CancellationException) {
            if (destinationFile.exists()) destinationFile.delete()
            updateActiveDownload(
                ActiveDownload(
                    id = id,
                    fileName = fileName,
                    repoFullName = repoFullName,
                    versionTag = versionTag,
                    downloadedBytes = downloadedBytes,
                    totalBytes = expectedSize,
                    progressPercent = 0,
                    isApk = isApk,
                    status = DownloadStatus.CANCELLED
                )
            )
        } catch (e: Exception) {
            if (destinationFile.exists()) destinationFile.delete()
            updateActiveDownload(
                ActiveDownload(
                    id = id,
                    fileName = fileName,
                    repoFullName = repoFullName,
                    versionTag = versionTag,
                    downloadedBytes = downloadedBytes,
                    totalBytes = expectedSize,
                    progressPercent = 0,
                    isApk = isApk,
                    status = DownloadStatus.FAILED,
                    errorMessage = e.localizedMessage ?: "Download failed"
                )
            )
        } finally {
            activeJobs.remove(id)
        }
    }

    fun cancelDownload(downloadId: String) {
        activeJobs[downloadId]?.cancel()
        activeJobs.remove(downloadId)
    }

    fun dismissActiveDownload(downloadId: String) {
        val current = _activeDownloads.value.toMutableMap()
        current.remove(downloadId)
        _activeDownloads.value = current
    }

    suspend fun deleteDownload(record: DownloadRecordEntity) = withContext(Dispatchers.IO) {
        try {
            val file = File(record.filePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {
        }
        downloadRecordDao.deleteDownloadById(record.id)
    }

    private fun updateActiveDownload(download: ActiveDownload) {
        val current = _activeDownloads.value.toMutableMap()
        current[download.id] = download
        _activeDownloads.value = current
    }
}
