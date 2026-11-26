package com.zalmoxis.app

import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*

@Composable internal fun KokoroSettings(enabled:Boolean, preferred:Boolean, locale:java.util.Locale, voiceId:String?, onVoice:(String)->Unit, onPreferred:(Boolean)->Unit) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    var installed by remember { mutableStateOf(KokoroPack.installed(context)) }
    var job by remember { mutableStateOf<Job?>(null) }
    var bytes by remember { mutableStateOf(0L) }
    var status by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf(false) }
    var voiceMenu by remember { mutableStateOf(false) }
    val voices = KokoroVoices.forLocale(locale)
    Text("Kokoro local neural voice · 54 voices / 8 languages", style=MaterialTheme.typography.titleSmall)
    Text(if(installed) "Installed · offline" else "Optional 295 MB download · no external app")
    if(job!=null) {
        LinearProgressIndicator(progress={(bytes.toFloat()/KokoroPack.BYTES).coerceIn(0f,1f)})
        TextButton(onClick={job?.cancel()}) { Text("Cancel download") }
    } else if(!installed) {
        Button(enabled=enabled,onClick={confirm=true}) { Text("Download Kokoro") }
    } else {
        Row {
            Switch(checked=preferred,onCheckedChange=onPreferred,enabled=enabled)
            Text("Prefer Kokoro")
        }
        if (voices.isNotEmpty()) {
            Box {
                TextButton(enabled=enabled,onClick={voiceMenu=true}) { Text("Voice: " + KokoroVoices.resolve(locale,voiceId)) }
                DropdownMenu(expanded=voiceMenu,onDismissRequest={voiceMenu=false}) {
                    voices.forEach { voice -> DropdownMenuItem(text={Text(voice)},onClick={onVoice(voice);voiceMenu=false}) }
                }
            }
        } else Text("This language uses Google or Android offline speech.")
        TextButton(enabled=enabled,onClick={KokoroPack.remove(context); installed=KokoroPack.installed(context); onPreferred(false)}) { Text("Remove model") }
    }
    if(status.isNotBlank()) Text(status)
    if(confirm) AlertDialog(onDismissRequest={confirm=false},title={Text("Download Kokoro?")},
        text={Text("295 MB download. Allow 400 MB free space during setup. Speech is generated locally after installation.")},
        confirmButton={TextButton(onClick={
            confirm=false; bytes=0; status=""
            job=scope.launch {
                try {
                    KokoroPack.install(context) { count -> bytes=count }
                    installed=KokoroPack.installed(context); status="Kokoro ready for the next voice session."
                } catch(e:CancellationException) { status="Download cancelled."; throw e }
                catch(e:Exception) { status=e.message ?: "Download failed." }
                finally { job=null }
            }
        }){Text("Download")}},dismissButton={TextButton(onClick={confirm=false}){Text("Cancel")}})
}
