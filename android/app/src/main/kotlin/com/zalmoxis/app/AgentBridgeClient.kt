package com.zalmoxis.app

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal data class BridgeConnection(val baseUrl:String,val provider:String,val bridgeId:String,val token:String)
internal data class BridgeMessage(val id:String,val body:String,val created:Long)
internal data class BridgeReply(val id:String,val reply:String?,val status:String,val created:Long)

/** No OpenAI/DeepSeek model API key. A bridge-issued device bearer is encrypted in Android Keystore. */
internal object AgentBridgeClient {
    private const val PREF="agent_bridge_v1"
    private const val KEY_ALIAS="zalmoxis-bridge-aes"
    private const val MAX_JSON=16000
    private fun prefs(c:Context)=c.getSharedPreferences(PREF,Context.MODE_PRIVATE)

    private fun secretKey():SecretKey {
        val ks=KeyStore.getInstance("AndroidKeyStore").apply{load(null)}
        (ks.getKey(KEY_ALIAS,null) as? SecretKey)?.let{return it}
        val gen=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore")
        gen.init(KeyGenParameterSpec.Builder(KEY_ALIAS,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256).build())
        return gen.generateKey()
    }
    private fun encrypt(clear:String):String {
        val cipher=Cipher.getInstance("AES/GCM/NoPadding").apply{init(Cipher.ENCRYPT_MODE,secretKey())}
        return Base64.encodeToString(cipher.iv+cipher.doFinal(clear.toByteArray(Charsets.UTF_8)),Base64.NO_WRAP)
    }
    private fun decrypt(blob:String):String {
        val data=Base64.decode(blob,Base64.NO_WRAP)
        require(data.size>28)
        val cipher=Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE,secretKey(),GCMParameterSpec(128,data.copyOfRange(0,12)))
        return cipher.doFinal(data.copyOfRange(12,data.size)).toString(Charsets.UTF_8)
    }
    fun load(c:Context):BridgeConnection? = runCatching {
        val saved=prefs(c).getString("sealed",null)?:return null
        val record=JSONObject(decrypt(saved))
        BridgeConnection(record.getString("base_url"),record.getString("provider"),
            record.getString("bridge_id"),record.getString("token"))
    }.getOrNull()
    private fun store(c:Context,connection:BridgeConnection){
        val data=JSONObject().put("base_url",connection.baseUrl).put("provider",connection.provider)
            .put("bridge_id",connection.bridgeId).put("token",connection.token)
        prefs(c).edit().putString("sealed",encrypt(data.toString())).apply()
    }
    fun clearLocal(c:Context){prefs(c).edit().clear().apply()}

    fun normalizeUrl(text:String):String {
        val uri=URI(text.trim().trimEnd('/'))
        require(uri.scheme=="https"&&uri.host!=null&&uri.userInfo==null&&uri.port==-1 &&
            uri.rawPath.orEmpty().isEmpty()&&uri.rawQuery==null&&uri.rawFragment==null) {
            "Enter only the HTTPS relay origin, e.g. https://your-bridge.workers.dev"
        }
        val host=uri.host.lowercase()
        require(!host.equals("localhost")&&!host.endsWith(".local") &&
            !Regex("[0-9.]+").matches(host)&&!host.contains(':')) {
            "Use a public HTTPS hostname, not a local or IP address."
        }
        return uri.toASCIIString()
    }
    private suspend fun request(base:String,path:String,method:String="GET",token:String?=null,
        data:JSONObject?=null):JSONObject=withContext(Dispatchers.IO) {
        val url=URL(normalizeUrl(base)+path)
        val conn=(url.openConnection() as HttpURLConnection).apply{
            instanceFollowRedirects=false
            connectTimeout=10000
            readTimeout=20000
            requestMethod=method
            setRequestProperty("Accept","application/json")
            if(token!=null)setRequestProperty("Authorization","Bearer $token")
            if(data!=null){
                doOutput=true
                setRequestProperty("Content-Type","application/json")
            }
        }
        try {
            if(data!=null){
                val bytes=data.toString().toByteArray(Charsets.UTF_8)
                require(bytes.size<=MAX_JSON)
                conn.outputStream.use{it.write(bytes)}
            }
            val response=conn.responseCode
            require(response in 200..299) {
                when(response){
                    401->"Bridge authorization expired or was revoked."
                    403->"Invalid or expired pairing code."
                    409->"Bridge message or pairing already handled."
                    else->"Relay request failed (HTTP $response)."
                }
            }
            val body=conn.inputStream.bufferedReader().use{it.readText().take(200000)}
            JSONObject(body)
        }finally{conn.disconnect()}
    }

    suspend fun pair(c:Context,base:String,code:String):BridgeConnection {
        val origin=normalizeUrl(base)
        require(Regex("[A-Fa-f0-9]{16}").matches(code.trim())){"Enter the 16-character one-time pairing code."}
        val result=request(origin,"/v1/pair","POST",
            data=JSONObject().put("pairing_code",code.trim()))
        val connection=BridgeConnection(origin,result.getString("provider"),
            result.getString("bridge_id"),result.getString("device_token"))
        store(c,connection)
        return connection
    }
    suspend fun status(x:BridgeConnection):Boolean =
        request(x.baseUrl,"/v1/device/status",token=x.token).getBoolean("paired")
    suspend fun inbox(x:BridgeConnection):List<BridgeMessage>{
        val items=request(x.baseUrl,"/v1/device/inbox",token=x.token).getJSONArray("messages")
        return (0 until items.length()).mapNotNull{
            val obj=items.optJSONObject(it)?:return@mapNotNull null
            BridgeMessage(obj.getString("id"),obj.getString("body"),obj.getLong("created"))
        }
    }
    suspend fun outbox(x:BridgeConnection):List<BridgeReply>{
        val items=request(x.baseUrl,"/v1/device/outbox",token=x.token).getJSONArray("messages")
        return (0 until items.length()).mapNotNull{
            val obj=items.optJSONObject(it)?:return@mapNotNull null
            BridgeReply(obj.getString("id"),obj.optString("reply").takeIf{v->v.isNotBlank()},
                obj.getString("status"),obj.getLong("created"))
        }
    }
    suspend fun send(x:BridgeConnection,message:String){
        require(message.isNotBlank()&&message.length<=12000)
        request(x.baseUrl,"/v1/device/send","POST",x.token,JSONObject().put("message",message))
    }
    suspend fun answer(x:BridgeConnection,id:String,message:String?,reject:Boolean=false){
        require(reject||(message!=null&&message.isNotBlank()&&message.length<=12000))
        request(x.baseUrl,"/v1/device/answer","POST",x.token,
            JSONObject().put("message_id",id).put("reply",message?:"").put("reject",reject))
    }
    suspend fun disconnect(c:Context,x:BridgeConnection) {
        // Do not discard the only revocation credential if the phone is offline.
        // Keep the pairing until the server confirms revocation.
        request(x.baseUrl,"/v1/device/disconnect","POST",x.token,JSONObject())
        clearLocal(c)
    }
}
