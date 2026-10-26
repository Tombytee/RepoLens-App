package com.example.ui.screens.home

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.api.RateLimitState
import com.example.data.local.BookmarkEntity
import com.example.data.local.RecentRepoEntity
import com.example.data.model.RepoDto
import com.example.ui.components.CURATED_REPOSITORIES
import com.example.ui.components.CuratedRepo
import com.example.ui.components.MorphingLoadingBox
import com.example.ui.theme.ApkPurple
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.PlusJakartaSansFamily
import com.example.ui.theme.StarGold
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.UiState
import com.example.util.FileFormatters
import com.example.util.UrlParser

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onOpenRepo: (owner: String, repo: String) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val searchQuery by viewModel.searchInput.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val recentRepos by viewModel.recentRepos.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()
    val rateLimitState by viewModel.rateLimitState.collectAsState()

    var selectedCategory by remember { mutableStateOf("All") }
    val categories = remember { listOf("All", "Android Apps", "Developer Tools", "Libraries") }

    val filteredCurated = remember(selectedCategory) {
        if (selectedCategory == "All") CURATED_REPOSITORIES
        else CURATED_REPOSITORIES.filter { it.category == selectedCategory }
    }

    var inputError by remember { mutableStateOf<String?>(null) }
    val isAnalyzing = searchResults is UiState.Loading

    val handleAnalyze: (String) -> Unit = { raw ->
        inputError = null
        val trimmed = raw.trim()
        if (trimmed.isBlank()) {
            inputError = "Please enter or paste a GitHub repository URL or owner/repo"
        } else {
            val parsed = UrlParser.parse(trimmed)
            if (parsed != null) {
                onOpenRepo(parsed.owner, parsed.repo)
            } else {
                if (trimmed.contains("github.com", ignoreCase = true) ||
                    trimmed.startsWith("http://", ignoreCase = true) ||
                    trimmed.startsWith("https://", ignoreCase = true)) {
                    inputError = "Invalid GitHub repository URL. Use format: github.com/owner/repo"
                } else if (trimmed.contains("/")) {
                    val parts = trimmed.split("/").filter { it.isNotBlank() }
                    if (parts.size >= 2) {
                        onOpenRepo(parts[0], parts[1])
                    } else {
                        inputError = "Invalid repository format. Please check owner and repository name."
                    }
                } else {
                    viewModel.executeSearch(trimmed)
                }
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen_content"),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Expressive App Header
        item(key = "header") {
            HomeHeader(
                rateLimitState = rateLimitState,
                onOpenSettings = onOpenSettings
            )
        }

        // Search & Paste Input Card
        item(key = "search_card") {
            SearchInputCard(
                query = searchQuery,
                onQueryChange = {
                    inputError = null
                    viewModel.onSearchQueryChanged(it)
                },
                onAnalyze = { handleAnalyze(it) },
                onSearch = { handleAnalyze(it) },
                isAnalyzing = isAnalyzing,
                errorMessage = inputError,
                onPasteClipboard = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    if (clipboard.hasPrimaryClip() &&
                        (clipboard.primaryClipDescription?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) == true ||
                         clipboard.primaryClipDescription?.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML) == true)
                    ) {
                        val item = clipboard.primaryClip?.getItemAt(0)
                        val text = item?.text?.toString() ?: ""
                        if (text.isNotBlank()) {
                            val trimmed = text.trim()
                            inputError = null
                            viewModel.onSearchQueryChanged(trimmed)
                            val parsed = UrlParser.parse(trimmed)
                            if (parsed != null) {
                                onOpenRepo(parsed.owner, parsed.repo)
                            }
                        }
                    }
                }
            )
        }

        // Live Search Results
        searchResults?.let { state ->
            item(key = "search_results_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Search Results",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = PlusJakartaSansFamily
                        )
                    )
                }
            }

            when (state) {
                is UiState.Loading -> {
                    item(key = "search_loading") {
                        MorphingLoadingBox(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            message = "Searching GitHub repositories...",
                            size = 42.dp
                        )
                    }
                }
                is UiState.Error -> {
                    item(key = "search_error") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                            )
                        ) {
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
                is UiState.Success -> {
                    if (state.data.isEmpty()) {
                        item(key = "search_empty") {
                            Text(
                                text = "No repositories found matching your query.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                            )
                        }
                    } else {
                        items(state.data, key = { it.id }) { repo ->
                            SearchResultItem(
                                repo = repo,
                                onClick = { onOpenRepo(repo.owner.login, repo.name) }
                            )
                        }
                    }
                }
            }
        }

        // Recent Repositories
        if (recentRepos.isNotEmpty() && searchResults == null) {
            item(key = "recent_repos_section") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Recently Viewed",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = PlusJakartaSansFamily
                                )
                            )
                        }
                        Text(
                            text = "Clear all",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.clearHistory() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    LazyRow(
                        modifier = Modifier.padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp)
                    ) {
                        items(recentRepos, key = { it.fullName }) { recent ->
                            RecentRepoChip(
                                recent = recent,
                                onClick = { onOpenRepo(recent.owner, recent.repoName) }
                            )
                        }
                    }
                }
            }
        }

        // Bookmarks
        if (bookmarks.isNotEmpty() && searchResults == null) {
            item(key = "bookmarks_section") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Saved Bookmarks",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = PlusJakartaSansFamily
                            )
                        )
                    }

                    LazyRow(
                        modifier = Modifier.padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp)
                    ) {
                        items(bookmarks, key = { it.fullName }) { bookmark ->
                            BookmarkRepoChip(
                                bookmark = bookmark,
                                onClick = { onOpenRepo(bookmark.owner, bookmark.repoName) }
                            )
                        }
                    }
                }
            }
        }

        // Curated Repositories Section
        if (searchResults == null) {
            item(key = "curated_section_header") {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Featured Open-Source",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = PlusJakartaSansFamily
                            )
                        )
                    }

                    Text(
                        text = "Explore trusted repositories with source code, release notes, and installable APKs",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        ),
                        modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
                    )

                    // Filter Chips Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = { selectedCategory = category },
                                label = {
                                    Text(
                                        category,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = PlusJakartaSansFamily,
                                            fontWeight = if (selectedCategory == category) FontWeight.Bold else FontWeight.Medium
                                        )
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selectedCategory == category,
                                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                                    selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                }
            }

            items(filteredCurated, key = { "${it.owner}/${it.name}" }) { item ->
                CuratedRepoCard(
                    repo = item,
                    onClick = { onOpenRepo(item.owner, item.name) }
                )
            }
        }
    }
}

@Composable
private fun HomeHeader(
    rateLimitState: RateLimitState,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "RepoLens",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = PlusJakartaSansFamily,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = (-0.5).sp
                )
            )
            Text(
                text = "GitHub Explorer & APK Installer",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = PlusJakartaSansFamily
                )
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Elegant Quota Chip
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.clickable { onOpenSettings() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(
                                if (rateLimitState.remaining > 10) MaterialTheme.colorScheme.primary
                                else Color(0xFFEF4444),
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${rateLimitState.remaining} / ${rateLimitState.limit}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = JetBrainsMonoFamily,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .testTag("settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun SearchInputCard(
    query: String,
    onQueryChange: (String) -> Unit,
    onAnalyze: (String) -> Unit,
    onSearch: (String) -> Unit,
    isAnalyzing: Boolean,
    errorMessage: String?,
    onPasteClipboard: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("repo_url_input"),
                placeholder = {
                    Text(
                        text = "Paste repo URL (e.g. github.com/owner/repo)",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = PlusJakartaSansFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 13.5.sp
                        )
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                },
                trailingIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        if (query.isNotBlank()) {
                            IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onPasteClipboard() }
                                .testTag("paste_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Paste",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = PlusJakartaSansFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onAnalyze(query) }),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            // Prominent "Analyze Repository" Button directly beneath the URL input
            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { onAnalyze(query) },
                enabled = !isAnalyzing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("analyze_repository_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 0.dp)
            ) {
                if (isAnalyzing) {
                    com.example.ui.components.MaterialMorphingLoader(
                        primaryColor = MaterialTheme.colorScheme.onPrimary,
                        haloColor = MaterialTheme.colorScheme.primaryContainer,
                        size = 22.dp,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Analyzing Repository...",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = PlusJakartaSansFamily,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Analyze Repository",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = PlusJakartaSansFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontFamily = PlusJakartaSansFamily,
                            fontSize = 12.sp
                        )
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, start = 4.dp, end = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Supports URLs, tree links, and 'owner/repo'",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = PlusJakartaSansFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.5.sp
                    )
                )

                if (query.isNotBlank() && !isAnalyzing) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSearch(query) }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Explore",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = PlusJakartaSansFamily,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CuratedRepoCard(
    repo: CuratedRepo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clickable(onClick = onClick)
            .testTag("curated_repo_${repo.name}"),
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "${repo.owner} / ",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = PlusJakartaSansFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Text(
                        text = repo.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = PlusJakartaSansFamily,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (repo.hasApkReleases) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ApkPurple.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, ApkPurple.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Android,
                                contentDescription = null,
                                tint = ApkPurple,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "APK Ready",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = PlusJakartaSansFamily,
                                    color = ApkPurple,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = repo.description,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = PlusJakartaSansFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 19.sp,
                    fontSize = 13.sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .background(
                                Color(FileFormatters.getLanguageColor(repo.language)),
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = repo.language,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = PlusJakartaSansFamily,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Stars",
                            tint = StarGold,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = repo.starsDisplay,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = JetBrainsMonoFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentRepoChip(
    recent: RecentRepoEntity,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!recent.avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = recent.avatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(10.dp))
            }
            Column {
                Text(
                    text = recent.repoName,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
                Text(
                    text = recent.owner,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = PlusJakartaSansFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.5.sp
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun BookmarkRepoChip(
    bookmark: BookmarkEntity,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Bookmark,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = bookmark.repoName,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = PlusJakartaSansFamily,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SearchResultItem(
    repo: RepoDto,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = repo.fullName,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSansFamily,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            if (!repo.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = repo.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = PlusJakartaSansFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                repo.language?.let { lang ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(FileFormatters.getLanguageColor(lang)), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = lang,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = PlusJakartaSansFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                }
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = StarGold,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = FileFormatters.formatNumber(repo.stargazersCount),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = JetBrainsMonoFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}
