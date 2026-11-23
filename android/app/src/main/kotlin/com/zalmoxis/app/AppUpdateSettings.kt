package com.zalmoxis.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Checks GitHub on entry to Settings, not in the background.
 * Installation always requires a user tap followed by the Android confirmation.
 */
@Composable
internal fun AppUpdateSettings() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var candidate by remember { mutableStateOf<AppUpdate?>(null) }
    var stagedApk by remember { mutableStateOf<File?>(null) }
    var checking by remember { mutableStateOf(false) }
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf("GitHub update channel: signed Android builds on main.") }
    val installedVersion = remember { GitHubAppUpdates.installedVersion(context) }

    suspend fun check() {
        checking = true
        stagedApk = null
        candidate = null
        message = "Checking GitHub for a newer signed update…"
        try {
            candidate = GitHubAppUpdates.check(context)
            message = if (candidate == null) {
                "No newer signed APK release found. Regular Actions artifacts are not installable updates."
            } else {
                "New build available: ${candidate?.tag}. Confirm before downloading."
            }
        } catch (error: Exception) {
            message = error.message ?: "Could not reach GitHub. Check your connection."
        } finally {
            checking = false
        }
    }

    LaunchedEffect(Unit) { check() }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = ZalmoxisV2.panel,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, ZalmoxisV2.stroke),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("App updates", style = MaterialTheme.typography.titleLarge)
            Text(
                "Installed build: $installedVersion · GitHub signed pre-release channel",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(message, style = MaterialTheme.typography.bodyMedium)
            if (checking || downloading) {
                LinearProgressIndicator(
                    progress = { if (downloading) progress / 100f else 0f },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Button(
                onClick = { scope.launch { check() } },
                enabled = !checking && !downloading,
            ) { Text(if (checking) "Checking…" else "Check for updates") }

            val update = candidate
            if (update != null && stagedApk == null) {
                Text(
                    "Build ${update.versionCode} · ${update.sizeBytes / 1_048_576} MiB · SHA-256 verified before install",
                    style = MaterialTheme.typography.bodySmall,
                )
                Button(
                    enabled = !checking && !downloading,
                    onClick = {
                        scope.launch {
                            downloading = true
                            progress = 0
                            message = "Downloading the signed APK from GitHub…"
                            try {
                                val file = GitHubAppUpdates.download(context, update) { amount ->
                                    scope.launch(Dispatchers.Main) { progress = amount }
                                }
                                stagedApk = file
                                message = "APK verified: checksum, version, package and signing certificate match."
                            } catch (error: Exception) {
                                message = error.message ?: "Could not download or verify this update."
                            } finally {
                                downloading = false
                            }
                        }
                    },
                ) { Text(if (downloading) "Downloading…" else "Download verified update") }
            }

            val ready = stagedApk
            if (ready != null && candidate != null) {
                Button(onClick = {
                    scope.launch {
                        try {
                            message = "Rechecking APK integrity and signing certificate…"
                            GitHubAppUpdates.verifyStaged(context, ready, requireNotNull(candidate))
                            GitHubAppUpdates.install(context, ready)
                            message = "Follow Android's installation confirmation. If prompted, allow installation from this app, then tap Install again."
                        } catch (error: Exception) {
                            message = error.message ?: "Could not open Android installer."
                        }
                    }
                }) { Text("Install update") }
            }
            Text(
                "Updates never install silently. Your existing app must have the same signing key as the GitHub update. " +
                    "If it was installed using a different debug key, Android will reject an in-place update.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
