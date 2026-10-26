package com.example.util

import java.util.Locale

object UrlParser {
    data class ParsedRepo(
        val owner: String,
        val repo: String
    )

    /**
     * Parses various formats of GitHub URLs or shorthand notation:
     * - https://github.com/owner/repo
     * - https://github.com/owner/repo/
     * - https://github.com/owner/repo/releases
     * - https://github.com/owner/repo/tree/main/...
     * - github.com/owner/repo
     * - owner/repo
     */
    fun parse(input: String?): ParsedRepo? {
        if (input.isNullOrBlank()) return null
        val trimmed = input.trim()

        // If it contains a protocol or domain, ensure it is from github.com
        if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            val lower = trimmed.lowercase(Locale.US)
            if (!lower.contains("github.com/")) {
                return null
            }
        }

        // Strip protocol and domains
        var cleaned = trimmed
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("git@github.com:")
            .removePrefix("github.com/")
            .removePrefix("www.github.com/")
            .removeSuffix(".git")
            .trim('/')

        // Now cleaned should start with "owner/repo"
        val segments = cleaned.split("/").filter { it.isNotBlank() }
        if (segments.size < 2) return null

        val owner = segments[0]
        val repo = segments[1]

        // GitHub usernames cannot contain dots or colons
        if (owner.contains(".") || owner.contains(":")) return null

        // Validate basic GitHub username/repo characters
        val validPattern = Regex("^[a-zA-Z0-9_.-]+$")
        if (!owner.matches(validPattern) || !repo.matches(validPattern)) {
            return null
        }

        return ParsedRepo(owner = owner, repo = repo)
    }
}

object FileFormatters {
    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val index = digitGroups.coerceIn(0, units.size - 1)
        val value = bytes / Math.pow(1024.0, index.toDouble())
        return String.format(Locale.US, "%.1f %s", value, units[index])
    }

    fun formatNumber(count: Int): String {
        return when {
            count >= 1_000_000 -> String.format(Locale.US, "%.1fM", count / 1_000_000.0)
            count >= 1_000 -> String.format(Locale.US, "%.1fk", count / 1_000.0)
            else -> count.toString()
        }
    }

    fun isApk(filename: String): Boolean {
        return filename.endsWith(".apk", ignoreCase = true)
    }

    fun isArchive(filename: String): Boolean {
        val lower = filename.lowercase(Locale.US)
        return lower.endsWith(".zip") || lower.endsWith(".tar.gz") || lower.endsWith(".tgz") || lower.endsWith(".7z")
    }

    fun getLanguageColor(language: String?): Long {
        return when (language?.lowercase(Locale.US)) {
            "kotlin" -> 0xFFA97BFF
            "java" -> 0xFFB07219
            "python" -> 0xFF3572A5
            "javascript" -> 0xFFF1E05A
            "typescript" -> 0xFF3178C6
            "c++", "cpp" -> 0xFFF34B7D
            "c" -> 0xFF555555
            "rust" -> 0xFFDEA584
            "go" -> 0xFF00ADD8
            "swift" -> 0xFFF05138
            "dart" -> 0xFF00B4AB
            "ruby" -> 0xFF701516
            "html" -> 0xFFE34C26
            "css" -> 0xFF563D7C
            "shell", "bash" -> 0xFF89E051
            else -> 0xFF58A6FF
        }
    }
}
