package com.example.model

import androidx.annotation.DrawableRes

enum class DhakaArea(val displayName: String) {
    ALL("All"),
    DHANMONDI("Dhanmondi"),
    BANANI("Banani"),
    UTTARA("Uttara"),
    MIRPUR("Mirpur"),
    KHILGAON("Khilgaon")
}

data class MenuItem(
    val name: String,
    val description: String,
    val priceBdt: Int,
    val isHouseFavorite: Boolean = false
)

data class CoffeeShop(
    val id: String,
    val name: String,
    val area: DhakaArea,
    val fullAddress: String,
    val hoursDisplay: String,
    val openMinuteOfDay: Int,
    val closeMinuteOfDay: Int,
    val rating: Double,
    val isFamilyFriendly: Boolean,
    val phoneNumber: String,
    val latitude: Double,
    val longitude: Double,
    val ambianceNote: String,
    @param:DrawableRes val imageRes: Int,
    val popularMenu: List<MenuItem>
) {
    /**
     * Evaluates whether the shop is open at [minuteOfDay] (0..1439).
     * Supports schedules that cross midnight (e.g. 10:00 AM to 2:00 AM).
     */
    fun isOpenAt(minuteOfDay: Int): Boolean {
        val normalized = ((minuteOfDay % MINUTES_PER_DAY) + MINUTES_PER_DAY) % MINUTES_PER_DAY
        return if (openMinuteOfDay < closeMinuteOfDay) {
            normalized in openMinuteOfDay until closeMinuteOfDay
        } else {
            normalized >= openMinuteOfDay || normalized < closeMinuteOfDay
        }
    }

    companion object {
        private const val MINUTES_PER_DAY = 24 * 60
    }
}
