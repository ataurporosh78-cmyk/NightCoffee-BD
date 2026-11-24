package com.example.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.PlatformCatalogRepository
import com.example.data.PlatformCommunityRepository
import com.example.data.PlatformEngagementRepository
import com.example.data.local.ConnectivityObserver
import com.example.model.AggregatedCrowdStatus
import com.example.model.CoffeeEventDoc
import com.example.model.CoffeeShop
import com.example.model.CouponDiscountType
import com.example.model.CouponDoc
import com.example.model.CouponRedemptionDoc
import com.example.model.CrowdLevel
import com.example.model.CrowdReportDoc
import com.example.model.DhakaArea
import com.example.model.DigitalMenuItemDoc
import com.example.model.EventCategory
import com.example.model.EventRegistrationDoc
import com.example.model.LiveShopStatus
import com.example.model.MenuCategory
import com.example.model.NotificationCategory
import com.example.model.NotificationItemDoc
import com.example.model.PhotoReviewDoc
import com.example.model.ReportVerificationState
import com.example.model.ReportedStatusType
import com.example.model.ReservationDoc
import com.example.model.ReservationStatus
import com.example.model.ResolvedShopStatus
import com.example.model.ShopOverlayDoc
import com.example.model.ShopStatusReportDoc
import com.example.model.ShopTimeParser
import com.example.model.SupportTicketDoc
import com.example.model.SupportTicketStatus
import com.example.model.SupportTicketType
import com.example.model.UserPreferenceDoc
import com.example.model.UserRoleState
import com.example.model.VerificationRequestDoc
import com.example.util.PlatformLogic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class PlatformViewModel(
    private val currentUid: String?,
    private val catalogRepo: PlatformCatalogRepository,
    private val engagementRepo: PlatformEngagementRepository,
    private val communityRepo: PlatformCommunityRepository,
    connectivityObserver: ConnectivityObserver
) : ViewModel() {

    private val signedInUid: String? = currentUid?.takeIf { it.isNotBlank() && it != "guest" }

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    val isOnline: StateFlow<Boolean> = connectivityObserver.isOnlineFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = connectivityObserver.isCurrentlyOnline()
        )

    val pendingSyncCount: StateFlow<Int> = communityRepo.observePendingSyncCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), 0)

    val userRole: StateFlow<UserRoleState> = catalogRepo.observeUserRole(signedInUid)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), UserRoleState())

    val shopOverlays: StateFlow<Map<String, ShopOverlayDoc>> = catalogRepo.observeShopOverlays()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyMap())

    val statusReports: StateFlow<List<ShopStatusReportDoc>> = userRole
        .flatMapLatest { role -> catalogRepo.observeStatusReports(includeAllForAdmin = role.isAdmin) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    val crowdReports: StateFlow<List<CrowdReportDoc>> = userRole
        .flatMapLatest { role -> catalogRepo.observeCrowdReports(includeAllForAdmin = role.isAdmin) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    private val couponsPairFlow = engagementRepo.observeCoupons()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList<CouponDoc>() to false)

    val coupons: StateFlow<List<CouponDoc>> = couponsPairFlow
        .map { it.first }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    val couponsOfflineCached: StateFlow<Boolean> = couponsPairFlow
        .map { it.second }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), false)

    val myCouponRedemptions: StateFlow<Map<String, CouponRedemptionDoc>> =
        engagementRepo.observeMyCouponRedemptions(signedInUid)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyMap())

    val photoReviews: StateFlow<List<PhotoReviewDoc>> = userRole
        .flatMapLatest { role -> engagementRepo.observePhotoReviews(includeFlaggedForAdmin = role.isAdmin) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    val myReviewVotes: StateFlow<Set<String>> = engagementRepo.observeMyReviewVotes(signedInUid)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptySet())

    val myReservations: StateFlow<List<ReservationDoc>> = engagementRepo.observeMyReservations(signedInUid)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    val adminReservations: StateFlow<List<ReservationDoc>> = userRole
        .flatMapLatest { role -> engagementRepo.observeAllReservationsForAdmin(role.isAdmin) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    private val eventsPairFlow = communityRepo.observeEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList<CoffeeEventDoc>() to false)

    val events: StateFlow<List<CoffeeEventDoc>> = eventsPairFlow
        .map { it.first }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    val eventsOfflineCached: StateFlow<Boolean> = eventsPairFlow
        .map { it.second }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), false)

    val myEventRegistrations: StateFlow<Map<String, EventRegistrationDoc>> =
        communityRepo.observeMyEventRegistrations(signedInUid)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyMap())

    val notifications: StateFlow<List<NotificationItemDoc>> = communityRepo.observeNotifications(signedInUid)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    val userPreferences: StateFlow<UserPreferenceDoc> = communityRepo.observeUserPreferences(signedInUid)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), UserPreferenceDoc())

    val compareShopIds: StateFlow<List<String>> = userPreferences
        .map { it.compareShopIds }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    val myVerificationRequests: StateFlow<List<VerificationRequestDoc>> =
        communityRepo.observeMyVerificationRequests(signedInUid)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    val adminVerificationRequests: StateFlow<List<VerificationRequestDoc>> = userRole
        .flatMapLatest { role -> communityRepo.observeAdminVerificationRequests(role.isAdmin) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    val mySupportTickets: StateFlow<List<SupportTicketDoc>> =
        communityRepo.observeMySupportTickets(signedInUid)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    val adminSupportTickets: StateFlow<List<SupportTicketDoc>> = userRole
        .flatMapLatest { role -> communityRepo.observeAdminSupportTickets(role.isAdmin) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    init {
        viewModelScope.launch {
            isOnline.collect { online ->
                if (online && pendingSyncCount.value > 0) {
                    communityRepo.triggerOfflineSync()
                }
            }
        }
    }

    fun observeMenuForShop(shopId: String): Flow<Pair<List<DigitalMenuItemDoc>, Boolean>> =
        catalogRepo.observeAllMenuItemsForShop(shopId)

    fun observeShopReservations(shopId: String): Flow<List<ReservationDoc>> =
        engagementRepo.observeShopReservations(shopId)

    fun resolveStatusForShop(shop: CoffeeShop): ResolvedShopStatus {
        return PlatformLogic.resolveShopStatus(
            shop = shop,
            overlay = shopOverlays.value[shop.id],
            reports = statusReports.value,
            minuteOfDay = ShopTimeParser.currentDeviceMinuteOfDay()
        )
    }

    fun crowdForShop(shopId: String): AggregatedCrowdStatus {
        return PlatformLogic.aggregateCrowdReports(shopId, crowdReports.value)
    }

    fun triggerOfflineSyncNow() {
        communityRepo.triggerOfflineSync()
    }

    suspend fun toggleCompareShop(shopId: String): Boolean {
        val current = userPreferences.value.compareShopIds
        val (updated, changed) = PlatformLogic.toggleCompareShopIds(current, shopId)
        if (changed) {
            communityRepo.saveCompareIds(updated, userPreferences.value)
        }
        return changed
    }

    suspend fun clearCompareShops() {
        communityRepo.saveCompareIds(emptyList(), userPreferences.value)
    }

    suspend fun submitStatusReport(
        shopId: String,
        status: ReportedStatusType,
        note: String
    ): Result<Boolean> = runBusy {
        val isStaff = userRole.value.canManageShop(shopId)
        catalogRepo.submitStatusReport(shopId, status, note, isStaff)
    }

    suspend fun updateOfficialShopStatus(shop: CoffeeShop, status: LiveShopStatus): Result<Unit> = runBusy {
        catalogRepo.updateOfficialShopStatus(shop, status)
    }

    suspend fun verifyStatusReport(reportId: String, state: ReportVerificationState): Result<Unit> = runBusy {
        catalogRepo.verifyStatusReport(reportId, state)
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
    ): Result<Unit> = runBusy {
        catalogRepo.upsertMenuItem(
            existingId = existingId,
            shopId = shopId,
            category = category,
            name = name,
            description = description,
            imageUrl = imageUrl,
            priceBdt = priceBdt,
            available = available,
            special = special
        )
    }

    suspend fun deleteMenuItem(itemId: String): Result<Unit> = runBusy {
        catalogRepo.deleteMenuItem(itemId)
    }

    suspend fun submitCrowdReport(shopId: String, level: CrowdLevel, note: String): Result<Boolean> = runBusy {
        catalogRepo.submitCrowdReport(shopId, level, note)
    }

    suspend fun flagCrowdReport(reportId: String, reason: String): Result<Unit> = runBusy {
        catalogRepo.flagCrowdReport(reportId, reason)
    }

    suspend fun dismissCrowdReport(reportId: String): Result<Unit> = runBusy {
        catalogRepo.dismissCrowdReport(reportId)
    }

    suspend fun saveCoupon(coupon: CouponDoc): Result<Unit> = runBusy {
        engagementRepo.saveCoupon(coupon)
    }

    suspend fun redeemCoupon(couponId: String): Result<String> = runBusy {
        engagementRepo.redeemCoupon(couponId)
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
    ): Result<Unit> = runBusy {
        engagementRepo.createCoupon(
            shopId = shopId,
            shopName = shopName,
            code = code,
            title = title,
            description = description,
            discountType = discountType,
            discountValue = discountValue,
            minSpendBdt = minSpendBdt,
            maxUsageLimit = maxUsageLimit,
            terms = terms,
            validDays = validDays
        )
    }

    suspend fun toggleCouponActive(coupon: CouponDoc): Result<Unit> = runBusy {
        engagementRepo.deactivateCoupon(coupon)
    }

    suspend fun submitPhotoReview(
        shopId: String,
        rating: Int,
        comment: String,
        pickedImageUris: List<Uri>
    ): Result<Unit> = runBusy {
        engagementRepo.submitPhotoReview(shopId, rating, comment, pickedImageUris)
    }

    suspend fun toggleHelpfulVote(reviewId: String): Result<Unit> {
        val voted = reviewId in myReviewVotes.value
        return engagementRepo.toggleHelpfulVote(reviewId, voted)
    }

    suspend fun reportReview(reviewId: String, reason: String): Result<Unit> = runBusy {
        engagementRepo.reportReview(reviewId, reason)
    }

    suspend fun moderateReview(reviewId: String, visibility: String): Result<Unit> = runBusy {
        engagementRepo.moderateReview(reviewId, visibility)
    }

    suspend fun deletePhotoReview(reviewId: String): Result<Unit> = runBusy {
        engagementRepo.deletePhotoReview(reviewId)
    }

    suspend fun createReservation(
        shopId: String,
        shopName: String,
        guestName: String,
        contactPhone: String,
        dateDhaka: String,
        timeSlot: String,
        guestCount: Int,
        specialRequest: String
    ): Result<Unit> = runBusy {
        engagementRepo.createReservation(
            shopId = shopId,
            shopName = shopName,
            guestName = guestName,
            contactPhone = contactPhone,
            dateDhaka = dateDhaka,
            timeSlot = timeSlot,
            guestCount = guestCount,
            specialRequest = specialRequest,
            existingMyReservations = myReservations.value
        )
    }

    suspend fun cancelReservation(reservationId: String): Result<Unit> = runBusy {
        engagementRepo.cancelReservation(reservationId)
    }

    suspend fun updateReservationStatus(
        reservationId: String,
        status: ReservationStatus,
        staffNote: String
    ): Result<Unit> = runBusy {
        engagementRepo.updateReservationStatus(reservationId, status, staffNote)
    }

    suspend fun toggleEventRegistration(
        eventId: String,
        targetStatus: String,
        note: String
    ): Result<Unit> = runBusy {
        val exists = eventId in myEventRegistrations.value
        communityRepo.toggleEventRegistration(eventId, targetStatus, note, exists)
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
    ): Result<Unit> = runBusy {
        communityRepo.createEvent(
            title = title,
            category = category,
            description = description,
            venueName = venueName,
            area = area,
            shopId = shopId,
            eventDateDhaka = eventDateDhaka,
            startTime = startTime,
            endTime = endTime,
            organizer = organizer,
            registrationInfo = registrationInfo
        )
    }

    suspend fun markNotificationRead(item: NotificationItemDoc): Result<Unit> =
        communityRepo.markNotificationRead(item)

    suspend fun saveNotificationPreferences(updated: UserPreferenceDoc): Result<Unit> = runBusy {
        communityRepo.saveUserPreferences(updated)
    }

    suspend fun sendPlatformAnnouncement(
        category: NotificationCategory,
        title: String,
        body: String,
        targetRoute: String,
        targetId: String
    ): Result<Unit> = runBusy {
        communityRepo.sendPlatformAnnouncement(category, title, body, targetRoute, targetId)
    }

    suspend fun submitVerificationRequest(
        shopId: String,
        shopName: String,
        businessOwnerName: String,
        businessPhone: String,
        registrationNote: String
    ): Result<Unit> = runBusy {
        communityRepo.submitVerificationRequest(
            shopId = shopId,
            shopName = shopName,
            businessOwnerName = businessOwnerName,
            businessPhone = businessPhone,
            registrationNote = registrationNote
        )
    }

    suspend fun reviewVerificationRequest(
        request: VerificationRequestDoc,
        approve: Boolean
    ): Result<Unit> = runBusy {
        communityRepo.reviewVerificationRequest(request, approve)
    }

    suspend fun submitSupportTicket(
        type: SupportTicketType,
        targetId: String,
        targetName: String,
        subject: String,
        description: String,
        contactInfo: String
    ): Result<Boolean> = runBusy {
        communityRepo.submitSupportTicket(type, targetId, targetName, subject, description, contactInfo)
    }

    suspend fun updateSupportTicketAdmin(
        ticketId: String,
        status: SupportTicketStatus,
        adminResponse: String
    ): Result<Unit> = runBusy {
        communityRepo.updateSupportTicketAdmin(ticketId, status, adminResponse)
    }

    private suspend fun <T> runBusy(block: suspend () -> Result<T>): Result<T> {
        _isBusy.value = true
        return try {
            block()
        } finally {
            _isBusy.value = false
        }
    }

    companion object {
        fun factory(context: Context, currentUid: String?): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PlatformViewModel(
                        currentUid = currentUid,
                        catalogRepo = PlatformCatalogRepository(appContext),
                        engagementRepo = PlatformEngagementRepository(appContext),
                        communityRepo = PlatformCommunityRepository(appContext),
                        connectivityObserver = ConnectivityObserver(appContext)
                    ) as T
                }
            }
        }
    }
}
