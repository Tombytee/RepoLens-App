package com.example.ui.screens.downloads

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DownloadRecordEntity
import com.example.data.repository.ActiveDownload
import com.example.data.repository.DownloadStatus
import com.example.ui.components.MaterialMorphingLoader
import com.example.ui.theme.ApkPurple
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.PlusJakartaSansFamily
import com.example.ui.viewmodel.MainViewModel
import com.example.util.ApkInstaller
import com.example.util.FileFormatters
import com.example.util.InstallResult
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeDownloads by viewModel.activeDownloads.collectAsState()
    val completedDownloads by viewModel.completedDownloads.collectAsState()

    var showPermissionDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<DownloadRecordEntity?>(null) }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.InstallMobile,
                    contentDescription = null,
                    tint = ApkPurple,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Permission Required",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            text = {
                Text(
                    text = "Android requires your permission to install APK packages downloaded through RepoLens. Please toggle 'Allow from this source' in the system settings.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = PlusJakartaSansFamily,
                        lineHeight = 20.sp
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionDialog = false
                        val intent = ApkInstaller.getUnknownSourcesSettingsIntent(context)
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ApkPurple),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Open Settings", style = MaterialTheme.typography.labelLarge.copy(fontFamily = PlusJakartaSansFamily))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("Cancel", style = MaterialTheme.typography.labelLarge.copy(fontFamily = PlusJakartaSansFamily))
                }
            }
        )
    }

    itemToDelete?.let { record ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = {
                Text(
                    text = "Delete File?",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete '${record.fileName}' from your device storage?",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = PlusJakartaSansFamily
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCompletedDownload(record)
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Downloads & Library",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = PlusJakartaSansFamily
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        val activeList = activeDownloads.values.toList()

        if (activeList.isEmpty() && completedDownloads.isEmpty()) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DownloadDone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Text(
                        text = "No Downloads Yet",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = PlusJakartaSansFamily
                        )
                    )
                    Text(
                        text = "Release assets and APKs you download from repositories will be stored here for easy direct installation.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = PlusJakartaSansFamily,
                            lineHeight = 22.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Active Downloads Section
                if (activeList.isNotEmpty()) {
                    item(key = "active_downloads_header") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MaterialMorphingLoader(size = 18.dp, strokeWidth = 2.dp)
                            Text(
                                text = "Active Downloads (${activeList.size})",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = PlusJakartaSansFamily
                                )
                            )
                        }
                    }

                    items(activeList, key = { it.id }) { active ->
                        ActiveDownloadCard(
                            download = active,
                            onCancel = { viewModel.cancelDownload(active.id) },
                            onDismiss = { viewModel.dismissActiveDownload(active.id) },
                            onInstall = {
                                active.localFilePath?.let { path ->
                                    val result = viewModel.installApk(context, path)
                                    if (result is InstallResult.PermissionRequired) {
                                        showPermissionDialog = true
                                    }
                                }
                            }
                        )
                    }
                }

                // Completed Downloads Section
                if (completedDownloads.isNotEmpty()) {
                    item(key = "completed_downloads_header") {
                        Text(
                            text = "Completed Files (${completedDownloads.size})",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = PlusJakartaSansFamily
                            ),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    items(completedDownloads, key = { it.id }) { record ->
                        CompletedDownloadCard(
                            record = record,
                            onInstall = {
                                val result = viewModel.installApk(context, record.filePath)
                                if (result is InstallResult.PermissionRequired) {
                                    showPermissionDialog = true
                                }
                            },
                            onOpenFile = {
                                viewModel.openFile(context, record.filePath)
                            },
                            onDelete = { itemToDelete = record }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveDownloadCard(
    download: ActiveDownload,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
    onInstall: () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = (download.progressPercent / 100f).coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "download_progress"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = if (download.isApk) Icons.Default.Android else Icons.Default.FolderZip,
                        contentDescription = null,
                        tint = if (download.isApk) ApkPurple else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = download.fileName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 13.5.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (download.status == DownloadStatus.DOWNLOADING) {
                    IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = "Cancel",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Dismiss",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${download.repoFullName} • ${download.versionTag}",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = PlusJakartaSansFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            when (download.status) {
                DownloadStatus.DOWNLOADING -> {
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(3.5.dp)),
                        color = if (download.isApk) ApkPurple else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${FileFormatters.formatBytes(download.downloadedBytes)} / ${FileFormatters.formatBytes(download.totalBytes)}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Text(
                            text = "${download.progressPercent}%",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = JetBrainsMonoFamily,
                                color = if (download.isApk) ApkPurple else MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
                DownloadStatus.COMPLETED -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Downloaded successfully",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontFamily = PlusJakartaSansFamily,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }

                        if (download.isApk) {
                            Button(
                                onClick = onInstall,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ApkPurple)
                            ) {
                                Text("Install APK", style = MaterialTheme.typography.labelMedium.copy(fontFamily = PlusJakartaSansFamily))
                            }
                        }
                    }
                }
                DownloadStatus.FAILED -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = download.errorMessage ?: "Download failed",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.error,
                                fontFamily = PlusJakartaSansFamily
                            )
                        )
                    }
                }
                DownloadStatus.CANCELLED -> {
                    Text(
                        text = "Download cancelled",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = PlusJakartaSansFamily
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun CompletedDownloadCard(
    record: DownloadRecordEntity,
    onInstall: () -> Unit,
    onOpenFile: () -> Unit,
    onDelete: () -> Unit
) {
    val fileExists = remember(record.filePath) { File(record.filePath).exists() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = if (record.isApk) Icons.Default.Android else Icons.Default.FolderZip,
                        contentDescription = null,
                        tint = if (record.isApk) ApkPurple else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = record.fileName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 13.5.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${record.repoFullName} • ${record.versionTag} • ${FileFormatters.formatBytes(record.fileSize)}",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = JetBrainsMonoFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (!fileExists) {
                Text(
                    text = "File no longer exists on disk",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.error,
                        fontFamily = PlusJakartaSansFamily
                    )
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onOpenFile,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open", style = MaterialTheme.typography.labelMedium.copy(fontFamily = PlusJakartaSansFamily))
                    }

                    if (record.isApk) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = onInstall,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ApkPurple)
                        ) {
                            Icon(Icons.Default.Android, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Install APK",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = PlusJakartaSansFamily
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
