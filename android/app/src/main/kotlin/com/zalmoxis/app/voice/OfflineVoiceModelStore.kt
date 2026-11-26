package com.zalmoxis.app.voice

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

/**
 * App-private, user-approved speech model installation with SHA-256 verification.
 * Installers never perform HTTP requests or write to broad device storage.
 *
 * Model artifacts are provided as Uris chosen by the user (or packaged assets in a
 * separate integration). Native TTS/ASR engines load these files through adapters.
 */
internal object OfflineVoiceModelStore {
    data class Part(val filename: String, val bytes: Long, val sha256: String)
    data class Manifest(
        val id: String,
        val engine: String,
        val revision: String,
        val licenseId: String,
        val profile: SpeechStoragePolicy.Profile,
        val parts: List<Part>,
    ) {
        val bytes: Long get() = parts.sumOf { it.bytes }
    }

    data class Installed(val manifest: Manifest, val directory: File)

    private fun root(context: Context) = File(context.filesDir, "voice-models").apply { mkdirs() }
    private val safeToken = Regex("[a-zA-Z0-9][a-zA-Z0-9_.-]{0,95}")

    private fun validate(manifest: Manifest) {
        require(safeToken.matches(manifest.id) && safeToken.matches(manifest.engine)) { "Invalid model ID or engine." }
        require(manifest.revision.isNotBlank() && manifest.licenseId.isNotBlank()) { "Model revision/license required." }
        require(manifest.parts.isNotEmpty() && manifest.parts.size <= 32) { "Missing or excessive model parts." }
        require(manifest.parts.map { it.filename }.distinct().size == manifest.parts.size) { "Duplicate part name." }
        require(manifest.parts.all {
            safeToken.matches(it.filename) &&
                it.bytes > 0 &&
                it.sha256.matches(Regex("[a-fA-F0-9]{64}"))
        }) { "Invalid part manifest." }
    }

    fun installed(context: Context): List<Installed> =
        root(context).listFiles().orEmpty().filter { it.isDirectory && !it.name.endsWith(".staging") }
            .mapNotNull { folder ->
                runCatching {
                    val receipt = JSONObject(File(folder, "manifest.json").readText())
                    val partsJson = receipt.getJSONArray("parts")
                    val parts = (0 until partsJson.length()).map { i ->
                        val part = partsJson.getJSONObject(i)
                        Part(part.getString("filename"), part.getLong("bytes"), part.getString("sha256"))
                    }
                    val manifest = Manifest(
                        receipt.getString("id"), receipt.getString("engine"),
                        receipt.getString("revision"), receipt.getString("license"),
                        SpeechStoragePolicy.Profile.valueOf(receipt.getString("profile")), parts,
                    )
                    validate(manifest)
                    check(parts.all { File(folder, it.filename).isFile && File(folder, it.filename).length() == it.bytes })
                    Installed(manifest, folder)
                }.getOrNull()
            }

    suspend fun install(
        context: Context,
        manifest: Manifest,
        selectedUris: Map<String, Uri>,
        approved: Boolean,
    ): Installed = withContext(Dispatchers.IO) {
        validate(manifest)
        require(selectedUris.keys == manifest.parts.map { it.filename }.toSet()) {
            "Select every required model part exactly once."
        }
        val dataDir = root(context)
        val total = manifest.bytes
        val decision = SpeechStoragePolicy.mayInstall(
            manifest.profile, total, total, dataDir.usableSpace, approved,
        )
        require(decision.allowed) { decision.reason }
        val destination = File(dataDir, manifest.id)
        // A model cannot be installed over an active engine. The caller must unload
        // it before calling install; replace is intentionally not implicit.
        check(!destination.exists()) { "This voice model is already installed. Delete it explicitly before replacing." }
        val stage = File(dataDir, manifest.id + ".staging")
        check(!stage.exists()) { "A previous interrupted installation needs cleanup first." }
        check(stage.mkdir()) { "Cannot create model staging area." }
        try {
            manifest.parts.forEach { part ->
                val uri = requireNotNull(selectedUris[part.filename])
                val target = File(stage, part.filename)
                val digest = MessageDigest.getInstance("SHA-256")
                var copied = 0L
                val input = requireNotNull(context.contentResolver.openInputStream(uri)) { "Model source is unavailable." }
                input.use { source ->
                    target.outputStream().buffered().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val amount = source.read(buffer)
                            if (amount < 0) break
                            copied += amount
                            require(copied <= part.bytes) { "Model part is larger than its signed manifest." }
                            digest.update(buffer, 0, amount)
                            output.write(buffer, 0, amount)
                        }
                    }
                }
                require(copied == part.bytes) { "Model size mismatch for ${part.filename}." }
                val checksum = digest.digest().joinToString("") { "%02x".format(it) }
                require(checksum.equals(part.sha256, ignoreCase = true)) {
                    "Model hash mismatch for ${part.filename}."
                }
            }
            val parts = JSONArray()
            manifest.parts.forEach { p ->
                parts.put(JSONObject().put("filename", p.filename).put("bytes", p.bytes).put("sha256", p.sha256))
            }
            val json = JSONObject().put("id", manifest.id).put("engine", manifest.engine)
                .put("revision", manifest.revision).put("license", manifest.licenseId)
                .put("profile", manifest.profile.name).put("parts", parts)
            File(stage, "manifest.json").writeText(json.toString())
            check(stage.renameTo(destination)) { "Cannot atomically publish verified model." }
            Installed(manifest, destination)
        } catch (error: Exception) {
            stage.deleteRecursively()
            throw error
        }
    }

    suspend fun delete(context: Context, id: String): Boolean = withContext(Dispatchers.IO) {
        require(safeToken.matches(id)) { "Invalid model ID." }
        File(root(context), id).let { it.isDirectory && it.deleteRecursively() }
    }

    suspend fun cleanInterruptedInstalls(context: Context): Int = withContext(Dispatchers.IO) {
        root(context).listFiles().orEmpty().filter { it.isDirectory && it.name.endsWith(".staging") }
            .count { it.deleteRecursively() }
    }
}
