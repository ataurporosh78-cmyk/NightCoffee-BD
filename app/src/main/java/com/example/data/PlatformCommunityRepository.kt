package com.example.data

import android.content.Context
import android.util.Log
import com.example.data.local.CachedEventEntity
import com.example.data.local.NightCoffeeDatabase
import com.example.data.local.OfflineSyncWorker
import com.example.data.local.PendingSyncActionEntity
import com.example.data.local.PlatformPreferencesStorage
import com.example.model.CoffeeEventDoc
import com.example.model.DhakaArea
import com.example.model.EventCategory
import com.example.model.EventRegistrationDoc
import com.example.model.EventStatus
import com.example.model.NotificationCategory
import com.example.model.NotificationItemDoc
import com.example.model.SupportTicketDoc
import com.example.model.SupportTicketStatus
import com.example.model.SupportTicketType
import com.example.model.UserPreferenceDoc
import com.example.model.VerificationRequestDoc
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.messaging.FirebaseMessaging
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

class PlatformCommunityRepository(
    private val context: Context,
    private val db: FirebaseFirestore? = FirebaseSafeAccess.safeFirestore(context),
    private val auth: FirebaseAuth? = FirebaseSafeAccess.safeAuth(context),
    private val functions: FirebaseFunctions? = FirebaseSafeAccess.safeFunctions(context),
    private val localDb: NightCoffeeDatabase = NightCoffeeDatabase.getInstance(context),
    private val prefsStorage: PlatformPreferencesStorage = PlatformPreferencesStorage(context)
) {
    private fun requireUid(): String =
        auth?.currentUser?.uid ?: throw IllegalStateException("Please sign in with Google to continue.")

    fun observePendingSyncCount(): Flow<Int> = localDb.pendingSyncDao().observePendingCount()

    fun triggerOfflineSync() {
        OfflineSyncWorker.scheduleSync(context)
    }

    // --- FEATURE 8: NOTIFICATIONS & PREFERENCES ---

    fun observeNotifications(uid: String?): Flow<List<NotificationItemDoc>> {
        val cleanUid = uid?.trim().orEmpty()
        val firestore = db
        if (cleanUid.isEmpty() || firestore == null) return flowOf(emptyList())

        val personalFlow = firestore.collection("notifications")
            .whereEqualTo("userId", cleanUid)
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    NotificationItemDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { emit(emptyList()) }

        val broadcastFlow = firestore.collection("notifications")
            .whereEqualTo("userId", "broadcast")
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    NotificationItemDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { emit(emptyList()) }

        return combine(personalFlow, broadcastFlow) { personal, broadcast ->
            (personal + broadcast)
                .distinctBy { it.id }
                .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
        }
    }

    suspend fun markNotificationRead(notification: NotificationItemDoc): Result<Unit> {
        return try {
            val uid = requireUid()
            if (notification.userId != uid) return Result.success(Unit)
            val firestore = db ?: return Result.success(Unit)
            firestore.collection("notifications").document(notification.id)
                .update("read", true)
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeUserPreferences(uid: String?): Flow<UserPreferenceDoc> {
        val cleanUid = uid?.trim().orEmpty()
        val localFlow = prefsStorage.localPreferences
        val firestore = db
        if (cleanUid.isEmpty() || firestore == null) return localFlow

        val remoteFlow = firestore.collection("user_preferences").document(cleanUid)
            .snapshots()
            .map { snap ->
                if (snap.exists()) {
                    snap.toObject(UserPreferenceDoc::class.java)
                } else {
                    null
                }
            }
            .catch { emit(null) }

        return combine(localFlow, remoteFlow) { local, remote ->
            remote ?: local
        }
    }

    suspend fun saveUserPreferences(updated: UserPreferenceDoc): Result<Unit> {
        return try {
            prefsStorage.saveNotificationPrefs(updated)
            syncFcmTopics(updated)
            val uid = auth?.currentUser?.uid ?: return Result.success(Unit)
            val token = fetchFcmTokenSafe() ?: updated.fcmToken
            if (token.isNotBlank()) prefsStorage.saveFcmToken(token)

            val payload = mapOf(
                "userId" to uid,
                "fcmToken" to token,
                "optInNewOpenings" to updated.optInNewOpenings,
                "optInOffers" to updated.optInOffers,
                "optInSavedShops" to updated.optInSavedShops,
                "optInReservations" to updated.optInReservations,
                "optInEvents" to updated.optInEvents,
                "optInAnnouncements" to updated.optInAnnouncements,
                "compareShopIds" to updated.compareShopIds.distinct().take(3),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val firestore = db
            if (firestore == null) {
                enqueuePreferenceSync(uid, payload)
                return Result.success(Unit)
            }
            try {
                firestore.collection("user_preferences").document(uid)
                    .set(payload, SetOptions.merge())
                    .await()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                enqueuePreferenceSync(uid, payload)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveCompareIds(ids: List<String>, currentPrefs: UserPreferenceDoc) {
        val cleanIds = ids.distinct().take(3)
        prefsStorage.saveCompareIds(cleanIds)
        if (auth?.currentUser?.uid != null) {
            saveUserPreferences(currentPrefs.copy(compareShopIds = cleanIds))
        }
    }

    private suspend fun syncFcmTopics(prefs: UserPreferenceDoc) {
        val messaging = runCatching { FirebaseMessaging.getInstance() }.getOrNull() ?: return
        val mapping = listOf(
            NotificationCategory.NEW_OPENING.topicName to prefs.optInNewOpenings,
            NotificationCategory.EXCLUSIVE_OFFER.topicName to prefs.optInOffers,
            NotificationCategory.SAVED_SHOP_UPDATE.topicName to prefs.optInSavedShops,
            NotificationCategory.RESERVATION_UPDATE.topicName to prefs.optInReservations,
            NotificationCategory.COFFEE_EVENT.topicName to prefs.optInEvents,
            NotificationCategory.ANNOUNCEMENT.topicName to prefs.optInAnnouncements
        )
        for ((topic, enabled) in mapping) {
            runCatching {
                if (enabled) {
                    messaging.subscribeToTopic(topic).await()
                } else {
                    messaging.unsubscribeFromTopic(topic).await()
                }
            }
        }
    }

    private suspend fun fetchFcmTokenSafe(): String? {
        return runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull()
    }

    suspend fun sendPlatformAnnouncement(
        category: NotificationCategory,
        title: String,
        body: String,
        targetRoute: String,
        targetId: String
    ): Result<Unit> {
        return try {
            requireUid()
            val cleanTitle = title.trim().take(100)
            val cleanBody = body.trim().take(500)
            require(cleanTitle.length >= 2 && cleanBody.length >= 2) {
                "Announcement title and message are required."
            }
            val fn = functions
            if (fn != null) {
                val cfRes = runCatching {
                    fn.getHttpsCallable("sendPlatformNotification")
                        .call(
                            mapOf(
                                "category" to category.name,
                                "title" to cleanTitle,
                                "body" to cleanBody,
                                "targetRoute" to targetRoute,
                                "targetId" to targetId,
                                "topic" to category.topicName
                            )
                        )
                        .await()
                }
                if (cfRes.isSuccess) return Result.success(Unit)
            }
            val firestore = db ?: return Result.failure(IllegalStateException("Cloud database unavailable."))
            firestore.collection("notifications").document()
                .set(
                    mapOf(
                        "userId" to "broadcast",
                        "category" to category.name,
                        "title" to cleanTitle,
                        "body" to cleanBody,
                        "targetRoute" to targetRoute,
                        "targetId" to targetId,
                        "read" to false,
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

    // --- FEATURE 10: BUSINESS VERIFICATION ---

    fun observeMyVerificationRequests(uid: String?): Flow<List<VerificationRequestDoc>> {
        val cleanUid = uid?.trim().orEmpty()
        val firestore = db
        if (cleanUid.isEmpty() || firestore == null) return flowOf(emptyList())

        return firestore.collection("verification_requests")
            .whereEqualTo("requesterUid", cleanUid)
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    VerificationRequestDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            }
            .catch { emit(emptyList()) }
    }

    fun observeAdminVerificationRequests(isAdmin: Boolean): Flow<List<VerificationRequestDoc>> {
        val firestore = db
        if (!isAdmin || firestore == null) return flowOf(emptyList())

        return firestore.collection("verification_requests")
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    VerificationRequestDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            }
            .catch { emit(emptyList()) }
    }

    suspend fun submitVerificationRequest(
        shopId: String,
        shopName: String,
        businessOwnerName: String,
        businessPhone: String,
        registrationNote: String
    ): Result<Unit> {
        return try {
            val uid = requireUid()
            val firestore = db ?: return Result.failure(IllegalStateException("Cloud database unavailable."))
            val cleanOwner = businessOwnerName.trim()
            val cleanPhone = businessPhone.trim()
            val cleanNote = registrationNote.trim()
            require(cleanOwner.length in 2..80) { "Owner/representative name is required." }
            require(cleanPhone.length in 6..24) { "Valid business phone number is required." }
            require(cleanNote.length in 5..500) { "Please provide business verification details (5+ characters)." }

            firestore.collection("verification_requests").document()
                .set(
                    mapOf(
                        "shopId" to shopId,
                        "shopName" to shopName.take(80),
                        "requesterUid" to uid,
                        "businessOwnerName" to cleanOwner,
                        "businessPhone" to cleanPhone,
                        "registrationNote" to cleanNote,
                        "status" to "Pending",
                        "reviewedBy" to "",
                        "createdAt" to FieldValue.serverTimestamp(),
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

    suspend fun reviewVerificationRequest(request: VerificationRequestDoc, approve: Boolean): Result<Unit> {
        return try {
            val adminUid = requireUid()
            val firestore = db ?: return Result.failure(IllegalStateException("Cloud database unavailable."))
            val cfAttempt = functions?.let { fn ->
                runCatching {
                    fn.getHttpsCallable("reviewVerificationRequest")
                        .call(mapOf("requestId" to request.id, "approve" to approve))
                        .await()
                }
            }
            if (cfAttempt != null && cfAttempt.isSuccess) return Result.success(Unit)

            // Admin fallback write guarded by Firestore admin rules
            val batch = firestore.batch()
            val reqRef = firestore.collection("verification_requests").document(request.id)
            batch.update(
                reqRef,
                mapOf(
                    "status" to if (approve) "Approved" else "Rejected",
                    "reviewedBy" to adminUid,
                    "reviewedAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (approve) {
                val shopRef = firestore.collection("shops").document(request.shopId)
                val snap = shopRef.get().await()
                val shopPayload = linkedMapOf<String, Any>(
                    "name" to request.shopName.take(80),
                    "area" to (snap.getString("area") ?: "Banani"),
                    "address" to (snap.getString("address") ?: "Dhaka"),
                    "openTime" to (snap.getString("openTime") ?: "10:00 AM"),
                    "closeTime" to (snap.getString("closeTime") ?: "11:00 PM"),
                    "priceRange" to (snap.getString("priceRange") ?: ""),
                    "phone" to (snap.getString("phone") ?: request.businessPhone),
                    "familyFriendly" to (snap.getBoolean("familyFriendly") ?: true),
                    "acAvailable" to (snap.getBoolean("acAvailable") ?: true),
                    "wifiAvailable" to (snap.getBoolean("wifiAvailable") ?: true),
                    "rooftopAvailable" to (snap.getBoolean("rooftopAvailable") ?: false),
                    "verified" to true,
                    "verifiedAt" to FieldValue.serverTimestamp(),
                    "verifiedBy" to adminUid,
                    "ownerUid" to request.requesterUid,
                    "officialOverride" to (snap.getBoolean("officialOverride") ?: false),
                    "visibility" to "public",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!snap.exists()) shopPayload["createdAt"] = FieldValue.serverTimestamp()
                batch.set(shopRef, shopPayload, SetOptions.merge())

                val staffRef = firestore.collection("shop_staff").document("${request.shopId}_${request.requesterUid}")
                batch.set(
                    staffRef,
                    mapOf(
                        "shopId" to request.shopId,
                        "uid" to request.requesterUid,
                        "role" to "OWNER",
                        "active" to true
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- FEATURE 11: COFFEE EVENTS ---

    fun observeEvents(): Flow<Pair<List<CoffeeEventDoc>, Boolean>> {
        val localFlow = localDb.cachedEventDao().observeAllEvents().map { list -> list.map { it.toDoc() } }
        val firestore = db ?: return localFlow.map { it to true }

        val remoteLoaded = MutableStateFlow<List<CoffeeEventDoc>?>(null)
        val remoteFlow = firestore.collection("events")
            .whereEqualTo("visibility", "public")
            .snapshots()
            .map { snap ->
                val list = snap.toObjects(
                    CoffeeEventDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                val now = System.currentTimeMillis()
                localDb.cachedEventDao().clearAll()
                if (list.isNotEmpty()) {
                    localDb.cachedEventDao().insertAll(list.map { CachedEventEntity.fromDoc(it, now) })
                }
                remoteLoaded.value = list
                list
            }
            .catch { err ->
                Log.w(TAG, "observeEvents using Room cache: ${err.message}")
                emit(emptyList())
            }

        return combine(localFlow, remoteFlow, remoteLoaded) { cached, _, loaded ->
            if (loaded != null) loaded to false else cached to cached.isNotEmpty()
        }
    }

    fun observeMyEventRegistrations(uid: String?): Flow<Map<String, EventRegistrationDoc>> {
        val cleanUid = uid?.trim().orEmpty()
        val firestore = db
        if (cleanUid.isEmpty() || firestore == null) return flowOf(emptyMap())

        return firestore.collection("event_registrations")
            .whereEqualTo("userId", cleanUid)
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    EventRegistrationDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).associateBy { it.eventId }
            }
            .catch { emit(emptyMap()) }
    }

    suspend fun toggleEventRegistration(
        eventId: String,
        targetStatus: String,
        note: String,
        alreadyExists: Boolean
    ): Result<Unit> {
        return try {
            val uid = requireUid()
            val firestore = db ?: return Result.failure(IllegalStateException("Cloud database unavailable."))
            val ref = firestore.collection("event_registrations").document("${eventId}_${uid}")
            val attendeeName = (auth?.currentUser?.displayName?.trim().takeIf { !it.isNullOrEmpty() }
                ?: "Dhaka Coffee Explorer").take(80)
            if (alreadyExists && targetStatus == "Remove") {
                ref.delete().await()
            } else if (alreadyExists) {
                ref.update(
                    mapOf(
                        "attendeeName" to attendeeName,
                        "note" to note.trim().take(250),
                        "status" to targetStatus,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ref.set(
                    mapOf(
                        "eventId" to eventId,
                        "userId" to uid,
                        "attendeeName" to attendeeName,
                        "note" to note.trim().take(250),
                        "status" to targetStatus,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createEvent(
        title: String,
        category: EventCategory,
        description: String,
        venueName: String,
        area: DhakaArea,
        shopId: String?,
        eventDateDhaka: String,
        startTime: String,
        endTime: String,
        organizer: String,
        registrationInfo: String
    ): Result<Unit> {
        return try {
            val uid = requireUid()
            val firestore = db ?: return Result.failure(IllegalStateException("Cloud database unavailable."))
            val cleanTitle = title.trim()
            val cleanDesc = description.trim()
            val cleanVenue = venueName.trim()
            require(cleanTitle.length in 3..100) { "Event title must be 3 to 100 characters." }
            require(cleanDesc.length in 5..1000) { "Event description must be 5+ characters." }
            require(cleanVenue.length in 2..120) { "Venue name is required." }
            require(area != DhakaArea.ALL) { "Select a Dhaka area." }
            require(Regex("""^\d{4}-\d{2}-\d{2}$""").matches(eventDateDhaka.trim())) {
                "Event date must follow YYYY-MM-DD."
            }

            val payload = linkedMapOf<String, Any>(
                "title" to cleanTitle,
                "category" to category.name,
                "description" to cleanDesc,
                "imageUrl" to "",
                "venueName" to cleanVenue,
                "area" to area.displayName,
                "eventDateDhaka" to eventDateDhaka.trim(),
                "startTime" to startTime.trim().ifBlank { "7:00 PM" },
                "endTime" to endTime.trim().ifBlank { "10:00 PM" },
                "organizer" to organizer.trim().ifBlank { cleanVenue }.take(80),
                "registrationInfo" to registrationInfo.trim().take(300),
                "status" to EventStatus.UPCOMING.label,
                "visibility" to "public",
                "createdBy" to uid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!shopId.isNullOrBlank()) {
                payload["shopId"] = shopId.trim()
            }
            firestore.collection("events").document().set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- FEATURE 12: HELP & SUPPORT TICKETS ---

    fun observeMySupportTickets(uid: String?): Flow<List<SupportTicketDoc>> {
        val cleanUid = uid?.trim().orEmpty()
        val firestore = db
        if (cleanUid.isEmpty() || firestore == null) return flowOf(emptyList())

        return firestore.collection("support_tickets")
            .whereEqualTo("userId", cleanUid)
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    SupportTicketDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            }
            .catch { emit(emptyList()) }
    }

    fun observeAdminSupportTickets(isAdmin: Boolean): Flow<List<SupportTicketDoc>> {
        val firestore = db
        if (!isAdmin || firestore == null) return flowOf(emptyList())

        return firestore.collection("support_tickets")
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    SupportTicketDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            }
            .catch { emit(emptyList()) }
    }

    suspend fun submitSupportTicket(
        type: SupportTicketType,
        targetId: String,
        targetName: String,
        subject: String,
        description: String,
        contactInfo: String
    ): Result<Boolean> {
        return try {
            val uid = requireUid()
            val cleanSubject = subject.trim()
            val cleanDesc = description.trim()
            require(cleanSubject.length in 3..120) { "Subject must be 3 to 120 characters." }
            require(cleanDesc.length in 5..1500) { "Description must be 5 to 1500 characters." }

            val payload = mapOf(
                "userId" to uid,
                "type" to type.name,
                "targetId" to targetId.trim().take(128),
                "targetName" to targetName.trim().take(120),
                "subject" to cleanSubject,
                "description" to cleanDesc,
                "contactInfo" to contactInfo.trim().take(120),
                "status" to SupportTicketStatus.OPEN.label,
                "adminResponse" to "",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val firestore = db
            if (firestore == null) {
                enqueueTicketOffline(payload)
                return Result.success(true)
            }
            try {
                firestore.collection("support_tickets").document().set(payload).await()
                Result.success(false)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                enqueueTicketOffline(payload)
                Result.success(true)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateSupportTicketAdmin(
        ticketId: String,
        status: SupportTicketStatus,
        adminResponse: String
    ): Result<Unit> {
        return try {
            requireUid()
            val firestore = db ?: return Result.failure(IllegalStateException("Cloud database unavailable."))
            firestore.collection("support_tickets").document(ticketId)
                .update(
                    mapOf(
                        "status" to status.label,
                        "adminResponse" to adminResponse.trim().take(1000),
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

    private suspend fun enqueueTicketOffline(payload: Map<String, Any>) {
        val json = JSONObject(payload.filterValues { it !is FieldValue }).apply {
            put("__serverTimestampFields", JSONArray(listOf("createdAt", "updatedAt")))
        }.toString()
        localDb.pendingSyncDao().enqueue(
            PendingSyncActionEntity(
                actionId = UUID.randomUUID().toString(),
                actionType = PendingSyncActionEntity.TYPE_SUPPORT_TICKET,
                targetCollection = "support_tickets",
                targetDocId = "",
                payloadJson = json
            )
        )
        OfflineSyncWorker.scheduleSync(context)
    }

    private suspend fun enqueuePreferenceSync(uid: String, payload: Map<String, Any>) {
        val json = JSONObject(payload.filterValues { it !is FieldValue }).apply {
            put("__serverTimestampFields", JSONArray(listOf("updatedAt")))
        }.toString()
        localDb.pendingSyncDao().enqueue(
            PendingSyncActionEntity(
                actionId = "pref_$uid",
                actionType = PendingSyncActionEntity.TYPE_USER_PREFERENCE,
                targetCollection = "user_preferences",
                targetDocId = uid,
                payloadJson = json
            )
        )
        OfflineSyncWorker.scheduleSync(context)
    }

    companion object {
        private const val TAG = "PlatformCommunityRepo"
    }
}
