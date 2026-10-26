# RepoLens - Modern GitHub Repository Explorer & APK Installer

RepoLens is a native Android application built with Jetpack Compose and Material 3 that simplifies exploring public GitHub repositories for mobile users and beginners. It delivers rich repository insights, file trees, commits, releases, in-app file downloading with progress tracking, and direct APK installation via Android's PackageInstaller.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> The following design and architectural choices have been confirmed through initial clarification:

- **Home Screen Experience**: Starts with curated popular open-source repositories (e.g., Kotlin, Termux, NewPipe, Retrofit, Tachiyomi/Mihon, Blender, VSCode) alongside recent search history and a prominent URL paste bar with smart parsing.
- **GitHub API Rate Limits**: Operates unauthenticated by default (60 req/hr per IP) with graceful rate-limit detection, remaining quota badges, and an optional Personal Access Token (PAT) configuration in Settings for power users (up to 5,000 req/hr).
- **Download & Install Center**: Features a dedicated in-app "Downloads" destination with real-time download progress, file management, mime-type badges (clearly highlighting APKs), and Android standard APK installation flows (`FileProvider` + `ACTION_INSTALL_PACKAGE` / `ACTION_VIEW`).

---

### 1. Overview & Core Concept

- **What It Does**: Allows users to paste any GitHub repository URL (or `owner/repo` shorthand) and browse the repository through a clean, modern, native mobile UI. Users can view README markdown, repository metadata (stars, forks, watchers, open issues, license), directory file trees, file code viewers, release notes, and download release assets directly to device storage.
- **Target Persona**: Android power users, mobile developers, tech enthusiasts, and beginners looking to discover open-source projects or download Android APK releases directly without dealing with GitHub's complex desktop-oriented web layout.
- **Key Value**: Streamlines the journey from finding an open-source GitHub project to understanding what it does and installing its APK release in seconds, entirely within a native Android app.

---

### 2. User Experience & Visual Design

#### Navigation & Screen Structure
RepoLens uses an M3 Navigation Bar with three core tabs:
1. **Explore (Home & Search)**: Quick paste bar with clipboard auto-detect, curated featured projects (Android utilities, open-source apps, developer tools), and recent exploration history.
2. **Repository Detail**: Multi-tabbed comprehensive repository view:
   - **Overview**: Description, quick statistics badges (stars, forks, open issues, watchers, license), top programming languages with visual percentage bar, owner profile card, and beautifully formatted README with collapsible sections.
   - **Files & Code**: Interactive directory browser with folder drill-down, breadcrumbs, file sizes, and syntax-styled code view for text files.
   - **Releases**: Chronological releases, pre-release/latest tags, publication dates, formatted changelog/markdown notes, and assets list with prominent badges for `.apk` files and one-tap download.
   - **Activity & Community**: Recent commits list with commit messages and authors, top contributors list with avatars, and open issues/pull requests overview.
3. **Downloads & Library**: Active downloads with byte progress and speed, completed downloads archive, storage space indicators, one-tap APK installation trigger, file sharing, and deletion.
4. **Settings Dialog**: GitHub API token input with validation & quota counter, dark/light/system theme toggles, download folder preferences, and app info.

#### Visual Identity & Theme
- **Theme Direction**: Deep Tech Minimalist (inspired by modern developer tools like GitHub Mobile, Raycast, and Linear).
- **Color Palette**:
  - Dark Theme: Deep Slate/Charcoal background (`#0D1117`), elevated surfaces (`#161B22`), GitHub Emerald Primary Accent (`#2EA043`), Electric Azure for Links & Info (`#58A6FF`), Golden Amber for Stars (`#E3B341`), Vibrant Purple for APK Badges (`#8957E5`).
  - Light Theme: Clean Crisp White/Soft Gray (`#F6F8FA`), cards (`#FFFFFF`), Forest Green (`#1A7F37`), Sapphire Blue (`#0969DA`), warm accents.
- **Typography & Rhythm**: Roboto / System Sans with bold monospace accents for code snippets, commit SHAs, file sizes, and release tags.
- **Feedback & Animations**:
  - Tonal elevation on repository cards with fluid ripple responses.
  - Linear progress bars with byte-accurate download stats (`12.4 MB / 45.2 MB - 78%`).
  - Animated tab transitions and expandable changelog cards.

---

### 3. Key Product Decisions & Trade-Offs

- **Standard HTTP Streaming Downloader vs. Android DownloadManager**:
  - *Chosen*: Custom OkHttp streaming downloader with Room persistence and Notification/UI Flow updates.
  - *Why*: The system `DownloadManager` has known reliability issues on modern Android versions (API 30+ scoped storage restrictions, lack of fine-grained coroutine progress callbacks, and redirect issues with GitHub asset S3 pre-signed URLs). An OkHttp streaming downloader running in Kotlin coroutines gives real-time byte progress, precise error handling, and direct control over APK file handoff to `FileProvider`.
- **Markdown & Code Rendering**:
  - *Chosen*: Native Compose styled text parser supporting GitHub Markdown (headings, lists, bold, links, code blocks, tables, images via Coil).
  - *Why*: Eliminates sluggish WebViews and maintains consistent native theming, fast scrolling, and zero memory leaks.
- **Local Persistence via Room**:
  - *Chosen*: Room database to store:
    1. Search history & recent repositories
    2. Bookmarked/starred repositories for offline reference
    3. Downloaded files metadata (file path, size, repo name, version tag, completion timestamp)
- **APK Installation Flow**:
  - Uses `FileProvider` (`content://com.example.repolens.fileprovider/...`) and queries `context.packageManager.canRequestPackageInstalls()`. If permission is missing on Android 8.0+, directs the user seamlessly to the system "Install unknown apps" toggle (`Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES`) before completing the install prompt via `ACTION_INSTALL_PACKAGE` / `ACTION_VIEW`.

---

### 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                          RepoLens UI Layer                             │
│                                                                        │
│  ┌─────────────────────────┐  ┌─────────────────────────────────────┐  │
│  │   HomeScreen (Curated,  │  │        RepoDetailScreen             │  │
│  │   Search & History)     │  │  ┌───────────┬──────────┬────────┐  │  │
│  └────────────┬────────────┘  │  │ Overview  │  Files   │Releases│  │  │
│               │               │  └───────────┴──────────┴────────┘  │  │
│               │               └──────────────────┬──────────────────┘  │
│               │                                  │                     │
│  ┌────────────┴────────────┐  ┌──────────────────┴──────────────────┐  │
│  │  DownloadsManagerScreen │  │       Settings & Token Dialog       │  │
│  └────────────┬────────────┘  └──────────────────┬──────────────────┘  │
└───────────────┼──────────────────────────────────┼─────────────────────┘
                │                                  │
                ▼                                  ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        RepoLens ViewModel Layer                        │
│             (MainViewModel, RepoViewModel, DownloadViewModel)          │
└───────────────┬──────────────────────────────────┬─────────────────────┘
                │                                  │
                ▼                                  ▼
┌────────────────────────────────────────────────────────────────────────┐
│                            Repository Layer                            │
│  ┌──────────────────────────────────┐  ┌────────────────────────────┐  │
│  │         GitHubRepository         │  │      DownloadRepository    │  │
│  │  - Parses repo URLs / owner/repo │  │  - OkHttp stream buffer    │  │
│  │  - Manages API caching & headers │  │  - Byte progress updates   │  │
│  │  - Rate-limit state tracking     │  │  - FileProvider APK intent │  │
│  └──────────────────┬───────────────┘  └─────────────┬──────────────┘  │
└─────────────────────┼────────────────────────────────┼─────────────────┘
                      │                                │
                      ▼                                ▼
┌────────────────────────────────────────┐ ┌─────────────────────────────┐
│             Data Sources               │ │        Local Storage        │
│  ┌──────────────────────────────────┐  │ │  ┌────────────────────────┐ │
│  │       GitHub REST API v3         │  │ │  │      Room Database     │ │
│  │  - /repos/{owner}/{repo}         │  │ │  │ - Recent repos DAO     │ │
│  │  - /repos/{owner}/{repo}/readme  │  │ │  │ - Bookmarks DAO        │ │
│  │  - /repos/{owner}/{repo}/releases│  │ │  │ - Download history DAO │ │
│  │  - /repos/{owner}/{repo}/contents│  │ │  └────────────────────────┘ │
│  │  - /repos/{owner}/{repo}/commits │  │ │  ┌────────────────────────┐ │
│  │  - /rate_limit                   │  │ │  │ App Scoped File Storage│ │
│  └──────────────────────────────────┘  │ │  │ (context.getExternalFiles│
│                                        │ │  │  Dir / apks / assets)  │ │
│                                        │ │  └────────────────────────┘ │
└────────────────────────────────────────┘ └─────────────────────────────┘
```

#### Key Implementation Steps
1. **Manifest & Permissions Setup**:
   - Add `INTERNET`, `ACCESS_NETWORK_STATE`, and `REQUEST_INSTALL_PACKAGES` to `AndroidManifest.xml`.
   - Configure `FileProvider` with `file_paths.xml` mapping external files path for APK installations.
2. **Metadata & Branding**:
   - Update `metadata.json` with app name "RepoLens" and comprehensive description.
   - Update `res/values/strings.xml` with `app_name = "RepoLens"` and localized resource strings.
   - Generate custom vector/adaptive launcher icon.
3. **Data Layer & GitHub API**:
   - Implement Retrofit interface for GitHub REST API (`RepoDto`, `ReleaseDto`, `AssetDto`, `ContentDto`, `CommitDto`, `ContributorDto`).
   - Implement Moshi JSON converters with null-safety and ISO date handling.
   - Build `GitHubRepository` with support for optional Auth token header (`Authorization: Bearer <token>`).
4. **Room Database**:
   - Define entities: `RecentRepoEntity`, `BookmarkEntity`, `DownloadRecordEntity`.
   - Setup `AppDatabase` and DAOs with reactive `Flow` outputs.
5. **Downloader Engine & APK Installer**:
   - OkHttp streaming download worker that writes chunk-by-chunk to app-specific external storage, emitting progress flows.
   - Standard Android APK install helper using `FileProvider.getUriForFile` and Intent launch.
6. **Compose UI Components**:
   - Top App Bar with repository quick-search & rate-limit status indicator.
   - Overview Tab with badges, languages meter, and native README parser.
   - Tree/File Browser with breadcrumbs and syntax view.
   - Releases Tab highlighting `.apk` assets with direct download actions.
   - Downloads Screen with progress meters, cancel/delete, and "Install APK" buttons.
   - Settings sheet for token entry and rate-limit verification.
7. **Testing & Verification**:
   - Compile and verify with `compile_applet`.
   - Validate with Robolectric unit tests for URL parsing, entity persistence, and state management.
