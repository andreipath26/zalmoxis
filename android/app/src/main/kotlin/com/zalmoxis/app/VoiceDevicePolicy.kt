package com.zalmoxis.app

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.PowerManager

internal object VoiceDevicePolicy {
    fun lowMemory(context: Context): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        manager.getMemoryInfo(info)
        return info.lowMemory || info.availMem < 350L * 1024 * 1024
    }
    fun preferCpu(context: Context): Boolean {
        val power = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return power.isPowerSaveMode || (Build.VERSION.SDK_INT >= 29 && power.currentThermalStatus >= PowerManager.THERMAL_STATUS_SEVERE)
    }
    fun phraseSize(context: Context): Int = if (preferCpu(context) || lowMemory(context)) 180 else 100
}
