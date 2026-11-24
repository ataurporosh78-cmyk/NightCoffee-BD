package com.example.data

import android.content.Context
import android.util.Log
import com.example.data.local.CachedMenuItemEntity
import com.example.data.local.NightCoffeeDatabase
import com.example.data.local.OfflineSyncWorker
import com.example.data.local.PendingSyncActionEntity
import com.example.data.local.PlatformPreferencesStorage
import com.example.model.CrowdLevel
import com.example.model.CrowdReportDoc
import com.example.model.DigitalMenuItemDoc
import com.example.model.LiveShopStatus
import com.example.model.MenuCategory
import com.example.model.ReportVerificationState
import com.example.model.ReportedStatusType
import com.example.model.ShopOverlayDoc
import com.example.model.ShopStaffDoc
import com.example.model.ShopStatusReportDoc
import com.example.model.UserRoleState
import com.example.util.PlatformLogic
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

class PlatformCatalogRepository(
    private val context: Context,
    private val db: FirebaseFirestore? = FirebaseSafeAccess.safeFirestore(context),
    private val auth: FirebaseAuth? = FirebaseSafeAccess.safeAuth(context),
    private val localDb: NightCoffeeDatabase = NightCoffeeDatabase.getInstance(context),
    private val prefsStorage: PlatformPreferencesStorage = PlatformPreferencesStorage(context)
) {
    private fun requireUid(): String =
        auth?.currentUser?.uid ?: throw IllegalStateException("Please sign in with Google to continue.")

    fun observeUserRole(uid: String?): Flow<UserRoleState> {
        val cleanUid = uid?.trim().orEmpty()
        val firestore = db
        if (cleanUid.isEmpty() || firestore == null) return flowOf(UserRoleState())

        val adminFlow: Flow<Boolean> = firestore.collection("admins").document(cleanUid)
            .snapshots()
            .map { snap -> snap.exists() && snap.getBoolean("active") == true }
            .catch { emit(false) }

        val staffFlow: Flow<Set<String>> = firestore.collection("shop_staff")
            .whereEqualTo("uid", cleanUid)
            .snapshots()
            .map { snap ->
                snap.toObjects(ShopStaffDoc::class.java)
                    .filter { it.active && it.shopId.isNotBlank() }
                    .map { it.shopId }
                    .toSet()
            }
            .catch { emit(emptySet()) }

        return combine(adminFlow, staffFlow) { isAdmin, staffIds ->
            UserRoleState(isAdmin = isAdmin, staffShopIds = staffIds)
        }
    }

    fun observeShopOverlays(): Flow<Map<String, ShopOverlayDoc>> {
        val firestore = db ?: return flowOf(emptyMap())
        return firestore.collection("shops")
            .whereEqualTo("visibility", "public")
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    ShopOverlayDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).associateBy { it.id }
            }
            .catch { err ->
                Log.w(TAG, "observeShopOverlays fallback: ${err.message}")
                emit(emptyMap())
            }
    }

    fun observeStatusReports(includeAllForAdmin: Boolean = false): Flow<List<ShopStatusReportDoc>> {
        val firestore = db ?: return flowOf(emptyList())
        val query = if (includeAllForAdmin) {
            firestore.collection("shop_status_reports")
        } else {
            firestore.collection("shop_status_reports").whereEqualTo("visibility", "public")
        }
        return query.snapshots()
            .map { snap ->
                snap.toObjects(
                    ShopStatusReportDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            }
            .catch { err ->
                Log.w(TAG, "observeStatusReports fallback: ${err.message}")
                emit(emptyList())
            }
    }

    suspend fun submitStatusReport(
        shopId: String,
        reportedStatus: ReportedStatusType,
        note: String,
        isStaffReport: Boolean
    ): Result<Boolean> {
        return try {
            val uid = requireUid()
            val cleanShopId = shopId.trim()
            val cleanNote = note.trim().take(280)
            require(cleanShopId.isNotEmpty()) { "Invalid cafe ID." }
            val reporterName = auth?.currentUser?.displayName?.trim().takeIf { !it.isNullOrEmpty() }
                ?: "Dhaka Member"

            val firestore = db
            val docPayload = mapOf(
                "shopId" to cleanShopId,
                "userId" to uid,
                "reporterName" to reporterName.take(60),
                "reportedStatus" to reportedStatus.name,
                "note" to cleanNote,
                "staffReport" to isStaffReport,
                "verificationState" to if (isStaffReport) {
                    ReportVerificationState.VERIFIED.name
                } else {
                    ReportVerificationState.UNVERIFIED.name
                },
                "visibility" to "public",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            if (firestore == null) {
                enqueueOfflineAction(
                    type = PendingSyncActionEntity.TYPE_STATUS_REPORT,
                    collection = "shop_status_reports",
                    docId = "",
                    json = JSONObject(docPayload.filterValues { it !is FieldValue }).apply {
                        put("__serverTimestampFields", JSONArray(listOf("createdAt", "updatedAt")))
                    }.toString()
                )
                return Result.success(true) // Queued offline
            }

            try {
                firestore.collection("shop_status_reports").document().set(docPayload).await()
                Result.success(false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (isNetworkLikelyError(e)) {
                    enqueueOfflineAction(
                        type = PendingSyncActionEntity.TYPE_STATUS_REPORT,
                        collection = "shop_status_reports",
                        docId = "",
                        json = JSONObject(docPayload.filterValues { it !is FieldValue }).apply {
                            put("__serverTimestampFields", JSONArray(listOf("createdAt", "updatedAt")))
                        }.toString()
                    )
                    Result.success(true)
                } else {
                    Result.failure(e)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateOfficialShopStatus(
        shop: com.example.model.CoffeeShop,
        liveStatus: LiveShopStatus
    ): Result<Unit> {
        return try {
            val uid = requireUid()
            val firestore = db ?: return Result.failure(IllegalStateException("Cloud database unavailable."))
            val ref = firestore.collection("shops").document(shop.id)
            val snap = ref.get().await()
            val payload = linkedMapOf<String, Any>(
                "name" to shop.name.take(80),
                "area" to shop.area.displayName,
                "address" to shop.address.ifBlank { "Dhaka" }.take(200),
                "openTime" to shop.openTime.ifBlank { "10:00 AM" },
                "closeTime" to shop.closeTime.ifBlank { "11:00 PM" },
                "priceRange" to shop.priceRange.take(40),
                "phone" to shop.phone.take(24),
                "familyFriendly" to shop.isFamilyFriendly,
                "acAvailable" to true,
                "wifiAvailable" to (com.example.model.ShopFacility.WIFI in shop.resolvedFacilities),
                "rooftopAvailable" to (com.example.model.ShopFacility.ROOFTOP in shop.resolvedFacilities),
                "verified" to (snap.getBoolean("verified") ?: false),
                "liveStatus" to liveStatus.name,
                "liveStatusUpdatedAt" to FieldValue.serverTimestamp(),
                "liveStatusUpdatedBy" to uid,
                "officialOverride" to true,
                "visibility" to "public",
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!snap.exists()) {
                payload["createdAt"] = FieldValue.serverTimestamp()
            }
            ref.set(payload, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyStatusReport(
        reportId: String,
        newState: ReportVerificationState
    ): Result<Unit> {
        return try {
            requireUid()
            val firestore = db ?: return Result.failure(IllegalStateException("Cloud database unavailable."))
            firestore.collection("shop_status_reports").document(reportId)
                .update(
                    mapOf(
                        "verificationState" to newState.name,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeMenuForShop(shopId: String): Flow<Pair<List<DigitalMenuItemDoc>, Boolean>> {
        val localFlow = localDb.cachedMenuDao().observeMenuForShop(shopId)
            .map { entities -> entities.map { it.toDoc() } }

        val firestore = db ?: return localFlow.map { it to true }

        val remoteFlow = flow {
            firestore.collection("menu_items")
                .whereEqualTo("visibility", "public")
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        DigitalMenuItemDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).filter { it.shopId == shopId }
                }
                .collect { remoteItems ->
                    val now = System.currentTimeMillis()
                    localDb.cachedMenuDao().deleteForShop(shopId)
                    if (remoteItems.isNotEmpty()) {
                        localDb.cachedMenuDao().insertAll(
                            remoteItems.map { CachedMenuItemEntity.fromDoc(it, now) }
                        )
                    }
                    emit(remoteItems to false)
                }
        }.catch { err ->
            Log.w(TAG, "observeMenuForShop using offline Room cache: ${err.message}")
        }

        return combine(localFlow, remoteFlow) { cached, remotePair ->
            val (remote, _) = remotePair
            if (remote.isNotEmpty()) remote to false else cached to cached.isNotEmpty()
        }.catch {
            val fallback = localDb.cachedMenuDao().getMenuForShopOnce(shopId).map { it.toDoc() }
            emit(fallback to fallback.isNotEmpty())
        }.flowOn(Dispatchers.IO)
    }

    fun observeAllMenuItemsForShop(shopId: String): Flow<Pair<List<DigitalMenuItemDoc>, Boolean>> {
        val localFlow = localDb.cachedMenuDao().observeMenuForShop(shopId)
            .map { entities -> entities.map { it.toDoc() } }
        val firestore = db ?: return localFlow.map { it to true }

        val remoteSuccess = kotlinx.coroutines.flow.MutableStateFlow<List<DigitalMenuItemDoc>?>(null)
        val remoteListenerFlow = firestore.collection("menu_items")
            .whereEqualTo("visibility", "public")
            .snapshots()
            .map { snap ->
                val list = snap.toObjects(
                    DigitalMenuItemDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).filter { it.shopId == shopId }
                val now = System.currentTimeMillis()
                localDb.cachedMenuDao().deleteForShop(shopId)
                if (list.isNotEmpty()) {
                    localDb.cachedMenuDao().insertAll(list.map { CachedMenuItemEntity.fromDoc(it, now) })
                }
                remoteSuccess.value = list
                list
            }
            .catch { err ->
                Log.w(TAG, "Menu sync error, relying on Room cache: ${err.message}")
                emit(emptyList())
            }

        return combine(localFlow, remoteListenerFlow, remoteSuccess) { cached, _, remoteLoaded ->
            if (remoteLoaded != null) {
                remoteLoaded to false
            } else {
                cached to cached.isNotEmpty()
            }
        }
    }

    suspend fun upsertMenuItem(
        existingId: String?,
        shopId: String,
        category: MenuCategory,
        name: String,
        description: String,
        imageUrl: String,
        priceBdt: Long,
        available: Boolean,
        special: Boolean
    ): Result<Unit> {
        return try {
            val uid = requireUid()
            val firestore = db ?: return Result.failure(IllegalStateException("Cloud database unavailable."))
            val cleanName = name.trim()
            require(cleanName.length in 2..80) { "Item name must be 2 to 80 characters." }
            require(priceBdt in 1..50000) { "Price must be between ৳1 and ৳50,000." }

            val col = firestore.collection("menu_items")
            val ref = if (existingId.isNullOrBlank()) col.document() else col.document(existingId)
            val payload = linkedMapOf<String, Any>(
                "shopId" to shopId,
                "category" to category.displayName,
                "name" to cleanName,
                "description" to description.trim().take(300),
                "imageUrl" to imageUrl.trim().take(500),
                "priceBdt" to priceBdt,
                "available" to available,
                "special" to special,
                "visibility" to "public",
                "updatedBy" to uid,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (existingId.isNullOrBlank()) {
                payload["createdAt"] = FieldValue.serverTimestamp()
                ref.set(payload).await()
            } else {
                ref.update(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteMenuItem(itemId: String): Result<Unit> {
        return try {
            requireUid()
            val firestore = db ?: return Result.failure(IllegalStateException("Cloud database unavailable."))
            firestore.collection("menu_items").document(itemId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeCrowdReports(includeAllForAdmin: Boolean = false): Flow<List<CrowdReportDoc>> {
        val firestore = db ?: return flowOf(emptyList())
        val query = if (includeAllForAdmin) {
            firestore.collection("crowd_reports")
        } else {
            firestore.collection("crowd_reports").whereEqualTo("visibility", "public")
        }
        return query.snapshots()
            .map { snap ->
                snap.toObjects(
                    CrowdReportDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            }
            .catch { err ->
                Log.w(TAG, "observeCrowdReports fallback: ${err.message}")
                emit(emptyList())
            }
    }

    suspend fun submitCrowdReport(
        shopId: String,
        level: CrowdLevel,
        note: String
    ): Result<Boolean> {
        return try {
            val uid = requireUid()
            val now = System.currentTimeMillis()
            val allowed = prefsStorage.canSubmitCrowdReport(shopId, PlatformLogic.CROWD_COOLDOWN_MS, now)
            if (!allowed) {
                return Result.failure(IllegalStateException("Please wait 15 minutes before reporting crowd again for this cafe."))
            }

            val expiresAtMillis = now + PlatformLogic.CROWD_RECENT_THRESHOLD_MS
            val cleanNote = note.trim().take(200)
            val payload = mapOf(
                "shopId" to shopId,
                "userId" to uid,
                "level" to level.name,
                "note" to cleanNote,
                "flaggedIncorrectCount" to 0L,
                "verificationState" to "ACTIVE",
                "visibility" to "public",
                "createdAt" to FieldValue.serverTimestamp(),
                "expiresAt" to Timestamp(Date(expiresAtMillis))
            )

            val firestore = db
            if (firestore == null) {
                enqueueCrowdOffline(payload, expiresAtMillis)
                prefsStorage.recordCrowdReportSubmitted(shopId, now)
                return Result.success(true)
            }

            try {
                firestore.collection("crowd_reports").document().set(payload).await()
                prefsStorage.recordCrowdReportSubmitted(shopId, now)
                Result.success(false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (isNetworkLikelyError(e)) {
                    enqueueCrowdOffline(payload, expiresAtMillis)
                    prefsStorage.recordCrowdReportSubmitted(shopId, now)
                    Result.success(true)
                } else {
                    Result.failure(e)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun flagCrowdReport(reportId: String, reason: String): Result<Unit> {
        return try {
            val uid = requireUid()
            val firestore = db ?: return Result.failure(IllegalStateException("Cloud database unavailable."))
            val cleanReason = reason.trim().ifBlank { "Crowd level looks inaccurate" }.take(200)
            require(cleanReason.length >= 3) { "Please describe why the crowd report is incorrect." }
            val docId = "${reportId}_${uid}"
            firestore.collection("crowd_flags").document(docId)
                .set(
                    mapOf(
                        "reportId" to reportId,
                        "userId" to uid,
                        "reason" to cleanReason,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun dismissCrowdReport(reportId: String): Result<Unit> {
        return try {
            requireUid()
            val firestore = db ?: return Result.failure(IllegalStateException("Cloud database unavailable."))
            firestore.collection("crowd_reports").document(reportId)
                .update("verificationState", "DISMISSED")
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun enqueueCrowdOffline(payload: Map<String, Any>, expiresAtMillis: Long) {
        val json = JSONObject().apply {
            put("shopId", payload["shopId"])
            put("userId", payload["userId"])
            put("level", payload["level"])
            put("note", payload["note"])
            put("flaggedIncorrectCount", 0L)
            put("verificationState", "ACTIVE")
            put("visibility", "public")
            put("expiresAtMillis", expiresAtMillis)
            put("__serverTimestampFields", JSONArray(listOf("createdAt")))
        }.toString()
        enqueueOfflineAction(
            type = PendingSyncActionEntity.TYPE_CROWD_REPORT,
            collection = "crowd_reports",
            docId = "",
            json = json
        )
    }

    private suspend fun enqueueOfflineAction(
        type: String,
        collection: String,
        docId: String,
        json: String
    ) {
        localDb.pendingSyncDao().enqueue(
            PendingSyncActionEntity(
                actionId = UUID.randomUUID().toString(),
                actionType = type,
                targetCollection = collection,
                targetDocId = docId,
                payloadJson = json
            )
        )
        OfflineSyncWorker.scheduleSync(context)
    }

    private fun isNetworkLikelyError(e: Exception): Boolean {
        val msg = e.message.orEmpty().lowercase()
        return msg.contains("unavailable") || msg.contains("offline") || msg.contains("network")
    }

    companion object {
        private const val TAG = "PlatformCatalogRepo"
    }
}
