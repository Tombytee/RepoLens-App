package com.example

import com.example.data.model.UpdateConfig
import com.example.data.updater.ApkVerificationResult
import com.example.data.updater.ApkVerifier
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class InAppUpdateTest {

    @Test
    fun `parses valid complete update JSON successfully`() {
        val json = """
            {
              "versionCode": 3,
              "versionName": "1.2.0",
              "releaseNotes": "Improved scrolling performance, smoother repository browsing, and bug fixes.",
              "apkUrl": "https://github.com/Tombytee/RepoLens/releases/download/v1.2.0/repolens-v1.2.0.apk",
              "minSupportedVersionCode": 2,
              "forceUpdate": true,
              "sha256": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
            }
        """.trimIndent()

        val config = UpdateConfig.parseFromJson(json)
        assertNotNull(config)
        assertEquals(3, config!!.versionCode)
        assertEquals("1.2.0", config.versionName)
        assertEquals("Improved scrolling performance, smoother repository browsing, and bug fixes.", config.releaseNotes)
        assertEquals("https://github.com/Tombytee/RepoLens/releases/download/v1.2.0/repolens-v1.2.0.apk", config.apkUrl)
        assertEquals(2, config.minSupportedVersionCode)
        assertTrue(config.forceUpdate)
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", config.sha256)
    }

    @Test
    fun `parses update JSON with missing optional fields using safe defaults`() {
        val json = """
            {
              "versionCode": 2,
              "versionName": "1.1.0",
              "apkUrl": "https://github.com/Tombytee/RepoLens/releases/download/v1.1.0/app.apk"
            }
        """.trimIndent()

        val config = UpdateConfig.parseFromJson(json)
        assertNotNull(config)
        assertEquals(2, config!!.versionCode)
        assertEquals("1.1.0", config.versionName)
        assertEquals("https://github.com/Tombytee/RepoLens/releases/download/v1.1.0/app.apk", config.apkUrl)
        assertEquals(1, config.minSupportedVersionCode)
        assertFalse(config.forceUpdate)
        assertNull(config.sha256)
    }

    @Test
    fun `parses exact user JSON structure without sha256 field successfully`() {
        val json = """
            {
              "versionCode": 2,
              "versionName": "1.2",
              "releaseNotes": "Performance improvements and bug fixes.",
              "apkUrl": "https://github.com/Tombytee/kaamkaaj/releases/download/v1.2.4/KaamKaaj_1.2.4.apk",
              "minSupportedVersionCode": 1,
              "forceUpdate": false
            }
        """.trimIndent()

        val config = UpdateConfig.parseFromJson(json)
        assertNotNull(config)
        assertEquals(2, config!!.versionCode)
        assertEquals("1.2", config.versionName)
        assertEquals("Performance improvements and bug fixes.", config.releaseNotes)
        assertEquals("https://github.com/Tombytee/kaamkaaj/releases/download/v1.2.4/KaamKaaj_1.2.4.apk", config.apkUrl)
        assertEquals(1, config.minSupportedVersionCode)
        assertFalse(config.forceUpdate)
        assertNull("SHA-256 must be null when field is absent from JSON", config.sha256)
    }

    @Test
    fun `sanitizes placeholder or invalid sha256 strings`() {
        val json = """
            {
              "versionCode": 3,
              "versionName": "1.2.0",
              "apkUrl": "https://example.com/app.apk",
              "sha256": "REPLACE_WITH_THE_REAL_SHA256_HASH"
            }
        """.trimIndent()

        val config = UpdateConfig.parseFromJson(json)
        assertNotNull(config)
        assertNull("Placeholder SHA-256 should be sanitized to null", config!!.sha256)
    }

    @Test
    fun `rejects invalid update JSON configs`() {
        // Missing apkUrl
        val missingUrl = """{"versionCode": 2, "versionName": "1.1.0"}"""
        assertNull(UpdateConfig.parseFromJson(missingUrl))

        // Non-https URL
        val insecureUrl = """{"versionCode": 2, "versionName": "1.1.0", "apkUrl": "http://example.com/app.apk"}"""
        assertNull(UpdateConfig.parseFromJson(insecureUrl))

        // Zero or negative versionCode
        val invalidVersion = """{"versionCode": 0, "versionName": "1.1.0", "apkUrl": "https://example.com/app.apk"}"""
        assertNull(UpdateConfig.parseFromJson(invalidVersion))

        // Empty versionName
        val emptyVersionName = """{"versionCode": 2, "versionName": "   ", "apkUrl": "https://example.com/app.apk"}"""
        assertNull(UpdateConfig.parseFromJson(emptyVersionName))

        // Malformed JSON
        assertNull(UpdateConfig.parseFromJson("not json"))
    }

    @Test
    fun `version comparison logic behaves correctly`() {
        val config = UpdateConfig(
            versionCode = 3,
            versionName = "1.2.0",
            releaseNotes = "Notes",
            apkUrl = "https://example.com/app.apk",
            minSupportedVersionCode = 2,
            forceUpdate = false
        )

        // Version comparison
        assertTrue(config.isUpdateAvailable(currentVersionCode = 1))
        assertTrue(config.isUpdateAvailable(currentVersionCode = 2))
        assertFalse(config.isUpdateAvailable(currentVersionCode = 3))
        assertFalse(config.isUpdateAvailable(currentVersionCode = 4))

        // Mandatory checks:
        // currentVersion 1 < minSupportedVersionCode 2 -> Mandatory!
        assertTrue(config.isUpdateMandatory(currentVersionCode = 1))
        // currentVersion 2 == minSupportedVersionCode 2, forceUpdate = false -> Optional
        assertFalse(config.isUpdateMandatory(currentVersionCode = 2))
    }

    @Test
    fun `forceUpdate flag makes update mandatory regardless of minSupportedVersion`() {
        val config = UpdateConfig(
            versionCode = 3,
            versionName = "1.2.0",
            releaseNotes = "Critical fix",
            apkUrl = "https://example.com/app.apk",
            minSupportedVersionCode = 1,
            forceUpdate = true
        )

        assertTrue(config.isUpdateMandatory(currentVersionCode = 2))
    }

    @Test
    fun `computes correct sha256 checksum for file`() {
        runBlocking {
            val tempFile = File.createTempFile("update_test", ".apk")
            tempFile.deleteOnExit()
            val content = "RepoLens In-App Update Verification Payload".toByteArray()
            tempFile.writeBytes(content)

            val computedHash = ApkVerifier.computeSha256(tempFile)

            // Verify with Java's standard MessageDigest
            val digest = MessageDigest.getInstance("SHA-256")
            val expectedHash = digest.digest(content).joinToString("") { "%02x".format(it) }

            assertEquals(expectedHash, computedHash)
            tempFile.delete()
        }
    }

    @Test
    fun `detects sha256 checksum mismatch`() {
        runBlocking {
            val tempFile = File.createTempFile("update_mismatch_test", ".apk")
            tempFile.deleteOnExit()
            tempFile.writeBytes("Some corrupted APK content payload line ".repeat(50).toByteArray())

            val config = UpdateConfig(
                versionCode = 2,
                versionName = "1.1.0",
                releaseNotes = "Notes",
                apkUrl = "https://example.com/app.apk",
                sha256 = "0000000000000000000000000000000000000000000000000000000000000000"
            )

            // Note: verifyApk checks sha256 first before inspecting package manager
            // Context isn't needed if sha256 fails
            val mockContext = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
            val result = ApkVerifier.verifyApk(mockContext, tempFile, config)

            assertTrue(result is ApkVerificationResult.Invalid)
            assertTrue((result as ApkVerificationResult.Invalid).reason.contains("checksum mismatch", ignoreCase = true))
            tempFile.delete()
        }
    }

    @Test
    fun `rejects non-zip file or html document gracefully without invoking package parser`() {
        runBlocking {
            val htmlFile = File.createTempFile("html_fake_apk", ".apk")
            htmlFile.deleteOnExit()
            val htmlContent = """
                <!DOCTYPE html>
                <html lang="en">
                <head><title>GitHub: Where the world builds software</title></head>
                <body><h1>Welcome to GitHub</h1></body>
                </html>
            """.trimIndent().repeat(10) // make it over 1024 bytes
            htmlFile.writeText(htmlContent)

            assertFalse(ApkVerifier.isValidZipArchive(htmlFile))

            val config = UpdateConfig(
                versionCode = 2,
                versionName = "1.1.0",
                releaseNotes = "Notes",
                apkUrl = "https://github.com/",
                sha256 = null
            )

            val mockContext = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
            val result = ApkVerifier.verifyApk(mockContext, htmlFile, config)

            assertTrue(result is ApkVerificationResult.Invalid)
            val reason = (result as ApkVerificationResult.Invalid).reason
            assertTrue(reason.contains("HTML", ignoreCase = true) || reason.contains("not a valid", ignoreCase = true))
            htmlFile.delete()
        }
    }

    @Test
    fun `rejects zip archives missing AndroidManifest xml`() {
        runBlocking {
            val zipFile = File.createTempFile("non_apk_zip", ".apk")
            zipFile.deleteOnExit()

            // Create a valid zip file containing a high-entropy payload, but no AndroidManifest.xml
            java.util.zip.ZipOutputStream(java.io.FileOutputStream(zipFile)).use { zos ->
                val entry = java.util.zip.ZipEntry("sample.txt")
                zos.putNextEntry(entry)
                val payload = ByteArray(10240)
                java.util.Random(42).nextBytes(payload)
                zos.write(payload)
                zos.closeEntry()
            }

            assertTrue("Zip file size must be >= 1024 bytes, got ${zipFile.length()}", zipFile.length() >= 1024L)
            assertFalse("ZIP without AndroidManifest.xml must not be recognized as valid APK", ApkVerifier.isValidZipArchive(zipFile))
            zipFile.delete()
        }
    }
}
