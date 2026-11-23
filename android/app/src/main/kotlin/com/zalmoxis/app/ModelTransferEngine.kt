package com.zalmoxis.app

import android.content.Context
import android.os.StatFs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Shared verified transfer path for WorkManager and the API 34+ user-initiated job adapter. */
object ModelTransferEngine {
    data class Progress(val entry: ModelCatalogEntry, val receivedBytes: Long)

    suspend fun download(
        context: Context,
        catalogId: String,
        onProgress: suspend (Progress) -> Unit = {},
    ): File {
        val entry = ModelCatalog.find(catalogId)
            ?: throw InvalidTransferException("This model is not in the approved catalog.")
        requireSafeEntry(entry)
        val staging = File(context.filesDir, "model-downloads").apply { mkdirs() }
        val partial = File(staging, "${entry.id}.part")
        checkFreeSpace(context, partial, entry.sizeBytes)
        transfer(entry, partial, onProgress)
        return LocalModelCatalog.installDownloaded(context, entry, partial)
    }

    private suspend fun transfer(
        entry: ModelCatalogEntry,
        partial: File,
        onProgress: suspend (Progress) -> Unit,
    ) = withContext(Dispatchers.IO) {
        if (partial.length() > entry.sizeBytes) partial.delete()
        val offset = partial.length()
        val connection = connect(entry.downloadUrl, offset)
        try {
            val status = connection.responseCode
            if (status == 416) {
                if (partial.length() == entry.sizeBytes &&
                    LocalModelCatalog.matchesPinnedDigest(partial, entry.sizeBytes, entry.sha256)
                ) return@withContext
                partial.delete()
                throw IOException("The server rejected the resume range; retrying from byte zero.")
            }
            if (status == 408 || status == 429 || status >= 500) {
                throw IOException("Temporary model server error (HTTP $status).")
            }
            if (status in setOf(400, 401, 403, 404, 410)) {
                throw InvalidTransferException("The model source rejected the download (HTTP $status).")
            }
            val append = when (status) {
                HttpURLConnection.HTTP_PARTIAL -> {
                    val range = parseRange(connection.getHeaderField("Content-Range"))
                    if (range.first != offset || range.third != entry.sizeBytes || range.second < range.first) {
                        throw InvalidTransferException("The server returned an invalid resume range.")
                    }
                    true
                }
                HttpURLConnection.HTTP_OK -> {
                    partial.delete()
                    false
                }
                else -> throw InvalidTransferException("Unexpected model server response (HTTP $status).")
            }
            val expectedResponseBytes = entry.sizeBytes - if (append) offset else 0L
            val responseBytes = connection.getHeaderFieldLong("Content-Length", -1L)
            if (responseBytes >= 0 && responseBytes != expectedResponseBytes) {
                throw InvalidTransferException("The server returned an unexpected file length.")
            }

            var received = if (append) offset else 0L
            connection.inputStream.buffered(BUFFER_BYTES).use { input ->
                FileOutputStream(partial, append).buffered(BUFFER_BYTES).use { output ->
                    val buffer = ByteArray(BUFFER_BYTES)
                    var lastUpdate = 0L
                    while (true) {
                        if (!kotlin.coroutines.coroutineContext.isActive) throw CancellationException("Download cancelled")
                        val count = input.read(buffer)
                        if (count < 0) break
                        if (count == 0) continue
                        received += count
                        if (received > entry.sizeBytes) {
                            throw InvalidTransferException("The server sent more bytes than expected.")
                        }
                        output.write(buffer, 0, count)
                        val now = System.currentTimeMillis()
                        if (now - lastUpdate >= PROGRESS_INTERVAL_MS) {
                            onProgress(Progress(entry, received))
                            lastUpdate = now
                        }
                    }
                }
            }
            if (partial.length() != entry.sizeBytes) {
                throw IOException("The transfer ended early; its partial file will be resumed.")
            }
            onProgress(Progress(entry, entry.sizeBytes))
        } finally {
            connection.disconnect()
        }
        if (!LocalModelCatalog.matchesPinnedDigest(partial, entry.sizeBytes, entry.sha256)) {
            throw InvalidTransferException("The downloaded model failed its pinned size or SHA-256 check.")
        }
    }

    private fun connect(initial: String, offset: Long): HttpURLConnection {
        var current = URL(initial)
        for (redirect in 0..MAX_REDIRECTS) {
            requireApprovedHost(current)
            val connection = (current.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                requestMethod = "GET"
                setRequestProperty("Accept-Encoding", "identity")
                setRequestProperty("User-Agent", "Zalmoxis-Android/1.0")
                if (offset > 0) setRequestProperty("Range", "bytes=$offset-")
            }
            val status = connection.responseCode
            if (status !in REDIRECT_CODES) return connection
            val location = connection.getHeaderField("Location")
            connection.disconnect()
            if (location.isNullOrBlank() || redirect == MAX_REDIRECTS) {
                throw InvalidTransferException("The model source returned an invalid redirect.")
            }
            current = URL(current, location)
        }
        throw InvalidTransferException("Too many redirects from the model source.")
    }

    private fun requireApprovedHost(url: URL) {
        val host = url.host.lowercase()
        val approved = host == "huggingface.co" || host.endsWith(".huggingface.co") ||
            host == "hf.co" || host.endsWith(".hf.co")
        if (url.protocol != "https" || url.userInfo != null || !approved) {
            throw InvalidTransferException("The model source redirected to an unapproved HTTPS host.")
        }
    }

    private fun parseRange(value: String?): Triple<Long, Long, Long> {
        val match = RANGE_PATTERN.matchEntire(value.orEmpty())
            ?: throw InvalidTransferException("The server omitted a valid Content-Range.")
        return Triple(match.groupValues[1].toLong(), match.groupValues[2].toLong(), match.groupValues[3].toLong())
    }

    private fun checkFreeSpace(context: Context, partial: File, expected: Long) {
        val remaining = (expected - partial.length()).coerceAtLeast(0L)
        val available = StatFs(context.filesDir.absolutePath).availableBytes
        if (available < remaining + STORAGE_MARGIN) {
            throw NoSpaceException("Free at least ${formatBytes(remaining + STORAGE_MARGIN)} to download this model.")
        }
    }

    private fun requireSafeEntry(entry: ModelCatalogEntry) {
        val safe = entry.id.matches(Regex("[a-z0-9][a-z0-9.-]{0,79}")) &&
            entry.fileName.endsWith(".litertlm", ignoreCase = true) &&
            '/' !in entry.fileName && '\\' !in entry.fileName &&
            entry.repoId.matches(Regex("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")) &&
            entry.revision.matches(Regex("[a-fA-F0-9]{40}")) &&
            entry.sizeBytes > 0L &&
            entry.sha256.matches(Regex("[a-fA-F0-9]{64}")) &&
            entry.downloadUrl == "https://huggingface.co/${entry.repoId}/resolve/${entry.revision}/${entry.fileName}?download=true"
        if (!safe) throw InvalidTransferException("Catalog metadata is invalid.")
    }

    private const val MAX_REDIRECTS = 8
    private const val CONNECT_TIMEOUT_MS = 30_000
    private const val READ_TIMEOUT_MS = 60_000
    private const val BUFFER_BYTES = 128 * 1024
    private const val PROGRESS_INTERVAL_MS = 500L
    private const val STORAGE_MARGIN = 128L * 1024 * 1024
    private val REDIRECT_CODES = setOf(301, 302, 303, 307, 308)
    private val RANGE_PATTERN = Regex("bytes ([0-9]+)-([0-9]+)/([0-9]+)")
}

internal class InvalidTransferException(message: String) : Exception(message)
internal class NoSpaceException(message: String) : Exception(message)

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var index = -1
    do {
        value /= 1024.0
        index++
    } while (value >= 1024.0 && index < units.lastIndex)
    return String.format(java.util.Locale.ROOT, "%.1f %s", value, units[index])
}
