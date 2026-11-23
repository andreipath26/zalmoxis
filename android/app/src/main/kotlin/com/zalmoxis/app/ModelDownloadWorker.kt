package com.zalmoxis.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.CancellationException
import java.util.UUID
import java.util.concurrent.TimeUnit

/** WorkManager adapter for API 26–33 and fallback/background recovery on newer Android versions. */
class ModelDownloadWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val catalogId = inputData.getString(KEY_CATALOG_ID)
            ?: return failure("Missing model catalog ID")
        val entry = ModelCatalog.find(catalogId)
            ?: return failure("This model is not in the approved catalog")
        return try {
            setForeground(foregroundInfo(entry, 0L))
            val installed = ModelTransferEngine.download(applicationContext, catalogId) { progress ->
                setProgress(
                    workDataOf(
                        KEY_CATALOG_ID to progress.entry.id,
                        KEY_MODEL_NAME to progress.entry.displayName,
                        KEY_BYTES_DOWNLOADED to progress.receivedBytes,
                        KEY_TOTAL_BYTES to progress.entry.sizeBytes,
                        KEY_PROGRESS_PERCENT to
                            ((progress.receivedBytes * 100L) / progress.entry.sizeBytes).toInt(),
                    ),
                )
                setForeground(foregroundInfo(progress.entry, progress.receivedBytes))
            }
            Result.success(
                workDataOf(KEY_CATALOG_ID to catalogId, KEY_INSTALLED_PATH to installed.absolutePath),
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: InvalidTransferException) {
            Result.failure(workDataOf(KEY_ERROR to error.message))
        } catch (error: NoSpaceException) {
            Result.failure(workDataOf(KEY_ERROR to error.message))
        } catch (error: Exception) {
            if (runAttemptCount >= MAX_RETRIES) {
                Result.failure(workDataOf(KEY_ERROR to (error.message ?: "Download failed")))
            } else {
                Result.retry()
            }
        }
    }

    private fun foregroundInfo(entry: ModelCatalogEntry, received: Long): ForegroundInfo {
        ensureChannel()
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Downloading ${entry.displayName}")
            .setContentText("${formatBytes(received)} of ${formatBytes(entry.sizeBytes)}")
            .setProgress(
                entry.sizeBytes.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                received.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                false,
            )
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Cancel",
                WorkManager.getInstance(applicationContext).createCancelPendingIntent(id),
            )
            .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = applicationContext.getSystemService(NotificationManager::class.java)
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                manager.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "Model downloads", NotificationManager.IMPORTANCE_LOW),
                )
            }
        }
    }

    private fun failure(message: String) = Result.failure(workDataOf(KEY_ERROR to message))

    companion object {
        const val KEY_CATALOG_ID = "catalog_id"
        const val KEY_MODEL_NAME = "model_name"
        const val KEY_BYTES_DOWNLOADED = "bytes_downloaded"
        const val KEY_TOTAL_BYTES = "total_bytes"
        const val KEY_PROGRESS_PERCENT = "progress_percent"
        const val KEY_INSTALLED_PATH = "installed_path"
        const val KEY_ERROR = "error"
        const val UNIQUE_WORK_PREFIX = "model-download-"

        private const val CHANNEL_ID = "model-downloads"
        private const val NOTIFICATION_ID = 7301
        private const val MAX_RETRIES = 5

        fun uniqueWorkName(catalogId: String) = UNIQUE_WORK_PREFIX + catalogId

        fun workInfo(context: Context, catalogId: String) =
            WorkManager.getInstance(context).getWorkInfosForUniqueWorkLiveData(uniqueWorkName(catalogId))

        fun enqueue(context: Context, catalogId: String): UUID {
            require(ModelCatalog.find(catalogId) != null) { "This model is not in the approved catalog." }
            val request = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
                .setInputData(workDataOf(KEY_CATALOG_ID to catalogId))
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .addTag(uniqueWorkName(catalogId))
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                uniqueWorkName(catalogId),
                androidx.work.ExistingWorkPolicy.APPEND_OR_REPLACE,
                request,
            )
            return request.id
        }

        fun cancel(context: Context, catalogId: String) {
            WorkManager.getInstance(context).cancelUniqueWork(uniqueWorkName(catalogId))
        }
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
