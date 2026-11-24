package com.example.data

import android.content.Context
import com.example.R
import com.example.model.CoffeeShop
import com.example.model.CoffeeShopJson
import com.example.model.CommunityShopDoc
import com.example.model.DhakaArea
import com.example.model.ShopReview
import com.example.model.ShopTimeParser
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

interface CoffeeAssetLoader {
    suspend fun loadCatalog(): List<CoffeeShop>
}

class AssetCoffeeLoader(
    private val context: Context,
    private val moshi: Moshi = Moshi.Builder().build()
) : CoffeeAssetLoader {

    override suspend fun loadCatalog(): List<CoffeeShop> = withContext(Dispatchers.IO) {
        val listType = Types.newParameterizedType(List::class.java, CoffeeShopJson::class.java)
        val adapter = moshi.adapter<List<CoffeeShopJson>>(listType)
        val rawJson = context.assets.open(ASSET_FILE_NAME).use { inputStream ->
            BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                reader.readText()
            }
        }
        val parsed = adapter.fromJson(rawJson).orEmpty()
        parsed.map { it.toDomain() }
    }

    companion object {
        private const val ASSET_FILE_NAME = "coffee_shops.json"
    }
}

class InMemoryCoffeeAssetLoader(
    private val shops: List<CoffeeShop>
) : CoffeeAssetLoader {
    override suspend fun loadCatalog(): List<CoffeeShop> = shops
}

interface CommunityDataSource {
    fun observeCommunityShops(): Flow<List<CoffeeShop>>
    fun observePublicReviews(): Flow<List<ShopReview>>
    suspend fun addCommunityShop(draft: NewCoffeeShopInput): Result<Unit>
    suspend fun submitReview(shopId: String, rating: Int, comment: String): Result<Unit>
    suspend fun deleteReview(reviewId: String): Result<Unit>
}

data class NewCoffeeShopInput(
    val name: String,
    val area: DhakaArea,
    val address: String,
    val openTime: String,
    val closeTime: String,
    val popularItems: List<String>,
    val priceRange: String,
    val phone: String,
    val familyFriendly: Boolean
)

class FirestoreCommunityDataSource(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) : CommunityDataSource {

    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    override fun observeCommunityShops(): Flow<List<CoffeeShop>> = flow {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = COMMUNITY_SHOPS_COLLECTION
        db.collection(path)
            .whereEqualTo("visibility", "public")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    CommunityShopDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).map { it.toDomain() }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
            .collect { emit(it) }
    }

    override fun observePublicReviews(): Flow<List<ShopReview>> = flow {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = SHOP_REVIEWS_COLLECTION
        db.collection(path)
            .whereEqualTo("visibility", "public")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    ShopReview::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
            .collect { emit(it) }
    }

    override suspend fun addCommunityShop(draft: NewCoffeeShopInput): Result<Unit> {
        var docPath = COMMUNITY_SHOPS_COLLECTION
        return try {
            val uid = requireUserId()
            val cleanName = draft.name.trim()
            val cleanAddress = draft.address.trim()
            val cleanOpen = draft.openTime.trim()
            val cleanClose = draft.closeTime.trim()
            val cleanPrice = draft.priceRange.trim()
            val cleanPhone = draft.phone.trim()
            val cleanItems = draft.popularItems.map { it.trim() }.filter { it.isNotEmpty() }.take(20)

            require(cleanName.length in 2..80) { "Shop name must be between 2 and 80 characters." }
            require(draft.area != DhakaArea.ALL) { "Please select a valid Dhaka area." }
            require(cleanAddress.length in 3..200) { "Address must be between 3 and 200 characters." }
            require(ShopTimeParser.parseMinuteOfDay(cleanOpen) != null) { "Open time must be valid (e.g. 10:00 AM or 16:00)." }
            require(ShopTimeParser.parseMinuteOfDay(cleanClose) != null) { "Close time must be valid (e.g. 02:00 AM or 02:00)." }
            require(cleanPrice.length in 1..40) { "Price range is required (max 40 characters)." }
            require(cleanPhone.length <= 24) { "Phone number must be 24 characters or fewer." }

            val docRef = db.collection(COMMUNITY_SHOPS_COLLECTION).document()
            docPath = docRef.path
            val payload = mapOf(
                "ownerId" to uid,
                "visibility" to "public",
                "name" to cleanName,
                "area" to draft.area.displayName,
                "address" to cleanAddress,
                "openTime" to cleanOpen,
                "closeTime" to cleanClose,
                "popularItems" to cleanItems,
                "priceRange" to cleanPrice,
                "phone" to cleanPhone,
                "familyFriendly" to draft.familyFriendly,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    override suspend fun submitReview(shopId: String, rating: Int, comment: String): Result<Unit> {
        var docPath = SHOP_REVIEWS_COLLECTION
        return try {
            val uid = requireUserId()
            val cleanShopId = shopId.trim()
            val cleanComment = comment.trim()
            val authorName = (auth.currentUser?.displayName?.trim().takeIf { !it.isNullOrEmpty() }
                ?: "Dhaka Coffee Lover").take(60)

            require(cleanShopId.matches(ID_REGEX) && cleanShopId.length <= 128) {
                "Invalid coffee shop ID."
            }
            require(rating in 1..5) { "Rating must be between 1 and 5 stars." }
            require(cleanComment.length in 1..500) { "Review comment must be 1 to 500 characters." }

            val docRef = db.collection(SHOP_REVIEWS_COLLECTION).document()
            docPath = docRef.path
            val payload = mapOf(
                "shopId" to cleanShopId,
                "userId" to uid,
                "authorName" to authorName,
                "rating" to rating.toLong(),
                "comment" to cleanComment,
                "visibility" to "public",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    override suspend fun deleteReview(reviewId: String): Result<Unit> {
        var docPath = "$SHOP_REVIEWS_COLLECTION/$reviewId"
        return try {
            requireUserId()
            require(reviewId.isNotBlank() && reviewId.matches(ID_REGEX)) { "Invalid review ID." }
            val docRef = db.collection(SHOP_REVIEWS_COLLECTION).document(reviewId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    companion object {
        const val COMMUNITY_SHOPS_COLLECTION = "community_shops"
        const val SHOP_REVIEWS_COLLECTION = "shop_reviews"
        private val ID_REGEX = Regex("^[a-zA-Z0-9_\\-]+$")
    }
}

class InMemoryCommunityDataSource(
    initialShops: List<CoffeeShop> = emptyList(),
    initialReviews: List<ShopReview> = emptyList(),
    private val currentUserId: String = "test_user"
) : CommunityDataSource {
    private val shopsFlow = MutableStateFlow(initialShops)
    private val reviewsFlow = MutableStateFlow(initialReviews)
    private var counter = 0

    override fun observeCommunityShops(): Flow<List<CoffeeShop>> = shopsFlow
    override fun observePublicReviews(): Flow<List<ShopReview>> = reviewsFlow

    override suspend fun addCommunityShop(draft: NewCoffeeShopInput): Result<Unit> {
        counter += 1
        val shop = CoffeeShop(
            id = "community-$counter",
            name = draft.name.trim(),
            area = draft.area,
            areaLabel = draft.area.displayName,
            address = draft.address.trim(),
            openTime = draft.openTime.trim(),
            closeTime = draft.closeTime.trim(),
            hoursDisplay = ShopTimeParser.formatHoursRange(draft.openTime, draft.closeTime),
            openMinuteOfDay = ShopTimeParser.parseMinuteOfDay(draft.openTime),
            closeMinuteOfDay = ShopTimeParser.parseMinuteOfDay(draft.closeTime),
            popularItems = draft.popularItems,
            priceRange = draft.priceRange.trim(),
            rating = 0.0,
            phone = draft.phone.trim(),
            isFamilyFriendly = draft.familyFriendly,
            isCommunitySubmitted = true
        )
        shopsFlow.value = shopsFlow.value + shop
        return Result.success(Unit)
    }

    override suspend fun submitReview(shopId: String, rating: Int, comment: String): Result<Unit> {
        counter += 1
        val review = ShopReview(
            id = "review-$counter",
            shopId = shopId,
            userId = currentUserId,
            authorName = "Test Reviewer",
            rating = rating.toLong(),
            comment = comment.trim(),
            visibility = "public"
        )
        reviewsFlow.value = listOf(review) + reviewsFlow.value
        return Result.success(Unit)
    }

    override suspend fun deleteReview(reviewId: String): Result<Unit> {
        reviewsFlow.value = reviewsFlow.value.filterNot { it.id == reviewId }
        return Result.success(Unit)
    }
}

data class IndexedCoffeeCatalog(
    val allShops: List<CoffeeShop>,
    val byArea: Map<DhakaArea, List<CoffeeShop>>,
    val byId: Map<String, CoffeeShop>
) {
    fun sliceForArea(area: DhakaArea): List<CoffeeShop> {
        return if (area == DhakaArea.ALL) allShops else byArea[area].orEmpty()
    }

    companion object {
        fun fromShops(shops: List<CoffeeShop>): IndexedCoffeeCatalog {
            return IndexedCoffeeCatalog(
                allShops = shops,
                byArea = shops.groupBy { it.area },
                byId = shops.associateBy { it.id }
            )
        }
    }
}

class CoffeeRepository(
    private val assetLoader: CoffeeAssetLoader,
    private val communityDataSource: CommunityDataSource
) {
    suspend fun loadInitialCatalog(): List<CoffeeShop> = assetLoader.loadCatalog()

    fun observeCommunityShops(): Flow<List<CoffeeShop>> = communityDataSource.observeCommunityShops()

    fun observePublicReviews(): Flow<List<ShopReview>> = communityDataSource.observePublicReviews()

    suspend fun addCommunityShop(draft: NewCoffeeShopInput): Result<Unit> =
        communityDataSource.addCommunityShop(draft)

    suspend fun submitReview(shopId: String, rating: Int, comment: String): Result<Unit> =
        communityDataSource.submitReview(shopId, rating, comment)

    suspend fun deleteReview(reviewId: String): Result<Unit> =
        communityDataSource.deleteReview(reviewId)
}
