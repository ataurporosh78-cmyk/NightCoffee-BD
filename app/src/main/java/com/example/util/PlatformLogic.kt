package com.example.util

import com.example.model.AggregatedCrowdStatus
import com.example.model.CoffeeEventDoc
import com.example.model.CoffeeShop
import com.example.model.CouponDoc
import com.example.model.CrowdLevel
import com.example.model.CrowdReportDoc
import com.example.model.EventCategory
import com.example.model.EventStatus
import com.example.model.LiveShopStatus
import com.example.model.ReportVerificationState
import com.example.model.ResolvedShopStatus
import com.example.model.ShopOverlayDoc
import com.example.model.ShopStatusReportDoc
import com.example.model.ShopTimeParser
import kotlin.math.roundToInt

object PlatformLogic {
    const val STATUS_STALE_THRESHOLD_MS = 3L * 60L * 60L * 1000L // 3 hours
    const val CROWD_RECENT_THRESHOLD_MS = 2L * 60L * 60L * 1000L // 2 hours
    const val CROWD_COOLDOWN_MS = 15L * 60L * 1000L // 15 minutes rate limit per user per shop
    const val MAX_COMPARE_SHOPS = 3

    fun resolveShopStatus(
        shop: CoffeeShop,
        overlay: ShopOverlayDoc?,
        reports: List<ShopStatusReportDoc>,
        minuteOfDay: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): ResolvedShopStatus {
        val scheduledOpen = shop.isOpenAt(minuteOfDay)
        val isClosingSoon = isClosingWithinMinutes(
            openMinute = shop.openMinuteOfDay,
            closeMinute = shop.closeMinuteOfDay,
            currentMinute = minuteOfDay,
            thresholdMinutes = 45
        )
        val scheduledLabel = when {
            isClosingSoon -> "Closing Soon"
            scheduledOpen -> "Open Now"
            else -> "Closed"
        }

        val officialStatus = overlay
            ?.takeIf { it.officialOverride }
            ?.liveStatus
            ?.let { LiveShopStatus.fromRaw(it) }
        val officialUpdatedMillis = overlay?.liveStatusUpdatedAt?.toDate()?.time

        val verifiedReport = reports
            .filter {
                it.shopId == shop.id &&
                    it.verificationEnum() == ReportVerificationState.VERIFIED
            }
            .maxByOrNull { it.updatedAt?.toDate()?.time ?: it.createdAt?.toDate()?.time ?: 0L }

        val verifiedFromReport = verifiedReport?.let {
            when (it.statusEnum()) {
                com.example.model.ReportedStatusType.OPEN_NOW -> LiveShopStatus.OPEN_NOW
                com.example.model.ReportedStatusType.CLOSED -> LiveShopStatus.CLOSED
                com.example.model.ReportedStatusType.TEMPORARILY_CLOSED -> LiveShopStatus.TEMPORARILY_CLOSED
                com.example.model.ReportedStatusType.CLOSING_SOON -> LiveShopStatus.CLOSING_SOON
                com.example.model.ReportedStatusType.INCORRECT_HOURS -> null
            }
        }
        val verifiedReportMillis = verifiedReport?.let {
            it.updatedAt?.toDate()?.time ?: it.createdAt?.toDate()?.time
        }

        val effectiveVerifiedStatus: LiveShopStatus?
        val effectiveVerifiedTime: Long?
        if (officialStatus != null && (officialUpdatedMillis ?: 0L) >= (verifiedReportMillis ?: 0L)) {
            effectiveVerifiedStatus = officialStatus
            effectiveVerifiedTime = officialUpdatedMillis
        } else if (verifiedFromReport != null) {
            effectiveVerifiedStatus = verifiedFromReport
            effectiveVerifiedTime = verifiedReportMillis
        } else {
            effectiveVerifiedStatus = officialStatus
            effectiveVerifiedTime = officialUpdatedMillis
        }

        val latestReport = reports
            .filter {
                it.shopId == shop.id &&
                    it.verificationEnum() != ReportVerificationState.DISMISSED
            }
            .maxByOrNull { it.createdAt?.toDate()?.time ?: 0L }

        val latestReportMillis = latestReport?.createdAt?.toDate()?.time
        val isStale = latestReport != null && (
            latestReportMillis == null || (nowMillis - latestReportMillis) > STATUS_STALE_THRESHOLD_MS
        )

        return ResolvedShopStatus(
            scheduledOpen = scheduledOpen,
            scheduledLabel = scheduledLabel,
            verifiedStatus = effectiveVerifiedStatus,
            verifiedUpdatedAtMillis = effectiveVerifiedTime,
            lastReportedStatus = latestReport,
            isLastReportStale = isStale
        )
    }

    fun isClosingWithinMinutes(
        openMinute: Int?,
        closeMinute: Int?,
        currentMinute: Int,
        thresholdMinutes: Int = 45
    ): Boolean {
        if (openMinute == null || closeMinute == null) return false
        if (!ShopTimeParser.isOpenAt(openMinute, closeMinute, currentMinute)) return false
        if (openMinute == closeMinute) return false
        val normalizedNow = ((currentMinute % 1440) + 1440) % 1440
        val diff = if (closeMinute >= normalizedNow) {
            closeMinute - normalizedNow
        } else {
            (1440 - normalizedNow) + closeMinute
        }
        return diff in 1..thresholdMinutes
    }

    fun aggregateCrowdReports(
        shopId: String,
        reports: List<CrowdReportDoc>,
        nowMillis: Long = System.currentTimeMillis()
    ): AggregatedCrowdStatus {
        val active = reports.filter { report ->
            if (report.shopId != shopId) return@filter false
            if (report.verificationState == "DISMISSED") return@filter false
            val expMs = report.expiresAt?.toDate()?.time
            if (expMs != null && expMs <= nowMillis) return@filter false
            val createdMs = report.createdAt?.toDate()?.time
            createdMs == null || (nowMillis - createdMs) <= CROWD_RECENT_THRESHOLD_MS
        }
        if (active.isEmpty()) {
            return AggregatedCrowdStatus(
                level = null,
                activeReportCount = 0,
                latestUpdatedMillis = null,
                isSufficientlyRecent = false
            )
        }

        val avgWeight = active.map { it.levelEnum().weight }.average().roundToInt().coerceIn(1, 4)
        val aggregatedLevel = CrowdLevel.entries.firstOrNull { it.weight == avgWeight } ?: CrowdLevel.MODERATE
        val latestMillis = active.mapNotNull { it.createdAt?.toDate()?.time }.maxOrNull()
        val recentEnough = latestMillis != null && (nowMillis - latestMillis) <= CROWD_RECENT_THRESHOLD_MS
        return AggregatedCrowdStatus(
            level = aggregatedLevel,
            activeReportCount = active.size,
            latestUpdatedMillis = latestMillis,
            isSufficientlyRecent = recentEnough
        )
    }

    fun splitCoupons(
        coupons: List<CouponDoc>,
        nowMillis: Long = System.currentTimeMillis()
    ): Pair<List<CouponDoc>, List<CouponDoc>> {
        val activeList = mutableListOf<CouponDoc>()
        val expiredList = mutableListOf<CouponDoc>()
        coupons.forEach { coupon ->
            val expired = !coupon.active ||
                coupon.isExpiredAt(nowMillis) ||
                coupon.redeemedCount >= coupon.maxUsageLimit
            if (expired) {
                expiredList.add(coupon)
            } else {
                activeList.add(coupon)
            }
        }
        return activeList to expiredList
    }

    fun filterAndSortEvents(
        events: List<CoffeeEventDoc>,
        selectedCategory: EventCategory?,
        todayDateDhaka: String = BangladeshTimeUtil.todayDateDhaka()
    ): List<CoffeeEventDoc> {
        return events
            .filter { selectedCategory == null || it.categoryEnum() == selectedCategory }
            .sortedWith(
                compareBy<CoffeeEventDoc>(
                    { event ->
                        when (event.statusEnum()) {
                            EventStatus.ONGOING -> 0
                            EventStatus.UPCOMING -> if (event.eventDateDhaka >= todayDateDhaka) 1 else 2
                            EventStatus.COMPLETED -> 3
                            EventStatus.CANCELLED -> 4
                        }
                    },
                    { it.eventDateDhaka },
                    { it.startTime }
                )
            )
    }

    fun toggleCompareShopIds(current: List<String>, shopId: String): Pair<List<String>, Boolean> {
        val clean = shopId.trim()
        if (clean.isEmpty()) return current to false
        return if (clean in current) {
            current.filterNot { it == clean } to true
        } else if (current.size >= MAX_COMPARE_SHOPS) {
            current to false
        } else {
            (current + clean) to true
        }
    }
}
