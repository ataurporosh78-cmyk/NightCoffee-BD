package com.example

import com.example.model.CoffeeEventDoc
import com.example.model.CoffeeShop
import com.example.model.CouponDiscountType
import com.example.model.CouponDoc
import com.example.model.CrowdLevel
import com.example.model.CrowdReportDoc
import com.example.model.DhakaArea
import com.example.model.EventCategory
import com.example.model.EventStatus
import com.example.model.LiveShopStatus
import com.example.model.ReportVerificationState
import com.example.model.ReportedStatusType
import com.example.model.ShopOverlayDoc
import com.example.model.ShopStatusReportDoc
import com.example.util.BangladeshTimeUtil
import com.example.util.PlatformLogic
import com.google.firebase.Timestamp
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlatformLogicUnitTest {

    private val sampleShop = CoffeeShop(
        id = "midnight-banani",
        name = "Midnight Roast Banani",
        area = DhakaArea.BANANI,
        areaLabel = "Banani",
        address = "Road 11, Banani, Dhaka",
        openTime = "04:00 PM",
        closeTime = "02:00 AM",
        hoursDisplay = "4:00 PM - 2:00 AM",
        openMinuteOfDay = 16 * 60,
        closeMinuteOfDay = 2 * 60
    )

    @Test
    fun `resolveShopStatus separates scheduled, unverified customer, stale, and verified official status`() {
        val now = 1_764_000_000_000L
        val fourHoursAgo = Timestamp(Date(now - 4L * 60L * 60L * 1000L))
        val thirtyMinAgo = Timestamp(Date(now - 30L * 60L * 1000L))

        val staleCustomerReport = ShopStatusReportDoc(
            id = "rep-stale",
            shopId = sampleShop.id,
            reportedStatus = ReportedStatusType.TEMPORARILY_CLOSED.name,
            verificationState = ReportVerificationState.UNVERIFIED.name,
            createdAt = fourHoursAgo
        )

        // At 1:30 AM (90), shop is scheduled Closing Soon (within 45 mins of 2:00 AM)
        val staleResolved = PlatformLogic.resolveShopStatus(
            shop = sampleShop,
            overlay = null,
            reports = listOf(staleCustomerReport),
            minuteOfDay = 90,
            nowMillis = now
        )
        assertTrue(staleResolved.scheduledOpen)
        assertEquals("Closing Soon", staleResolved.scheduledLabel)
        assertNull(staleResolved.verifiedStatus)
        assertEquals("rep-stale", staleResolved.lastReportedStatus?.id)
        assertTrue(staleResolved.isLastReportStale)
        // Effective status falls back to scheduled when unverified report exists without official override
        assertTrue(staleResolved.effectiveOpenNow)

        // Official staff/admin override supersedes scheduled open
        val overlay = ShopOverlayDoc(
            id = sampleShop.id,
            verified = true,
            liveStatus = LiveShopStatus.TEMPORARILY_CLOSED.name,
            liveStatusUpdatedAt = thirtyMinAgo,
            officialOverride = true
        )
        val officialResolved = PlatformLogic.resolveShopStatus(
            shop = sampleShop,
            overlay = overlay,
            reports = listOf(staleCustomerReport),
            minuteOfDay = 90,
            nowMillis = now
        )
        assertEquals(LiveShopStatus.TEMPORARILY_CLOSED, officialResolved.verifiedStatus)
        assertFalse(officialResolved.effectiveOpenNow)
    }

    @Test
    fun `aggregateCrowdReports expires old reports and rejects dismissed ones`() {
        val now = 1_764_000_000_000L
        val expiredReport = CrowdReportDoc(
            id = "c-old",
            shopId = sampleShop.id,
            level = CrowdLevel.VERY_BUSY.name,
            createdAt = Timestamp(Date(now - 3L * 60L * 60L * 1000L)),
            expiresAt = Timestamp(Date(now - 60L * 60L * 1000L))
        )
        val dismissedReport = CrowdReportDoc(
            id = "c-dismissed",
            shopId = sampleShop.id,
            level = CrowdLevel.VERY_BUSY.name,
            verificationState = "DISMISSED",
            createdAt = Timestamp(Date(now - 10L * 60L * 1000L)),
            expiresAt = Timestamp(Date(now + 60L * 60L * 1000L))
        )
        val emptyAgg = PlatformLogic.aggregateCrowdReports(
            shopId = sampleShop.id,
            reports = listOf(expiredReport, dismissedReport),
            nowMillis = now
        )
        assertNull(emptyAgg.level)
        assertFalse(emptyAgg.isSufficientlyRecent)
        assertEquals(0, emptyAgg.activeReportCount)

        val recentQuiet = CrowdReportDoc(
            id = "c-1",
            shopId = sampleShop.id,
            level = CrowdLevel.QUIET.name,
            createdAt = Timestamp(Date(now - 20L * 60L * 1000L)),
            expiresAt = Timestamp(Date(now + 100L * 60L * 1000L))
        )
        val recentBusy = CrowdReportDoc(
            id = "c-2",
            shopId = sampleShop.id,
            level = CrowdLevel.BUSY.name,
            createdAt = Timestamp(Date(now - 5L * 60L * 1000L)),
            expiresAt = Timestamp(Date(now + 115L * 60L * 1000L))
        )
        val activeAgg = PlatformLogic.aggregateCrowdReports(
            shopId = sampleShop.id,
            reports = listOf(expiredReport, dismissedReport, recentQuiet, recentBusy),
            nowMillis = now
        )
        assertEquals(2, activeAgg.activeReportCount)
        assertTrue(activeAgg.isSufficientlyRecent)
        assertEquals(CrowdLevel.MODERATE, activeAgg.level)
    }

    @Test
    fun `splitCoupons separates active vs expired or exhausted coupons`() {
        val now = 1_764_000_000_000L
        val active = CouponDoc(
            id = "cup-active",
            code = "NIGHT20",
            discountType = CouponDiscountType.PERCENTAGE.name,
            discountValue = 20L,
            maxUsageLimit = 50L,
            redeemedCount = 10L,
            active = true,
            expiresAt = Timestamp(Date(now + 86_400_000L))
        )
        val expiredByDate = active.copy(
            id = "cup-date",
            expiresAt = Timestamp(Date(now - 1_000L))
        )
        val exhausted = active.copy(
            id = "cup-full",
            redeemedCount = 50L
        )

        val (act, exp) = PlatformLogic.splitCoupons(listOf(active, expiredByDate, exhausted), nowMillis = now)
        assertEquals(listOf("cup-active"), act.map { it.id })
        assertEquals(listOf("cup-date", "cup-full"), exp.map { it.id })
    }

    @Test
    fun `filterAndSortEvents places ongoing and upcoming events in chronological order`() {
        val events = listOf(
            CoffeeEventDoc(
                id = "ev-later",
                title = "Latte Art Throwdown",
                category = EventCategory.COFFEE_WORKSHOP.name,
                eventDateDhaka = "2026-12-05",
                startTime = "07:00 PM",
                status = EventStatus.UPCOMING.label
            ),
            CoffeeEventDoc(
                id = "ev-sooner",
                title = "Pour-Over Night",
                category = EventCategory.COFFEE_WORKSHOP.name,
                eventDateDhaka = "2026-11-28",
                startTime = "08:00 PM",
                status = EventStatus.UPCOMING.label
            ),
            CoffeeEventDoc(
                id = "ev-live",
                title = "Acoustic Session",
                category = EventCategory.LIVE_MUSIC.name,
                eventDateDhaka = "2026-11-24",
                startTime = "09:00 PM",
                status = EventStatus.ONGOING.label
            )
        )

        val allSorted = PlatformLogic.filterAndSortEvents(events, null, todayDateDhaka = "2026-11-24")
        assertEquals(listOf("ev-live", "ev-sooner", "ev-later"), allSorted.map { it.id })

        val workshops = PlatformLogic.filterAndSortEvents(events, EventCategory.COFFEE_WORKSHOP, todayDateDhaka = "2026-11-24")
        assertEquals(listOf("ev-sooner", "ev-later"), workshops.map { it.id })
    }

    @Test
    fun `toggleCompareShopIds enforces maximum 3 shops and BangladeshTimeUtil validates reservation dates`() {
        var ids = emptyList<String>()
        listOf("s1", "s2", "s3").forEach { id ->
            val (next, changed) = PlatformLogic.toggleCompareShopIds(ids, id)
            assertTrue(changed)
            ids = next
        }
        val (rejected, changedFourth) = PlatformLogic.toggleCompareShopIds(ids, "s4")
        assertFalse(changedFourth)
        assertEquals(listOf("s1", "s2", "s3"), rejected)

        val (removed, changedRemove) = PlatformLogic.toggleCompareShopIds(ids, "s2")
        assertTrue(changedRemove)
        assertEquals(listOf("s1", "s3"), removed)

        val now = 1_764_000_000_000L
        val today = BangladeshTimeUtil.todayDateDhaka(now)
        val upcoming = BangladeshTimeUtil.upcomingDatesDhaka(3, now)
        assertEquals(3, upcoming.size)
        assertTrue(BangladeshTimeUtil.isValidReservationDate(today, now))
        assertTrue(BangladeshTimeUtil.isValidReservationDate(upcoming.last(), now))
        assertFalse(BangladeshTimeUtil.isValidReservationDate("2020-01-01", now))
        assertFalse(BangladeshTimeUtil.isValidReservationDate("invalid", now))
        assertNotNull(BangladeshTimeUtil.formatRelativeTime(now - 120_000L, now))
    }
}
