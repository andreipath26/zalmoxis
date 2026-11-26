package com.zalmoxis.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import kotlinx.coroutines.flow.MutableStateFlow

/** Foreground microphone lease owned by a user-started voice session.
 * The activity retains inference ownership. Destruction stops capture rather than
 * silently restarting it. A visible notification provides an unconditional stop.
 */
internal class LiveVoiceService : Service() {
    companion object {
        private const val CHANNEL = "live_voice"
        private const val STOP = "com.zalmoxis.app.STOP_LIVE_VOICE"
        val stopSignal = MutableStateFlow(0L)
        val running = MutableStateFlow(false)
        fun start(context:Context) {
            context.startForegroundService(Intent(context,LiveVoiceService::class.java))
        }
        fun stop(context:Context) { context.stopService(Intent(context,LiveVoiceService::class.java)) }
    }
    override fun onBind(intent:Intent?):IBinder? = null
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
        if(intent?.action==STOP) {
            stopSignal.value++
            stopSelf()
            return START_NOT_STICKY
        }
        val manager=getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL,"Live conversation",NotificationManager.IMPORTANCE_LOW))
        val stop=PendingIntent.getService(this,1,Intent(this,LiveVoiceService::class.java).setAction(STOP),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val open=PendingIntent.getActivity(this,2,Intent(this,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification=Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Zalmoxis voice conversation").setContentText("Microphone session active. Tap Stop to end.")
            .setOngoing(true).setContentIntent(open)
            .addAction(Notification.Action.Builder(null,"Stop",stop).build()).build()
        try {
            if(Build.VERSION.SDK_INT>=30) startForeground(812,notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
            else startForeground(812,notification)
            running.value=true
        } catch(e:Exception) {
            stopSignal.value++
            stopSelf()
        }
        return START_NOT_STICKY
    }
    override fun onDestroy() {
        running.value=false
        stopSignal.value++
        super.onDestroy()
    }
}
