package com.zalmoxis.app

import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlinx.coroutines.*

@Composable internal fun VoiceExperienceSettings(enabled:Boolean,mode:VoiceMode,onMode:(VoiceMode)->Unit,locale:Locale,preferred:String?,onPreferred:(String?)->Unit) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    var menu by remember { mutableStateOf(false) }
    var job by remember { mutableStateOf<Job?>(null) }
    var preview by remember { mutableStateOf<EmbeddedKokoro?>(null) }
    var message by remember { mutableStateOf("") }
    var pending by remember { mutableStateOf<VoicePack?>(null) }
    var downloaded by remember { mutableStateOf(0L) }
    var revision by remember { mutableStateOf(0) }
    DisposableEffect(Unit) { onDispose { preview?.close(); job?.cancel() } }
    Text("Voice experience",style=MaterialTheme.typography.titleMedium)
    Box {
        OutlinedButton(enabled=enabled,onClick={menu=true}) { Text(mode.label) }
        DropdownMenu(expanded=menu,onDismissRequest={menu=false}) {
            VoiceMode.entries.forEach { item -> DropdownMenuItem(text={Text(item.label)},onClick={onMode(item);menu=false}) }
        }
    }
    Text("Optional local voices. Download only the ones you need.")
    val packs=remember(locale,revision){ ExtraVoicePacks.available(locale).map { it to ExtraVoicePacks.installed(context,it) } }
    if(preferred!=null) TextButton(enabled=enabled,onClick={onPreferred(null)}) { Text("Use automatic engine selection") }
    packs.forEach { (pack,installed) ->
        Text(pack.title+" · "+pack.mb+" MB",style=MaterialTheme.typography.titleSmall)
        if(installed) {
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                TextButton(enabled=enabled&&job==null,onClick={onPreferred(pack.id)}) { Text(if(preferred==pack.id) "Selected" else "Select") }
                TextButton(enabled=enabled&&job==null,onClick={
                    job=scope.launch {
                        try {
                            val local=withContext(Dispatchers.IO){EmbeddedKokoro(context,locale,null,pack)}
                            preview=local
                            local.speak("Hello! I'm Zalmoxis. Let's talk.")
                        } catch(e:CancellationException){throw e}
                        catch(e:Exception){message=e.message?:"Preview unavailable"}
                        finally { preview?.close();preview=null;job=null }
                    }
                }){Text("Preview")}
                TextButton(enabled=enabled&&job==null,onClick={ExtraVoicePacks.remove(context,pack);if(preferred==pack.id)onPreferred(null);revision++}){Text("Remove")}
            }
        } else Button(enabled=enabled&&job==null,onClick={pending=pack}){Text("Download")}
    }
    if(job!=null) { Text("Working · "+downloaded/1000000+" MB");TextButton(onClick={preview?.stop();job?.cancel()}){Text("Cancel")} }
    if(message.isNotBlank()) Text(message)
    pending?.let { pack -> AlertDialog(onDismissRequest={pending=null},title={Text("Download "+pack.title+"?")},text={Text("Optional "+pack.mb+" MB download. All speech runs locally.")},confirmButton={TextButton(onClick={
        pending=null;downloaded=0;message=""
        job=scope.launch {
            try { ExtraVoicePacks.install(context,pack){downloaded=it};revision++;onPreferred(pack.id);message="Voice installed." }
            catch(e:CancellationException){throw e}
            catch(e:Exception){message=e.message?:"Download failed."}
            finally{job=null}
        }
    }){Text("Download")}},dismissButton={TextButton(onClick={pending=null}){Text("Cancel")}}) }
}
