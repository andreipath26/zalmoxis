package com.zalmoxis.app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.security.MessageDigest
import java.util.Locale

/**
 * First Agent Reach-inspired public source slice: GitHub repository metadata and
 * raw GitHub text files. No API key, shell, cookies, model-driven tool execution,
 * arbitrary host crawling, redirects, or automatic Knowledge import.
 *
 * Hosts are fixed to GitHub's public endpoints to avoid arbitrary URL fetching.
 */
internal data class ResearchResult(
    val adapter: String,
    val requestedUrl: String,
    val canonicalUrl: String,
    val title: String,
    val text: String,
    val fetchedAtMillis: Long,
    val sha256: String,
    val bytesRead: Int,
    val truncated: Boolean,
)

/** Public GitHub search result. A search hit is not imported until explicitly selected. */
internal data class ResearchSearchHit(
    val title: String,
    val url: String,
    val description: String,
    val stars: Long,
    val updatedAt: String,
)

internal interface ResearchSourceAdapter {
    val id: String
    fun accepts(uri: URI): Boolean
    suspend fun fetch(uri: URI): ResearchResult
}

internal object NativeResearch {
    private val adapters: List<ResearchSourceAdapter> = listOf(
        GitHubRepositoryAdapter,
        GitHubRawTextAdapter,
    )
    fun supportedSources() = listOf(
        "Public GitHub repositories (github.com/owner/repo)",
        "Public GitHub raw text files (raw.githubusercontent.com/owner/repo/revision/path)",
    )

    /** User-approved topic search, limited to public repository metadata; no browser credentials. */
    suspend fun searchRepositories(topic: String): List<ResearchSearchHit> = withContext(Dispatchers.IO) {
        val query = topic.trim().replace(Regex("\\s+"), " ")
        require(query.length in 3..160) { "Enter a research topic between 3 and 160 characters." }
        require(query.none { it.isISOControl() }) { "The topic contains unsupported control characters." }
        val encoded = java.net.URLEncoder.encode(query, "UTF-8")
        val endpoint = "https://api.github.com/search/repositories?q=$encoded&per_page=8&sort=best-match"
        val bytes = fetchBytes(endpoint, "application/vnd.github+json", 384 * 1024)
        val json = JSONObject(bytes.toString(Charsets.UTF_8))
        val items = json.optJSONArray("items") ?: return@withContext emptyList()
        buildList {
            for (i in 0 until minOf(items.length(), 8)) {
                val item = items.optJSONObject(i) ?: continue
                if (item.optBoolean("private", false)) continue
                val url = item.optString("html_url")
                val uri = runCatching { parseUrl(url) }.getOrNull() ?: continue
                if (!GitHubRepositoryAdapter.accepts(uri)) continue
                add(
                    ResearchSearchHit(
                        title = item.optString("full_name").take(120),
                        url = url,
                        description = item.optString("description").take(500)
                            .takeUnless { it == "null" }.orEmpty(),
                        stars = item.optLong("stargazers_count"),
                        updatedAt = item.optString("updated_at").take(40),
                    )
                )
            }
        }
    }

    suspend fun fetch(userInput: String): ResearchResult = withContext(Dispatchers.IO) {
        val uri = parseUrl(userInput)
        val adapter = adapters.firstOrNull { it.accepts(uri) }
            ?: throw IllegalArgumentException(
                "This research channel supports public GitHub repositories and raw text files only."
            )
        adapter.fetch(uri)
    }

    private fun parseUrl(input: String): URI {
        val uri = try { URI(input.trim()) } catch (_: Exception) {
            throw IllegalArgumentException("Enter a complete HTTPS GitHub URL.")
        }
        // Reject dot segments before URI.normalize() could erase traversal evidence.
        require(uri.path.orEmpty().split('/').none { it == "." || it == ".." }) {
            "GitHub paths with dot or parent-directory segments are not allowed."
        }
        require(uri.scheme.equals("https", ignoreCase = true)) { "HTTPS is required." }
        require(uri.userInfo == null && uri.port == -1 && uri.rawFragment == null) {
            "URLs with credentials, custom ports or fragments are not supported."
        }
        require(uri.rawQuery.isNullOrBlank()) { "Research URLs cannot include query parameters." }
        require(uri.host != null && uri.host == uri.host.lowercase(Locale.ROOT)) {
            "Use an unambiguous lowercase public GitHub hostname."
        }
        val decoded = uri.path.split('/').drop(1)
        require(decoded.none { it == ".." || it == "." || it.isBlank() } && decoded.isNotEmpty()) {
            "Enter a complete GitHub repository or raw-file URL."
        }
        return uri
    }

    internal fun hostForConsent(userInput: String): String = parseUrl(userInput).host

    private object GitHubRepositoryAdapter : ResearchSourceAdapter {
        override val id = "github-repository"
        private val safeName = Regex("[A-Za-z0-9_.-]{1,100}")
        override fun accepts(uri: URI): Boolean {
            if (uri.host != "github.com") return false
            val parts = uri.path.trim('/').split('/')
            return parts.size == 2 && parts.all { safeName.matches(it) && it != "." && it != ".." }
        }
        override suspend fun fetch(uri: URI): ResearchResult {
            val parts = uri.path.trim('/').split('/')
            val canonical = "https://github.com/${parts[0]}/${parts[1]}"
            val endpoint = "https://api.github.com/repos/${parts[0]}/${parts[1]}"
            val bytes = fetchBytes(endpoint, "application/vnd.github+json", 128 * 1024)
            val record = JSONObject(bytes.toString(Charsets.UTF_8))
            require(!record.optBoolean("private", true)) { "This repository is not public." }
            require(record.optString("html_url") == canonical) {
                "GitHub returned a different canonical repository. Use its canonical URL and retry."
            }
            val name = record.optString("full_name", "${parts[0]}/${parts[1]}")
            val title = name.take(120)
            val description = record.optString("description").takeUnless { it == "null" }.orEmpty()
            val language = record.optString("language").takeUnless { it == "null" }.orEmpty()
            val topics = record.optJSONArray("topics")
            val topicNames = buildList {
                if (topics != null) for (i in 0 until topics.length()) add(topics.optString(i))
            }.joinToString(", ")
            val text = listOf(
                "Repository: $name",
                "Source URL: $canonical",
                "Description: $description",
                "Primary language: $language",
                "Topics: $topicNames",
                "Stars: ${record.optLong("stargazers_count")}",
                "Open issues: ${record.optLong("open_issues_count")}",
                "Default branch: ${record.optString("default_branch")}",
                "Last updated: ${record.optString("updated_at")}",
                "Note: This is public repository metadata, not a security audit or code review.",
            ).joinToString("\n")
            return result(id, uri.toString(), canonical, title, text, false)
        }
    }

    private object GitHubRawTextAdapter : ResearchSourceAdapter {
        override val id = "github-raw-text"
        private val safeSegment = Regex("[A-Za-z0-9_.@+-]{1,120}")
        override fun accepts(uri: URI): Boolean {
            if (uri.host != "raw.githubusercontent.com") return false
            val parts = uri.path.trim('/').split('/')
            return parts.size >= 4 && parts.size <= 25 &&
                parts.all { safeSegment.matches(it) } &&
                parts.last().lowercase(Locale.ROOT).let {
                    it.endsWith(".md") || it.endsWith(".txt") || it.endsWith(".json") ||
                        it.endsWith(".yaml") || it.endsWith(".yml") || it.endsWith(".csv")
                }
        }
        override suspend fun fetch(uri: URI): ResearchResult {
            val canonical = uri.toASCIIString()
            val bytes = fetchBytes(canonical, "text/plain", MAX_RAW_BYTES + 1)
            val truncated = bytes.size > MAX_RAW_BYTES
            val actual = if (truncated) bytes.copyOf(MAX_RAW_BYTES) else bytes
            require(actual.none { it == 0.toByte() }) {
                "The response appears to be binary. Only readable UTF-8 text is supported."
            }
            val text = try {
                Charsets.UTF_8.newDecoder()
                    .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                    .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
                    .decode(java.nio.ByteBuffer.wrap(actual))
                    .toString()
            } catch (_: Exception) {
                throw IllegalArgumentException("The file is not valid UTF-8 text.")
            }
            val title = uri.path.substringAfterLast('/').take(120)
            return result(id, canonical, canonical, title, text, truncated)
        }
    }

    private const val MAX_RAW_BYTES = 256 * 1024

    private fun result(
        adapter: String, input: String, canonical: String, title: String,
        text: String, truncated: Boolean,
    ): ResearchResult {
        val encoded = text.toByteArray(Charsets.UTF_8)
        val sha = MessageDigest.getInstance("SHA-256").digest(encoded)
            .joinToString("") { "%02x".format(it) }
        return ResearchResult(adapter, input, canonical, title, text,
            System.currentTimeMillis(), sha, encoded.size, truncated)
    }

    private fun fetchBytes(endpoint: String, accept: String, maxBytes: Int): ByteArray {
        val url = URL(endpoint)
        require(url.protocol == "https") { "HTTPS is required." }
        require(url.host == "api.github.com" || url.host == "raw.githubusercontent.com") {
            "External research host not allowed."
        }
        val conn = (url.openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = false
            connectTimeout = 10_000
            readTimeout = 15_000
            setRequestProperty("User-Agent", "Zalmoxis-Research/1")
            setRequestProperty("Accept", accept)
        }
        try {
            require(conn.responseCode == 200) {
                when (conn.responseCode) {
                    403, 429 -> "GitHub rate limit or access denial. Retry later."
                    404 -> "GitHub resource not found or not public."
                    in 300..399 -> "Research redirects are blocked. Use a canonical public URL."
                    else -> "GitHub returned HTTP ${conn.responseCode}."
                }
            }
            val type = conn.contentType.orEmpty().lowercase(Locale.ROOT)
            require(type.contains("json") || type.startsWith("text/") || type.contains("octet-stream")) {
                "Unsupported content type: $type"
            }
            conn.inputStream.use { stream ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(16 * 1024)
                while (output.size() < maxBytes) {
                    val read = stream.read(buffer, 0, minOf(buffer.size, maxBytes - output.size()))
                    if (read < 0) break
                    output.write(buffer, 0, read)
                }
                return output.toByteArray()
            }
        } finally {
            conn.disconnect()
        }
    }
}
