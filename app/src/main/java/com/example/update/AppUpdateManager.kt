package com.example.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URI
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class AppUpdateStatus {
    IDLE,
    UP_TO_DATE,
    UPDATE_AVAILABLE,
    ERROR
}

data class AppUpdateUiState(
    val currentVersion: String = BuildConfig.VERSION_NAME,
    val latestVersion: String = "",
    val releaseTitle: String = "",
    val releaseNotes: String = "",
    val publishedAt: String = "",
    val downloadUrl: String = AppUpdateManager.DEFAULT_RELEASES_PAGE_URL,
    val releasePageUrl: String = AppUpdateManager.DEFAULT_RELEASES_PAGE_URL,
    val apkSizeMb: Double? = null,
    val isChecking: Boolean = false,
    val status: AppUpdateStatus = AppUpdateStatus.IDLE,
    val errorMessage: String? = null,
    val lastCheckedAtMs: Long = 0L,
    val bannerDismissed: Boolean = false
) {
    val shouldShowBanner: Boolean
        get() = status == AppUpdateStatus.UPDATE_AVAILABLE && !bannerDismissed
}

sealed class AppUpdateCheckResult {
    data class Success(
        val currentVersion: String,
        val latestVersion: String,
        val isUpdateAvailable: Boolean,
        val releaseTitle: String,
        val releaseNotes: String,
        val publishedAt: String,
        val downloadUrl: String,
        val releasePageUrl: String,
        val apkSizeMb: Double?
    ) : AppUpdateCheckResult()

    data class Error(val message: String) : AppUpdateCheckResult()
}

/**
 * Play-Policy-compliant app update checker for Concord & Atoz Gas Trading LLC (C&A Gas).
 * Queries the public GitHub Releases API for the official repository and opens verified
 * HTTPS release links via standard browser Intent.ACTION_VIEW without requesting
 * REQUEST_INSTALL_PACKAGES or performing dynamic code loading.
 */
class AppUpdateManager(
    private val httpClient: OkHttpClient = defaultClient(),
    private val latestReleaseApiUrl: String = DEFAULT_LATEST_API_URL
) {

    suspend fun checkForUpdate(
        currentVersionName: String = BuildConfig.VERSION_NAME
    ): AppUpdateCheckResult = withContext(Dispatchers.IO) {
        val currentClean = normalizeVersion(currentVersionName).ifBlank { "1.0.0" }
        try {
            val request = Request.Builder()
                .url(latestReleaseApiUrl)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", "CA-Gas-Android/$currentClean")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val reason = when (response.code) {
                        404 -> "No published release found yet."
                        403, 429 -> "Update check rate limit reached. Please try again in a few minutes."
                        else -> "Update server returned HTTP ${response.code}."
                    }
                    return@withContext AppUpdateCheckResult.Error(reason)
                }

                val rawBody = response.body?.string().orEmpty()
                if (rawBody.isBlank()) {
                    return@withContext AppUpdateCheckResult.Error("Empty response from update server.")
                }

                val json = JSONObject(rawBody)
                val tagName = json.optString("tag_name", "").trim()
                if (tagName.isBlank()) {
                    return@withContext AppUpdateCheckResult.Error("Latest release tag is missing.")
                }

                val latestClean = normalizeVersion(tagName)
                val releaseTitle = json.optString("name", "").trim().ifBlank { "C&A Gas v$latestClean" }
                val releaseNotes = json.optString("body", "").trim()
                val publishedAt = json.optString("published_at", "").trim().take(10)

                val candidateHtmlUrl = json.optString("html_url", "").trim()
                val safeReleasePageUrl = if (isTrustedReleaseUrl(candidateHtmlUrl)) {
                    candidateHtmlUrl
                } else {
                    DEFAULT_RELEASES_PAGE_URL
                }

                var safeDownloadUrl = safeReleasePageUrl
                var apkSizeMb: Double? = null
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.optJSONObject(i) ?: continue
                        val assetName = asset.optString("name", "")
                        val candidateDownloadUrl = asset.optString("browser_download_url", "").trim()
                        if (assetName.lowercase(Locale.US).endsWith(".apk") &&
                            isTrustedReleaseUrl(candidateDownloadUrl)
                        ) {
                            safeDownloadUrl = candidateDownloadUrl
                            val bytes = asset.optLong("size", 0L)
                            if (bytes > 0L) {
                                apkSizeMb = (bytes.toDouble() / (1024.0 * 1024.0))
                            }
                            break
                        }
                    }
                }

                val isUpdateAvailable = compareSemVer(latestClean, currentClean) > 0
                AppUpdateCheckResult.Success(
                    currentVersion = currentClean,
                    latestVersion = latestClean,
                    isUpdateAvailable = isUpdateAvailable,
                    releaseTitle = releaseTitle,
                    releaseNotes = releaseNotes,
                    publishedAt = publishedAt,
                    downloadUrl = safeDownloadUrl,
                    releasePageUrl = safeReleasePageUrl,
                    apkSizeMb = apkSizeMb
                )
            }
        } catch (e: Exception) {
            AppUpdateCheckResult.Error(
                e.localizedMessage?.takeIf { it.isNotBlank() }
                    ?: "Unable to reach update server. Check internet connection."
            )
        }
    }

    companion object {
        const val REPO_OWNER = "atozgas26-star"
        const val REPO_NAME = "Concord-Atoz-gas"
        const val DEFAULT_LATEST_API_URL =
            "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases/latest"
        const val DEFAULT_RELEASES_PAGE_URL =
            "https://github.com/$REPO_OWNER/$REPO_NAME/releases/latest"
        private const val TRUSTED_RELEASES_PATH_PREFIX = "/$REPO_OWNER/$REPO_NAME/releases/"

        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        fun normalizeVersion(raw: String): String {
            return raw.trim()
                .removePrefix("v")
                .removePrefix("V")
                .substringBefore("-")
                .substringBefore("+")
                .trim()
        }

        /**
         * Returns > 0 if [remote] is newer than [current], 0 if equal, < 0 if older.
         */
        fun compareSemVer(remote: String, current: String): Int {
            val remoteParts = normalizeVersion(remote).split(".").mapNotNull { it.toIntOrNull() }
            val currentParts = normalizeVersion(current).split(".").mapNotNull { it.toIntOrNull() }
            val maxLen = maxOf(remoteParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val r = remoteParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (r != c) return r.compareTo(c)
            }
            return 0
        }

        /**
         * Strictly validates that an external update URL uses HTTPS on github.com
         * within the official Concord-Atoz-gas repository releases path.
         */
        fun isTrustedReleaseUrl(rawUrl: String): Boolean {
            if (rawUrl.isBlank()) return false
            return try {
                val uri = URI(rawUrl.trim())
                val schemeOk = uri.scheme?.equals("https", ignoreCase = true) == true
                val hostOk = uri.host?.equals("github.com", ignoreCase = true) == true
                val userInfoAbsent = uri.userInfo == null
                val path = uri.normalize().path.orEmpty()
                val pathOk = path.startsWith(TRUSTED_RELEASES_PATH_PREFIX) && !path.contains("..")
                schemeOk && hostOk && userInfoAbsent && pathOk
            } catch (_: Exception) {
                false
            }
        }

        /**
         * Opens a verified official GitHub Release URL in the device's default browser.
         */
        fun openTrustedUpdateUrl(context: Context, rawUrl: String): Boolean {
            val targetUrl = if (isTrustedReleaseUrl(rawUrl)) rawUrl.trim() else DEFAULT_RELEASES_PAGE_URL
            return try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                true
            } catch (_: Exception) {
                false
            }
        }
    }
}
