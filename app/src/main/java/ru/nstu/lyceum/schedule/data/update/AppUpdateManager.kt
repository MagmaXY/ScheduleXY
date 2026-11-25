package ru.nstu.lyceum.schedule.data.update

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.nstu.lyceum.schedule.data.repository.PreferencesRepository
import java.io.File
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

data class ReleaseInfo(
    val tagName: String,
    val releaseName: String,
    val changelog: String,
    val apkUrl: String,
    val apkName: String,
    val apkSize: Long,
    val sha256: String,
    val hasExactSha256: Boolean,
    val publishedAt: String
)

sealed class UpdateCheckResult {
    data object Skipped : UpdateCheckResult()
    data object UpToDate : UpdateCheckResult()
    data class UpdateAvailable(
        val release: ReleaseInfo,
        val isDismissed: Boolean
    ) : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

class AppUpdateManager(
    private val context: Context,
    private val prefs: PreferencesRepository
) {
    private val gson = Gson()
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    @Volatile
    private var cachedCurrentApkSha256: String? = null

    fun getCurrentVersionName(): String {
        return try {
            val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    android.content.pm.PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            pInfo.versionName ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }
    }

    fun getCurrentApkSha256(): String? {
        cachedCurrentApkSha256?.let { return it }
        val computed = computeFileSha256(File(context.applicationInfo.sourceDir))
        if (computed != null) {
            cachedCurrentApkSha256 = computed
        }
        return computed
    }

    fun getCachedAvailableRelease(): ReleaseInfo? {
        val raw = prefs.cachedAvailableReleaseJson ?: return null
        val release = try {
            gson.fromJson(raw, ReleaseInfo::class.java)
        } catch (_: Exception) {
            null
        } ?: return null

        // If user already installed this exact release, clear the cached prompt
        val currentSha = getCurrentApkSha256()
        if (release.hasExactSha256 && currentSha != null && currentSha.equals(release.sha256, ignoreCase = true)) {
            prefs.cachedAvailableReleaseJson = null
            prefs.dismissedUpdateHash = null
            return null
        }
        return release
    }

    fun dismissRelease(release: ReleaseInfo) {
        prefs.dismissedUpdateHash = release.sha256
    }

    fun cleanupOldUpdates() {
        try {
            val updatesDir = File(context.cacheDir, "updates")
            if (updatesDir.exists() && updatesDir.isDirectory) {
                updatesDir.listFiles()?.forEach { it.delete() }
            }
        } catch (_: Exception) {}
    }

    suspend fun checkForUpdate(forceManual: Boolean): UpdateCheckResult = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val currentSha = getCurrentApkSha256()

        // Ensure cached release is cleared if the installed APK already matches it
        getCachedAvailableRelease()

        if (!forceManual) {
            if (!prefs.autoUpdateCheckEnabled) {
                return@withContext UpdateCheckResult.Skipped
            }
            if (now - prefs.lastUpdateCheckTime < CHECK_INTERVAL_MS) {
                return@withContext UpdateCheckResult.Skipped
            }
        }

        val releaseResult = fetchReleaseFromApi().let { apiRes ->
            if (apiRes.isSuccess) apiRes
            else {
                val webRes = fetchReleaseFromWeb()
                if (webRes.isSuccess) webRes else apiRes
            }
        }

        val releaseInfo = releaseResult.getOrElse { err ->
            return@withContext UpdateCheckResult.Error(err.message ?: "Ошибка проверки обновлений")
        }

        prefs.lastUpdateCheckTime = now

        val isDifferent = if (releaseInfo.hasExactSha256 && currentSha != null) {
            !currentSha.equals(releaseInfo.sha256, ignoreCase = true)
        } else {
            val normalizedTag = releaseInfo.tagName.removePrefix("v").removePrefix("V").trim()
            val currentVersion = getCurrentVersionName().removePrefix("v").removePrefix("V").trim()
            normalizedTag.isNotBlank() && normalizedTag != currentVersion
        }

        if (isDifferent) {
            prefs.cachedAvailableReleaseJson = gson.toJson(releaseInfo)
            val isDismissed = !forceManual && prefs.dismissedUpdateHash == releaseInfo.sha256
            UpdateCheckResult.UpdateAvailable(releaseInfo, isDismissed = isDismissed)
        } else {
            prefs.cachedAvailableReleaseJson = null
            prefs.dismissedUpdateHash = null
            cleanupOldUpdates()
            UpdateCheckResult.UpToDate
        }
    }

    private fun fetchReleaseFromApi(): Result<ReleaseInfo> {
        return try {
            val request = Request.Builder()
                .url(GITHUB_LATEST_RELEASE_URL)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "ScheduleXY-Android")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return Result.failure(IllegalStateException("GitHub API: ${response.code}"))
                }
                val bodyStr = response.body?.string()
                    ?: return Result.failure(IllegalStateException("Пустой ответ от GitHub"))

                val root = JsonParser.parseString(bodyStr).asJsonObject
                val releaseId = root.get("id")?.asLong ?: 0L
                val tagName = root.get("tag_name")?.asString?.trim().orEmpty()
                val releaseName = root.get("name")?.asString?.trim().takeUnless { it.isNullOrBlank() } ?: tagName
                val changelog = root.get("body")?.asString?.trim().orEmpty()
                val publishedAt = root.get("published_at")?.asString?.trim().orEmpty()

                val assets = root.getAsJsonArray("assets")
                val apkAsset = assets?.firstOrNull { el ->
                    val name = el.asJsonObject.get("name")?.asString ?: ""
                    name.endsWith(".apk", ignoreCase = true)
                }?.asJsonObject
                    ?: return Result.failure(IllegalStateException("APK-файл не найден в последнем релизе"))

                val apkUrl = apkAsset.get("browser_download_url")?.asString
                    ?: return Result.failure(IllegalStateException("Не найдена ссылка на APK"))
                val apkName = apkAsset.get("name")?.asString ?: "ScheduleXY-update.apk"
                val apkSize = apkAsset.get("size")?.asLong ?: 0L
                val updatedAt = apkAsset.get("updated_at")?.asString ?: ""

                val rawDigest = apkAsset.get("digest")?.takeIf { !it.isJsonNull }?.asString?.trim()
                val hasExactSha256 = !rawDigest.isNullOrBlank() && rawDigest.startsWith("sha256:", ignoreCase = true)
                val releaseHash = if (hasExactSha256) {
                    rawDigest!!.substringAfter(":").trim().lowercase()
                } else {
                    "id:$releaseId|size:$apkSize|updated:$updatedAt"
                }

                Result.success(
                    ReleaseInfo(
                        tagName = tagName,
                        releaseName = releaseName,
                        changelog = changelog,
                        apkUrl = apkUrl,
                        apkName = apkName,
                        apkSize = apkSize,
                        sha256 = releaseHash,
                        hasExactSha256 = hasExactSha256,
                        publishedAt = publishedAt
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun fetchReleaseFromWeb(): Result<ReleaseInfo> {
        return try {
            val latestReq = Request.Builder()
                .url(GITHUB_WEB_LATEST_RELEASE_URL)
                .header("User-Agent", "Mozilla/5.0 (Android) ScheduleXY")
                .build()

            val (tagName, releaseHtml) = httpClient.newCall(latestReq).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return Result.failure(IllegalStateException("GitHub Web: ${resp.code}"))
                }
                val finalUrl = resp.request.url.toString()
                val rawTag = finalUrl.substringAfter("/releases/tag/", "")
                    .substringBefore("?")
                    .substringBefore("#")
                if (rawTag.isBlank()) {
                    return Result.failure(IllegalStateException("Релизы не найдены"))
                }
                val decodedTag = URLDecoder.decode(rawTag, "UTF-8")
                decodedTag to (resp.body?.string() ?: "")
            }

            val encodedTag = URLEncoder.encode(tagName, "UTF-8").replace("+", "%20")
            val assetsUrl = "$GITHUB_WEB_EXPANDED_ASSETS_BASE/$encodedTag"
            val assetsReq = Request.Builder()
                .url(assetsUrl)
                .header("User-Agent", "Mozilla/5.0 (Android) ScheduleXY")
                .build()

            val assetsHtml = httpClient.newCall(assetsReq).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return Result.failure(IllegalStateException("GitHub Assets: ${resp.code}"))
                }
                resp.body?.string() ?: ""
            }

            val apkHrefRegex = Regex("""href="(/MagmaXY/ScheduleXY/releases/download/[^"]+\.apk)"""", RegexOption.IGNORE_CASE)
            val apkMatch = apkHrefRegex.find(assetsHtml)
                ?: return Result.failure(IllegalStateException("APK-файл не найден в последнем релизе"))

            val apkPath = apkMatch.groupValues[1]
            val apkUrl = "https://github.com$apkPath"
            val apkName = URLDecoder.decode(apkPath.substringAfterLast("/"), "UTF-8")

            // Look for sha256 digest in the <li> block around this APK asset (or anywhere in expanded_assets)
            val liBlockStart = assetsHtml.lastIndexOf("<li", apkMatch.range.first).takeIf { it >= 0 } ?: 0
            val liBlockEnd = assetsHtml.indexOf("</li>", apkMatch.range.last).takeIf { it >= 0 } ?: assetsHtml.length
            val assetBlock = assetsHtml.substring(liBlockStart, liBlockEnd)

            val shaRegex = Regex("""sha256:([0-9a-fA-F]{64})""")
            val shaMatch = shaRegex.find(assetBlock) ?: shaRegex.find(assetsHtml)
            val hasExactSha256 = shaMatch != null
            val releaseHash = shaMatch?.groupValues?.get(1)?.lowercase() ?: "tag:$tagName"

            val changelog = extractChangelogFromHtml(releaseHtml)

            Result.success(
                ReleaseInfo(
                    tagName = tagName,
                    releaseName = tagName,
                    changelog = changelog,
                    apkUrl = apkUrl,
                    apkName = apkName,
                    apkSize = 0L,
                    sha256 = releaseHash,
                    hasExactSha256 = hasExactSha256,
                    publishedAt = ""
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractChangelogFromHtml(html: String): String {
        if (html.isBlank()) return ""
        val mdMatch = Regex(
            """<div[^>]*class="[^"]*markdown-body[^"]*"[^>]*>(.*?)</div>""",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
        ).find(html) ?: return ""

        val raw = mdMatch.groupValues[1]
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("</(p|div|li|h[1-6])>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<li[^>]*>", RegexOption.IGNORE_CASE), "• ")
            .replace(Regex("<[^>]+>"), "")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
        return raw
    }

    suspend fun downloadAndInstallApk(
        release: ReleaseInfo,
        onProgress: (Float) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            updatesDir.listFiles()?.forEach { it.delete() }

            val safeName = release.apkName.replace(Regex("[^a-zA-Z0-9._-]"), "_").ifBlank { "update.apk" }
            val destFile = File(updatesDir, safeName)

            val request = Request.Builder()
                .url(release.apkUrl)
                .header("User-Agent", "ScheduleXY-Android")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException("Ошибка загрузки: HTTP ${response.code}"))
                }
                val body = response.body
                    ?: return@withContext Result.failure(IllegalStateException("Пустой файл обновления"))

                val totalBytes = body.contentLength().takeIf { it > 0 } ?: release.apkSize
                val digest = MessageDigest.getInstance("SHA-256")
                var downloaded = 0L

                body.byteStream().use { input ->
                    destFile.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            digest.update(buffer, 0, read)
                            downloaded += read
                            if (totalBytes > 0) {
                                onProgress((downloaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f))
                            } else {
                                onProgress(-1f)
                            }
                        }
                    }
                }

                if (release.hasExactSha256) {
                    val downloadedSha = digest.digest().joinToString("") { "%02x".format(it) }
                    if (!downloadedSha.equals(release.sha256, ignoreCase = true)) {
                        destFile.delete()
                        return@withContext Result.failure(
                            IllegalStateException("Хэш скачанного APK не совпадает с релизом")
                        )
                    }
                }
            }

            withContext(Dispatchers.Main) {
                val contentUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    destFile
                )
                val installIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(contentUri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(installIntent)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun computeFileSha256(file: File): String? {
        return try {
            if (!file.exists() || !file.canRead()) return null
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        const val CHECK_INTERVAL_MS = 24L * 60L * 60L * 1000L // 24 hours
        private const val GITHUB_LATEST_RELEASE_URL =
            "https://api.github.com/repos/MagmaXY/ScheduleXY/releases/latest"
        private const val GITHUB_WEB_LATEST_RELEASE_URL =
            "https://github.com/MagmaXY/ScheduleXY/releases/latest"
        private const val GITHUB_WEB_EXPANDED_ASSETS_BASE =
            "https://github.com/MagmaXY/ScheduleXY/releases/expanded_assets"
    }
}
