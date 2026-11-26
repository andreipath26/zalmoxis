package com.zalmoxis.app

import android.content.Context
import java.io.File
import java.net.URL
import java.security.MessageDigest
import java.util.Locale
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal enum class VoiceMode(val label:String) {
    AUTO("Intelligent auto"), QUALITY("Maximum quality"), FAST("Instant response"), SAVER("Battery saver");
    companion object { fun read(value:String?) = entries.firstOrNull { it.name == value } ?: AUTO }
}
internal data class VoicePack(val id:String,val title:String,val repo:String,val file:String,val sha:String,val mb:Int,val voices:List<String>)
internal object ExtraVoicePacks {
    val phonon = VoicePack("phonon","Phonon · low latency","phonon-31m-en","phonon_int8.litertlm","b33a9a0d65acec36762db38a2bff6d899f4aab34b196bb56d89fab5665eabebe",88,listOf("freya","claire","lukas","sofia","lucas"))
    val pocket = VoicePack("pocket","PocketTTS · natural speech","pocket-tts","pocket_tts_en_int8.litertlm","465e0083cd61188772aed76160b07af0dcf59057497d7659f43f3f2b13a174a1",163,listOf("alba","marius","javert","jean","fantine","cosette","eponine","azelma"))
    val all = listOf(phonon,pocket)
    private val lock=Mutex()
    fun available(locale:Locale)=if(locale.language=="en") all else emptyList()
    private fun dir(c:Context,p:VoicePack)=File(c.filesDir,"voice-packs/"+p.id)
    fun model(c:Context,p:VoicePack)=File(dir(c,p),p.file)
    fun installed(c:Context,p:VoicePack)=model(c,p).isFile && model(c,p).length() in 1L..(p.mb+5L)*1000000 && runCatching { File(dir(c,p),".verified").readText()==p.sha }.getOrDefault(false)
    suspend fun install(c:Context,p:VoicePack,progress:(Long)->Unit)=lock.withLock {
        withContext(Dispatchers.IO) {
            if(installed(c,p)) return@withContext
            check(c.filesDir.usableSpace>(p.mb+80L)*1000000) { "Not enough free storage." }
            val root=dir(c,p).apply { mkdirs() }
            val tmp=File(root,"download.part")
            try {
                val hash=MessageDigest.getInstance("SHA-256")
                val connection=URL("https://huggingface.co/litert-community/"+p.repo+"/resolve/main/"+p.file).openConnection().apply { connectTimeout=20000;readTimeout=30000 }
                var count=0L
                connection.getInputStream().use { input -> tmp.outputStream().use { output ->
                    val b=ByteArray(65536)
                    while(true) {
                        ensureActive()
                        val n=input.read(b);if(n<0)break
                        count+=n;check(count<=(p.mb+5L)*1000000)
                        hash.update(b,0,n);output.write(b,0,n);progress(count)
                    }
                } }
                check(hash.digest().joinToString(""){"%02x".format(it.toInt() and 255)}==p.sha) { "Model checksum mismatch." }
                check(tmp.renameTo(model(c,p))) { "Could not activate model." }
                File(root,".verified").writeText(p.sha)
            } finally { tmp.delete() }
        }
    }
    fun remove(c:Context,p:VoicePack)=dir(c,p).deleteRecursively()
    fun ordered(mode:VoiceMode, preferred:String?, installed:List<VoicePack>):List<VoicePack> =
        installed.sortedBy { if(it.id==preferred) -1 else if(mode==VoiceMode.QUALITY) { if(it.id=="pocket") 0 else 1 } else { if(it.id=="phonon") 0 else 1 } }
}
