package com.example.model

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class LiveShopStatus(val label: String) {
    OPEN_NOW("Open Now"),
    CLOSED("Closed"),
    TEMPORARILY_CLOSED("Temporarily Closed"),
    CLOSING_SOON("Closing Soon");

    companion object {
        fun fromRaw(raw: String?): LiveShopStatus? =
            entries.firstOrNull { it.name.equals(raw?.trim(), ignoreCase = true) }
    }
}

enum class ReportedStatusType(val label: String) {
    OPEN_NOW("Open Now"),
    CLOSED("Closed"),
    TEMPORARILY_CLOSED("Temporarily Closed"),
    CLOSING_SOON("Closing Soon"),
    INCORRECT_HOURS("Incorrect Hours");

    companion object {
        fun fromRaw(raw: String?): ReportedStatusType =
            entries.firstOrNull { it.name.equals(raw?.trim(), ignoreCase = true) } ?: OPEN_NOW
    }
}

enum class ReportVerificationState(val label: String) {
    UNVERIFIED("Unverified"),
    VERIFIED("Verified"),
    DISMISSED("Dismissed");

    companion object {
        fun fromRaw(raw: String?): ReportVerificationState =
            entries.firstOrNull { it.name.equals(raw?.trim(), ignoreCase = true) } ?: UNVERIFIED
    }
}

enum class MenuCategory(val displayName: String) {
    HOT_COFFEE("Hot Coffee"),
    COLD_COFFEE("Cold Coffee"),
    ESPRESSO("Espresso"),
    CAPPUCCINO("Cappuccino"),
    LATTE("Latte"),
    SNACKS("Snacks"),
    DESSERTS("Desserts");

    companion object {
        fun fromRaw(raw: String?): MenuCategory =
            entries.firstOrNull {
                it.displayName.equals(raw?.trim(), ignoreCase = true) ||
                    it.name.equals(raw?.trim(), ignoreCase = true)
            } ?: HOT_COFFEE
    }
}

enum class CrowdLevel(val label: String, val weight: Int) {
    QUIET("Quiet", 1),
    MODERATE("Moderate", 2),
    BUSY("Busy", 3),
    VERY_BUSY("Very Busy", 4);

    companion object {
        fun fromRaw(raw: String?): CrowdLevel =
            entries.firstOrNull { it.name.equals(raw?.trim(), ignoreCase = true) } ?: MODERATE
    }
}

enum class CouponDiscountType {
    PERCENTAGE,
    FIXED_BDT;

    companion object {
        fun fromRaw(raw: String?): CouponDiscountType =
            entries.firstOrNull { it.name.equals(raw?.trim(), ignoreCase = true) } ?: PERCENTAGE
    }
}

enum class ReservationStatus(val label: String) {
    PENDING("Pending"),
    CONFIRMED("Confirmed"),
    REJECTED("Rejected"),
    CANCELLED("Cancelled"),
    COMPLETED("Completed");

    companion object {
        fun fromRaw(raw: String?): ReservationStatus =
            entries.firstOrNull { it.label.equals(raw?.trim(), ignoreCase = true) || it.name.equals(raw?.trim(), ignoreCase = true) }
                ?: PENDING
    }
}

enum class NotificationCategory(val label: String, val topicName: String) {
    NEW_OPENING("New Openings", "new_openings"),
    EXCLUSIVE_OFFER("Exclusive Offers", "exclusive_offers"),
    SAVED_SHOP_UPDATE("Saved Shop Updates", "saved_shop_updates"),
    RESERVATION_UPDATE("Reservations", "reservation_updates"),
    COFFEE_EVENT("Coffee Events", "coffee_events"),
    ANNOUNCEMENT("Announcements", "announcements");

    companion object {
        fun fromRaw(raw: String?): NotificationCategory =
            entries.firstOrNull { it.name.equals(raw?.trim(), ignoreCase = true) } ?: ANNOUNCEMENT
    }
}

enum class EventCategory(val label: String) {
    LIVE_MUSIC("Live Music"),
    COFFEE_WORKSHOP("Coffee Workshops"),
    COFFEE_TASTING("Coffee Tasting"),
    SPECIAL_OFFER("Special Offers"),
    COMMUNITY_MEETUP("Community Meetups");

    companion object {
        fun fromRaw(raw: String?): EventCategory =
            entries.firstOrNull { it.name.equals(raw?.trim(), ignoreCase = true) } ?: COMMUNITY_MEETUP
    }
}

enum class EventStatus(val label: String) {
    UPCOMING("Upcoming"),
    ONGOING("Ongoing"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    companion object {
        fun fromRaw(raw: String?): EventStatus =
            entries.firstOrNull { it.label.equals(raw?.trim(), ignoreCase = true) || it.name.equals(raw?.trim(), ignoreCase = true) }
                ?: UPCOMING
    }
}

enum class SupportTicketType(val label: String) {
    INCORRECT_SHOP_INFO("Incorrect Shop Info"),
    INCORRECT_PRICE("Incorrect Prices"),
    APP_PROBLEM("App Problem"),
    INAPPROPRIATE_REVIEW("Inappropriate Review"),
    GENERAL_SUPPORT("General Support");

    companion object {
        fun fromRaw(raw: String?): SupportTicketType =
            entries.firstOrNull { it.name.equals(raw?.trim(), ignoreCase = true) } ?: GENERAL_SUPPORT
    }
}

enum class SupportTicketStatus(val label: String) {
    OPEN("Open"),
    IN_PROGRESS("In Progress"),
    RESOLVED("Resolved"),
    CLOSED("Closed");

    companion object {
        fun fromRaw(raw: String?): SupportTicketStatus =
            entries.firstOrNull { it.label.equals(raw?.trim(), ignoreCase = true) || it.name.equals(raw?.trim(), ignoreCase = true) }
                ?: OPEN
    }
}

@Immutable
@Keep
@IgnoreExtraProperties
data class ShopOverlayDoc(
    @DocumentId val id: String = "",
    val name: String = "",
    val area: String = "Banani",
    val address: String = "",
    val openTime: String = "10:00 AM",
    val closeTime: String = "02:00 AM",
    val priceRange: String = "",
    val phone: String = "",
    val familyFriendly: Boolean = true,
    val acAvailable: Boolean = true,
    val wifiAvailable: Boolean = true,
    val rooftopAvailable: Boolean = false,
    val verified: Boolean = false,
    val verifiedAt: Timestamp? = null,
    val verifiedBy: String = "",
    val ownerUid: String = "",
    val liveStatus: String? = null,
    val liveStatusUpdatedAt: Timestamp? = null,
    val liveStatusUpdatedBy: String = "",
    val officialOverride: Boolean = false,
    val visibility: String = "public",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Immutable
@Keep
@IgnoreExtraProperties
data class ShopStatusReportDoc(
    @DocumentId val id: String = "",
    val shopId: String = "",
    val userId: String = "",
    val reporterName: String = "",
    val reportedStatus: String = ReportedStatusType.OPEN_NOW.name,
    val note: String = "",
    val staffReport: Boolean = false,
    val verificationState: String = ReportVerificationState.UNVERIFIED.name,
    val visibility: String = "public",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun statusEnum(): ReportedStatusType = ReportedStatusType.fromRaw(reportedStatus)
    fun verificationEnum(): ReportVerificationState = ReportVerificationState.fromRaw(verificationState)
}

@Immutable
data class ResolvedShopStatus(
    val scheduledOpen: Boolean,
    val scheduledLabel: String,
    val verifiedStatus: LiveShopStatus?,
    val verifiedUpdatedAtMillis: Long?,
    val lastReportedStatus: ShopStatusReportDoc?,
    val isLastReportStale: Boolean
) {
    val effectiveOpenNow: Boolean
        get() = when (verifiedStatus) {
            LiveShopStatus.OPEN_NOW, LiveShopStatus.CLOSING_SOON -> true
            LiveShopStatus.CLOSED, LiveShopStatus.TEMPORARILY_CLOSED -> false
            null -> scheduledOpen
        }
}

@Immutable
@Keep
@IgnoreExtraProperties
data class DigitalMenuItemDoc(
    @DocumentId val id: String = "",
    val shopId: String = "",
    val category: String = MenuCategory.HOT_COFFEE.displayName,
    val name: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val priceBdt: Long = 0L,
    val available: Boolean = true,
    val special: Boolean = false,
    val visibility: String = "public",
    val updatedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun categoryEnum(): MenuCategory = MenuCategory.fromRaw(category)
}

@Immutable
@Keep
@IgnoreExtraProperties
data class CrowdReportDoc(
    @DocumentId val id: String = "",
    val shopId: String = "",
    val userId: String = "",
    val level: String = CrowdLevel.MODERATE.name,
    val note: String = "",
    val flaggedIncorrectCount: Long = 0L,
    val verificationState: String = "ACTIVE",
    val visibility: String = "public",
    @ServerTimestamp val createdAt: Timestamp? = null,
    val expiresAt: Timestamp? = null
) {
    fun levelEnum(): CrowdLevel = CrowdLevel.fromRaw(level)
}

@Immutable
data class AggregatedCrowdStatus(
    val level: CrowdLevel?,
    val activeReportCount: Int,
    val latestUpdatedMillis: Long?,
    val isSufficientlyRecent: Boolean
)

@Immutable
@Keep
@IgnoreExtraProperties
data class CouponDoc(
    @DocumentId val id: String = "",
    val shopId: String = "",
    val shopName: String = "",
    val code: String = "",
    val title: String = "",
    val description: String = "",
    val discountType: String = CouponDiscountType.PERCENTAGE.name,
    val discountValue: Long = 0L,
    val minSpendBdt: Long = 0L,
    val maxUsageLimit: Long = 1L,
    val redeemedCount: Long = 0L,
    val terms: String = "",
    val expiresAt: Timestamp? = null,
    val active: Boolean = true,
    val visibility: String = "public",
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun discountTypeEnum(): CouponDiscountType = CouponDiscountType.fromRaw(discountType)

    fun formattedDiscount(): String = if (discountTypeEnum() == CouponDiscountType.PERCENTAGE) {
        "${discountValue}% OFF"
    } else {
        "৳${discountValue} OFF"
    }

    fun isExpiredAt(nowMillis: Long): Boolean {
        val exp = expiresAt?.toDate()?.time ?: return false
        return exp <= nowMillis
    }
}

@Immutable
@Keep
@IgnoreExtraProperties
data class CouponRedemptionDoc(
    @DocumentId val id: String = "",
    val couponId: String = "",
    val userId: String = "",
    val shopId: String = "",
    val code: String = "",
    val status: String = "SAVED",
    val discountLabel: String = "",
    @ServerTimestamp val savedAt: Timestamp? = null,
    val redeemedAt: Timestamp? = null
)

@Immutable
@Keep
@IgnoreExtraProperties
data class PhotoReviewDoc(
    @DocumentId val id: String = "",
    val shopId: String = "",
    val userId: String = "",
    val authorName: String = "",
    val rating: Long = 5L,
    val comment: String = "",
    val photoUrls: List<String> = emptyList(),
    val helpfulCount: Long = 0L,
    val reportCount: Long = 0L,
    val visibility: String = "public",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Immutable
@Keep
@IgnoreExtraProperties
data class ReservationDoc(
    @DocumentId val id: String = "",
    val shopId: String = "",
    val shopName: String = "",
    val userId: String = "",
    val guestName: String = "",
    val contactPhone: String = "",
    val dateDhaka: String = "",
    val timeSlot: String = "",
    val guestCount: Long = 2L,
    val specialRequest: String = "",
    val status: String = ReservationStatus.PENDING.label,
    val staffNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun statusEnum(): ReservationStatus = ReservationStatus.fromRaw(status)
}

@Immutable
@Keep
@IgnoreExtraProperties
data class NotificationItemDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val category: String = NotificationCategory.ANNOUNCEMENT.name,
    val title: String = "",
    val body: String = "",
    val targetRoute: String = "",
    val targetId: String = "",
    val read: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun categoryEnum(): NotificationCategory = NotificationCategory.fromRaw(category)
}

@Immutable
@Keep
@IgnoreExtraProperties
data class UserPreferenceDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fcmToken: String = "",
    val optInNewOpenings: Boolean = false,
    val optInOffers: Boolean = false,
    val optInSavedShops: Boolean = true,
    val optInReservations: Boolean = true,
    val optInEvents: Boolean = false,
    val optInAnnouncements: Boolean = true,
    val compareShopIds: List<String> = emptyList(),
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Immutable
@Keep
@IgnoreExtraProperties
data class CoffeeEventDoc(
    @DocumentId val id: String = "",
    val title: String = "",
    val category: String = EventCategory.COMMUNITY_MEETUP.name,
    val description: String = "",
    val imageUrl: String = "",
    val shopId: String? = null,
    val venueName: String = "",
    val area: String = "Banani",
    val eventDateDhaka: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val organizer: String = "",
    val registrationInfo: String = "",
    val status: String = EventStatus.UPCOMING.label,
    val visibility: String = "public",
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun categoryEnum(): EventCategory = EventCategory.fromRaw(category)
    fun statusEnum(): EventStatus = EventStatus.fromRaw(status)
}

@Immutable
@Keep
@IgnoreExtraProperties
data class EventRegistrationDoc(
    @DocumentId val id: String = "",
    val eventId: String = "",
    val userId: String = "",
    val attendeeName: String = "",
    val note: String = "",
    val status: String = "Saved",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Immutable
@Keep
@IgnoreExtraProperties
data class SupportTicketDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val type: String = SupportTicketType.GENERAL_SUPPORT.name,
    val targetId: String = "",
    val targetName: String = "",
    val subject: String = "",
    val description: String = "",
    val contactInfo: String = "",
    val status: String = SupportTicketStatus.OPEN.label,
    val adminResponse: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun typeEnum(): SupportTicketType = SupportTicketType.fromRaw(type)
    fun statusEnum(): SupportTicketStatus = SupportTicketStatus.fromRaw(status)
}

@Immutable
@Keep
@IgnoreExtraProperties
data class VerificationRequestDoc(
    @DocumentId val id: String = "",
    val shopId: String = "",
    val shopName: String = "",
    val requesterUid: String = "",
    val businessOwnerName: String = "",
    val businessPhone: String = "",
    val registrationNote: String = "",
    val status: String = "Pending",
    val reviewedBy: String = "",
    val reviewedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Immutable
@Keep
@IgnoreExtraProperties
data class ShopStaffDoc(
    @DocumentId val id: String = "",
    val shopId: String = "",
    val uid: String = "",
    val role: String = "STAFF",
    val active: Boolean = false
)

@Immutable
data class UserRoleState(
    val isAdmin: Boolean = false,
    val staffShopIds: Set<String> = emptySet()
) {
    fun canManageShop(shopId: String): Boolean = isAdmin || shopId in staffShopIds
    val hasAnyManagementAccess: Boolean get() = isAdmin || staffShopIds.isNotEmpty()
}
