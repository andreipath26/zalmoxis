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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.path
import io.ktor.server.response.respondText
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.mcpStreamableHttp
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID

/**
 * On-device MCP endpoint: never binds to a LAN or public interface.
 * No filesystem, device controls, or automatic Knowledge access.
 * A token is required EVEN on loopback, since other Android apps may access localhost.
 */
internal object LocalMcpState {
    const val PORT = 8765
    const val URL = "http://127.0.0.1:$PORT/mcp"
    private const val STORE = "zalmoxis_local_mcp"
    private val random = SecureRandom()
    @Volatile var running by mutableStateOf(false)
        private set
    @Volatile var diagnostic by mutableStateOf("Stopped")
        private set

    internal data class Task(val id: String, val message: String, val createdAt: Long, val reply: String? = null)
    private val tasks = LinkedHashMap<String, Task>()
    @Volatile var revision by mutableStateOf(0)
        private set

    @Synchronized fun inbox(): List<Task> = tasks.values.filter { it.reply == null }.toList()
    @Synchronized fun all(): List<Task> = tasks.values.toList()
    @Synchronized fun submit(message: String): String {
        require(message.isNotBlank() && message.length <= 6000) { "Message must be 1–6000 characters." }
        require(tasks.size < 50) { "The local approval inbox is full." }
        val id = UUID.randomUUID().toString()
        tasks[id] = Task(id, message, System.currentTimeMillis())
        revision++
        return id
    }
    @Synchronized fun answer(id: String, reply: String): Boolean {
        require(reply.length in 1..12000)
        val original = tasks[id] ?: return false
        if (original.reply != null) return false
        tasks[id] = original.copy(reply = reply)
        revision++
        return true
    }
    @Synchronized fun read(id: String): Task? = tasks[id]

    fun credential(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences(STORE, Context.MODE_PRIVATE)
        val existing = prefs.getString("bearer", null)
        if (existing != null && Regex("[a-f0-9]{64}").matches(existing)) return existing
        val bytes = ByteArray(32).also(random::nextBytes)
        val secret = bytes.joinToString("") { "%02x".format(it) }
        check(prefs.edit().putString("bearer", secret).commit()) { "Local MCP authentication setup failed." }
        return secret
    }
    internal fun allowed(presented: String?, credential: String): Boolean {
        if (presented == null || !presented.startsWith("Bearer ")) return false
        return MessageDigest.isEqual(
            presented.removePrefix("Bearer ").toByteArray(Charsets.US_ASCII),
            credential.toByteArray(Charsets.US_ASCII),
        )
    }
    internal fun markRunning() { running = true; diagnostic = "Listening on device loopback only" }
    internal fun markStopped(reason: String) { running = false; diagnostic = reason }
}

/** Android controls lifecycle. No server is started until the user taps Start. */
class LocalMcpServerService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var instance: EmbeddedServer<*, *>? = null
    private val notificationId = 32841

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (instance != null) return START_NOT_STICKY
        startAsForeground()
        serviceScope.launch {
            try {
                val credential = LocalMcpState.credential(this@LocalMcpServerService)
                val server = createServer()
                val runningServer = embeddedServer(CIO, host = "127.0.0.1", port = LocalMcpState.PORT) {
                    intercept(ApplicationCallPipeline.Plugins) {
                        if (call.request.path() == "/mcp") {
                            val origin = call.request.headers["Origin"]
                            val supplied = call.request.headers["Authorization"]
                            // Origin is optional for native MCP clients, but remote browser origins
                            // must never be allowed to call an authenticated loopback endpoint.
                            if ((origin != null && origin != "http://127.0.0.1:${LocalMcpState.PORT}") ||
                                !LocalMcpState.allowed(supplied, credential)
                            ) {
                                call.respondText("Unauthorized", status = HttpStatusCode.Unauthorized)
                                finish()
                            }
                        }
                    }
                    mcpStreamableHttp(path = "/mcp") { server }
                }
                runningServer.start(wait = false)
                instance = runningServer
                LocalMcpState.markRunning()
            } catch (error: Exception) {
                LocalMcpState.markStopped("Server failed: ${error.message ?: "unknown error"}")
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun createServer(): Server {
        val mcp = Server(
            serverInfo = Implementation(name = "zalmoxis-on-device", version = "0.1.0"),
            options = ServerOptions(capabilities = ServerCapabilities(
                tools = ServerCapabilities.Tools(listChanged = false)
            ))
        )
        mcp.addTool(
            name = "status",
            description = "Read public Zalmoxis local MCP server status. Does not access Knowledge or model data.",
            inputSchema = ToolSchema(properties = buildJsonObject {}),
        ) {
            CallToolResult(content = listOf(TextContent(
                "Zalmoxis local MCP online; endpoint ${LocalMcpState.URL}; " +
                    "task approvals required; no direct device-control or Knowledge tools."
            )))
        }
        mcp.addTool(
            name = "submit_task",
            description = "Submit text to the user's local approval inbox. No automatic execution.",
            inputSchema = ToolSchema(properties = buildJsonObject {
                put("message", buildJsonObject { put("type", "string") })
            }),
        ) { request ->
            val message = runCatching {
                request.params.arguments?.get("message")?.jsonPrimitive?.content.orEmpty()
            }.getOrDefault("")
            val answer = runCatching { LocalMcpState.submit(message) }
            CallToolResult(
                content = listOf(TextContent(
                    answer.fold(
                        onSuccess = { "Queued for user approval. Task ID: $it" },
                        onFailure = { "Rejected: ${it.message}" }
                    )
                )),
                isError = answer.isFailure,
            )
        }
        mcp.addTool(
            name = "read_reply",
            description = "Poll the response to a previously submitted task by its task ID.",
            inputSchema = ToolSchema(properties = buildJsonObject {
                put("task_id", buildJsonObject { put("type", "string") })
            }),
        ) { request ->
            val id = runCatching {
                request.params.arguments?.get("task_id")?.jsonPrimitive?.content.orEmpty()
            }.getOrDefault("")
            val task = LocalMcpState.read(id)
            CallToolResult(content = listOf(TextContent(
                when {
                    task == null -> "Unknown task."
                    task.reply == null -> "Pending explicit user reply."
                    else -> task.reply
                }
            )))
        }
        return mcp
    }

    private fun startAsForeground() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(NotificationChannel(
            CHANNEL, "Zalmoxis local MCP", NotificationManager.IMPORTANCE_LOW
        ))
        val open = PendingIntent.getActivity(this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("Zalmoxis local MCP server")
            .setContentText("Listening only on 127.0.0.1 · tap to open Zalmoxis")
            .setContentIntent(open)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(notificationId, notification)
        }
    }

    override fun onDestroy() {
        instance?.stop(300, 1000)
        instance = null
        LocalMcpState.markStopped("Stopped")
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "com.zalmoxis.app.LOCAL_MCP_STOP"
        private const val CHANNEL = "zalmoxis_local_mcp"
        fun start(context: Context) {
            androidx.core.content.ContextCompat.startForegroundService(
                context, Intent(context, LocalMcpServerService::class.java)
            )
        }
        fun stop(context: Context) {
            context.startService(Intent(context, LocalMcpServerService::class.java).setAction(ACTION_STOP))
        }
    }
}
