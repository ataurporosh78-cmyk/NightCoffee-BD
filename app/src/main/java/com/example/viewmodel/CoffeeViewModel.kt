package com.example.viewmodel

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AssetCoffeeLoader
import com.example.data.CoffeeRepository
import com.example.data.DataStoreFavoritesStorage
import com.example.data.FavoritesStorage
import com.example.data.FirebaseSafeAccess
import com.example.data.FirestoreCommunityDataSource
import com.example.data.InMemoryFavoritesStorage
import com.example.data.IndexedCoffeeCatalog
import com.example.data.LocationProvider
import com.example.data.NewCoffeeShopInput
import com.example.data.UnavailableCommunityDataSource
import com.example.model.CoffeeShop
import com.example.model.DhakaArea
import com.example.model.ExploreCategory
import com.example.model.ShopEnrichment
import com.example.model.ShopFacility
import com.example.model.ShopReview
import com.example.model.ShopTimeParser
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class HomeTab {
    ALL_CAFES,
    FAVORITES
}

@Immutable
data class CoffeeShopListItem(
    val shop: CoffeeShop,
    val isOpenNow: Boolean,
    val isFavorite: Boolean,
    val effectiveRating: Double,
    val reviewCount: Int,
    val distanceKm: Double? = null,
    val isOpenAfterMidnight: Boolean = shop.isOpenAfterMidnight
)

@Immutable
data class CoffeeDetailState(
    val item: CoffeeShopListItem,
    val reviews: List<ShopReview>
)

@Immutable
data class CoffeeHomeUiState(
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val selectedTab: HomeTab = HomeTab.ALL_CAFES,
    val selectedCategory: ExploreCategory = ExploreCategory.ALL,
    val openNowOnly: Boolean = false,
    val familyFriendlyOnly: Boolean = false,
    val selectedArea: DhakaArea = DhakaArea.ALL,
    val localTimeDisplay: String = "",
    val totalCatalogSize: Int = 0,
    val favoritesCount: Int = 0,
    val hasLocationPermission: Boolean = false,
    val shops: List<CoffeeShopListItem> = emptyList(),
    val featuredShops: List<CoffeeShopListItem> = emptyList(),
    val openNowShops: List<CoffeeShopListItem> = emptyList(),
    val popularShops: List<CoffeeShopListItem> = emptyList(),
    val favoriteShops: List<CoffeeShopListItem> = emptyList(),
    val allCatalogItems: List<CoffeeShopListItem> = emptyList(),
    val errorMessage: String? = null,
    val isSubmittingAction: Boolean = false
)

private data class HomeFilterState(
    val selectedTab: HomeTab = HomeTab.ALL_CAFES,
    val selectedCategory: ExploreCategory = ExploreCategory.ALL,
    val openNowOnly: Boolean = false,
    val familyFriendlyOnly: Boolean = false,
    val selectedArea: DhakaArea = DhakaArea.ALL,
    val userLocation: Pair<Double, Double>? = null,
    val hasLocationPermission: Boolean = false,
    val refreshTick: Long = 0L
)

@OptIn(FlowPreview::class)
class CoffeeViewModel(
    private val repository: CoffeeRepository,
    private val favoritesStorage: FavoritesStorage = InMemoryFavoritesStorage(),
    private val currentMinuteProvider: () -> Int = { ShopTimeParser.currentDeviceMinuteOfDay() },
    private val currentTimeFormatter: () -> String = { ShopTimeParser.currentDeviceTimeFormatted() },
    private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
    searchDebounceMs: Long = 300L
) : ViewModel() {

    private val rawSearchQuery = MutableStateFlow("")
    private val debouncedSearchQuery: Flow<String> = if (searchDebounceMs > 0L) {
        rawSearchQuery.debounce(searchDebounceMs)
    } else {
        rawSearchQuery
    }

    private val filterState = MutableStateFlow(HomeFilterState())
    private val assetShopsState = MutableStateFlow<List<CoffeeShop>>(emptyList())
    private val catalogLoadedState = MutableStateFlow(false)
    private val actionErrorState = MutableStateFlow<String?>(null)
    private val submittingActionState = MutableStateFlow(false)

    private val _searchInputText = MutableStateFlow("")
    val searchInputText: StateFlow<String> = _searchInputText.asStateFlow()

    private val communityShopsFlow: Flow<List<CoffeeShop>> = repository.observeCommunityShops()
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Community shops stream error", rootError)
            actionErrorState.value = rootError.message ?: "Unable to sync community cafes"
            emit(emptyList())
        }

    private val publicReviewsFlow: Flow<List<ShopReview>> = repository.observePublicReviews()
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Reviews stream error", rootError)
            actionErrorState.value = rootError.message ?: "Unable to sync cafe reviews"
            emit(emptyList())
        }

    private val indexedCatalogFlow: Flow<IndexedCoffeeCatalog> = combine(
        assetShopsState,
        communityShopsFlow
    ) { assetShops, communityShops ->
        val merged = buildList(assetShops.size + communityShops.size) {
            addAll(communityShops)
            addAll(assetShops)
        }
        IndexedCoffeeCatalog.fromShops(merged)
    }.flowOn(computeDispatcher)

    private val reviewsByShopFlow: Flow<Map<String, List<ShopReview>>> = publicReviewsFlow
        .map { reviews ->
            reviews
                .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                .groupBy { it.shopId }
        }
        .flowOn(computeDispatcher)

    private val baseInputsFlow = combine(
        catalogLoadedState,
        indexedCatalogFlow,
        reviewsByShopFlow,
        favoritesStorage.favoriteIds
    ) { loaded, catalog, reviewsByShop, favoriteIds ->
        BaseCatalogInputs(loaded, catalog, reviewsByShop, favoriteIds)
    }

    private val filterInputsFlow = combine(
        debouncedSearchQuery,
        filterState,
        actionErrorState,
        submittingActionState
    ) { query, filters, errorMsg, submitting ->
        FilterInputs(query, filters, errorMsg, submitting)
    }

    val uiState: StateFlow<CoffeeHomeUiState> = combine(
        baseInputsFlow,
        filterInputsFlow
    ) { base, filters ->
        computeHomeUiState(base, filters)
    }
        .flowOn(computeDispatcher)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = CoffeeHomeUiState(
                isLoading = true,
                localTimeDisplay = currentTimeFormatter()
            )
        )

    init {
        viewModelScope.launch {
            try {
                val loaded = repository.loadInitialCatalog()
                assetShopsState.value = loaded
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load coffee_shops.json", e)
                actionErrorState.value = e.localizedMessage ?: "Failed to load coffee catalog"
            } finally {
                catalogLoadedState.value = true
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchInputText.value = query
        rawSearchQuery.value = query
    }

    fun clearSearch() {
        _searchInputText.value = ""
        rawSearchQuery.value = ""
    }

    fun selectTab(tab: HomeTab) {
        filterState.update { it.copy(selectedTab = tab) }
    }

    fun selectCategory(category: ExploreCategory) {
        filterState.update {
            it.copy(
                selectedCategory = category,
                openNowOnly = category == ExploreCategory.OPEN_NOW
            )
        }
    }

    fun setOpenNowOnly(enabled: Boolean) {
        filterState.update {
            it.copy(
                openNowOnly = enabled,
                selectedCategory = if (enabled) {
                    ExploreCategory.OPEN_NOW
                } else if (it.selectedCategory == ExploreCategory.OPEN_NOW) {
                    ExploreCategory.ALL
                } else {
                    it.selectedCategory
                }
            )
        }
    }

    fun setFamilyFriendlyOnly(enabled: Boolean) {
        filterState.update { it.copy(familyFriendlyOnly = enabled) }
    }

    fun selectArea(area: DhakaArea) {
        filterState.update { it.copy(selectedArea = area) }
    }

    fun refreshLocation(context: Context) {
        val granted = LocationProvider.hasCoarseLocationPermission(context)
        val coords = if (granted) LocationProvider.getLastKnownCoordinates(context) else null
        filterState.update {
            it.copy(
                hasLocationPermission = granted,
                userLocation = coords
            )
        }
    }

    fun updateLocation(latitude: Double?, longitude: Double?, hasPermission: Boolean) {
        val coords = if (latitude != null && longitude != null) latitude to longitude else null
        filterState.update {
            it.copy(
                hasLocationPermission = hasPermission,
                userLocation = coords
            )
        }
    }

    fun toggleFavorite(shopId: String) {
        viewModelScope.launch {
            favoritesStorage.toggleFavorite(shopId)
        }
    }

    fun resetFilters() {
        _searchInputText.value = ""
        rawSearchQuery.value = ""
        filterState.update { current ->
            HomeFilterState(
                userLocation = current.userLocation,
                hasLocationPermission = current.hasLocationPermission,
                refreshTick = System.currentTimeMillis()
            )
        }
    }

    fun refreshTime() {
        filterState.update { it.copy(refreshTick = it.refreshTick + 1) }
    }

    fun clearError() {
        actionErrorState.value = null
    }

    fun observeShopDetail(shopId: String): Flow<CoffeeDetailState?> {
        return combine(
            indexedCatalogFlow,
            reviewsByShopFlow,
            favoritesStorage.favoriteIds,
            filterState
        ) { catalog, reviewsByShop, favoriteIds, filters ->
            val shop = catalog.byId[shopId] ?: return@combine null
            val shopReviews = reviewsByShop[shop.id].orEmpty()
            val item = toListItem(
                shop = shop,
                minuteOfDay = currentMinuteProvider(),
                isFavorite = shop.id in favoriteIds,
                shopReviews = shopReviews,
                userLocation = filters.userLocation
            )
            CoffeeDetailState(item = item, reviews = shopReviews)
        }.flowOn(computeDispatcher)
    }

    suspend fun addShop(input: NewCoffeeShopInput): Result<Unit> {
        submittingActionState.value = true
        return try {
            val result = repository.addCommunityShop(input)
            result.onFailure { err ->
                actionErrorState.value = err.localizedMessage ?: "Failed to publish coffee shop"
            }
            result
        } finally {
            submittingActionState.value = false
        }
    }

    suspend fun submitReview(shopId: String, rating: Int, comment: String): Result<Unit> {
        submittingActionState.value = true
        return try {
            val result = repository.submitReview(shopId, rating, comment)
            result.onFailure { err ->
                actionErrorState.value = err.localizedMessage ?: "Failed to post review"
            }
            result
        } finally {
            submittingActionState.value = false
        }
    }

    suspend fun deleteReview(reviewId: String): Result<Unit> {
        submittingActionState.value = true
        return try {
            val result = repository.deleteReview(reviewId)
            result.onFailure { err ->
                actionErrorState.value = err.localizedMessage ?: "Failed to delete review"
            }
            result
        } finally {
            submittingActionState.value = false
        }
    }

    private fun computeHomeUiState(
        base: BaseCatalogInputs,
        filters: FilterInputs
    ): CoffeeHomeUiState {
        val minuteOfDay = currentMinuteProvider()
        val lowercaseQuery = filters.searchQuery.trim().lowercase(Locale.US)
        val areaSlice = base.catalog.sliceForArea(filters.filters.selectedArea)
        val userLocation = filters.filters.userLocation
        val category = filters.filters.selectedCategory

        val filteredList = areaSlice.mapNotNull { shop ->
            val isFavorite = shop.id in base.favoriteIds
            if (filters.filters.selectedTab == HomeTab.FAVORITES && !isFavorite) return@mapNotNull null
            if (filters.filters.familyFriendlyOnly && !shop.isFamilyFriendly) return@mapNotNull null

            val isOpen = shop.isOpenAt(minuteOfDay)
            if (filters.filters.openNowOnly && !isOpen) return@mapNotNull null

            val matchesCategory = when (category) {
                ExploreCategory.ALL -> true
                ExploreCategory.OPEN_NOW -> isOpen
                ExploreCategory.AFTER_MIDNIGHT -> shop.isOpenAfterMidnight
                ExploreCategory.NEAR_ME -> true
                ExploreCategory.ROOFTOP -> ShopFacility.ROOFTOP in shop.resolvedFacilities
                ExploreCategory.WIFI -> ShopFacility.WIFI in shop.resolvedFacilities
                ExploreCategory.QUIET -> ShopFacility.QUIET in shop.resolvedFacilities
            }
            if (!matchesCategory) return@mapNotNull null
            if (!shop.matchesQuery(lowercaseQuery)) return@mapNotNull null

            toListItem(
                shop = shop,
                minuteOfDay = minuteOfDay,
                isFavorite = isFavorite,
                shopReviews = base.reviewsByShop[shop.id].orEmpty(),
                userLocation = userLocation
            )
        }.let { items ->
            if (category == ExploreCategory.NEAR_ME && userLocation != null) {
                items.sortedWith(compareBy<CoffeeShopListItem> { it.distanceKm ?: Double.MAX_VALUE }
                    .thenByDescending { it.effectiveRating })
            } else if (category == ExploreCategory.NEAR_ME) {
                val popularNearOrder = listOf(
                    DhakaArea.BANANI,
                    DhakaArea.GULSHAN,
                    DhakaArea.DHANMONDI,
                    DhakaArea.UTTARA,
                    DhakaArea.BASHUNDHARA
                )
                items.sortedWith(compareBy<CoffeeShopListItem> {
                    val idx = popularNearOrder.indexOf(it.shop.area)
                    if (idx == -1) Int.MAX_VALUE else idx
                }.thenByDescending { it.effectiveRating })
            } else {
                items
            }
        }

        // Curated Home shelves respect selectedArea & userLocation seamlessly
        val homeSourceItems = areaSlice.map { shop ->
            toListItem(
                shop = shop,
                minuteOfDay = minuteOfDay,
                isFavorite = shop.id in base.favoriteIds,
                shopReviews = base.reviewsByShop[shop.id].orEmpty(),
                userLocation = userLocation
            )
        }

        val featuredShops = homeSourceItems
            .sortedWith(compareByDescending<CoffeeShopListItem> { it.isOpenNow }
                .thenByDescending { it.isOpenAfterMidnight }
                .thenByDescending { it.effectiveRating })
            .take(8)

        val openNowShops = homeSourceItems
            .filter { it.isOpenNow }
            .sortedByDescending { it.effectiveRating }
            .take(12)

        val popularShops = homeSourceItems
            .sortedWith(compareByDescending<CoffeeShopListItem> { it.effectiveRating }
                .thenByDescending { it.reviewCount })
            .take(15)

        val favoriteShops = base.catalog.allShops
            .filter { it.id in base.favoriteIds }
            .map { shop ->
                toListItem(
                    shop = shop,
                    minuteOfDay = minuteOfDay,
                    isFavorite = true,
                    shopReviews = base.reviewsByShop[shop.id].orEmpty(),
                    userLocation = userLocation
                )
            }

        val allCatalogItems = base.catalog.allShops.map { shop ->
            toListItem(
                shop = shop,
                minuteOfDay = minuteOfDay,
                isFavorite = shop.id in base.favoriteIds,
                shopReviews = base.reviewsByShop[shop.id].orEmpty(),
                userLocation = userLocation
            )
        }

        return CoffeeHomeUiState(
            isLoading = !base.catalogLoaded,
            searchQuery = filters.searchQuery,
            selectedTab = filters.filters.selectedTab,
            selectedCategory = category,
            openNowOnly = filters.filters.openNowOnly,
            familyFriendlyOnly = filters.filters.familyFriendlyOnly,
            selectedArea = filters.filters.selectedArea,
            localTimeDisplay = currentTimeFormatter(),
            totalCatalogSize = base.catalog.allShops.size,
            favoritesCount = base.favoriteIds.size,
            hasLocationPermission = filters.filters.hasLocationPermission,
            shops = filteredList,
            featuredShops = featuredShops,
            openNowShops = openNowShops,
            popularShops = popularShops,
            favoriteShops = favoriteShops,
            allCatalogItems = allCatalogItems,
            errorMessage = filters.errorMessage,
            isSubmittingAction = filters.submitting
        )
    }

    private fun toListItem(
        shop: CoffeeShop,
        minuteOfDay: Int,
        isFavorite: Boolean,
        shopReviews: List<ShopReview>,
        userLocation: Pair<Double, Double>?
    ): CoffeeShopListItem {
        val effectiveRating = computeEffectiveRating(shop.rating, shopReviews)
        val distanceKm = userLocation?.let { (uLat, uLng) ->
            ShopEnrichment.haversineKm(uLat, uLng, shop.latitude, shop.longitude)
        }
        return CoffeeShopListItem(
            shop = shop,
            isOpenNow = shop.isOpenAt(minuteOfDay),
            isFavorite = isFavorite,
            effectiveRating = effectiveRating,
            reviewCount = shopReviews.size,
            distanceKm = distanceKm,
            isOpenAfterMidnight = shop.isOpenAfterMidnight
        )
    }

    private fun computeEffectiveRating(baseRating: Double, reviews: List<ShopReview>): Double {
        if (reviews.isEmpty()) return baseRating
        val sum = reviews.sumOf { it.rating.toDouble() }
        return if (baseRating <= 0.0) {
            sum / reviews.size
        } else {
            ((baseRating * 5.0) + sum) / (5.0 + reviews.size)
        }
    }

    private data class BaseCatalogInputs(
        val catalogLoaded: Boolean,
        val catalog: IndexedCoffeeCatalog,
        val reviewsByShop: Map<String, List<ShopReview>>,
        val favoriteIds: Set<String>
    )

    private data class FilterInputs(
        val searchQuery: String,
        val filters: HomeFilterState,
        val errorMessage: String?,
        val submitting: Boolean
    )

    companion object {
        private const val TAG = "CoffeeViewModel"
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        fun factory(context: Context): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            val db = FirebaseSafeAccess.safeFirestore(appContext)
            val auth = FirebaseSafeAccess.safeAuth(appContext)
            val communityDataSource = if (db != null) {
                FirestoreCommunityDataSource(db = db, auth = auth)
            } else {
                UnavailableCommunityDataSource()
            }
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CoffeeViewModel(
                        repository = CoffeeRepository(
                            assetLoader = AssetCoffeeLoader(appContext),
                            communityDataSource = communityDataSource
                        ),
                        favoritesStorage = DataStoreFavoritesStorage(appContext)
                    ) as T
                }
            }
        }
    }
}
