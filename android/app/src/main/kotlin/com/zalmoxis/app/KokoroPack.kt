package com.zalmoxis.app

import android.content.Context
import java.io.File
import java.net.URL
import java.security.MessageDigest
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal object KokoroPack {
    const val BYTES = 295_000_000L // rounded estimate for display
    private const val MAX_BYTES = 320_000_000L
    private const val SHA = "e7b931ee5d4b7104012a838c9eee240d19ea69cb3ca269041a3274f83dbb7fa1"
    private const val URL_STRING = "https://huggingface.co/litert-community/Kokoro-82M/resolve/main/kokoro_int8_fp16.litertlm"
    private val lock = Mutex()
    fun root(context: Context) = File(context.filesDir, "kokoro-litert")
    fun model(context: Context) = File(root(context), "kokoro_int8_fp16.litertlm")
    fun cache(context: Context) = File(context.cacheDir, "kokoro-litert")
    fun installed(context: Context) = model(context).isFile && model(context).length() in 200_000_000L..MAX_BYTES &&
        runCatching { File(root(context), ".verified").readText() == SHA }.getOrDefault(false)
    suspend fun install(context: Context, progress: (Long) -> Unit) = lock.withLock {
        withContext(Dispatchers.IO) {
            if (installed(context)) return@withContext
            check(context.filesDir.usableSpace > 400_000_000L) { "Allow at least 400 MB free space for setup." }
            val dir = root(context).apply { mkdirs() }
            val temp = File(dir, "model.part")
            try {
                val digest = MessageDigest.getInstance("SHA-256")
                val connection = URL(URL_STRING).openConnection().apply { connectTimeout = 20000; readTimeout = 30000 }
                var count = 0L
                connection.getInputStream().use { input ->
                    temp.outputStream().use { output ->
                        val buffer = ByteArray(65536)
                        while (true) {
                            ensureActive()
                            val n = input.read(buffer)
                            if (n < 0) break
                            count += n
                            check(count <= MAX_BYTES) { "Unexpected model size." }
                            digest.update(buffer, 0, n)
                            output.write(buffer, 0, n)
                            progress(count)
                        }
                    }
                }
                check(digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) } == SHA) { "Model checksum mismatch." }
                check(temp.renameTo(model(context))) { "Unable to activate model." }
                File(dir, ".verified").writeText(SHA)
                // Reclaim the obsolete Sherpa model only after the replacement is verified.
                File(context.filesDir, "kokoro-v1_1").deleteRecursively()
            } finally { temp.delete() }
        }
    }
    fun remove(context: Context): Boolean {
        val removed = root(context).deleteRecursively()
        cache(context).deleteRecursively()
        File(context.filesDir, "kokoro-v1_1").deleteRecursively()
        return removed
    }
}
