package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import com.example.data.repository.DownloadStatus
import com.example.data.updater.UpdateState
import com.example.ui.components.SettingsDialog
import com.example.ui.components.UpdateDialog
import com.example.ui.screens.code.CodeViewScreen
import com.example.ui.screens.downloads.DownloadsScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.repo.RepoDetailScreen
import com.example.ui.theme.PlusJakartaSansFamily
import com.example.ui.theme.RepoLensTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.NavigationDestination

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            RepoLensTheme {
                RepoLensApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun RepoLensApp(viewModel: MainViewModel) {
    val currentDestination by viewModel.currentDestination.collectAsState()
    val rateLimitState by viewModel.rateLimitState.collectAsState()
    val savedToken by viewModel.savedToken.collectAsState()
    val activeDownloads by viewModel.activeDownloads.collectAsState()
    val updateState by viewModel.updateState.collectAsState()

    var showSettingsDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val activeCount = remember(activeDownloads) {
        activeDownloads.values.count { it.status == DownloadStatus.DOWNLOADING }
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Handle back button when inside nested screens
    BackHandler(enabled = currentDestination !is NavigationDestination.Home) {
        viewModel.navigateBack()
    }

    if (showSettingsDialog) {
        SettingsDialog(
            rateLimitState = rateLimitState,
            currentToken = savedToken,
            currentVersionName = viewModel.currentVersionName,
            onSaveToken = { viewModel.saveToken(it) },
            onClearHistory = { viewModel.clearHistory() },
            onCheckForUpdates = { viewModel.checkForAppUpdates(force = true) },
            onDismiss = { showSettingsDialog = false }
        )
    }

    // In-App Update Dialog (shown when update available, downloading, ready to install, or error)
    val shouldShowUpdateDialog = updateState is UpdateState.UpdateAvailable ||
        updateState is UpdateState.Downloading ||
        updateState is UpdateState.ReadyToInstall ||
        (updateState is UpdateState.Error && (updateState as UpdateState.Error).config != null)

    if (shouldShowUpdateDialog) {
        UpdateDialog(
            updateState = updateState,
            currentVersionName = viewModel.currentVersionName,
            onStartDownload = { config -> viewModel.startAppUpdateDownload(config) },
            onCancelDownload = { viewModel.cancelAppUpdateDownload() },
            onInstallUpdate = { ctx, apkFile -> viewModel.installAppUpdate(ctx, apkFile) },
            onDismiss = { viewModel.dismissAppUpdateDialog() }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            val showBottomBar = currentDestination is NavigationDestination.Home || currentDestination is NavigationDestination.Downloads

            AnimatedVisibility(
                visible = showBottomBar,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    NavigationBarItem(
                        selected = currentDestination is NavigationDestination.Home,
                        onClick = { viewModel.navigateTo(NavigationDestination.Home) },
                        icon = {
                            Icon(
                                imageVector = if (currentDestination is NavigationDestination.Home) Icons.Filled.Explore else Icons.Outlined.Explore,
                                contentDescription = "Explore"
                            )
                        },
                        label = {
                            Text(
                                "Explore",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = PlusJakartaSansFamily,
                                    fontWeight = if (currentDestination is NavigationDestination.Home) FontWeight.Bold else FontWeight.Medium
                                )
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_explore")
                    )

                    NavigationBarItem(
                        selected = currentDestination is NavigationDestination.Downloads,
                        onClick = { viewModel.navigateTo(NavigationDestination.Downloads) },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (activeCount > 0) {
                                        Badge { Text("$activeCount") }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (currentDestination is NavigationDestination.Downloads) Icons.Filled.CloudDownload else Icons.Outlined.CloudDownload,
                                    contentDescription = "Downloads"
                                )
                            }
                        },
                        label = {
                            Text(
                                "Downloads",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = PlusJakartaSansFamily,
                                    fontWeight = if (currentDestination is NavigationDestination.Downloads) FontWeight.Bold else FontWeight.Medium
                                )
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_downloads")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentDestination,
                transitionSpec = {
                    if (targetState is NavigationDestination.RepoDetail || targetState is NavigationDestination.CodeView) {
                        (slideInHorizontally(initialOffsetX = { it / 3 }) + fadeIn(animationSpec = tween(240)))
                            .togetherWith(slideOutHorizontally(targetOffsetX = { -it / 3 }) + fadeOut(animationSpec = tween(180)))
                    } else {
                        fadeIn(animationSpec = tween(220)).togetherWith(fadeOut(animationSpec = tween(180)))
                    }
                },
                label = "screen_navigation_animation"
            ) { dest ->
                when (dest) {
                    is NavigationDestination.Home -> {
                        HomeScreen(
                            viewModel = viewModel,
                            onOpenRepo = { owner, repo ->
                                viewModel.navigateTo(NavigationDestination.RepoDetail(owner, repo))
                            },
                            onOpenSettings = { showSettingsDialog = true }
                        )
                    }
                    is NavigationDestination.RepoDetail -> {
                        RepoDetailScreen(
                            owner = dest.owner,
                            repo = dest.repo,
                            viewModel = viewModel,
                            onBack = { viewModel.navigateBack() },
                            onOpenFile = { path, name, ref ->
                                viewModel.navigateTo(
                                    NavigationDestination.CodeView(
                                        owner = dest.owner,
                                        repo = dest.repo,
                                        filePath = path,
                                        fileName = name,
                                        ref = ref
                                    )
                                )
                            },
                            onNavigateToDownloads = {
                                viewModel.navigateTo(NavigationDestination.Downloads)
                            }
                        )
                    }
                    is NavigationDestination.Downloads -> {
                        DownloadsScreen(
                            viewModel = viewModel
                        )
                    }
                    is NavigationDestination.CodeView -> {
                        CodeViewScreen(
                            owner = dest.owner,
                            repo = dest.repo,
                            filePath = dest.filePath,
                            fileName = dest.fileName,
                            ref = dest.ref,
                            viewModel = viewModel,
                            onBack = { viewModel.navigateBack() }
                        )
                    }
                }
            }
        }
    }
}
