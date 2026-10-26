package com.example.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.UpdateConfig
import com.example.data.updater.UpdateState
import com.example.ui.theme.PlusJakartaSansFamily
import com.example.util.ApkInstaller
import java.io.File
import java.util.Locale

@Composable
fun UpdateDialog(
    updateState: UpdateState,
    currentVersionName: String,
    onStartDownload: (UpdateConfig) -> Unit,
    onCancelDownload: () -> Unit,
    onInstallUpdate: (Context, File) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var permissionNeeded by remember { mutableStateOf(false) }

    val isMandatory = when (updateState) {
        is UpdateState.UpdateAvailable -> updateState.isMandatory
        is UpdateState.Downloading -> updateState.isMandatory
        is UpdateState.ReadyToInstall -> updateState.isMandatory
        else -> false
    }

    AlertDialog(
        onDismissRequest = {
            if (!isMandatory) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !isMandatory,
            dismissOnClickOutside = !isMandatory
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("dialog_in_app_update"),
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (updateState) {
                            is UpdateState.ReadyToInstall -> Icons.Filled.CheckCircle
                            is UpdateState.Error -> Icons.Filled.ErrorOutline
                            is UpdateState.Downloading -> Icons.Filled.CloudDownload
                            else -> if (isMandatory) Icons.Filled.Lock else Icons.Filled.SystemUpdate
                        },
                        contentDescription = "Update status",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Text(
                        text = when (updateState) {
                            is UpdateState.ReadyToInstall -> "Update Ready to Install"
                            is UpdateState.Downloading -> "Downloading Update"
                            is UpdateState.Error -> "Update Interrupted"
                            else -> if (isMandatory) "Mandatory Update" else "New Version Available"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = PlusJakartaSansFamily,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    val targetVer = when (updateState) {
                        is UpdateState.UpdateAvailable -> updateState.config.versionName
                        is UpdateState.Downloading -> updateState.config.versionName
                        is UpdateState.ReadyToInstall -> updateState.config.versionName
                        is UpdateState.Error -> updateState.config?.versionName
                        else -> null
                    }

                    if (targetVer != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "v$currentVersionName",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = PlusJakartaSansFamily,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp)
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "v$targetVer",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = PlusJakartaSansFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        text = {
            AnimatedContent(
                targetState = updateState,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "update_dialog_content"
            ) { state ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    when (state) {
                        is UpdateState.UpdateAvailable -> {
                            if (state.isMandatory) {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "This update is required to continue using RepoLens safely.",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = PlusJakartaSansFamily,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "What's New:",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = PlusJakartaSansFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 180.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = state.config.releaseNotes,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = PlusJakartaSansFamily,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 20.sp
                                        )
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.padding(top = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Secure update verified with APK signature checks.",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = PlusJakartaSansFamily,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        is UpdateState.Downloading -> {
                            val progressMB = (state.progressBytes / (1024.0 * 1024.0))
                            val totalMB = (state.totalBytes / (1024.0 * 1024.0))

                            Text(
                                text = "Downloading update package in background...",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = PlusJakartaSansFamily,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            if (state.totalBytes > 0) {
                                LinearProgressIndicator(
                                    progress = { state.percent / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = String.format(Locale.US, "%.1f MB / %.1f MB", progressMB, totalMB),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = PlusJakartaSansFamily,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                    Text(
                                        text = "${state.percent}%",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = PlusJakartaSansFamily,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }
                            } else {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Starting download...",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = PlusJakartaSansFamily,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        is UpdateState.ReadyToInstall -> {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(
                                        text = "APK verified successfully and ready to install.",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = PlusJakartaSansFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    )
                                }
                            }

                            if (permissionNeeded || !ApkInstaller.canInstallPackages(context)) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Settings,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "Permission Required",
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = PlusJakartaSansFamily,
                                                    color = MaterialTheme.colorScheme.onErrorContainer
                                                )
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "RepoLens needs permission to install packages from unknown sources. Tap below to grant permission in Settings, then return to complete installation.",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = PlusJakartaSansFamily,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = {
                                                context.startActivity(ApkInstaller.getUnknownSourcesSettingsIntent(context))
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.error
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                "Allow in Settings",
                                                fontFamily = PlusJakartaSansFamily,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        is UpdateState.Error -> {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ErrorOutline,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Error",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = PlusJakartaSansFamily,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = state.message,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = PlusJakartaSansFamily,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    )

                                    if (state.config != null && state.config.apkUrl.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        OutlinedButton(
                                            onClick = {
                                                try {
                                                    val browserIntent = android.content.Intent(
                                                        android.content.Intent.ACTION_VIEW,
                                                        android.net.Uri.parse(state.config.apkUrl)
                                                    ).apply {
                                                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    }
                                                    context.startActivity(browserIntent)
                                                } catch (_: Exception) {}
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                "Open Link in Browser",
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontFamily = PlusJakartaSansFamily,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        else -> Unit
                    }
                }
            }
        },
        confirmButton = {
            when (updateState) {
                is UpdateState.UpdateAvailable -> {
                    Button(
                        onClick = { onStartDownload(updateState.config) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("btn_download_update")
                    ) {
                        Text(
                            "Download Update",
                            fontFamily = PlusJakartaSansFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                is UpdateState.Downloading -> {
                    OutlinedButton(
                        onClick = onCancelDownload,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("btn_cancel_download")
                    ) {
                        Text(
                            "Cancel",
                            fontFamily = PlusJakartaSansFamily
                        )
                    }
                }

                is UpdateState.ReadyToInstall -> {
                    Button(
                        onClick = {
                            if (!ApkInstaller.canInstallPackages(context)) {
                                permissionNeeded = true
                                context.startActivity(ApkInstaller.getUnknownSourcesSettingsIntent(context))
                            } else {
                                permissionNeeded = false
                                onInstallUpdate(context, updateState.apkFile)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("btn_install_update")
                    ) {
                        Text(
                            "Install Update",
                            fontFamily = PlusJakartaSansFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                is UpdateState.Error -> {
                    if (updateState.config != null) {
                        Button(
                            onClick = { onStartDownload(updateState.config) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                "Retry",
                                fontFamily = PlusJakartaSansFamily,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("OK", fontFamily = PlusJakartaSansFamily)
                        }
                    }
                }

                else -> Unit
            }
        },
        dismissButton = {
            if (!isMandatory && (updateState is UpdateState.UpdateAvailable || updateState is UpdateState.ReadyToInstall || updateState is UpdateState.Error)) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("btn_dismiss_update")
                ) {
                    Text(
                        if (updateState is UpdateState.Error) "Close" else "Later",
                        fontFamily = PlusJakartaSansFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    )
}
