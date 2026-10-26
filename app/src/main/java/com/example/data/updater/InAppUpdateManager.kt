package com.example.data.updater

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Environment
import com.example.data.model.UpdateConfig
import com.example.util.ApkInstaller
import com.example.util.InstallResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data class UpdateAvailable(
        val config: UpdateConfig,
        val isMandatory: Boolean
    ) : UpdateState
    data class Downloading(
        val config: UpdateConfig,
        val progressBytes: Long,
        val totalBytes: Long,
        val percent: Int,
        val isMandatory: Boolean
    ) : UpdateState
    data class ReadyToInstall(
        val config: UpdateConfig,
        val apkFile: File,
        val isMandatory: Boolean
    ) : UpdateState
    data class Error(
        val message: String,
        val config: UpdateConfig? = null
    ) : UpdateState
}

class InAppUpdateManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    companion object {
        const val UPDATE_JSON_URL =
            "https://raw.githubusercontent.com/Tombytee/RepoLens/refs/heads/main/update.json"

        private const val PREFS_NAME = "repolens_update_prefs"
        private const val KEY_LAST_CHECK_TIME = "last_check_timestamp"
        private const val KEY_CONFIG_JSON = "saved_config_json"
        private const val KEY_APK_PATH = "saved_apk_path"

        private const val CHECK_DEBOUNCE_MS = 15 * 60 * 1000L // 15 minutes
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private var downloadJob: Job? = null

    init {
        cleanStaleInvalidDownloads()
        restoreState()
    }

    /**
     * Cleans up any leftover corrupted, zero-byte, or HTML files in the updates folder.
     */
    private fun cleanStaleInvalidDownloads() {
        try {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: return
            dir.listFiles()?.forEach { file ->
                if (file.name.startsWith("RepoLens-") && (file.name.endsWith(".apk") || file.name.endsWith(".tmp"))) {
                    if (!ApkVerifier.isValidZipArchive(file)) {
                        file.delete()
                    }
                }
            }
        } catch (_: Exception) {}
    }

    /**
     * Gets currently installed app's versionCode.
     */
    fun getCurrentVersionCode(): Int {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }
        } catch (_: Exception) {
            1
        }
    }

    /**
     * Gets currently installed app's versionName.
     */
    fun getCurrentVersionName(): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
        } catch (_: Exception) {
            "1.0"
        }
    }

    /**
     * Checks remote server for available updates.
     * @param force If true, bypasses the 15-minute debounce interval.
     */
    fun checkForUpdates(force: Boolean = false) {
        val now = System.currentTimeMillis()
        val lastCheck = prefs.getLong(KEY_LAST_CHECK_TIME, 0L)

        // If currently downloading or already ready to install, keep current state
        val current = _updateState.value
        if (current is UpdateState.Downloading || current is UpdateState.ReadyToInstall) {
            return
        }

        if (!force && (now - lastCheck < CHECK_DEBOUNCE_MS) && current is UpdateState.UpdateAvailable) {
            // Already aware of update from recent check
            return
        }

        scope.launch {
            _updateState.value = UpdateState.Checking
            val result = fetchRemoteUpdateConfig()

            if (result != null) {
                prefs.edit().putLong(KEY_LAST_CHECK_TIME, System.currentTimeMillis()).apply()
                val currentVersion = getCurrentVersionCode()

                if (result.isUpdateAvailable(currentVersion)) {
                    val isMandatory = result.isUpdateMandatory(currentVersion)
                    prefs.edit().putString(KEY_CONFIG_JSON, serializeConfig(result)).apply()

                    // Check if we already have this exact APK downloaded and verified
                    val savedApkPath = prefs.getString(KEY_APK_PATH, null)
                    if (savedApkPath != null) {
                        val existingFile = File(savedApkPath)
                        if (existingFile.exists()) {
                            if (!ApkVerifier.isValidZipArchive(existingFile)) {
                                existingFile.delete()
                                prefs.edit().remove(KEY_APK_PATH).apply()
                            } else {
                                val verification = ApkVerifier.verifyApk(context, existingFile, result)
                                if (verification is ApkVerificationResult.Valid) {
                                    _updateState.value = UpdateState.ReadyToInstall(
                                        config = result,
                                        apkFile = existingFile,
                                        isMandatory = isMandatory
                                    )
                                    return@launch
                                } else {
                                    existingFile.delete()
                                    prefs.edit().remove(KEY_APK_PATH).apply()
                                }
                            }
                        }
                    }

                    _updateState.value = UpdateState.UpdateAvailable(
                        config = result,
                        isMandatory = isMandatory
                    )
                } else {
                    _updateState.value = UpdateState.Idle
                }
            } else {
                if (force) {
                    _updateState.value = UpdateState.Error("Unable to check for updates. Please try again.")
                } else {
                    _updateState.value = UpdateState.Idle
                }
            }
        }
    }

    /**
     * Initiates downloading the APK update via a reliable background coroutine using OkHttp.
     * Accurately tracks progress, handles redirects, validates content type, and verifies APK integrity.
     */
    fun startDownload(config: UpdateConfig) {
        val currentVersion = getCurrentVersionCode()
        val isMandatory = config.isUpdateMandatory(currentVersion)

        // Cancel any active download
        downloadJob?.cancel()

        downloadJob = scope.launch(Dispatchers.IO) {
            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            if (downloadsDir == null) {
                _updateState.value = UpdateState.Error(
                    message = "Storage unavailable for update download.",
                    config = config
                )
                return@launch
            }

            val fileName = "RepoLens-v${config.versionName}-${config.versionCode}.apk"
            val targetFile = File(downloadsDir, fileName)
            val tempFile = File(downloadsDir, "$fileName.tmp")

            if (tempFile.exists()) tempFile.delete()
            if (targetFile.exists()) targetFile.delete()

            _updateState.value = UpdateState.Downloading(
                config = config,
                progressBytes = 0L,
                totalBytes = -1L,
                percent = 0,
                isMandatory = isMandatory
            )

            try {
                val request = Request.Builder()
                    .url(config.apkUrl)
                    .header("User-Agent", "RepoLens-Android-App")
                    .header("Accept", "application/vnd.android.package-archive, application/octet-stream, */*")
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        _updateState.value = UpdateState.Error(
                            message = "Download failed (HTTP ${response.code}: ${response.message}).",
                            config = config
                        )
                        return@launch
                    }

                    val contentType = response.header("Content-Type") ?: ""
                    if (contentType.contains("text/html", ignoreCase = true)) {
                        _updateState.value = UpdateState.Error(
                            message = "The update link returned a webpage instead of an APK binary. Please check the update configuration.",
                            config = config
                        )
                        return@launch
                    }

                    val body = response.body ?: throw java.io.IOException("Empty response body")
                    val totalBytes = body.contentLength()

                    var downloadedBytes = 0L
                    var lastUpdatePercent = -1

                    body.byteStream().use { input ->
                        FileOutputStream(tempFile).use { output ->
                            val buffer = ByteArray(8192)
                            var read: Int
                            var isFirstChunk = true

                            while (input.read(buffer).also { read = it } != -1) {
                                if (!isActive) {
                                    tempFile.delete()
                                    return@launch
                                }

                                if (isFirstChunk) {
                                    isFirstChunk = false
                                    // Check if header starts with HTML
                                    val prefix = String(buffer, 0, minOf(read, 32))
                                    if (prefix.contains("<html", ignoreCase = true) || prefix.contains("<!doc", ignoreCase = true)) {
                                        output.close()
                                        tempFile.delete()
                                        _updateState.value = UpdateState.Error(
                                            message = "The update link returned a webpage instead of an APK binary.",
                                            config = config
                                        )
                                        return@launch
                                    }
                                }

                                output.write(buffer, 0, read)
                                downloadedBytes += read

                                val percent = if (totalBytes > 0) {
                                    ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
                                } else {
                                    0
                                }

                                if (percent != lastUpdatePercent || downloadedBytes == totalBytes) {
                                    lastUpdatePercent = percent
                                    _updateState.value = UpdateState.Downloading(
                                        config = config,
                                        progressBytes = downloadedBytes,
                                        totalBytes = totalBytes,
                                        percent = percent,
                                        isMandatory = isMandatory
                                    )
                                }
                            }
                            output.flush()
                        }
                    }

                    if (tempFile.exists() && tempFile.length() > 0L) {
                        tempFile.renameTo(targetFile)
                    }

                    handleDownloadCompleted(config, targetFile, isMandatory)
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    tempFile.delete()
                    return@launch
                }
                tempFile.delete()
                targetFile.delete()
                _updateState.value = UpdateState.Error(
                    message = "Download error: ${e.localizedMessage ?: "Unknown error"}",
                    config = config
                )
            }
        }
    }

    /**
     * Cancels an ongoing download and deletes temporary files.
     */
    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null

        val apkPath = prefs.getString(KEY_APK_PATH, null)
        if (apkPath != null) {
            File(apkPath).delete()
            File("$apkPath.tmp").delete()
        }

        val savedConfig = getSavedConfig()
        prefs.edit()
            .remove(KEY_APK_PATH)
            .apply()

        if (savedConfig != null) {
            _updateState.value = UpdateState.UpdateAvailable(
                config = savedConfig,
                isMandatory = savedConfig.isUpdateMandatory(getCurrentVersionCode())
            )
        } else {
            _updateState.value = UpdateState.Idle
        }
    }

    /**
     * Dismisses the update dialog for non-mandatory updates.
     */
    fun dismissDialog() {
        val state = _updateState.value
        if (state is UpdateState.UpdateAvailable && !state.isMandatory) {
            _updateState.value = UpdateState.Idle
        } else if (state is UpdateState.Error) {
            _updateState.value = UpdateState.Idle
        }
    }

    /**
     * Triggers package installer with strict verification.
     */
    fun installUpdate(context: Context, apkFile: File): InstallResult {
        if (!apkFile.exists()) {
            return InstallResult.Error("Downloaded APK file not found.")
        }

        if (!ApkInstaller.canInstallPackages(context)) {
            return InstallResult.PermissionRequired
        }

        return ApkInstaller.installApk(context, apkFile)
    }

    private suspend fun handleDownloadCompleted(
        config: UpdateConfig,
        targetFile: File,
        isMandatory: Boolean
    ) {
        // Run strict APK verification
        val verification = ApkVerifier.verifyApk(context, targetFile, config)
        if (verification is ApkVerificationResult.Valid) {
            prefs.edit()
                .putString(KEY_APK_PATH, targetFile.absolutePath)
                .putString(KEY_CONFIG_JSON, serializeConfig(config))
                .apply()

            _updateState.value = UpdateState.ReadyToInstall(
                config = config,
                apkFile = targetFile,
                isMandatory = isMandatory
            )
        } else {
            val reason = (verification as ApkVerificationResult.Invalid).reason
            targetFile.delete()
            prefs.edit().remove(KEY_APK_PATH).apply()
            _updateState.value = UpdateState.Error(
                message = "APK verification failed: $reason",
                config = config
            )
        }
    }

    private fun restoreState() {
        val apkPath = prefs.getString(KEY_APK_PATH, null)
        val savedConfig = getSavedConfig()

        if (savedConfig != null && savedConfig.isUpdateAvailable(getCurrentVersionCode())) {
            val isMandatory = savedConfig.isUpdateMandatory(getCurrentVersionCode())

            if (apkPath != null) {
                val apkFile = File(apkPath)
                if (apkFile.exists()) {
                    if (!ApkVerifier.isValidZipArchive(apkFile)) {
                        apkFile.delete()
                        prefs.edit().remove(KEY_APK_PATH).apply()
                        return
                    }

                    scope.launch {
                        val verification = ApkVerifier.verifyApk(context, apkFile, savedConfig)
                        if (verification is ApkVerificationResult.Valid) {
                            _updateState.value = UpdateState.ReadyToInstall(
                                config = savedConfig,
                                apkFile = apkFile,
                                isMandatory = isMandatory
                            )
                        } else {
                            apkFile.delete()
                            prefs.edit().remove(KEY_APK_PATH).apply()
                        }
                    }
                }
            }
        }
    }

    private suspend fun fetchRemoteUpdateConfig(): UpdateConfig? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(UPDATE_JSON_URL)
                .header("User-Agent", "RepoLens-Android-App")
                .header("Cache-Control", "no-cache")
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                UpdateConfig.parseFromJson(body)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun serializeConfig(config: UpdateConfig): String {
        val obj = org.json.JSONObject().apply {
            put("versionCode", config.versionCode)
            put("versionName", config.versionName)
            put("releaseNotes", config.releaseNotes)
            put("apkUrl", config.apkUrl)
            put("minSupportedVersionCode", config.minSupportedVersionCode)
            put("forceUpdate", config.forceUpdate)
            config.sha256?.let { put("sha256", it) }
        }
        return obj.toString()
    }

    private fun getSavedConfig(): UpdateConfig? {
        val raw = prefs.getString(KEY_CONFIG_JSON, null) ?: return null
        return UpdateConfig.parseFromJson(raw)
    }

    fun cleanUp() {
        downloadJob?.cancel()
    }
}
