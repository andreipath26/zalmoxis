package com.zalmoxis.app

import kotlin.math.sqrt

/** Adaptive acoustic gate for PCM16. Not a neural Silero or Smart Turn model. */
internal class AdaptiveVoiceGate {
    private var floor = 120.0
    private var voiced = 0
    private var silent = 0
    var speaking = false
        private set
    fun accept(bytes: ByteArray, size: Int): Boolean {
        var sum = 0.0
        var samples = 0
        var index = 0
        while (index + 1 < size) {
            val x = ((bytes[index].toInt() and 255) or (bytes[index+1].toInt() shl 8)).toShort().toDouble()
            sum += x*x
            samples++
            index += 2
        }
        if(samples==0) return speaking
        val rms=sqrt(sum/samples)
        val active=rms>maxOf(250.0,floor*3.0)
        if(active) { voiced++; silent=0; if(voiced>=3) speaking=true }
        else { voiced=0; silent++; if(silent>=12) speaking=false; if(!speaking) floor=0.98*floor+0.02*rms }
        return speaking
    }
}
internal object TurnRecovery {
    fun pauseBudget(partial:String):Long {
        val text=partial.trim()
        if(text.isEmpty())return 1200
        if(text.last() in ".!?。！？")return 500
        val tail=text.substringAfterLast(' ').lowercase().trimEnd(',','.')
        return if(tail in setOf("and","or","because","but","the","et","mais","parce","și","und","oder")) 1600 else 900
    }
}
