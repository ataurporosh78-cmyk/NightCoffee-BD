package com.example.model

import android.os.Build
import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import com.squareup.moshi.JsonClass
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

enum class DhakaArea(val displayName: String) {
    ALL("All"),
    DHANMONDI("Dhanmondi"),
    GULSHAN("Gulshan"),
    BANANI("Banani"),
    UTTARA("Uttara"),
    BASHUNDHARA("Bashundhara"),
    BARIDHARA("Baridhara"),
    MIRPUR("Mirpur"),
    MOHAMMADPUR("Mohammadpur"),
    KHILGAON("Khilgaon"),
    BAILEY_ROAD("Bailey Road"),
    OLD_DHAKA("Old Dhaka"),
    KERANIGANJ("Keraniganj"),
    OTHER("Other");

    companion object {
        val selectableAreas: List<DhakaArea> = entries.filter { it != ALL }

        fun fromRaw(raw: String): DhakaArea {
            val cleaned = raw.trim()
            return entries.firstOrNull {
                it != ALL && it.displayName.equals(cleaned, ignoreCase = true)
            } ?: OTHER
        }
    }
}

object ShopTimeParser {
    private const val MINUTES_PER_DAY = 24 * 60
    private val time12hRegex = Regex("""^(\d{1,2}):(\d{2})\s*([APap][Mm])$""")
    private val time24hRegex = Regex("""^(\d{1,2}):(\d{2})$""")

    /**
     * Parses both 12-hour ("10:00 AM", "02:00 AM") and 24-hour ("22:00", "02:00") strings
     * into minutes from midnight [0, 1439]. Returns null if malformed.
     */
    fun parseMinuteOfDay(rawTime: String): Int? {
        val trimmed = rawTime.trim()
        time12hRegex.matchEntire(trimmed)?.let { match ->
            val hour12 = match.groupValues[1].toIntOrNull() ?: return null
            val minute = match.groupValues[2].toIntOrNull() ?: return null
            val meridiem = match.groupValues[3].uppercase(Locale.US)
            if (hour12 !in 1..12 || minute !in 0..59) return null
            val hour24 = when {
                meridiem == "AM" && hour12 == 12 -> 0
                meridiem == "AM" -> hour12
                meridiem == "PM" && hour12 == 12 -> 12
                else -> hour12 + 12
            }
            return hour24 * 60 + minute
        }

        time24hRegex.matchEntire(trimmed)?.let { match ->
            val hour24 = match.groupValues[1].toIntOrNull() ?: return null
            val minute = match.groupValues[2].toIntOrNull() ?: return null
            if (hour24 == 24 && minute == 0) return 0
            if (hour24 !in 0..23 || minute !in 0..59) return null
            return hour24 * 60 + minute
        }

        return null
    }

    fun isOpenAt(openMinute: Int?, closeMinute: Int?, currentMinute: Int): Boolean {
        if (openMinute == null || closeMinute == null) return false
        val normalized = ((currentMinute % MINUTES_PER_DAY) + MINUTES_PER_DAY) % MINUTES_PER_DAY
        return if (openMinute < closeMinute) {
            normalized in openMinute until closeMinute
        } else {
            normalized >= openMinute || normalized < closeMinute
        }
    }

    fun formatHoursRange(openTime: String, closeTime: String): String {
        val openFormatted = formatDisplayTime(openTime)
        val closeFormatted = formatDisplayTime(closeTime)
        return "$openFormatted - $closeFormatted"
    }

    fun formatDisplayTime(rawTime: String): String {
        val minuteOfDay = parseMinuteOfDay(rawTime) ?: return rawTime.trim()
        val hour24 = minuteOfDay / 60
        val minute = minuteOfDay % 60
        val meridiem = if (hour24 < 12) "AM" else "PM"
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        return String.format(Locale.US, "%d:%02d %s", hour12, minute, meridiem)
    }

    fun currentDeviceMinuteOfDay(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val now = LocalTime.now()
            now.hour * 60 + now.minute
        } else {
            val cal = Calendar.getInstance()
            cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        }
    }

    fun currentDeviceTimeFormatted(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a", Locale.US))
        } else {
            SimpleDateFormat("h:mm a", Locale.US).format(Calendar.getInstance().time)
        }
    }
}

@JsonClass(generateAdapter = true)
data class CoffeeShopJson(
    val id: String = "",
    val name: String = "",
    val area: String = "Other",
    val address: String = "",
    val openTime: String = "10:00 AM",
    val closeTime: String = "11:00 PM",
    val popularItems: List<String> = emptyList(),
    val priceRange: String = "",
    val rating: Double = 4.5,
    val phone: String = "",
    val isFamilyFriendly: Boolean = true
) {
    fun toDomain(): CoffeeShop {
        val resolvedArea = DhakaArea.fromRaw(area)
        val openMin = ShopTimeParser.parseMinuteOfDay(openTime)
        val closeMin = ShopTimeParser.parseMinuteOfDay(closeTime)
        return CoffeeShop(
            id = id.ifBlank { name.lowercase(Locale.US).replace(Regex("[^a-z0-9]+"), "-") },
            name = name.trim(),
            area = resolvedArea,
            areaLabel = area.trim().ifBlank { resolvedArea.displayName },
            address = address.trim(),
            openTime = openTime.trim(),
            closeTime = closeTime.trim(),
            hoursDisplay = ShopTimeParser.formatHoursRange(openTime, closeTime),
            openMinuteOfDay = openMin,
            closeMinuteOfDay = closeMin,
            popularItems = popularItems.map { it.trim() }.filter { it.isNotEmpty() },
            priceRange = priceRange.trim(),
            rating = rating.coerceIn(0.0, 5.0),
            phone = phone.trim(),
            isFamilyFriendly = isFamilyFriendly,
            isCommunitySubmitted = false
        )
    }
}

@Immutable
data class CoffeeShop(
    val id: String,
    val name: String,
    val area: DhakaArea,
    val areaLabel: String,
    val address: String,
    val openTime: String,
    val closeTime: String,
    val hoursDisplay: String,
    val openMinuteOfDay: Int?,
    val closeMinuteOfDay: Int?,
    val popularItems: List<String>,
    val priceRange: String,
    val rating: Double,
    val phone: String,
    val isFamilyFriendly: Boolean,
    val isCommunitySubmitted: Boolean = false
) {
    private val searchCorpus: String = buildString {
        append(name.lowercase(Locale.US))
        append('|')
        append(area.displayName.lowercase(Locale.US))
        append('|')
        append(areaLabel.lowercase(Locale.US))
        popularItems.forEach { item ->
            append('|')
            append(item.lowercase(Locale.US))
        }
    }

    fun isOpenAt(minuteOfDay: Int): Boolean =
        ShopTimeParser.isOpenAt(openMinuteOfDay, closeMinuteOfDay, minuteOfDay)

    fun matchesQuery(lowercaseTrimmedQuery: String): Boolean {
        if (lowercaseTrimmedQuery.isEmpty()) return true
        return searchCorpus.contains(lowercaseTrimmedQuery)
    }
}

@Keep
@IgnoreExtraProperties
data class CommunityShopDoc(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val visibility: String = "public",
    val name: String = "",
    val area: String = "",
    val address: String = "",
    val openTime: String = "",
    val closeTime: String = "",
    val popularItems: List<String> = emptyList(),
    val priceRange: String = "",
    val phone: String = "",
    val familyFriendly: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toDomain(): CoffeeShop {
        val resolvedArea = DhakaArea.fromRaw(area)
        return CoffeeShop(
            id = id,
            name = name,
            area = resolvedArea,
            areaLabel = area.ifBlank { resolvedArea.displayName },
            address = address,
            openTime = openTime,
            closeTime = closeTime,
            hoursDisplay = ShopTimeParser.formatHoursRange(openTime, closeTime),
            openMinuteOfDay = ShopTimeParser.parseMinuteOfDay(openTime),
            closeMinuteOfDay = ShopTimeParser.parseMinuteOfDay(closeTime),
            popularItems = popularItems,
            priceRange = priceRange,
            rating = 0.0,
            phone = phone,
            isFamilyFriendly = familyFriendly,
            isCommunitySubmitted = true
        )
    }
}

@Keep
@IgnoreExtraProperties
data class ShopReview(
    @DocumentId val id: String = "",
    val shopId: String = "",
    val userId: String = "",
    val authorName: String = "",
    val rating: Long = 5L,
    val comment: String = "",
    val visibility: String = "public",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
