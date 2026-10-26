package com.example.data.updater

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.example.data.model.UpdateConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

sealed interface ApkVerificationResult {
    data object Valid : ApkVerificationResult
    data class Invalid(val reason: String) : ApkVerificationResult
}

object ApkVerifier {

    /**
     * Checks if a file is a valid ZIP archive containing AndroidManifest.xml
     * without calling native Android package manager code.
     */
    fun isValidZipArchive(file: File): Boolean {
        if (!file.exists() || !file.isFile || file.length() < 1024L) {
            return false
        }
        return try {
            val hasZipMagic = FileInputStream(file).use { fis ->
                val header = ByteArray(4)
                val read = fis.read(header)
                read == 4 &&
                    header[0] == 0x50.toByte() &&
                    header[1] == 0x4B.toByte() &&
                    header[2] == 0x03.toByte() &&
                    header[3] == 0x04.toByte()
            }
            if (!hasZipMagic) return false

            java.util.zip.ZipFile(file).use { zip ->
                zip.getEntry("AndroidManifest.xml") != null
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Computes the SHA-256 hash of a file on a background dispatcher.
     */
    suspend fun computeSha256(file: File): String = withContext(Dispatchers.IO) {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Strictly verifies APK file integrity, package name, version code, SHA-256 (if provided),
     * and signing certificate compatibility before installation.
     */
    suspend fun verifyApk(
        context: Context,
        apkFile: File,
        expectedConfig: UpdateConfig
    ): ApkVerificationResult = withContext(Dispatchers.IO) {
        if (!apkFile.exists() || !apkFile.isFile || apkFile.length() <= 0L) {
            return@withContext ApkVerificationResult.Invalid("APK file is missing or empty.")
        }

        // 1. Verify file size
        if (apkFile.length() < 1024L) {
            return@withContext ApkVerificationResult.Invalid("Downloaded file is too small to be a valid APK package.")
        }

        // 2. Verify SHA-256 checksum if provided
        if (!expectedConfig.sha256.isNullOrBlank()) {
            val computedHash = try {
                computeSha256(apkFile)
            } catch (e: Exception) {
                return@withContext ApkVerificationResult.Invalid("Failed to compute SHA-256 hash: ${e.message}")
            }

            if (!computedHash.equals(expectedConfig.sha256, ignoreCase = true)) {
                return@withContext ApkVerificationResult.Invalid(
                    "SHA-256 checksum mismatch (expected: ${expectedConfig.sha256}, actual: $computedHash)"
                )
            }
        }

        // 3. Pre-flight check: Verify ZIP structure and AndroidManifest.xml existence
        // This is critical: passing an invalid or non-ZIP file (e.g. an HTML page) to
        // PackageManager.getPackageArchiveInfo() causes native Android ApkAssets to log
        // 'Failed to open APK ...: Invalid file' to system error logs.
        if (!isValidZipArchive(apkFile)) {
            val preview = try {
                FileInputStream(apkFile).use { fis ->
                    val buf = ByteArray(256)
                    val read = fis.read(buf)
                    if (read > 0) String(buf, 0, read) else ""
                }
            } catch (_: Exception) {
                ""
            }

            val reason = if (preview.contains("<html", ignoreCase = true) || preview.contains("<!doctype", ignoreCase = true)) {
                "Downloaded file is an HTML webpage, not an APK archive. The update link is pointing to a webpage rather than a direct APK download."
            } else {
                "Downloaded file is corrupted or not a valid Android APK archive."
            }
            return@withContext ApkVerificationResult.Invalid(reason)
        }

        // 4. Inspect APK Package Archive using PackageManager
        val pm = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }

        val archiveInfo = try {
            pm.getPackageArchiveInfo(apkFile.absolutePath, flags)
        } catch (_: Exception) {
            null
        } ?: return@withContext ApkVerificationResult.Invalid("Unable to parse APK archive: corrupted or malformed package.")

        // 5. Verify Package Name
        val archivePackageName = archiveInfo.packageName
        if (archivePackageName != context.packageName) {
            return@withContext ApkVerificationResult.Invalid(
                "Package name mismatch: expected '${context.packageName}', but found '$archivePackageName'."
            )
        }

        // 6. Verify Version Code
        val archiveVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            archiveInfo.longVersionCode.toInt()
        } else {
            @Suppress("DEPRECATION")
            archiveInfo.versionCode
        }

        if (archiveVersionCode != expectedConfig.versionCode) {
            return@withContext ApkVerificationResult.Invalid(
                "Version code mismatch: expected ${expectedConfig.versionCode}, but found $archiveVersionCode."
            )
        }

        // 7. Verify Signing Certificate Compatibility with currently installed app
        val installedInfo = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
            }
        } catch (_: Exception) {
            null
        }

        if (installedInfo != null) {
            val installedSigs: List<ByteArray> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                installedInfo.signingInfo?.apkContentsSigners?.map { it.toByteArray() } ?: emptyList()
            } else {
                @Suppress("DEPRECATION")
                installedInfo.signatures?.map { it.toByteArray() } ?: emptyList()
            }

            val archiveSigs: List<ByteArray> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                archiveInfo.signingInfo?.apkContentsSigners?.map { it.toByteArray() } ?: emptyList()
            } else {
                @Suppress("DEPRECATION")
                archiveInfo.signatures?.map { it.toByteArray() } ?: emptyList()
            }

            if (installedSigs.isNotEmpty() && archiveSigs.isNotEmpty()) {
                val matches = installedSigs.any { inst ->
                    archiveSigs.any { arch -> inst.contentEquals(arch) }
                }
                if (!matches) {
                    return@withContext ApkVerificationResult.Invalid(
                        "Signature mismatch: APK is signed with a different key than the installed application."
                    )
                }
            }
        }

        ApkVerificationResult.Valid
    }
}
