package com.zalmoxis.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/** Android 14+ native user-initiated data transfer adapter for large explicit model downloads. */
class ModelDownloadJobService : JobService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val active = ConcurrentHashMap<Int, RunningJob>()

    override fun onStartJob(params: JobParameters): Boolean {
        val catalogId = params.extras.getString(EXTRA_CATALOG_ID)
        val entry = catalogId?.let(ModelCatalog::find)
        if (catalogId == null || entry == null) return false

        ensureChannel()
        setNotification(
            params,
            notificationId(params.jobId),
            notification(entry.displayName, 0L, entry.sizeBytes),
            JOB_END_NOTIFICATION_POLICY_DETACH,
        )
        val running = RunningJob(params)
        val job = serviceScope.launch(start = kotlinx.coroutines.CoroutineStart.LAZY) {
            try {
                ModelDownloadCoordinator.publish(
                    this@ModelDownloadJobService,
                    ModelDownloadState(catalogId, entry.displayName, ModelDownloadStatus.RUNNING, totalBytes = entry.sizeBytes),
                )
                ModelTransferEngine.download(applicationContext, catalogId) { progress ->
                    ModelDownloadCoordinator.publish(
                        applicationContext,
                        ModelDownloadState(
                            catalogId,
                            progress.entry.displayName,
                            ModelDownloadStatus.RUNNING,
                            progress.receivedBytes,
                            progress.entry.sizeBytes,
                        ),
                    )
                    withContext(Dispatchers.Main.immediate) {
                        setNotification(
                            params,
                            notificationId(params.jobId),
                            notification(progress.entry.displayName, progress.receivedBytes, progress.entry.sizeBytes),
                            JOB_END_NOTIFICATION_POLICY_DETACH,
                        )
                    }
                }
                ModelDownloadCoordinator.publish(
                    applicationContext,
                    ModelDownloadState(catalogId, entry.displayName, ModelDownloadStatus.SUCCEEDED, entry.sizeBytes, entry.sizeBytes),
                )
            } catch (cancelled: CancellationException) {
                // onStopJob owns the state and rescheduling decision.
                throw cancelled
            } catch (error: Exception) {
                ModelDownloadCoordinator.publish(
                    applicationContext,
                    ModelDownloadState(
                        catalogId,
                        entry.displayName,
                        ModelDownloadStatus.FAILED,
                        totalBytes = entry.sizeBytes,
                        error = error.message ?: "Download failed",
                    ),
                )
            } finally {
                if (active.remove(params.jobId, running)) jobFinished(params, false)
            }
        }
        running.job = job
        active[params.jobId] = running
        job.start()
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        val running = active.remove(params.jobId)
        running?.job?.cancel(CancellationException("Job stopped"))
        val catalogId = params.extras.getString(EXTRA_CATALOG_ID) ?: return false
        val entry = ModelCatalog.find(catalogId)
        val cancelledByUser = ModelDownloadCoordinator.cancelRequested(this, catalogId) ||
            params.stopReason == JobParameters.STOP_REASON_USER
        if (cancelledByUser) {
            ModelDownloadCoordinator.clearCancelRequested(this, catalogId)
            ModelDownloadCoordinator.publish(
                this,
                ModelDownloadState(catalogId, entry?.displayName ?: catalogId, ModelDownloadStatus.CANCELLED, totalBytes = entry?.sizeBytes ?: 0L),
            )
            return false
        }
        if (entry != null) {
            ModelDownloadCoordinator.publish(
                this,
                ModelDownloadState(catalogId, entry.displayName, ModelDownloadStatus.ENQUEUED, totalBytes = entry.sizeBytes),
            )
        }
        return true
    }

    override fun onDestroy() {
        active.values.forEach { it.job?.cancel() }
        active.clear()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                manager.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "Model downloads", NotificationManager.IMPORTANCE_LOW),
                )
            }
        }
    }

    private fun notification(name: String, downloaded: Long, total: Long) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Downloading $name")
            .setContentText("${formatBytes(downloaded)} of ${formatBytes(total)}")
            .setProgress(
                total.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                downloaded.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                false,
            )
            .setOngoing(true)
            .build()

    private fun notificationId(jobId: Int) = 73_000 + jobId % 1_000

    private class RunningJob(val params: JobParameters) {
        var job: Job? = null
    }

    companion object {
        const val EXTRA_CATALOG_ID = "catalog_id"
        private const val CHANNEL_ID = "model-downloads"
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var index = -1
    do {
        value /= 1024.0
        index++
    } while (value >= 1024.0 && index < units.lastIndex)
    return String.format(java.util.Locale.ROOT, "%.1f %s", value, units[index])
}
