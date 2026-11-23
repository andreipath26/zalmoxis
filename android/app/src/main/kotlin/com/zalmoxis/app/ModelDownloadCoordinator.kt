package com.zalmoxis.app

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.work.WorkInfo
import androidx.work.WorkManager
import org.json.JSONObject

enum class ModelDownloadStatus { IDLE, ENQUEUED, RUNNING, SUCCEEDED, FAILED, CANCELLED }

data class ModelDownloadState(
    val catalogId: String,
    val displayName: String,
    val status: ModelDownloadStatus,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val error: String? = null,
)

/**
 * Single UI contract. Android 14+ schedules native user-initiated data transfers; Android 8–13
 * uses WorkManager. Both adapters publish the same state shape.
 */
object ModelDownloadCoordinator {
    private const val PREFS = "model_download_uidt"
    private const val CANCEL_PREFIX = "cancel-requested-"
    private const val WORK_ID_PREFIX = "latest-work-id-"
    private const val RETRY_PREFIX = "transient-retries-"

    fun start(context: Context, catalogId: String): Boolean {
        val app = context.applicationContext
        val entry = ModelCatalog.find(catalogId) ?: return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val scheduler = app.getSystemService(JobScheduler::class.java)
            val jobId = jobId(catalogId)
            prefs(app).edit().remove(cancelKey(catalogId)).remove(retryKey(catalogId)).apply()
            publish(
                app,
                ModelDownloadState(catalogId, entry.displayName, ModelDownloadStatus.ENQUEUED, totalBytes = entry.sizeBytes),
            )
            val extras = PersistableBundle().apply { putString(ModelDownloadJobService.EXTRA_CATALOG_ID, catalogId) }
            val info = JobInfo.Builder(
                jobId,
                ComponentName(app, ModelDownloadJobService::class.java),
            )
                .setUserInitiated(true)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setEstimatedNetworkBytes(entry.sizeBytes, 0L)
                .setBackoffCriteria(30_000L, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
                .setExtras(extras)
                .build()
            val result = scheduler.schedule(info)
            if (result == JobScheduler.RESULT_SUCCESS) return true
            publish(
                app,
                ModelDownloadState(catalogId, entry.displayName, ModelDownloadStatus.FAILED, totalBytes = entry.sizeBytes, error = "Android could not schedule the transfer."),
            )
            return false
        }
        val requestId = ModelDownloadWorker.enqueue(app, catalogId)
        prefs(app).edit().putString(workIdKey(catalogId), requestId.toString()).apply()
        return true
    }

    fun cancel(context: Context, catalogId: String) {
        val app = context.applicationContext
        val entry = ModelCatalog.find(catalogId)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            prefs(app).edit().putBoolean(cancelKey(catalogId), true).remove(retryKey(catalogId)).apply()
            app.getSystemService(JobScheduler::class.java).cancel(jobId(catalogId))
            publish(
                app,
                ModelDownloadState(
                    catalogId,
                    entry?.displayName ?: catalogId,
                    ModelDownloadStatus.CANCELLED,
                    totalBytes = entry?.sizeBytes ?: 0L,
                ),
            )
        } else {
            ModelDownloadWorker.cancel(app, catalogId)
        }
    }

    fun observe(context: Context, catalogId: String): LiveData<ModelDownloadState> {
        val app = context.applicationContext
        val entry = ModelCatalog.find(catalogId)
        val empty = ModelDownloadState(
            catalogId = catalogId,
            displayName = entry?.displayName ?: catalogId,
            status = ModelDownloadStatus.IDLE,
            totalBytes = entry?.sizeBytes ?: 0L,
        )
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            StoredStateLiveData(app, catalogId, empty)
        } else {
            // Observe the unique work chain, not a fixed request ID: retries create new IDs.
            val source = ModelDownloadWorker.workInfo(app, catalogId)
            MediatorLiveData<ModelDownloadState>().apply {
                value = empty
                addSource(source) { infos ->
                    val active = infos.orEmpty().firstOrNull { it.state == WorkInfo.State.RUNNING }
                        ?: infos.orEmpty().firstOrNull {
                            it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.BLOCKED
                        }
                    val latest = active ?: infos.orEmpty().lastOrNull()
                    value = latest?.let { mapWorkInfo(it, empty) } ?: empty
                }
            }
        }
    }

    internal fun publish(context: Context, state: ModelDownloadState) {
        val json = JSONObject()
            .put("catalog_id", state.catalogId)
            .put("display_name", state.displayName)
            .put("status", state.status.name)
            .put("bytes_downloaded", state.bytesDownloaded)
            .put("total_bytes", state.totalBytes)
            .put("error", state.error)
        prefs(context).edit().putString(state.catalogId, json.toString()).apply()
    }

    internal fun read(context: Context, catalogId: String, fallback: ModelDownloadState): ModelDownloadState {
        val raw = prefs(context).getString(catalogId, null) ?: return fallback
        return runCatching {
            val json = JSONObject(raw)
            ModelDownloadState(
                catalogId = json.getString("catalog_id"),
                displayName = json.optString("display_name", fallback.displayName),
                status = ModelDownloadStatus.valueOf(json.optString("status", "IDLE")),
                bytesDownloaded = json.optLong("bytes_downloaded", 0L),
                totalBytes = json.optLong("total_bytes", fallback.totalBytes),
                error = json.optString("error").takeIf { it.isNotBlank() && it != "null" },
            )
        }.getOrDefault(fallback)
    }

    internal fun cancelRequested(context: Context, catalogId: String): Boolean =
        prefs(context).getBoolean(cancelKey(catalogId), false)

    internal fun clearCancelRequested(context: Context, catalogId: String) {
        prefs(context).edit().remove(cancelKey(catalogId)).apply()
    }

    private fun mapWorkInfo(info: WorkInfo, empty: ModelDownloadState): ModelDownloadState {
        val data = if (info.state == WorkInfo.State.SUCCEEDED || info.state == WorkInfo.State.FAILED) {
            info.outputData
        } else {
            info.progress
        }
        val status = when (info.state) {
            WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> ModelDownloadStatus.ENQUEUED
            WorkInfo.State.RUNNING -> ModelDownloadStatus.RUNNING
            WorkInfo.State.SUCCEEDED -> ModelDownloadStatus.SUCCEEDED
            WorkInfo.State.FAILED -> ModelDownloadStatus.FAILED
            WorkInfo.State.CANCELLED -> ModelDownloadStatus.CANCELLED
        }
        return empty.copy(
            status = status,
            bytesDownloaded = data.getLong(ModelDownloadWorker.KEY_BYTES_DOWNLOADED, if (status == ModelDownloadStatus.SUCCEEDED) empty.totalBytes else 0L),
            totalBytes = data.getLong(ModelDownloadWorker.KEY_TOTAL_BYTES, empty.totalBytes),
            error = data.getString(ModelDownloadWorker.KEY_ERROR),
        )
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun cancelKey(catalogId: String) = CANCEL_PREFIX + catalogId
    private fun workIdKey(catalogId: String) = WORK_ID_PREFIX + catalogId
    private fun retryKey(catalogId: String) = RETRY_PREFIX + catalogId

    internal fun noteTransientFailure(context: Context, catalogId: String): Int {
        val preferences = prefs(context)
        val next = preferences.getInt(retryKey(catalogId), 0) + 1
        preferences.edit().putInt(retryKey(catalogId), next).apply()
        return next
    }

    internal fun clearTransientFailures(context: Context, catalogId: String) {
        prefs(context).edit().remove(retryKey(catalogId)).apply()
    }

    private fun jobId(catalogId: String): Int = 14_000 + (catalogId.hashCode() and 0x3fffffff) % 1_000_000

    private class StoredStateLiveData(
        private val context: Context,
        private val catalogId: String,
        private val fallback: ModelDownloadState,
    ) : LiveData<ModelDownloadState>() {
        private val preferences = prefs(context)
        private val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == catalogId) postValue(read(context, catalogId, fallback))
        }

        override fun onActive() {
            super.onActive()
            preferences.registerOnSharedPreferenceChangeListener(listener)
            value = read(context, catalogId, fallback)
        }

        override fun onInactive() {
            preferences.unregisterOnSharedPreferenceChangeListener(listener)
            super.onInactive()
        }
    }
}
