package com.example.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.local.CachedCouponEntity
import com.example.data.local.NightCoffeeDatabase
import com.example.model.CouponDiscountType
import com.example.model.CouponDoc
import com.example.model.CouponRedemptionDoc
import com.example.model.PhotoReviewDoc
import com.example.model.ReservationDoc
import com.example.model.ReservationStatus
import com.example.util.BangladeshTimeUtil
import com.example.util.ImageCompressionUtil
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class PlatformEngagementRepository(
    private val context: Context,
    private val db: FirebaseFirestore? = FirebaseSafeAccess.safeFirestore(context),
    private val auth: FirebaseAuth? = FirebaseSafeAccess.safeAuth(context),
    private val storage: FirebaseStorage? = FirebaseSafeAccess.safeStorage(context),
    private val functions: FirebaseFunctions? = FirebaseSafeAccess.safeFunctions(context),
    private val localDb: NightCoffeeDatabase = NightCoffeeDatabase.getInstance(context)
) {
    private fun requireUid(): String =
        auth?.currentUser?.uid ?: throw IllegalStateException("Please sign in with Google to continue.")

    // --- FEATURE 4: OFFERS & COUPONS ---

    fun observeCoupons(): Flow<Pair<List<CouponDoc>, Boolean>> {
        val localFlow = localDb.cachedCouponDao().observeAllCoupons()
            .map { list -> list.map { it.toDoc() } }
        val firestore = db ?: return localFlow.map { it to true }

        val remoteLoaded = MutableStateFlow<List<CouponDoc>?>(null)
        val remoteFlow = firestore.collection("coupons")
            .whereEqualTo("visibility", "public")
            .snapshots()
            .map { snap ->
                val list = snap.toObjects(
                    CouponDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
                val now = System.currentTimeMillis()
                localDb.cachedCouponDao().clearAll()
                if (list.isNotEmpty()) {
                    localDb.cachedCouponDao().insertAll(list.map { CachedCouponEntity.fromDoc(it, now) })
                }
                remoteLoaded.value = list
                list
            }
            .catch { err ->
                Log.w(TAG, "observeCoupons using Room cache: ${err.message}")
                emit(emptyList())
            }

        return combine(localFlow, remoteFlow, remoteLoaded) { cached, _, loaded ->
            if (loaded != null) loaded to false else cached to cached.isNotEmpty()
        }
    }

    fun observeMyCouponRedemptions(uid: String?): Flow<Map<String, CouponRedemptionDoc>> {
        val cleanUid = uid?.trim().orEmpty()
        val firestore = db
        if (cleanUid.isEmpty() || firestore == null) return flowOf(emptyMap())

        return firestore.collection("coupon_redemptions")
            .whereEqualTo("userId", cleanUid)
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    CouponRedemptionDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).associateBy { it.couponId }
            }
            .catch { emit(emptyMap()) }
    }

    suspend fun saveCoupon(coupon: CouponDoc): Result<Unit> {
        return try {
            val uid = requireUid()
            val firestore = db ?: return Result.failure(IllegalStateException("Cloud database unavailable."))
            val docId = "${coupon.id}_${uid}"
            firestore.collection("coupon_redemptions").document(docId)
                .set(
                    mapOf(
                        "couponId" to coupon.id,
                        "userId" to uid,
                        "shopId" to coupon.shopId,
                        "code" to coupon.code,
                        "status" to "SAVED",
                        "discountLabel" to coupon.formattedDiscount(),
                        "savedAt" to FieldValue.serverTimestamp()
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

    suspend fun redeemCoupon(couponId: String): Result<String> {
        return try {
            requireUid()
            val fn = functions ?: return Result.failure(
                IllegalStateException("Coupon redemption service is unavailable offline.")
            )
            val response = fn.getHttpsCallable("redeemCoupon")
                .call(mapOf("couponId" to couponId))
                .await()
            val data = response.getData() as? Map<*, *>
            val code = data?.get("code")?.toString().orEmpty()
            Result.success(if (code.isNotBlank()) "Redeemed code $code" else "Coupon redeemed")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(
                IllegalStateException(
                    e.message ?: "Coupon redemption requires deployed Cloud Functions & connectivity."
                )
            )
        }
    }

    suspend fun createCoupon(
        shopId: String,
        shopName: String,
        code: String,
        title: String,
        description: String,
        discountType: CouponDiscountType,
        discountValue: Long,
        minSpendBdt: Long,
        maxUsageLimit: Long,
        terms: String,
        validDays: Int
    ): Result<Unit> {
        return try {
            val uid = requireUid()
            val firestore = db ?: return Result.failure(IllegalStateException("Cloud database unavailable."))
            val cleanCode = code.trim().uppercase().take(24)
            val cleanTitle = title.trim().take(80)
            require(cleanCode.length >= 3) { "Coupon code must be at least 3 characters." }
            require(cleanTitle.length >= 3) { "Coupon title must be at least 3 characters." }
            require(maxUsageLimit >= 1) { "Usage limit must be at least 1." }
            if (discountType == CouponDiscountType.PERCENTAGE) {
                require(discountValue in 1..90) { "Percentage discount must be 1% to 90%." }
            } else {
                require(discountValue in 1..5000) { "Fixed BDT discount must be ৳1 to ৳5,000." }
            }
            val expMillis = System.currentTimeMillis() + (validDays.coerceIn(1, 365) * 86_400_000L)
            firestore.collection("coupons").document()
                .set(
                    mapOf(
                        "shopId" to shopId,
                        "shopName" to shopName.take(80),
                        "code" to cleanCode,
                        "title" to cleanTitle,
                        "description" to description.trim().take(300),
                        "discountType" to discountType.name,
                        "discountValue" to discountValue,
                        "minSpendBdt" to minSpendBdt.coerceAtLeast(0L),
                        "maxUsageLimit" to maxUsageLimit,
                        "redeemedCount" to 0L,
                        "terms" to terms.trim().take(500),
                        "expiresAt" to Timestamp(Date(expMillis)),
                        "active" to true,
                        "visibility" to "public",
                        "createdBy" to uid,
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

    suspend fun deactivateCoupon(coupon: CouponDoc): Result<Unit> {
        return try {
            requireUid()
            val firestore = db ?: return Result.failure(IllegalStateException("Cloud database unavailable."))
            firestore.collection("coupons").document(coupon.id)
                .update(
                    mapOf(
                        "active" to !coupon.active,
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

    // --- FEATURE 6: CUSTOMER PHOTO REVIEWS ---

    fun observePhotoReviews(includeFlaggedForAdmin: Boolean = false): Flow<List<PhotoReviewDoc>> {
        val firestore = db ?: return flowOf(emptyList())
        val query = if (includeFlaggedForAdmin) {
            firestore.collection("reviews")
        } else {
            firestore.collection("reviews").whereEqualTo("visibility", "public")
        }
        return query.snapshots()
            .map { snap ->
                snap.toObjects(
                    PhotoReviewDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            }
            .catch { err ->
                Log.w(TAG, "observePhotoReviews fallback: ${err.message}")
                emit(emptyList())
            }
    }

    fun observeMyReviewVotes(uid: String?): Flow<Set<String>> {
        val cleanUid = uid?.trim().orEmpty()
        val firestore = db
        if (cleanUid.isEmpty() || firestore == null) return flowOf(emptySet())

        return firestore.collection("review_votes")
            .whereEqualTo("userId", cleanUid)
            .snapshots()
            .map { snap -> snap.documents.mapNotNull { it.getString("reviewId") }.toSet() }
            .catch { emit(emptySet()) }
    }

    suspend fun submitPhotoReview(
        shopId: String,
        rating: Int,
        comment: String,
        pickedImageUris: List<Uri>
    ): Result<Unit> {
        return try {
            val uid = requireUid()
            val firestore = db ?: return Result.failure(IllegalStateException("Cloud database unavailable."))
            val cleanComment = comment.trim()
            require(rating in 1..5) { "Rating must be between 1 and 5." }
            require(cleanComment.length in 1..800) { "Review text must be 1 to 800 characters." }

            val uploadedUrls = mutableListOf<String>()
            val limitedUris = pickedImageUris.take(3)
            if (limitedUris.isNotEmpty()) {
                val fbStorage = storage ?: return Result.failure(
                    IllegalStateException("Photo upload storage is unavailable.")
                )
                for (uri in limitedUris) {
                    val compressed = ImageCompressionUtil.compressPickedImage(context, uri).getOrThrow()
                    val photoRef = fbStorage.reference
                        .child("review_photos/$shopId/$uid/${UUID.randomUUID()}.jpg")
                    photoRef.putBytes(compressed).await()
                    val downloadUrl = photoRef.downloadUrl.await().toString()
                    uploadedUrls.add(downloadUrl)
                }
            }

            val authorName = (auth?.currentUser?.displayName?.trim().takeIf { !it.isNullOrEmpty() }
                ?: "Dhaka Coffee Lover").take(60)

            firestore.collection("reviews").document()
                .set(
                    mapOf(
                        "shopId" to shopId,
                        "userId" to uid,
                        "authorName" to authorName,
                        "rating" to rating.toLong(),
                        "comment" to cleanComment,
                        "photoUrls" to uploadedUrls,
                        "helpfulCount" to 0L,
                        "reportCount" to 0L,
                        "visibility" to "public",
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

    suspend fun toggleHelpfulVote(reviewId: String, currentlyVoted: Boolean): Result<Unit> {
        return try {
            val uid = requireUid()
            val firestore = db ?: return Result.failure(IllegalStateException("Cloud database unavailable."))
            val ref = firestore.collection("review_votes").document("${reviewId}_${uid}")
            if (currentlyVoted) {
                ref.delete().await()
            } else {
                ref.set(
                    mapOf(
                        "reviewId" to reviewId,
                        "userId" to uid,
                        "createdAt" to FieldValue.serverTimestamp()
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

    suspend fun reportReview(reviewId: String, reason: String): Result<Unit> {
        return try {
            val uid = requireUid()
            val firestore = db ?: return Result.failure(IllegalStateException("Cloud database unavailable."))
            val cleanReason = reason.trim().ifBlank { "Inappropriate or misleading review" }.take(200)
            require(cleanReason.length >= 3) { "Please describe the issue." }
            firestore.collection("review_reports").document("${reviewId}_${uid}")
                .set(
                    mapOf(
                        "reviewId" to reviewId,
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

    suspend fun moderateReview(reviewId: String, visibility: String): Result<Unit> {
        return try {
            requireUid()
            val firestore = db ?: return Result.failure(IllegalStateException("Cloud database unavailable."))
            firestore.collection("reviews").document(reviewId)
                .update(
                    mapOf(
                        "visibility" to visibility,
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

    suspend fun deletePhotoReview(reviewId: String): Result<Unit> {
        return try {
            requireUid()
            val firestore = db ?: return Result.failure(IllegalStateException("Cloud database unavailable."))
            firestore.collection("reviews").document(reviewId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- FEATURE 7: TABLE RESERVATION ---

    fun observeMyReservations(uid: String?): Flow<List<ReservationDoc>> {
        val cleanUid = uid?.trim().orEmpty()
        val firestore = db
        if (cleanUid.isEmpty() || firestore == null) return flowOf(emptyList())

        return firestore.collection("reservations")
            .whereEqualTo("userId", cleanUid)
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    ReservationDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            }
            .catch { emit(emptyList()) }
    }

    fun observeShopReservations(shopId: String): Flow<List<ReservationDoc>> {
        val firestore = db ?: return flowOf(emptyList())
        return firestore.collection("reservations")
            .whereEqualTo("shopId", shopId)
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    ReservationDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            }
            .catch { emit(emptyList()) }
    }

    fun observeAllReservationsForAdmin(isAdmin: Boolean): Flow<List<ReservationDoc>> {
        val firestore = db
        if (!isAdmin || firestore == null) return flowOf(emptyList())
        return firestore.collection("reservations")
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    ReservationDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            }
            .catch { emit(emptyList()) }
    }

    suspend fun createReservation(
        shopId: String,
        shopName: String,
        guestName: String,
        contactPhone: String,
        dateDhaka: String,
        timeSlot: String,
        guestCount: Int,
        specialRequest: String,
        existingMyReservations: List<ReservationDoc>
    ): Result<Unit> {
        return try {
            val uid = requireUid()
            val firestore = db ?: return Result.failure(IllegalStateException("Cloud database unavailable."))
            val cleanGuest = guestName.trim()
            val cleanPhone = contactPhone.trim()
            val cleanDate = dateDhaka.trim()
            val cleanTime = timeSlot.trim()

            require(cleanGuest.length in 2..80) { "Guest name must be 2 to 80 characters." }
            require(cleanPhone.length in 6..24) { "Valid contact phone number is required." }
            require(BangladeshTimeUtil.isValidReservationDate(cleanDate)) {
                "Reservation date cannot be in the past (Bangladesh time)."
            }
            require(cleanTime.length in 4..16) { "Please select a valid time slot." }
            require(guestCount in 1..20) { "Guest count must be between 1 and 20." }

            val hasDuplicateActive = existingMyReservations.any {
                it.shopId == shopId &&
                    it.dateDhaka == cleanDate &&
                    it.timeSlot == cleanTime &&
                    (it.statusEnum() == ReservationStatus.PENDING || it.statusEnum() == ReservationStatus.CONFIRMED)
            }
            require(!hasDuplicateActive) {
                "You already have an active booking for $shopName on $cleanDate at $cleanTime."
            }

            firestore.collection("reservations").document()
                .set(
                    mapOf(
                        "shopId" to shopId,
                        "shopName" to shopName.take(80),
                        "userId" to uid,
                        "guestName" to cleanGuest,
                        "contactPhone" to cleanPhone,
                        "dateDhaka" to cleanDate,
                        "timeSlot" to cleanTime,
                        "guestCount" to guestCount.toLong(),
                        "specialRequest" to specialRequest.trim().take(300),
                        "status" to ReservationStatus.PENDING.label,
                        "staffNote" to "",
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

    suspend fun cancelReservation(reservationId: String): Result<Unit> {
        return try {
            requireUid()
            val firestore = db ?: return Result.failure(IllegalStateException("Cloud database unavailable."))
            firestore.collection("reservations").document(reservationId)
                .update(
                    mapOf(
                        "status" to ReservationStatus.CANCELLED.label,
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

    suspend fun updateReservationStatus(
        reservationId: String,
        newStatus: ReservationStatus,
        staffNote: String
    ): Result<Unit> {
        return try {
            requireUid()
            val firestore = db ?: return Result.failure(IllegalStateException("Cloud database unavailable."))
            // Prefer Cloud Function so FCM notification is dispatched; fall back to direct Firestore update if CF not deployed
            val fnResult = functions?.let { fn ->
                runCatching {
                    fn.getHttpsCallable("updateReservationStatus")
                        .call(
                            mapOf(
                                "reservationId" to reservationId,
                                "status" to newStatus.label,
                                "staffNote" to staffNote.trim().take(300)
                            )
                        )
                        .await()
                }
            }
            if (fnResult == null || fnResult.isFailure) {
                firestore.collection("reservations").document(reservationId)
                    .update(
                        mapOf(
                            "status" to newStatus.label,
                            "staffNote" to staffNote.trim().take(300),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    .await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "PlatformEngagementRepo"
    }
}
