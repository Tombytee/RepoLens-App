package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

sealed interface InstallResult {
    data object Success : InstallResult
    data object PermissionRequired : InstallResult
    data class Error(val message: String) : InstallResult
}

object ApkInstaller {

    fun canInstallPackages(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun getUnknownSourcesSettingsIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            )
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }
    }

    fun installApk(context: Context, apkFile: File): InstallResult {
        if (!apkFile.exists()) {
            return InstallResult.Error("APK file not found on disk")
        }

        if (!com.example.data.updater.ApkVerifier.isValidZipArchive(apkFile)) {
            return InstallResult.Error("The file is not a valid Android package (APK).")
        }

        if (!canInstallPackages(context)) {
            return InstallResult.PermissionRequired
        }

        try {
            val authority = "${context.packageName}.fileprovider"
            val apkUri: Uri = FileProvider.getUriForFile(context, authority, apkFile)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }

            context.startActivity(intent)
            return InstallResult.Success
        } catch (e: Exception) {
            return InstallResult.Error(e.localizedMessage ?: "Failed to launch package installer")
        }
    }

    fun openFile(context: Context, file: File, mimeType: String = "*/*") {
        if (!file.exists()) return
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(Intent.createChooser(intent, "Open with..."))
        } catch (_: Exception) {
        }
    }
}
