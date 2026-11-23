package com.zalmoxis.app

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class LocalModel(
    val name: String,
    val file: File,
    val sizeBytes: Long,
)

object LocalModelCatalog {
    private const val MODEL_EXTENSION = ".litertlm"

    fun list(context: Context): List<LocalModel> {
        val directory = File(context.filesDir, "models")
        return directory.listFiles()
            .orEmpty()
            .filter { it.isFile && it.name.endsWith(MODEL_EXTENSION, ignoreCase = true) }
            .sortedBy { it.name.lowercase() }
            .map { LocalModel(it.name, it, it.length()) }
    }

    fun import(context: Context, uri: Uri): File {
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
        try {
            context.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "Could not open the selected model file." }
                FileOutputStream(temporary).use { output -> input.copyTo(output) }
            }
            require(temporary.length() > 0L) { "The selected file is empty." }

            val hadDestination = destination.exists()
            if (hadDestination) {
                check(destination.renameTo(backup)) { "Could not safely replace the existing model." }
            }
            if (!temporary.renameTo(destination)) {
                if (hadDestination) backup.renameTo(destination)
                error("Could not finish importing the model.")
            }
            backup.delete()
            return destination
        } finally {
            temporary.delete()
            if (backup.exists() && !destination.exists()) backup.renameTo(destination)
            backup.delete()
        }
    }

    fun delete(model: LocalModel): Boolean = model.file.delete()

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
