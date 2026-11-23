package com.zalmoxis.app

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID

data class LocalModel(
    val name: String,
    val file: File,
    val sizeBytes: Long,
    val sha256: String,
    val sourceDescription: String,
    val licenseStatus: String,
    val importedAtEpochMs: Long,
    val provenanceValid: Boolean = true,
)

object LocalModelCatalog {
    private const val MODEL_EXTENSION = ".litertlm"

    fun list(context: Context): List<LocalModel> {
        val directory = File(context.filesDir, "models")
        return directory.listFiles()
            .orEmpty()
            .filter { it.isFile && it.name.endsWith(MODEL_EXTENSION, ignoreCase = true) }
            .sortedBy { it.name.lowercase() }
            .map { file ->
                val metadata = receiptFile(file)
                if (!metadata.exists()) {
                    createLegacyReceipt(file).also { writeReceipt(file, it) }
                } else {
                    readReceipt(file) ?: LocalModel(
                        name = file.name,
                        file = file,
                        sizeBytes = file.length(),
                        sha256 = "",
                        sourceDescription = "Invalid provenance record; reimport required",
                        licenseStatus = "Unknown; reimport the model to review its source and terms",
                        importedAtEpochMs = file.lastModified(),
                        provenanceValid = false,
                    )
                }
            }
    }

    fun importFile(context: Context, uri: Uri): File {
        val directory = File(context.filesDir, "models").apply { mkdirs() }
        val sourceName = queryDisplayName(context, uri) ?: "local-model.litertlm"
        require(sourceName.endsWith(MODEL_EXTENSION, ignoreCase = true)) {
            "This runtime accepts .litertlm files."
        }
        val safeName = sourceName.substringAfterLast('/').substringAfterLast('\\').trim()
        require(safeName.isNotEmpty() && safeName != "." && safeName != "..") {
            "The selected model has an invalid file name."
        }

        val destination = File(directory, safeName)
        val temporary = File(directory, ".import-${UUID.randomUUID()}.partial")
        val backup = File(directory, ".backup-${UUID.randomUUID()}.partial")
        val receipt = receiptFile(destination)
        val receiptTemporary = File(directory, ".receipt-${UUID.randomUUID()}.partial")
        val receiptBackup = File(directory, ".receipt-backup-${UUID.randomUUID()}.partial")
        try {
            context.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "Could not open the selected model file." }
                FileOutputStream(temporary).use { output -> input.copyTo(output) }
            }
            require(temporary.length() > 0L) { "The selected file is empty." }
            val digest = sha256(temporary)
            val receiptContent = receiptJson(
                name = safeName,
                file = temporary,
                digest = digest,
                sourceDescription = "Selected through Android document picker",
                importedAtEpochMs = System.currentTimeMillis(),
            )

            val hadDestination = destination.exists()
            if (hadDestination) {
                check(destination.renameTo(backup)) { "Could not safely replace the existing model." }
            }
            val hadReceipt = receipt.exists()
            if (hadReceipt && !receipt.renameTo(receiptBackup)) {
                if (hadDestination) backup.renameTo(destination)
                error("Could not safely replace the model provenance record.")
            }
            if (!temporary.renameTo(destination)) {
                if (hadDestination) backup.renameTo(destination)
                if (hadReceipt) receiptBackup.renameTo(receipt)
                error("Could not finish importing the model.")
            }

            try {
                writeJsonAtomically(receipt, receiptTemporary, receiptContent)
            } catch (error: Exception) {
                destination.delete()
                if (hadDestination) backup.renameTo(destination)
                if (hadReceipt) receiptBackup.renameTo(receipt)
                throw error
            }
            backup.delete()
            receiptBackup.delete()
            return destination
        } finally {
            temporary.delete()
            receiptTemporary.delete()
            if (backup.exists() && !destination.exists()) backup.renameTo(destination)
            if (receiptBackup.exists() && !receipt.exists()) receiptBackup.renameTo(receipt)
            backup.delete()
            receiptBackup.delete()
        }
    }

    /**
     * Detects local changes since import. A local SHA-256 receipt does not authenticate a publisher.
     */
    fun verify(context: Context, file: File): String {
        val metadata = receiptFile(file)
        val receipt = if (metadata.exists()) {
            requireNotNull(readReceipt(file)) {
                "The model provenance record is invalid. Reimport the model to review it."
            }
        } else {
            createLegacyReceipt(file).also { writeReceipt(file, it) }
        }
        val actual = sha256(file)
        check(actual.equals(receipt.sha256, ignoreCase = true)) {
            "Model file changed since its provenance record was created. Reimport the file to review it."
        }
        return actual
    }

    fun delete(model: LocalModel): Boolean {
        if (!model.file.delete() && model.file.exists()) return false
        val receipt = receiptFile(model.file)
        if (!receipt.delete() && receipt.exists()) return false
        return true
    }


    /**
     * Installs an allowlisted catalog download only after rechecking its pinned size and digest.
     * The partial file is created under filesDir, so the final rename stays on one filesystem.
     */
    fun installDownloaded(context: Context, entry: ModelCatalogEntry, partial: File): File =
        synchronized(this) {
            require(entry.fileName.endsWith(MODEL_EXTENSION, ignoreCase = true))
            require('/' !in entry.fileName && '\\' !in entry.fileName)
            require(partial.isFile && matchesPinnedDigest(partial, entry.sizeBytes, entry.sha256)) {
                "The downloaded model failed its pinned size or SHA-256 check."
            }

            val directory = File(context.filesDir, "models").apply { mkdirs() }
            val destination = File(directory, entry.fileName)
            val receipt = receiptFile(destination)
            val backup = File(directory, ".download-backup-${UUID.randomUUID()}.partial")
            val receiptBackup = File(directory, ".download-receipt-backup-${UUID.randomUUID()}.partial")
            val receiptTemporary = File(directory, ".download-receipt-${UUID.randomUUID()}.partial")
            val receiptContent = JSONObject()
                .put("schema_version", 1)
                .put("file_name", entry.fileName)
                .put("file_size_bytes", entry.sizeBytes)
                .put("sha256", entry.sha256.lowercase())
                .put("source_description", "Hugging Face: ${entry.repoId} @ ${entry.revision}")
                .put("license_status", "${entry.licenseName} (${entry.licenseUrl})")
                .put("license_name", entry.licenseName)
                .put("license_url", entry.licenseUrl)
                .put("source_repository", entry.repoId)
                .put("source_revision", entry.revision)
                .put("imported_at_epoch_ms", System.currentTimeMillis())
                .put("publisher_authenticity_verified", false)

            var hadDestination = false
            var hadReceipt = false
            try {
                if (destination.exists()) {
                    check(destination.renameTo(backup)) { "Could not safely replace the existing model." }
                    hadDestination = true
                }
                if (receipt.exists()) {
                    check(receipt.renameTo(receiptBackup)) {
                        "Could not safely replace the model provenance record."
                    }
                    hadReceipt = true
                }
                check(partial.renameTo(destination)) { "Could not finish installing the model." }
                writeJsonAtomically(receipt, receiptTemporary, receiptContent)
                backup.delete()
                receiptBackup.delete()
                destination
            } catch (error: Exception) {
                destination.delete()
                receipt.delete()
                if (hadDestination && backup.exists()) backup.renameTo(destination)
                if (hadReceipt && receiptBackup.exists()) receiptBackup.renameTo(receipt)
                throw error
            } finally {
                receiptTemporary.delete()
                if (backup.exists() && !destination.exists()) backup.renameTo(destination)
                if (receiptBackup.exists() && !receipt.exists()) receiptBackup.renameTo(receipt)
                backup.delete()
                receiptBackup.delete()
            }
        }

    fun matchesPinnedDigest(file: File, expectedBytes: Long, expectedSha256: String): Boolean {
        if (!file.isFile || file.length() != expectedBytes ||
            !expectedSha256.matches(Regex("[a-fA-F0-9]{64}"))
        ) return false
        return runCatching { sha256(file).equals(expectedSha256, ignoreCase = true) }.getOrDefault(false)
    }

    private fun createLegacyReceipt(file: File): LocalModel =
        LocalModel(
            name = file.name,
            file = file,
            sizeBytes = file.length(),
            sha256 = sha256(file),
            sourceDescription = "Existing local model; original source not recorded",
            licenseStatus = "Unknown; check the publisher's terms",
            importedAtEpochMs = file.lastModified(),
        )

    private fun readReceipt(file: File): LocalModel? = runCatching {
        val json = JSONObject(receiptFile(file).readText())
        val digest = json.optString("sha256")
        require(digest.matches(Regex("[a-fA-F0-9]{64}")))
        LocalModel(
            name = file.name,
            file = file,
            sizeBytes = file.length(),
            sha256 = digest.lowercase(),
            sourceDescription = json.optString("source_description", "Source not recorded"),
            licenseStatus = json.optString(
                "license_status",
                "Unknown; check the publisher's terms",
            ),
            importedAtEpochMs = json.optLong("imported_at_epoch_ms", 0L),
        )
    }.getOrNull()

    private fun writeReceipt(file: File, model: LocalModel) {
        val temporary = File(file.parentFile, ".receipt-${UUID.randomUUID()}.partial")
        writeJsonAtomically(
            receiptFile(file),
            temporary,
            receiptJson(
                name = model.name,
                file = file,
                digest = model.sha256,
                sourceDescription = model.sourceDescription,
                importedAtEpochMs = model.importedAtEpochMs,
                licenseStatus = model.licenseStatus,
            ),
        )
    }

    private fun receiptJson(
        name: String,
        file: File,
        digest: String,
        sourceDescription: String,
        importedAtEpochMs: Long,
        licenseStatus: String = "Unknown; check the publisher's terms",
    ): JSONObject = JSONObject()
        .put("schema_version", 1)
        .put("file_name", name)
        .put("file_size_bytes", file.length())
        .put("sha256", digest.lowercase())
        .put("source_description", sourceDescription)
        .put("license_status", licenseStatus)
        .put("imported_at_epoch_ms", importedAtEpochMs)
        .put("publisher_authenticity_verified", false)

    private fun writeJsonAtomically(destination: File, temporary: File, json: JSONObject) {
        FileOutputStream(temporary).use { output ->
            output.write(json.toString(2).toByteArray(Charsets.UTF_8))
            output.fd.sync()
        }
        check(temporary.renameTo(destination)) { "Could not save model provenance." }
    }

    private fun receiptFile(file: File): File =
        File(file.parentFile, ".${file.name}.provenance.json")

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).buffered(128 * 1024).use { input ->
            val buffer = ByteArray(128 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                if (read > 0) digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { byte ->
            "%02x".format(byte.toInt() and 0xff)
        }
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) return cursor.getString(index)
                }
            }
        return null
    }
}
