package com.example.data.model

import org.json.JSONObject

data class UpdateConfig(
    val versionCode: Int,
    val versionName: String,
    val releaseNotes: String,
    val apkUrl: String,
    val minSupportedVersionCode: Int = 1,
    val forceUpdate: Boolean = false,
    val sha256: String? = null
) {
    /**
     * An update is available if the remote version code is strictly greater
     * than the locally installed app's version code.
     */
    fun isUpdateAvailable(currentVersionCode: Int): Boolean {
        return versionCode > currentVersionCode
    }

    /**
     * An update is mandatory if either:
     * 1. `forceUpdate` is true in the remote configuration, OR
     * 2. The local version code is strictly lower than `minSupportedVersionCode`.
     */
    fun isUpdateMandatory(currentVersionCode: Int): Boolean {
        return forceUpdate || currentVersionCode < minSupportedVersionCode
    }

    companion object {
        private val SHA256_REGEX = Regex("^[a-fA-F0-9]{64}$")

        /**
         * Safely parses and validates remote update configuration JSON.
         * Returns null if required fields are missing or invalid.
         */
        fun parseFromJson(jsonString: String): UpdateConfig? {
            return try {
                val json = JSONObject(jsonString)

                if (!json.has("versionCode") || !json.has("versionName") || !json.has("apkUrl")) {
                    return null
                }

                val versionCode = json.optInt("versionCode", -1)
                if (versionCode <= 0) return null

                val versionName = json.optString("versionName", "").trim()
                if (versionName.isEmpty()) return null

                val apkUrl = json.optString("apkUrl", "").trim()
                if (!apkUrl.startsWith("https://", ignoreCase = true)) return null

                val releaseNotes = json.optString("releaseNotes", "New update available.").trim()
                    .ifEmpty { "New update available." }

                val minSupportedVersionCode = json.optInt("minSupportedVersionCode", 1)
                val forceUpdate = json.optBoolean("forceUpdate", false)

                val rawSha256 = if (json.has("sha256") && !json.isNull("sha256")) {
                    json.optString("sha256", "").trim()
                } else {
                    ""
                }
                val sha256 = if (rawSha256.isNotEmpty() && SHA256_REGEX.matches(rawSha256)) {
                    rawSha256.lowercase()
                } else {
                    null
                }

                UpdateConfig(
                    versionCode = versionCode,
                    versionName = versionName,
                    releaseNotes = releaseNotes,
                    apkUrl = apkUrl,
                    minSupportedVersionCode = minSupportedVersionCode,
                    forceUpdate = forceUpdate,
                    sha256 = sha256
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}
