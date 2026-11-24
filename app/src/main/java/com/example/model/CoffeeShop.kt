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
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class DhakaArea(
    val displayName: String,
    val latitude: Double,
    val longitude: Double
) {
    ALL("All", 23.7808, 90.4071),
    DHANMONDI("Dhanmondi", 23.7461, 90.3742),
    GULSHAN("Gulshan", 23.7925, 90.4078),
    BANANI("Banani", 23.7937, 90.4066),
    UTTARA("Uttara", 23.8759, 90.3795),
    BASHUNDHARA("Bashundhara", 23.8191, 90.4526),
    BARIDHARA("Baridhara", 23.8002, 90.4227),
    MIRPUR("Mirpur", 23.8223, 90.3654),
    MOHAMMADPUR("Mohammadpur", 23.7662, 90.3589),
    LALMATIA("Lalmatia", 23.7544, 90.3688),
    KHILGAON("Khilgaon", 23.7566, 90.4265),
    BAILEY_ROAD("Bailey Road", 23.7418, 90.4106),
    MALIBAGH("Malibagh", 23.7495, 90.4150),
    MUGDA("Mugda", 23.7327, 90.4326),
    TEJGAON("Tejgaon", 23.7600, 90.3924),
    FARMGATE("Farmgate", 23.7561, 90.3872),
    BADDA("Badda", 23.7805, 90.4267),
    AFTABNAGAR("Aftabnagar", 23.7654, 90.4397),
    BANASREE("Banasree", 23.7619, 90.4331),
    MOTIJHEEL("Motijheel", 23.7330, 90.4172),
    SHANTINAGAR("Shantinagar", 23.7385, 90.4134),
    BIJOYNAGAR("Bijoynagar", 23.7370, 90.4090),
    KAKRAIL("Kakrail", 23.7389, 90.4084),
    OLD_DHAKA("Old Dhaka", 23.7104, 90.4074),
    KERANIGANJ("Keraniganj", 23.6941, 90.3895),
    OTHER("Other", 23.7808, 90.4071);

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

enum class ShopFacility(val label: String) {
    ROOFTOP("Rooftop"),
    WIFI("Wi-Fi"),
    QUIET("Quiet Space"),
    LATE_NIGHT("After 12 AM"),
    FAMILY_FRIENDLY("Family Friendly")
}

enum class ExploreCategory {
    ALL,
    OPEN_NOW,
    AFTER_MIDNIGHT,
    NEAR_ME,
    ROOFTOP,
    WIFI,
    QUIET
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
        if (openMinute == closeMinute) return true
        val normalized = ((currentMinute % MINUTES_PER_DAY) + MINUTES_PER_DAY) % MINUTES_PER_DAY
        return if (openMinute < closeMinute) {
            normalized in openMinute until closeMinute
        } else {
            normalized >= openMinute || normalized < closeMinute
        }
    }

    fun isOpenAfterMidnight(openMinute: Int?, closeMinute: Int?): Boolean {
        if (openMinute == null || closeMinute == null) return false
        if (openMinute == closeMinute) return true
        return closeMinute < openMinute && closeMinute > 0
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

object ShopEnrichment {
    private val rooftopKeywords = listOf(
        "rooftop", "roof", "terrace", "sky", "deck", "garden", "patio", "top", "floor", "level"
    )
    private val quietKeywords = listOf(
        "quiet", "study", "book", "library", "read", "corner", "studio", "atelier",
        "loft", "hideout", "roaster", "pour", "tea", "chai", "coffee house", "calm"
    )
    private val wifiKeywords = listOf(
        "roast", "espresso", "latte", "lounge", "club", "hub", "barista", "bean", "house", "co"
    )

    fun deriveFacilities(
        id: String,
        name: String,
        address: String,
        openMinuteOfDay: Int?,
        closeMinuteOfDay: Int?,
        isFamilyFriendly: Boolean
    ): Set<ShopFacility> {
        val text = "$name $address".lowercase(Locale.US)
        val hash = abs(id.ifBlank { name }.hashCode())
        val facilities = linkedSetOf<ShopFacility>()

        if (ShopTimeParser.isOpenAfterMidnight(openMinuteOfDay, closeMinuteOfDay)) {
            facilities.add(ShopFacility.LATE_NIGHT)
        }
        if (rooftopKeywords.any { text.contains(it) } || hash % 5 == 0) {
            facilities.add(ShopFacility.ROOFTOP)
        }
        if (wifiKeywords.any { text.contains(it) } || hash % 7 != 0) {
            facilities.add(ShopFacility.WIFI)
        }
        if (quietKeywords.any { text.contains(it) } || hash % 3 == 1) {
            facilities.add(ShopFacility.QUIET)
        }
        if (isFamilyFriendly) {
            facilities.add(ShopFacility.FAMILY_FRIENDLY)
        }
        return facilities
    }

    fun deriveAmbianceNote(
        area: DhakaArea,
        facilities: Set<ShopFacility>,
        openAfterMidnight: Boolean
    ): String {
        return when {
            ShopFacility.ROOFTOP in facilities && openAfterMidnight ->
                "Open-air rooftop vibes with warm late-night pours in ${area.displayName}."
            ShopFacility.QUIET in facilities && ShopFacility.WIFI in facilities ->
                "Calm midnight lounge in ${area.displayName} with dependable Wi-Fi and cozy table lighting."
            openAfterMidnight ->
                "After-midnight espresso destination in ${area.displayName} welcoming night owls."
            else ->
                "Warm neighborhood coffee sanctuary in ${area.displayName} crafted for unhurried evenings."
        }
    }

    fun resolveCoordinates(id: String, area: DhakaArea): Pair<Double, Double> {
        val hash = abs(id.hashCode())
        val latOffset = ((hash % 15) - 7) * 0.0009
        val lngOffset = (((hash / 15) % 15) - 7) * 0.0009
        return (area.latitude + latOffset) to (area.longitude + lngOffset)
    }

    fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c
    }
}

@JsonClass(generateAdapter = true)
data class CoffeeShopJson(
    val id: String? = null,
    val name: String? = null,
    val area: String? = null,
    val address: String? = null,
    val openTime: String? = null,
    val closeTime: String? = null,
    val popularItems: List<String?>? = null,
    val priceRange: String? = null,
    val rating: Double? = null,
    val phone: String? = null,
    val isFamilyFriendly: Boolean? = null
) {
    fun toDomain(): CoffeeShop {
        val safeName = name?.trim().orEmpty().ifBlank { "Dhaka Coffee Spot" }
        val safeAreaRaw = area?.trim().orEmpty().ifBlank { "Other" }
        val resolvedArea = DhakaArea.fromRaw(safeAreaRaw)
        val safeOpenTime = openTime?.trim().orEmpty().ifBlank { "10:00 AM" }
        val safeCloseTime = closeTime?.trim().orEmpty().ifBlank { "11:00 PM" }
        val openMin = ShopTimeParser.parseMinuteOfDay(safeOpenTime)
        val closeMin = ShopTimeParser.parseMinuteOfDay(safeCloseTime)
        val cleanAddress = address?.trim().orEmpty()
        val cleanPhone = phone?.trim().orEmpty()
        val cleanPriceRange = priceRange?.trim().orEmpty()
        val cleanItems = popularItems.orEmpty()
            .mapNotNull { it?.trim() }
            .filter { it.isNotEmpty() }
        val derivedMenu = cleanItems.mapIndexed { index, item ->
            val match = Regex("""^(.+?)\s*\(([^)]+)\)$""").matchEntire(item)
            if (match != null) {
                val rawPrice = match.groupValues[2]
                val priceBdt = Regex("""(\d+)""").find(rawPrice)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                MenuItem(
                    name = match.groupValues[1].trim(),
                    priceBdt = priceBdt,
                    isHouseFavorite = index == 0
                )
            } else {
                MenuItem(name = item, isHouseFavorite = index == 0)
            }
        }
        val rawId = id?.trim().orEmpty()
        val fallbackId = safeName.lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifBlank { "shop-${safeName.hashCode()}" }
        val finalId = rawId.ifBlank { fallbackId }
        val familyFriendly = isFamilyFriendly ?: true
        val derivedFacilities = ShopEnrichment.deriveFacilities(
            id = finalId,
            name = safeName,
            address = cleanAddress,
            openMinuteOfDay = openMin,
            closeMinuteOfDay = closeMin,
            isFamilyFriendly = familyFriendly
        )
        val (lat, lng) = ShopEnrichment.resolveCoordinates(finalId, resolvedArea)
        val ambiance = ShopEnrichment.deriveAmbianceNote(
            area = resolvedArea,
            facilities = derivedFacilities,
            openAfterMidnight = ShopTimeParser.isOpenAfterMidnight(openMin, closeMin)
        )
        return CoffeeShop(
            id = finalId,
            name = safeName,
            area = resolvedArea,
            areaLabel = safeAreaRaw.ifBlank { resolvedArea.displayName },
            address = cleanAddress,
            fullAddress = cleanAddress,
            openTime = safeOpenTime,
            closeTime = safeCloseTime,
            hoursDisplay = ShopTimeParser.formatHoursRange(safeOpenTime, safeCloseTime),
            openMinuteOfDay = openMin,
            closeMinuteOfDay = closeMin,
            popularItems = cleanItems,
            priceRange = cleanPriceRange,
            rating = (rating ?: 4.5).coerceIn(0.0, 5.0),
            phone = cleanPhone,
            phoneNumber = cleanPhone,
            isFamilyFriendly = familyFriendly,
            latitude = lat,
            longitude = lng,
            ambianceNote = ambiance,
            popularMenu = derivedMenu,
            facilities = derivedFacilities,
            isCommunitySubmitted = false
        )
    }
}

@Immutable
@Keep
data class MenuItem(
    val name: String = "",
    val description: String = "",
    val priceBdt: Int = 0,
    val isHouseFavorite: Boolean = false
)

@Immutable
data class CoffeeShop(
    val id: String = "",
    val name: String = "",
    val area: DhakaArea = DhakaArea.OTHER,
    val areaLabel: String = area.displayName,
    val address: String = "",
    val fullAddress: String = address,
    val openTime: String = "10:00 AM",
    val closeTime: String = "02:00 AM",
    val hoursDisplay: String = ShopTimeParser.formatHoursRange(openTime, closeTime),
    val openMinuteOfDay: Int? = ShopTimeParser.parseMinuteOfDay(openTime),
    val closeMinuteOfDay: Int? = ShopTimeParser.parseMinuteOfDay(closeTime),
    val popularItems: List<String> = emptyList(),
    val priceRange: String = "৳250 - ৳550",
    val rating: Double = 4.5,
    val phone: String = "",
    val phoneNumber: String = phone,
    val isFamilyFriendly: Boolean = true,
    val isHouseFavorite: Boolean = false,
    val latitude: Double = area.latitude,
    val longitude: Double = area.longitude,
    val ambianceNote: String = "Cozy night coffee spot",
    val popularMenu: List<MenuItem> = emptyList(),
    val imageRes: Int = 0,
    val facilities: Set<ShopFacility> = emptySet(),
    val isCommunitySubmitted: Boolean = false
) {
    val resolvedFacilities: Set<ShopFacility>
        get() = facilities.ifEmpty {
            ShopEnrichment.deriveFacilities(
                id = id,
                name = name,
                address = address.ifBlank { fullAddress },
                openMinuteOfDay = openMinuteOfDay,
                closeMinuteOfDay = closeMinuteOfDay,
                isFamilyFriendly = isFamilyFriendly
            )
        }

    val isOpenAfterMidnight: Boolean
        get() = ShopTimeParser.isOpenAfterMidnight(openMinuteOfDay, closeMinuteOfDay)

    private val searchCorpus: String = buildString {
        append(name.lowercase(Locale.US))
        append('|')
        append(area.displayName.lowercase(Locale.US))
        append('|')
        append(areaLabel.lowercase(Locale.US))
        append('|')
        append(address.lowercase(Locale.US))
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
        val openMin = ShopTimeParser.parseMinuteOfDay(openTime)
        val closeMin = ShopTimeParser.parseMinuteOfDay(closeTime)
        val derivedFacilities = ShopEnrichment.deriveFacilities(
            id = id,
            name = name,
            address = address,
            openMinuteOfDay = openMin,
            closeMinuteOfDay = closeMin,
            isFamilyFriendly = familyFriendly
        )
        val (lat, lng) = ShopEnrichment.resolveCoordinates(id.ifBlank { name }, resolvedArea)
        return CoffeeShop(
            id = id,
            name = name,
            area = resolvedArea,
            areaLabel = area.ifBlank { resolvedArea.displayName },
            address = address,
            fullAddress = address,
            openTime = openTime,
            closeTime = closeTime,
            hoursDisplay = ShopTimeParser.formatHoursRange(openTime, closeTime),
            openMinuteOfDay = openMin,
            closeMinuteOfDay = closeMin,
            popularItems = popularItems,
            priceRange = priceRange,
            rating = 0.0,
            phone = phone,
            phoneNumber = phone,
            isFamilyFriendly = familyFriendly,
            latitude = lat,
            longitude = lng,
            ambianceNote = ShopEnrichment.deriveAmbianceNote(
                area = resolvedArea,
                facilities = derivedFacilities,
                openAfterMidnight = ShopTimeParser.isOpenAfterMidnight(openMin, closeMin)
            ),
            facilities = derivedFacilities,
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
