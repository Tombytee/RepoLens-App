package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.RateLimitState
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.PlusJakartaSansFamily

@Composable
fun SettingsDialog(
    rateLimitState: RateLimitState,
    currentToken: String?,
    currentVersionName: String = "1.0",
    onSaveToken: (String?) -> Unit,
    onClearHistory: () -> Unit,
    onCheckForUpdates: () -> Unit = {},
    onDismiss: () -> Unit
) {
    var tokenInput by remember { mutableStateOf(currentToken ?: "") }
    var showSavedConfirm by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "GitHub API & Settings",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Rate limit status card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "API Quota Status",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = PlusJakartaSansFamily,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (rateLimitState.hasCustomToken) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        else MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = if (rateLimitState.hasCustomToken) "Authenticated" else "Public IP",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (rateLimitState.hasCustomToken) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = PlusJakartaSansFamily
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${rateLimitState.remaining}",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = JetBrainsMonoFamily,
                                    color = if (rateLimitState.remaining < 5) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary
                                )
                            )
                            Text(
                                text = " / ${rateLimitState.limit} requests remaining",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = PlusJakartaSansFamily,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.padding(bottom = 4.dp, start = 8.dp)
                            )
                        }

                        if (rateLimitState.resetEpochSeconds > 0) {
                            val resetDate = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                                .format(java.util.Date(rateLimitState.resetEpochSeconds * 1000))
                            Text(
                                text = "Hourly quota resets at $resetDate",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = PlusJakartaSansFamily,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                // Personal access token section
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Personal Access Token (Optional)",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = PlusJakartaSansFamily
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Adding an unprivileged token raises your rate limit to 5,000 requests per hour without needing any private repository scopes.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = PlusJakartaSansFamily,
                            lineHeight = 18.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = { tokenInput = it },
                        label = { Text("GitHub Token (ghp_... or fine-grained)", fontFamily = PlusJakartaSansFamily) },
                        placeholder = { Text("Paste token here", fontFamily = PlusJakartaSansFamily) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 12.5.sp
                        ),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        trailingIcon = {
                            if (tokenInput.isNotBlank()) {
                                TextButton(onClick = { tokenInput = "" }) {
                                    Text("Clear", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                onSaveToken(tokenInput.trim().ifEmpty { null })
                                showSavedConfirm = true
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Save Token", fontFamily = PlusJakartaSansFamily)
                        }
                    }

                    if (showSavedConfirm) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Token saved successfully!",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontFamily = PlusJakartaSansFamily,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                }

                // Privacy and clear data
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Local Storage",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = PlusJakartaSansFamily
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Visited repositories, bookmarks, and downloads are stored on your device only.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = PlusJakartaSansFamily
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = onClearHistory,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Clear Recent Search History", fontFamily = PlusJakartaSansFamily)
                        }
                    }
                }

                // App Version and In-App Updates
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.SystemUpdate,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "App Updates",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = PlusJakartaSansFamily
                                    )
                                )
                            }

                            Text(
                                text = "v$currentVersionName",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = PlusJakartaSansFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "RepoLens automatically verifies and delivers official application updates.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = PlusJakartaSansFamily
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                onCheckForUpdates()
                                onDismiss()
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Check for Updates", fontFamily = PlusJakartaSansFamily, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Done", fontFamily = PlusJakartaSansFamily)
            }
        }
    )
}
