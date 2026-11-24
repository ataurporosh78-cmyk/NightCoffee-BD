package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object BangladeshTimeUtil {
    private val dhakaTz: TimeZone = TimeZone.getTimeZone("Asia/Dhaka")
    private val dateRegex = Regex("""^\d{4}-\d{2}-\d{2}$""")

    fun todayDateDhaka(nowMillis: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = dhakaTz
        }
        return sdf.format(Date(nowMillis))
    }

    fun upcomingDatesDhaka(daysCount: Int = 7, nowMillis: Long = System.currentTimeMillis()): List<String> {
        val cal = Calendar.getInstance(dhakaTz).apply { timeInMillis = nowMillis }
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = dhakaTz }
        return buildList(daysCount) {
            for (i in 0 until daysCount) {
                add(sdf.format(cal.time))
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
        }
    }

    fun isValidReservationDate(dateStr: String, nowMillis: Long = System.currentTimeMillis()): Boolean {
        val clean = dateStr.trim()
        if (!dateRegex.matches(clean)) return false
        val today = todayDateDhaka(nowMillis)
        return clean >= today
    }

    fun formatRelativeTime(timestampMillis: Long?, nowMillis: Long = System.currentTimeMillis()): String {
        if (timestampMillis == null || timestampMillis <= 0L) return "Unknown time"
        val diffMs = (nowMillis - timestampMillis).coerceAtLeast(0L)
        val minutes = diffMs / 60_000L
        val hours = minutes / 60L
        val days = hours / 24L
        return when {
            minutes < 1L -> "Just now"
            minutes < 60L -> "${minutes}m ago"
            hours < 24L -> "${hours}h ago"
            days < 7L -> "${days}d ago"
            else -> {
                val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.US).apply { timeZone = dhakaTz }
                sdf.format(Date(timestampMillis))
            }
        }
    }

    fun formatDhakaDateTime(timestampMillis: Long?): String {
        if (timestampMillis == null || timestampMillis <= 0L) return ""
        val sdf = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.US).apply { timeZone = dhakaTz }
        return "${sdf.format(Date(timestampMillis))} BST"
    }
}
