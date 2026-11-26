package com.zalmoxis.app

import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*

@Composable internal fun KokoroSettings(enabled:Boolean, preferred:Boolean, onPreferred:(Boolean)->Unit) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    var installed by remember { mutableStateOf(KokoroPack.installed(context)) }
    var job by remember { mutableStateOf<Job?>(null) }
    var bytes by remember { mutableStateOf(0L) }
    var status by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf(false) }
    Text("Kokoro local neural voice (English / Chinese)", style=MaterialTheme.typography.titleSmall)
    Text(if(installed) "Installed · offline" else "Optional 147 MB download · no external app")
    if(job!=null) {
        LinearProgressIndicator(progress={bytes.toFloat()/KokoroPack.BYTES})
        TextButton(onClick={job?.cancel()}) { Text("Cancel download") }
    } else if(!installed) {
        Button(enabled=enabled,onClick={confirm=true}) { Text("Download Kokoro") }
    } else {
        Row {
            Switch(checked=preferred,onCheckedChange=onPreferred,enabled=enabled)
            Text("Prefer Kokoro")
        }
        TextButton(enabled=enabled,onClick={KokoroPack.remove(context); installed=KokoroPack.installed(context); onPreferred(false)}) { Text("Remove model") }
    }
    if(status.isNotBlank()) Text(status)
    if(confirm) AlertDialog(onDismissRequest={confirm=false},title={Text("Download Kokoro?")},
        text={Text("147 MB download. Allow 500 MB free space during setup. Speech is generated locally after installation.")},
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
