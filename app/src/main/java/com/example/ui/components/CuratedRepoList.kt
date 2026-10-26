package com.example.ui.components

data class CuratedRepo(
    val owner: String,
    val name: String,
    val description: String,
    val category: String,
    val language: String,
    val starsDisplay: String,
    val hasApkReleases: Boolean = false
)

val CURATED_REPOSITORIES = listOf(
    CuratedRepo(
        owner = "termux",
        name = "termux-app",
        description = "Terminal emulator application for Android OS with extensive Linux package ecosystem.",
        category = "Android Apps",
        language = "Java",
        starsDisplay = "40k+",
        hasApkReleases = true
    ),
    CuratedRepo(
        owner = "ankidroid",
        name = "Anki-Android",
        description = "AnkiDroid: Powerful, intelligent flashcards spaced repetition app for Android.",
        category = "Android Apps",
        language = "Kotlin",
        starsDisplay = "9k+",
        hasApkReleases = true
    ),
    CuratedRepo(
        owner = "TeamNewPipe",
        name = "NewPipe",
        description = "A libre lightweight streaming frontend for Android that does not use Google Play Services.",
        category = "Android Apps",
        language = "Java",
        starsDisplay = "31k+",
        hasApkReleases = true
    ),
    CuratedRepo(
        owner = "JetBrains",
        name = "kotlin",
        description = "The Kotlin Programming Language developed by JetBrains.",
        category = "Developer Tools",
        language = "Kotlin",
        starsDisplay = "49k+"
    ),
    CuratedRepo(
        owner = "square",
        name = "retrofit",
        description = "A type-safe HTTP client for Android and the JVM by Square.",
        category = "Libraries",
        language = "Kotlin",
        starsDisplay = "43k+"
    ),
    CuratedRepo(
        owner = "signalapp",
        name = "Signal-Android",
        description = "A private messenger for Android with end-to-end encryption.",
        category = "Android Apps",
        language = "Kotlin",
        starsDisplay = "26k+",
        hasApkReleases = true
    ),
    CuratedRepo(
        owner = "flutter",
        name = "flutter",
        description = "Flutter makes it easy and fast to build beautiful apps for mobile and beyond.",
        category = "Developer Tools",
        language = "Dart",
        starsDisplay = "168k+"
    )
)
