package com.example

import com.example.util.FileFormatters
import com.example.util.UrlParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testUrlParser_variousGitHubFormats() {
        // Standard HTTPS URL
        val res1 = UrlParser.parse("https://github.com/torvalds/linux")
        assertNotNull(res1)
        assertEquals("torvalds", res1?.owner)
        assertEquals("linux", res1?.repo)

        // Trailing slash
        val res2 = UrlParser.parse("https://github.com/JetBrains/kotlin/")
        assertNotNull(res2)
        assertEquals("JetBrains", res2?.owner)
        assertEquals("kotlin", res2?.repo)

        // Subpath / releases
        val res3 = UrlParser.parse("https://github.com/square/retrofit/releases")
        assertNotNull(res3)
        assertEquals("square", res3?.owner)
        assertEquals("retrofit", res3?.repo)

        // Tree branch
        val res4 = UrlParser.parse("https://github.com/flutter/flutter/tree/main/packages")
        assertNotNull(res4)
        assertEquals("flutter", res4?.owner)
        assertEquals("flutter", res4?.repo)

        // Without protocol
        val res5 = UrlParser.parse("github.com/termux/termux-app")
        assertNotNull(res5)
        assertEquals("termux", res5?.owner)
        assertEquals("termux-app", res5?.repo)

        // Shorthand owner/repo
        val res6 = UrlParser.parse("ankidroid/Anki-Android")
        assertNotNull(res6)
        assertEquals("ankidroid", res6?.owner)
        assertEquals("Anki-Android", res6?.repo)

        // Invalid inputs
        assertNull(UrlParser.parse(""))
        assertNull(UrlParser.parse("   "))
        assertNull(UrlParser.parse("justoneWord"))
        assertNull(UrlParser.parse("https://google.com/search"))
    }

    @Test
    fun testFileFormatters() {
        assertEquals("0 B", FileFormatters.formatBytes(0))
        assertEquals("1.0 KB", FileFormatters.formatBytes(1024))
        assertEquals("15.5 MB", FileFormatters.formatBytes(16252928))

        assertEquals("500", FileFormatters.formatNumber(500))
        assertEquals("1.5k", FileFormatters.formatNumber(1500))
        assertEquals("2.4M", FileFormatters.formatNumber(2400000))

        assertTrue(FileFormatters.isApk("app-debug.apk"))
        assertTrue(FileFormatters.isApk("v1.0.0_release.APK"))
        assertFalse(FileFormatters.isApk("archive.zip"))
        assertFalse(FileFormatters.isApk("README.md"))
    }
}
