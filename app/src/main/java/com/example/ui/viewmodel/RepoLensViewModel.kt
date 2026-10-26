package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.ApiClient
import com.example.data.api.RateLimitState
import com.example.data.local.AppDatabase
import com.example.data.local.BookmarkEntity
import com.example.data.local.DownloadRecordEntity
import com.example.data.local.RecentRepoEntity
import com.example.data.model.CommitDto
import com.example.data.model.ContentItemDto
import com.example.data.model.ContributorDto
import com.example.data.model.ReleaseDto
import com.example.data.model.RepoDto
import com.example.data.repository.ActiveDownload
import com.example.data.repository.DownloadRepository
import com.example.data.repository.GitHubRepository
import com.example.data.model.UpdateConfig
import com.example.data.updater.InAppUpdateManager
import com.example.data.updater.UpdateState
import com.example.util.ApkInstaller
import com.example.util.InstallResult
import com.example.util.UrlParser
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

sealed interface NavigationDestination {
    data object Home : NavigationDestination
    data class RepoDetail(val owner: String, val repo: String) : NavigationDestination
    data object Downloads : NavigationDestination
    data class CodeView(
        val owner: String,
        val repo: String,
        val filePath: String,
        val fileName: String,
        val ref: String? = null
    ) : NavigationDestination
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<out T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    val gitHubRepository = GitHubRepository(
        api = ApiClient.service,
        recentRepoDao = db.recentRepoDao(),
        bookmarkDao = db.bookmarkDao()
    )
    val downloadRepository = DownloadRepository(
        context = application,
        downloadRecordDao = db.downloadRecordDao()
    )

    // Navigation backstack
    private val _navigationStack = MutableStateFlow<List<NavigationDestination>>(listOf(NavigationDestination.Home))
    val currentDestination: StateFlow<NavigationDestination> = _navigationStack
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = NavigationDestination.Home
        ).run {
            // Derived flow returning the top of the stack
            val topFlow = MutableStateFlow<NavigationDestination>(NavigationDestination.Home)
            viewModelScope.launch {
                _navigationStack.collect { stack ->
                    topFlow.value = stack.lastOrNull() ?: NavigationDestination.Home
                }
            }
            topFlow.asStateFlow()
        }

    fun navigateTo(destination: NavigationDestination) {
        val current = _navigationStack.value.toMutableList()
        // If clicking same tab as root, replace or pop to root
        if (destination is NavigationDestination.Home && current.size == 1 && current.first() is NavigationDestination.Home) {
            return
        }
        current.add(destination)
        _navigationStack.value = current
    }

    fun navigateBack(): Boolean {
        val current = _navigationStack.value.toMutableList()
        if (current.size > 1) {
            current.removeAt(current.size - 1)
            _navigationStack.value = current
            return true
        }
        return false
    }

    // Rate limits & Token
    val rateLimitState: StateFlow<RateLimitState> = ApiClient.rateLimitState
    private val prefs = application.getSharedPreferences("repolens_prefs", Context.MODE_PRIVATE)

    private val _savedToken = MutableStateFlow<String?>(null)
    val savedToken = _savedToken.asStateFlow()

    // In-App Updater
    val inAppUpdateManager = InAppUpdateManager(
        context = application,
        scope = viewModelScope
    )
    val updateState: StateFlow<UpdateState> = inAppUpdateManager.updateState
    val currentVersionName: String = inAppUpdateManager.getCurrentVersionName()

    init {
        val token = prefs.getString("github_pat", null)
        _savedToken.value = token
        ApiClient.setCustomToken(token)

        // Automatic background update check on app launch (debounced)
        inAppUpdateManager.checkForUpdates(force = false)
    }

    fun saveToken(token: String?) {
        prefs.edit().apply {
            if (token.isNullOrBlank()) {
                remove("github_pat")
            } else {
                putString("github_pat", token)
            }
            apply()
        }
        _savedToken.value = token
        ApiClient.setCustomToken(token)
        // Refresh rate limit
        viewModelScope.launch {
            gitHubRepository.getRateLimit()
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            gitHubRepository.clearAllRecent()
        }
    }

    // Home Screen Data
    val recentRepos: StateFlow<List<RecentRepoEntity>> = gitHubRepository.recentRepos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarks: StateFlow<List<BookmarkEntity>> = gitHubRepository.bookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search input
    val searchInput = MutableStateFlow("")
    val searchResults = MutableStateFlow<UiState<List<RepoDto>>?>(null)

    fun onSearchQueryChanged(query: String) {
        searchInput.value = query
        if (query.isBlank()) {
            searchResults.value = null
        }
    }

    fun executeSearch(query: String) {
        val parsed = UrlParser.parse(query)
        if (parsed != null) {
            navigateTo(NavigationDestination.RepoDetail(parsed.owner, parsed.repo))
            return
        }

        // Otherwise keyword search
        if (query.trim().length >= 2) {
            viewModelScope.launch {
                searchResults.value = UiState.Loading
                val result = gitHubRepository.searchRepositories(query.trim())
                result.fold(
                    onSuccess = { searchResults.value = UiState.Success(it) },
                    onFailure = { searchResults.value = UiState.Error(it.localizedMessage ?: "Search failed") }
                )
            }
        }
    }

    // Single Repository Details State
    val repoState = MutableStateFlow<UiState<RepoDto>>(UiState.Loading)
    val readmeState = MutableStateFlow<UiState<String>>(UiState.Loading)
    val languagesState = MutableStateFlow<UiState<Map<String, Long>>>(UiState.Loading)
    val releasesState = MutableStateFlow<UiState<List<ReleaseDto>>>(UiState.Loading)
    val contentsState = MutableStateFlow<UiState<List<ContentItemDto>>>(UiState.Loading)
    val commitsState = MutableStateFlow<UiState<List<CommitDto>>>(UiState.Loading)
    val contributorsState = MutableStateFlow<UiState<List<ContributorDto>>>(UiState.Loading)
    val isCurrentRepoBookmarked = MutableStateFlow(false)

    // Current directory path inside Files tab
    val currentFolderPath = MutableStateFlow("")

    fun loadRepository(owner: String, repo: String) {
        repoState.value = UiState.Loading
        readmeState.value = UiState.Loading
        languagesState.value = UiState.Loading
        releasesState.value = UiState.Loading
        contentsState.value = UiState.Loading
        commitsState.value = UiState.Loading
        contributorsState.value = UiState.Loading
        currentFolderPath.value = ""

        viewModelScope.launch {
            val result = gitHubRepository.getRepo(owner, repo)
            result.fold(
                onSuccess = { dto ->
                    repoState.value = UiState.Success(dto)
                    // Check bookmark status
                    gitHubRepository.isBookmarked(dto.fullName).collect { isBookmarked ->
                        isCurrentRepoBookmarked.value = isBookmarked
                    }
                },
                onFailure = { error ->
                    repoState.value = UiState.Error(error.localizedMessage ?: "Failed to load repository")
                }
            )
        }

        // Concurrently load supplementary tabs
        viewModelScope.launch {
            val r = gitHubRepository.getReadme(owner, repo)
            r.fold(
                onSuccess = { readmeState.value = UiState.Success(it) },
                onFailure = { readmeState.value = UiState.Error(it.localizedMessage ?: "Failed to load README") }
            )
        }

        viewModelScope.launch {
            val r = gitHubRepository.getLanguages(owner, repo)
            r.fold(
                onSuccess = { languagesState.value = UiState.Success(it) },
                onFailure = { languagesState.value = UiState.Error(it.localizedMessage ?: "Failed to load languages") }
            )
        }

        viewModelScope.launch {
            val r = gitHubRepository.getReleases(owner, repo)
            r.fold(
                onSuccess = { releasesState.value = UiState.Success(it) },
                onFailure = { releasesState.value = UiState.Error(it.localizedMessage ?: "Failed to load releases") }
            )
        }

        viewModelScope.launch {
            loadContents(owner, repo, "")
        }

        viewModelScope.launch {
            val r = gitHubRepository.getCommits(owner, repo)
            r.fold(
                onSuccess = { commitsState.value = UiState.Success(it) },
                onFailure = { commitsState.value = UiState.Error(it.localizedMessage ?: "Failed to load commits") }
            )
        }

        viewModelScope.launch {
            val r = gitHubRepository.getContributors(owner, repo)
            r.fold(
                onSuccess = { contributorsState.value = UiState.Success(it) },
                onFailure = { contributorsState.value = UiState.Error(it.localizedMessage ?: "Failed to load contributors") }
            )
        }
    }

    fun loadContents(owner: String, repo: String, path: String) {
        currentFolderPath.value = path
        contentsState.value = UiState.Loading
        viewModelScope.launch {
            val r = gitHubRepository.getContents(owner, repo, path)
            r.fold(
                onSuccess = { contentsState.value = UiState.Success(it) },
                onFailure = { contentsState.value = UiState.Error(it.localizedMessage ?: "Failed to load directory") }
            )
        }
    }

    fun toggleCurrentRepoBookmark() {
        val current = repoState.value
        if (current is UiState.Success) {
            val repo = current.data
            viewModelScope.launch {
                if (isCurrentRepoBookmarked.value) {
                    gitHubRepository.removeBookmark(repo.fullName)
                    isCurrentRepoBookmarked.value = false
                } else {
                    gitHubRepository.addBookmark(repo)
                    isCurrentRepoBookmarked.value = true
                }
            }
        }
    }

    // Code Viewer State
    val codeViewState = MutableStateFlow<UiState<String>>(UiState.Loading)

    fun loadFileContent(owner: String, repo: String, path: String, ref: String?) {
        codeViewState.value = UiState.Loading
        viewModelScope.launch {
            val r = gitHubRepository.getFileContent(owner, repo, path, ref)
            r.fold(
                onSuccess = { codeViewState.value = UiState.Success(it) },
                onFailure = { codeViewState.value = UiState.Error(it.localizedMessage ?: "Failed to load file") }
            )
        }
    }

    // Downloads
    val activeDownloads: StateFlow<Map<String, ActiveDownload>> = downloadRepository.activeDownloads
    val completedDownloads: StateFlow<List<DownloadRecordEntity>> = downloadRepository.completedDownloads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _snackbarMessages = MutableSharedFlow<String>()
    val snackbarMessages = _snackbarMessages.asSharedFlow()

    fun triggerDownload(
        downloadUrl: String,
        fileName: String,
        repoFullName: String,
        versionTag: String,
        expectedSize: Long = 0L
    ) {
        val id = downloadRepository.startDownload(downloadUrl, fileName, repoFullName, versionTag, expectedSize)
        viewModelScope.launch {
            _snackbarMessages.emit("Started downloading $fileName")
        }
    }

    fun cancelDownload(downloadId: String) {
        downloadRepository.cancelDownload(downloadId)
    }

    fun dismissActiveDownload(downloadId: String) {
        downloadRepository.dismissActiveDownload(downloadId)
    }

    fun deleteCompletedDownload(record: DownloadRecordEntity) {
        viewModelScope.launch {
            downloadRepository.deleteDownload(record)
            _snackbarMessages.emit("Deleted ${record.fileName}")
        }
    }

    fun installApk(context: Context, filePath: String): InstallResult {
        val file = File(filePath)
        val result = ApkInstaller.installApk(context, file)
        if (result is InstallResult.Error) {
            viewModelScope.launch {
                _snackbarMessages.emit("Install error: ${result.message}")
            }
        }
        return result
    }

    fun openFile(context: Context, filePath: String, mimeType: String = "*/*") {
        val file = File(filePath)
        ApkInstaller.openFile(context, file, mimeType)
    }

    // In-App Update Actions
    fun checkForAppUpdates(force: Boolean = true) {
        inAppUpdateManager.checkForUpdates(force = force)
    }

    fun startAppUpdateDownload(config: UpdateConfig) {
        inAppUpdateManager.startDownload(config)
    }

    fun cancelAppUpdateDownload() {
        inAppUpdateManager.cancelDownload()
    }

    fun dismissAppUpdateDialog() {
        inAppUpdateManager.dismissDialog()
    }

    fun installAppUpdate(context: Context, apkFile: File): InstallResult {
        return inAppUpdateManager.installUpdate(context, apkFile)
    }

    override fun onCleared() {
        super.onCleared()
        inAppUpdateManager.cleanUp()
    }
}
