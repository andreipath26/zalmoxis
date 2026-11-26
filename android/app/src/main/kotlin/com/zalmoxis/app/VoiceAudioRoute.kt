package com.zalmoxis.app
import android.content.Context
import android.media.AudioManager
import android.media.AudioDeviceInfo
import android.os.Build

/** Scoped communication route; restores normal audio mode when voice ends. */
internal class VoiceAudioRoute(context:Context):AutoCloseable {
    private val audio=context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val previous=audio.mode
    private var closed=false
    init {
        runCatching {
            audio.mode=AudioManager.MODE_IN_COMMUNICATION
            if(Build.VERSION.SDK_INT>=31){
                val device=audio.availableCommunicationDevices.firstOrNull {
                    it.type==AudioDeviceInfo.TYPE_BLUETOOTH_SCO || it.type==AudioDeviceInfo.TYPE_BLE_HEADSET || it.type==AudioDeviceInfo.TYPE_WIRED_HEADSET
                }
                if(device!=null) audio.setCommunicationDevice(device)
            }
        }
    }
    override fun close(){
        if(closed)return
        closed=true
        runCatching { if(Build.VERSION.SDK_INT>=31) audio.clearCommunicationDevice();audio.mode=previous }
    }
}
