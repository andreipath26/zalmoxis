package com.zalmoxis.app

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale

/** GitHub Actions uploads are ephemeral ZIP artifacts; only signed APK releases are installable updates. */
internal data class AppUpdate(
    val versionCode: Long,
    val tag: String,
    val name: String,
    val downloadUrl: String,
    val expectedSha256: String,
    val sizeBytes: Long,
)

internal object GitHubAppUpdates {
    private const val REPOSITORY = "andreipath26/zalmoxis"
    private const val RELEASES_API = "https://api.github.com/repos/$REPOSITORY/releases?per_page=30"
    private const val APK_NAME = "zalmoxis-update.apk"
    private const val BUILD_BASE = 100_000L
    private const val MAX_APK_BYTES = 800L * 1024L * 1024L
    private val tagPattern = Regex("android-build-([0-9]+)")

    fun installedVersion(context: Context): Long {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        return if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
    }

    /** Explicitly user-visible check, no GitHub authentication token or background telemetry. */
    suspend fun check(context: Context): AppUpdate? = withContext(Dispatchers.IO) {
        val releases = readJson(RELEASES_API)
        val currentVersion = installedVersion(context)
        val choices = ArrayList<AppUpdate>()
        for (i in 0 until releases.length()) {
            val release = releases.optJSONObject(i) ?: continue
            if (release.optBoolean("draft")) continue
            val tag = release.optString("tag_name")
            val runNumber = tagPattern.matchEntire(tag)?.groupValues?.get(1)?.toLongOrNull()
                ?: continue
            val expectedVersion = BUILD_BASE + runNumber
            if (expectedVersion <= currentVersion || expectedVersion > Int.MAX_VALUE) continue
            val assets = release.optJSONArray("assets") ?: continue
            for (j in 0 until assets.length()) {
                val asset = assets.optJSONObject(j) ?: continue
                if (asset.optString("name") != APK_NAME || asset.optString("state") != "uploaded") continue
                val url = asset.optString("browser_download_url")
                val digest = asset.optString("digest").removePrefix("sha256:").lowercase(Locale.ROOT)
                val size = asset.optLong("size")
                if (!isTrustedReleaseUrl(url, tag) || !digest.matches(Regex("[a-f0-9]{64}")) ||
                    size <= 0L || size > MAX_APK_BYTES
                ) continue
                choices += AppUpdate(expectedVersion, tag, release.optString("name", tag), url, digest, size)
            }
        }
        choices.maxByOrNull { it.versionCode }
    }

    suspend fun download(
        context: Context,
        update: AppUpdate,
        onProgress: (Int) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        require(isTrustedReleaseUrl(update.downloadUrl, update.tag)) { "Update URL is not trusted." }
        require(update.expectedSha256.matches(Regex("[a-f0-9]{64}")))
        val directory = File(context.cacheDir, "updates").apply { mkdirs() }
        val partial = File(directory, "pending.apk.part")
        val destination = File(directory, "zalmoxis-update.apk")
        partial.delete()
        val digest = MessageDigest.getInstance("SHA-256")
        try {
            openTrusted(update.downloadUrl).use { connection ->
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    error("GitHub returned HTTP ${connection.responseCode}.")
                }
                var received = 0L
                connection.inputStream.use { input ->
                    partial.outputStream().buffered().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            received += count
                            check(received <= MAX_APK_BYTES && received <= update.sizeBytes) {
                                "APK size exceeds the published release metadata."
                            }
                            digest.update(buffer, 0, count)
                            output.write(buffer, 0, count)
                            onProgress((received * 100L / update.sizeBytes).toInt().coerceIn(0, 100))
                        }
                    }
                }
                require(received == update.sizeBytes) { "Downloaded APK size does not match GitHub." }
            }
            val actualSha = digest.digest().joinToString("") { "%02x".format(it) }
            require(actualSha == update.expectedSha256) { "APK SHA-256 verification failed." }
            verifyInstallableUpdate(context, partial, update.versionCode)
            destination.delete()
            check(partial.renameTo(destination)) { "Could not stage the verified APK." }
            destination
        } catch (error: Throwable) {
            partial.delete()
            throw error
        }
    }

    @Suppress("DEPRECATION")
    private fun verifyInstallableUpdate(context: Context, apk: File, requiredVersion: Long) {
        val pm = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES
                    else PackageManager.GET_SIGNATURES
        val installed = pm.getPackageInfo(context.packageName, flags)
        val candidate = pm.getPackageArchiveInfo(apk.absolutePath, flags)
            ?: error("Downloaded file is not a valid Android APK.")
        require(candidate.packageName == context.packageName) { "Update package ID differs from the installed app." }
        val candidateVersion = if (Build.VERSION.SDK_INT >= 28) candidate.longVersionCode
                               else candidate.versionCode.toLong()
        require(candidateVersion == requiredVersion && candidateVersion > installedVersion(context)) {
            "Update version is not newer than the installed version or differs from its release tag."
        }
        fun signerDigests(info: android.content.pm.PackageInfo): Set<String> {
            val signatures = if (Build.VERSION.SDK_INT >= 28) {
                val signingInfo = info.signingInfo ?: error("Package signature information is missing.")
                if (signingInfo.hasMultipleSigners()) signingInfo.apkContentsSigners.toList()
                else signingInfo.signingCertificateHistory.toList()
            } else {
                info.signatures.toList()
            }
            return signatures.map { signature ->
                val bytes = MessageDigest.getInstance("SHA-256").digest(signature.toByteArray())
                bytes.joinToString("") { "%02x".format(it) }
            }.toSet()
        }
        require(signerDigests(installed).isNotEmpty() && signerDigests(installed) == signerDigests(candidate)) {
            "APK signing certificate differs from the installed app. Android cannot install it as an update."
        }
    }

    /** Repeat integrity and package verification immediately before the installer is invoked. */
    suspend fun verifyStaged(context: Context, apk: File, update: AppUpdate) = withContext(Dispatchers.IO) {
        require(apk.isFile && apk.parentFile == File(context.cacheDir, "updates")) {
            "The staged APK is no longer available."
        }
        require(apk.length() == update.sizeBytes) { "The staged APK changed size." }
        val digest = MessageDigest.getInstance("SHA-256")
        apk.inputStream().buffered().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        require(actual == update.expectedSha256) { "The staged APK failed integrity verification." }
        verifyInstallableUpdate(context, apk, update.versionCode)
    }

    /** Android installer is always user-initiated and retains the OS confirmation dialog. */
    fun install(context: Context, apk: File) {
        require(apk.isFile && apk.parentFile == File(context.cacheDir, "updates")) {
            "Only a verified staged APK may be installed."
        }
        if (Build.VERSION.SDK_INT >= 26 && !context.packageManager.canRequestPackageInstalls()) {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}"))
            )
            return
        }
        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.updates", apk,
        )
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun isTrustedReleaseUrl(url: String, tag: String): Boolean {
        val parsed = runCatching { URL(url) }.getOrNull() ?: return false
        return parsed.protocol == "https" &&
            parsed.host == "github.com" &&
            parsed.path == "/$REPOSITORY/releases/download/$tag/$APK_NAME"
    }

    private fun allowedHost(url: URL): Boolean = url.protocol == "https" && (
        url.host == "github.com" ||
            url.host == "release-assets.githubusercontent.com" ||
            url.host == "objects.githubusercontent.com" ||
            url.host == "github-releases.githubusercontent.com"
        )

    private class ManagedConnection(private val connection: HttpURLConnection) : AutoCloseable {
        val responseCode: Int get() = connection.responseCode
        val inputStream get() = connection.inputStream
        override fun close() = connection.disconnect()
    }

    private fun openTrusted(initial: String): ManagedConnection {
        var url = URL(initial)
        repeat(6) {
            require(allowedHost(url)) { "Untrusted update download redirect." }
            val connection = (url.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = 15_000
                readTimeout = 30_000
                setRequestProperty("User-Agent", "Zalmoxis-Android-Update")
                setRequestProperty("Accept", "application/octet-stream")
            }
            if (connection.responseCode in 300..399) {
                val target = connection.getHeaderField("Location")
                    ?: error("GitHub redirect did not include a destination.")
                url = URL(url, target)
                connection.disconnect()
            } else {
                return ManagedConnection(connection)
            }
        }
        error("Too many redirects downloading the update.")
    }

    private fun readJson(url: String): JSONArray {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "Zalmoxis-Android-Update")
        }
        return try {
            require(connection.responseCode == 200) {
                "Could not check GitHub releases (HTTP ${connection.responseCode})."
            }
            val json = connection.inputStream.bufferedReader().use { it.readText() }
            JSONArray(json)
        } finally {
            connection.disconnect()
        }
    }
}
