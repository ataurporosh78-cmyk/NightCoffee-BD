package com.example.data.local

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.FirebaseSafeAccess
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

class OfflineSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val auth = FirebaseSafeAccess.safeAuth(applicationContext)
        val db = FirebaseSafeAccess.safeFirestore(applicationContext)
        val currentUid = auth?.currentUser?.uid
        if (db == null || currentUid.isNullOrBlank()) {
            return Result.retry()
        }

        val pendingDao = NightCoffeeDatabase.getInstance(applicationContext).pendingSyncDao()
        val pendingItems = pendingDao.getPendingActions()
        if (pendingItems.isEmpty()) return Result.success()

        var needsRetry = false
        for (item in pendingItems) {
            try {
                val payload = parsePayload(item.payloadJson, item.actionType, currentUid)
                if (payload == null) {
                    // Belongs to another signed-in user or malformed; avoid poisoning queue
                    if (item.retryCount >= 3) {
                        pendingDao.markRetryOrFailed(item.actionId, PendingSyncActionEntity.SYNC_STATUS_FAILED_PERM)
                    } else {
                        pendingDao.markRetryOrFailed(item.actionId, PendingSyncActionEntity.SYNC_STATUS_PENDING)
                        needsRetry = true
                    }
                    continue
                }
                val docRef = if (item.targetDocId.isBlank()) {
                    db.collection(item.targetCollection).document()
                } else {
                    db.collection(item.targetCollection).document(item.targetDocId)
                }
                if (item.actionType == PendingSyncActionEntity.TYPE_USER_PREFERENCE) {
                    docRef.set(payload, SetOptions.merge()).await()
                } else {
                    docRef.set(payload).await()
                }
                pendingDao.deleteById(item.actionId)
            } catch (e: FirebaseFirestoreException) {
                Log.w(TAG, "Firestore sync error for ${item.actionId}: ${e.code}")
                if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ||
                    e.code == FirebaseFirestoreException.Code.INVALID_ARGUMENT
                ) {
                    pendingDao.markRetryOrFailed(item.actionId, PendingSyncActionEntity.SYNC_STATUS_FAILED_PERM)
                } else {
                    pendingDao.markRetryOrFailed(item.actionId, PendingSyncActionEntity.SYNC_STATUS_PENDING)
                    needsRetry = true
                }
            } catch (e: Exception) {
                Log.w(TAG, "Transient sync failure for ${item.actionId}: ${e.message}")
                pendingDao.markRetryOrFailed(item.actionId, PendingSyncActionEntity.SYNC_STATUS_PENDING)
                needsRetry = true
            }
        }

        return if (needsRetry) Result.retry() else Result.success()
    }

    private fun parsePayload(
        rawJson: String,
        actionType: String,
        currentUid: String
    ): Map<String, Any>? {
        return runCatching {
            val obj = JSONObject(rawJson)
            val ownerField = when (actionType) {
                PendingSyncActionEntity.TYPE_USER_PREFERENCE -> "userId"
                else -> "userId"
            }
            val payloadUid = obj.optString(ownerField, "")
            if (payloadUid.isNotBlank() && payloadUid != currentUid) {
                return null
            }

            val map = linkedMapOf<String, Any>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = obj.get(key)
                when {
                    key == "__serverTimestampFields" && value is JSONArray -> {
                        for (i in 0 until value.length()) {
                            val tsKey = value.optString(i)
                            if (tsKey.isNotBlank()) {
                                map[tsKey] = FieldValue.serverTimestamp()
                            }
                        }
                    }
                    key.endsWith("Millis") && value is Number -> {
                        val targetKey = key.removeSuffix("Millis")
                        map[targetKey] = Timestamp(Date(value.toLong()))
                    }
                    value is JSONArray -> {
                        val list = buildList {
                            for (i in 0 until value.length()) {
                                add(value.get(i).toString())
                            }
                        }
                        map[key] = list
                    }
                    value != JSONObject.NULL -> {
                        map[key] = value
                    }
                }
            }
            map
        }.getOrNull()
    }

    companion object {
        private const val TAG = "OfflineSyncWorker"
        private const val UNIQUE_SYNC_WORK = "nightcoffee_offline_sync"

        fun scheduleSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<OfflineSyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                .build()
            runCatching {
                WorkManager.getInstance(context.applicationContext)
                    .enqueueUniqueWork(UNIQUE_SYNC_WORK, ExistingWorkPolicy.REPLACE, request)
            }
        }
    }
}
