package com.zalmoxis.app

import android.content.Context
import java.io.File
import java.net.URL
import java.security.MessageDigest
import kotlinx.coroutines.*
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream

internal object KokoroPack {
    const val BYTES = 147052523L
    private const val SHA = "259c9c8bc906d6078e4e0570421885dabb68b539c2f970fcbb890a58eeaf3e9c"
    private const val URL_STRING = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-int8-multi-lang-v1_1.tar.bz2"
    fun root(context: Context) = File(context.filesDir, "kokoro-v1_1")
    fun installed(context: Context) = valid(root(context)) && File(root(context), ".verified").readTextOrNull() == SHA
    private fun File.readTextOrNull() = runCatching { readText() }.getOrNull()
    private fun valid(dir: File) = listOf("model.int8.onnx", "voices.bin", "tokens.txt").all { File(dir,it).isFile && File(dir,it).length()>0 } && File(dir,"espeak-ng-data").isDirectory
    suspend fun install(context: Context, progress: (Long) -> Unit) = withContext(Dispatchers.IO) {
        val base = context.filesDir
        val stage = File(base,"kokoro-stage")
        val archive = File(base,"kokoro-download.part")
        check(base.usableSpace > 500_000_000L) { "At least 500 MB free space is needed during installation." }
        stage.deleteRecursively(); stage.mkdirs()
        try {
            val connection = URL(URL_STRING).openConnection().apply { connectTimeout=20000; readTimeout=30000 }
            val digest=MessageDigest.getInstance("SHA-256")
            var count=0L
            connection.getInputStream().use { input -> archive.outputStream().use { output ->
                val buffer=ByteArray(65536)
                while(true) {
                    ensureActive()
                    val n=input.read(buffer); if(n<0) break
                    count+=n; check(count<=BYTES) { "Unexpected model size." }
                    digest.update(buffer,0,n); output.write(buffer,0,n); progress(count)
                }
            } }
            check(count==BYTES && digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) }==SHA) { "Model checksum mismatch." }
            var expanded=0L
            TarArchiveInputStream(BZip2CompressorInputStream(archive.inputStream().buffered())).use { tar ->
                while(true) {
                    ensureActive()
                    val entry=tar.nextTarEntry ?: break
                    val relative=entry.name.removePrefix("./").substringAfter('/',"")
                    if(relative.isEmpty()) continue
                    val target=File(stage,relative).canonicalFile
                    check(target.path.startsWith(stage.canonicalPath + File.separator))
                    check(entry.isDirectory || entry.isFile && !entry.isSymbolicLink && !entry.isLink)
                    if(entry.isDirectory) target.mkdirs() else {
                        expanded+=entry.size; check(expanded<=400_000_000L)
                        target.parentFile?.mkdirs()
                        target.outputStream().use { output -> tar.copyTo(output) }
                    }
                }
            }
            check(valid(stage)) { "Incomplete Kokoro model." }
            File(stage,".verified").writeText(SHA)
            check(!root(context).exists() || root(context).deleteRecursively())
            check(stage.renameTo(root(context))) { "Unable to install model." }
        } finally { archive.delete(); stage.deleteRecursively() }
    }
    fun remove(context: Context) = root(context).deleteRecursively()
}
