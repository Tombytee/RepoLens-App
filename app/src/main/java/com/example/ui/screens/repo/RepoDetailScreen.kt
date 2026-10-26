package com.example.ui.screens.repo

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.CommitDto
import com.example.data.model.ContentItemDto
import com.example.data.model.ContributorDto
import com.example.data.model.ReleaseAssetDto
import com.example.data.model.ReleaseDto
import com.example.data.model.RepoDto
import com.example.ui.components.MarkdownViewer
import com.example.ui.components.MaterialMorphingLoader
import com.example.ui.components.MorphingLoadingBox
import com.example.ui.theme.ApkPurple
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.PlusJakartaSansFamily
import com.example.ui.theme.StarGold
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.UiState
import com.example.util.FileFormatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoDetailScreen(
    owner: String,
    repo: String,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onOpenFile: (filePath: String, fileName: String, ref: String?) -> Unit,
    onNavigateToDownloads: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = remember { listOf("Overview", "Files", "Releases", "Activity") }

    val repoState by viewModel.repoState.collectAsState()
    val readmeState by viewModel.readmeState.collectAsState()
    val languagesState by viewModel.languagesState.collectAsState()
    val releasesState by viewModel.releasesState.collectAsState()
    val contentsState by viewModel.contentsState.collectAsState()
    val commitsState by viewModel.commitsState.collectAsState()
    val contributorsState by viewModel.contributorsState.collectAsState()
    val isBookmarked by viewModel.isCurrentRepoBookmarked.collectAsState()
    val currentFolderPath by viewModel.currentFolderPath.collectAsState()

    // Handle back inside directory structure
    BackHandler {
        if (selectedTabIndex == 1 && currentFolderPath.isNotEmpty()) {
            val parentPath = if (currentFolderPath.contains("/")) {
                currentFolderPath.substringBeforeLast("/")
            } else ""
            viewModel.loadContents(owner, repo, parentPath)
        } else {
            onBack()
        }
    }

    LaunchedEffect(owner, repo) {
        viewModel.loadRepository(owner, repo)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = repo,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = PlusJakartaSansFamily
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = owner,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = PlusJakartaSansFamily
                            ),
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleCurrentRepoBookmark() },
                        modifier = Modifier.testTag("bookmark_button")
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (isBookmarked) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "$owner/$repo on GitHub")
                                putExtra(Intent.EXTRA_TEXT, "https://github.com/$owner/$repo")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share repository"))
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = {
                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/$owner/$repo"))
                            context.startActivity(browserIntent)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = "Open in browser",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = repoState) {
                is UiState.Loading -> {
                    MorphingLoadingBox(
                        modifier = Modifier.fillMaxSize(),
                        message = "Fetching repository details & metadata...",
                        size = 56.dp
                    )
                }
                is UiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "Unable to load repository",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = PlusJakartaSansFamily
                                )
                            )
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = PlusJakartaSansFamily
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = { viewModel.loadRepository(owner, repo) },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retry")
                            }
                        }
                    }
                }
                is UiState.Success -> {
                    val repoDto = state.data

                    // Refined Material 3 Tab Row
                    PrimaryTabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        divider = {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                        }
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontFamily = PlusJakartaSansFamily,
                                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium
                                        )
                                    )
                                },
                                modifier = Modifier.testTag("tab_$title")
                            )
                        }
                    }

                    // Fluid Animated Tab Content Switching
                    AnimatedContent(
                        targetState = selectedTabIndex,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                        },
                        label = "tab_content_transition"
                    ) { tabIndex ->
                        when (tabIndex) {
                            0 -> OverviewTab(
                                repo = repoDto,
                                readmeState = readmeState,
                                languagesState = languagesState
                            )
                            1 -> FilesTab(
                                owner = owner,
                                repo = repo,
                                currentPath = currentFolderPath,
                                contentsState = contentsState,
                                onNavigateFolder = { path -> viewModel.loadContents(owner, repo, path) },
                                onOpenFile = onOpenFile
                            )
                            2 -> ReleasesTab(
                                owner = owner,
                                repo = repo,
                                releasesState = releasesState,
                                onDownloadAsset = { asset, releaseTag ->
                                    viewModel.triggerDownload(
                                        downloadUrl = asset.browserDownloadUrl,
                                        fileName = asset.name,
                                        repoFullName = "$owner/$repo",
                                        versionTag = releaseTag,
                                        expectedSize = asset.size
                                    )
                                    onNavigateToDownloads()
                                }
                            )
                            3 -> ActivityTab(
                                commitsState = commitsState,
                                contributorsState = contributorsState
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OverviewTab(
    repo: RepoDto,
    readmeState: UiState<String>,
    languagesState: UiState<Map<String, Long>>
) {
    val languageStats = remember(languagesState) {
        if (languagesState is UiState.Success && languagesState.data.isNotEmpty()) {
            val totalBytes = languagesState.data.values.sum().coerceAtLeast(1L)
            val entries = languagesState.data.entries.sortedByDescending { it.value }
            val topLanguages = entries.take(6).map { (lang, bytes) ->
                Triple(lang, bytes, bytes * 100.0 / totalBytes)
            }
            totalBytes to topLanguages
        } else null
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quick Stats Cards Grid
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (!repo.description.isNullOrBlank()) {
                        Text(
                            text = repo.description,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = PlusJakartaSansFamily,
                                lineHeight = 22.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // 4 Stat Cards in a 2x2 or 4x1 grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatBadgeItem(
                            icon = Icons.Default.Star,
                            iconTint = StarGold,
                            value = FileFormatters.formatNumber(repo.stargazersCount),
                            label = "Stars",
                            modifier = Modifier.weight(1f)
                        )
                        StatBadgeItem(
                            icon = Icons.AutoMirrored.Filled.CallSplit,
                            iconTint = MaterialTheme.colorScheme.secondary,
                            value = FileFormatters.formatNumber(repo.forksCount),
                            label = "Forks",
                            modifier = Modifier.weight(1f)
                        )
                        StatBadgeItem(
                            icon = Icons.Default.ErrorOutline,
                            iconTint = Color(0xFFF59E0B),
                            value = FileFormatters.formatNumber(repo.openIssuesCount),
                            label = "Issues",
                            modifier = Modifier.weight(1f)
                        )
                        StatBadgeItem(
                            icon = Icons.Default.Public,
                            iconTint = MaterialTheme.colorScheme.primary,
                            value = repo.defaultBranch,
                            label = "Branch",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Topics / Tags
        if (!repo.topics.isNullOrEmpty()) {
            item {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repo.topics.forEach { topic ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            border = androidx.compose.foundation.BorderStroke(
                                0.5.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            )
                        ) {
                            Text(
                                text = "#$topic",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = PlusJakartaSansFamily,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }

        // Languages breakdown
        if (languageStats != null) {
            val (totalBytes, topLanguages) = languageStats
            item(key = "languages_card") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Languages",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = PlusJakartaSansFamily
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Multi-segment color bar with rounded pill
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(9.dp)
                                .clip(RoundedCornerShape(4.5.dp))
                        ) {
                            (languagesState as UiState.Success).data.forEach { (lang, bytes) ->
                                val weight = (bytes.toFloat() / totalBytes.toFloat()).coerceAtLeast(0.01f)
                                Box(
                                    modifier = Modifier
                                        .weight(weight)
                                        .height(9.dp)
                                        .background(Color(FileFormatters.getLanguageColor(lang)))
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            topLanguages.forEach { (lang, _, pct) ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color(FileFormatters.getLanguageColor(lang)), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "$lang ${String.format(java.util.Locale.US, "%.1f%%", pct)}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = PlusJakartaSansFamily,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Metadata row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repo.license?.name?.let { lic ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = lic,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = PlusJakartaSansFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
                Text(
                    text = "Repo Size: ${FileFormatters.formatBytes(repo.sizeKb * 1024)}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = JetBrainsMonoFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }

        // README Section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "README.md",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = JetBrainsMonoFamily
                            )
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 0.5.dp
                    )

                    when (readmeState) {
                        is UiState.Loading -> {
                            MorphingLoadingBox(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp),
                                message = "Rendering README markdown...",
                                size = 36.dp
                            )
                        }
                        is UiState.Error -> {
                            Text(
                                text = "Could not load README: ${readmeState.message}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        is UiState.Success -> {
                            MarkdownViewer(markdownText = readmeState.data)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBadgeItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = JetBrainsMonoFamily
                ),
                maxLines = 1
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = PlusJakartaSansFamily,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        )
    }
}

@Composable
private fun FilesTab(
    owner: String,
    repo: String,
    currentPath: String,
    contentsState: UiState<List<ContentItemDto>>,
    onNavigateFolder: (String) -> Unit,
    onOpenFile: (filePath: String, fileName: String, ref: String?) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Modern Pill Breadcrumb Bar
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.clickable { onNavigateFolder("") }
                ) {
                    Text(
                        text = repo,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                            fontFamily = PlusJakartaSansFamily
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                if (currentPath.isNotBlank()) {
                    val segments = currentPath.split("/").filter { it.isNotBlank() }
                    var accumulated = ""
                    segments.forEachIndexed { index, seg ->
                        accumulated = if (accumulated.isEmpty()) seg else "$accumulated/$seg"
                        val targetPath = accumulated
                        Text(
                            text = " / ",
                            style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Text(
                            text = seg,
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = if (index == segments.lastIndex) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                                fontWeight = if (index == segments.lastIndex) FontWeight.Bold else FontWeight.Medium,
                                fontFamily = PlusJakartaSansFamily
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onNavigateFolder(targetPath) }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

        when (contentsState) {
            is UiState.Loading -> {
                MorphingLoadingBox(
                    modifier = Modifier.fillMaxSize(),
                    message = "Loading directory contents...",
                    size = 46.dp
                )
            }
            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Error loading contents: ${contentsState.message}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { onNavigateFolder(currentPath) }) {
                            Text("Retry")
                        }
                    }
                }
            }
            is UiState.Success -> {
                val sortedItems = remember(contentsState.data) {
                    contentsState.data.sortedWith(
                        compareBy<ContentItemDto>({ it.type != "dir" }).thenBy { it.name.lowercase() }
                    )
                }

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (currentPath.isNotBlank()) {
                        item(key = "nav_up_dir") {
                            val parent = if (currentPath.contains("/")) currentPath.substringBeforeLast("/") else ""
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigateFolder(parent) }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = "Parent folder",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "..",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = JetBrainsMonoFamily
                                    )
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                        }
                    }

                    if (sortedItems.isEmpty()) {
                        item(key = "empty_dir") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "This directory is empty.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(sortedItems, key = { it.path }) { item ->
                            val isDir = item.type == "dir"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isDir) {
                                            onNavigateFolder(item.path)
                                        } else {
                                            onOpenFile(item.path, item.name, null)
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (isDir) Icons.Default.Folder else Icons.AutoMirrored.Filled.InsertDriveFile,
                                        contentDescription = null,
                                        tint = if (isDir) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isDir) FontWeight.SemiBold else FontWeight.Normal,
                                            fontFamily = if (!isDir) JetBrainsMonoFamily else PlusJakartaSansFamily,
                                            fontSize = if (!isDir) 13.sp else 14.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (!isDir) {
                                    Text(
                                        text = FileFormatters.formatBytes(item.size),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontFamily = JetBrainsMonoFamily
                                        )
                                    )
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReleasesTab(
    owner: String,
    repo: String,
    releasesState: UiState<List<ReleaseDto>>,
    onDownloadAsset: (ReleaseAssetDto, releaseTag: String) -> Unit
) {
    when (releasesState) {
        is UiState.Loading -> {
            MorphingLoadingBox(
                modifier = Modifier.fillMaxSize(),
                message = "Loading release history and downloadable APKs...",
                size = 50.dp
            )
        }
        is UiState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Could not load releases: ${releasesState.message}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        is UiState.Success -> {
            val releases = releasesState.data
            if (releases.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NewReleases,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(52.dp)
                        )
                        Text(
                            text = "No published releases found",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = PlusJakartaSansFamily
                            )
                        )
                        Text(
                            text = "This repository has not published standalone release binaries or APKs.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = PlusJakartaSansFamily
                            )
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(releases, key = { it.id }) { release ->
                        ReleaseCard(
                            release = release,
                            onDownloadAsset = { asset -> onDownloadAsset(asset, release.tagName) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReleaseCard(
    release: ReleaseDto,
    onDownloadAsset: (ReleaseAssetDto) -> Unit
) {
    var expandedNotes by remember { mutableStateOf(false) }
    val sortedAssets = remember(release.assets) {
        release.assets.sortedByDescending { FileFormatters.isApk(it.name) }
    }
    var showAllAssets by remember(release.assets) { mutableStateOf(false) }
    val displayedAssets = if (showAllAssets || sortedAssets.size <= 5) sortedAssets else sortedAssets.take(5)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = release.name?.ifBlank { release.tagName } ?: release.tagName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = PlusJakartaSansFamily
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = release.tagName,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = JetBrainsMonoFamily,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                if (release.prerelease) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF59E0B).copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "Pre-release",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFD97706),
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Assets Section
            Text(
                text = "Release Assets (${release.assets.size})",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSansFamily
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (release.assets.isEmpty()) {
                Text(
                    text = "No downloadable binary packages attached.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    displayedAssets.forEach { asset ->
                        val isApk = FileFormatters.isApk(asset.name)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isApk) ApkPurple.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainer,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isApk) ApkPurple.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (isApk) Icons.Default.Android else Icons.Default.CloudDownload,
                                        contentDescription = null,
                                        tint = if (isApk) ApkPurple else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = asset.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isApk) FontWeight.Bold else FontWeight.Medium,
                                                fontFamily = JetBrainsMonoFamily,
                                                fontSize = 13.sp
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${FileFormatters.formatBytes(asset.size)} • ${FileFormatters.formatNumber(asset.downloadCount)} downloads",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontFamily = PlusJakartaSansFamily,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                }

                                Button(
                                    onClick = { onDownloadAsset(asset) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isApk) ApkPurple else MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDownload,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = if (isApk) "Install APK" else "Download",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = PlusJakartaSansFamily
                                        )
                                    )
                                }
                            }
                        }
                    }

                    if (!showAllAssets && sortedAssets.size > 5) {
                        TextButton(
                            onClick = { showAllAssets = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.ExpandMore, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Show all ${sortedAssets.size} assets (${sortedAssets.size - 5} more)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = PlusJakartaSansFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }

            // Release Notes body preview
            if (!release.body.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedNotes = !expandedNotes },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Release Notes",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = PlusJakartaSansFamily
                        )
                    )
                    Text(
                        text = if (expandedNotes) "Collapse ▲" else "Expand ▼",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                if (expandedNotes) {
                    Spacer(modifier = Modifier.height(8.dp))
                    MarkdownViewer(markdownText = release.body)
                }
            }
        }
    }
}

@Composable
private fun ActivityTab(
    commitsState: UiState<List<CommitDto>>,
    contributorsState: UiState<List<ContributorDto>>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Contributors row
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Top Contributors",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = PlusJakartaSansFamily
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    when (contributorsState) {
                        is UiState.Loading -> {
                            MaterialMorphingLoader(size = 32.dp)
                        }
                        is UiState.Error -> Text(
                            text = contributorsState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        is UiState.Success -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                contributorsState.data.forEach { contributor ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.width(68.dp)
                                    ) {
                                        AsyncImage(
                                            model = contributor.avatarUrl,
                                            contentDescription = contributor.login,
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = contributor.login,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = PlusJakartaSansFamily,
                                                fontWeight = FontWeight.Medium
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${contributor.contributions}c",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = JetBrainsMonoFamily,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 10.5.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Recent Commits header
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Timeline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Recent Commits",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSansFamily
                    )
                )
            }
        }

        when (commitsState) {
            is UiState.Loading -> {
                item {
                    MorphingLoadingBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        message = "Loading commit history...",
                        size = 38.dp
                    )
                }
            }
            is UiState.Error -> {
                item {
                    Text(
                        text = "Could not load commits: ${commitsState.message}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            is UiState.Success -> {
                items(commitsState.data, key = { it.sha }) { commit ->
                    CommitItem(commit = commit)
                }
            }
        }
    }
}

@Composable
private fun CommitItem(commit: CommitDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = commit.commit.message.lines().firstOrNull() ?: "",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = PlusJakartaSansFamily
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (commit.author?.avatarUrl != null) {
                        AsyncImage(
                            model = commit.author.avatarUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = commit.author?.login ?: commit.commit.author?.name ?: "Unknown",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = PlusJakartaSansFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Text(
                        text = commit.sha.take(7),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
