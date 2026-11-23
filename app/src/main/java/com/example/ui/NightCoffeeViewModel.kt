package com.example.ui

import androidx.lifecycle.ViewModel
import com.example.data.CoffeeShopRepository
import com.example.data.DhakaCoffeeShopRepository
import com.example.model.CoffeeShop
import com.example.model.DhakaArea
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CoffeeShopUiItem(
    val shop: CoffeeShop,
    val isOpenNow: Boolean
)

data class NightCoffeeUiState(
    val openNowOnly: Boolean = false,
    val selectedArea: DhakaArea = DhakaArea.ALL,
    val dhakaTimeDisplay: String = "",
    val allShopsCount: Int = 0,
    val filteredShops: List<CoffeeShopUiItem> = emptyList()
)

class NightCoffeeViewModel(
    private val repository: CoffeeShopRepository = DhakaCoffeeShopRepository(),
    private val currentDhakaMinuteProvider: () -> Int = { defaultDhakaMinuteOfDay() },
    private val currentDhakaTimeFormatter: () -> String = { defaultDhakaTimeFormatted() }
) : ViewModel() {

    private val _uiState = MutableStateFlow(buildUiState(openNowOnly = false, selectedArea = DhakaArea.ALL))
    val uiState: StateFlow<NightCoffeeUiState> = _uiState.asStateFlow()

    fun setOpenNowOnly(enabled: Boolean) {
        _uiState.update { current ->
            buildUiState(openNowOnly = enabled, selectedArea = current.selectedArea)
        }
    }

    fun selectArea(area: DhakaArea) {
        _uiState.update { current ->
            buildUiState(openNowOnly = current.openNowOnly, selectedArea = area)
        }
    }

    fun resetFilters() {
        _uiState.value = buildUiState(openNowOnly = false, selectedArea = DhakaArea.ALL)
    }

    fun refreshTime() {
        _uiState.update { current ->
            buildUiState(openNowOnly = current.openNowOnly, selectedArea = current.selectedArea)
        }
    }

    fun getShopUiItem(shopId: String): CoffeeShopUiItem? {
        val shop = repository.getCoffeeShopById(shopId) ?: return null
        val minuteOfDay = currentDhakaMinuteProvider()
        return CoffeeShopUiItem(
            shop = shop,
            isOpenNow = shop.isOpenAt(minuteOfDay)
        )
    }

    private fun buildUiState(
        openNowOnly: Boolean,
        selectedArea: DhakaArea
    ): NightCoffeeUiState {
        val minuteOfDay = currentDhakaMinuteProvider()
        val allShops = repository.getCoffeeShops()
        val items = allShops
            .map { shop ->
                CoffeeShopUiItem(
                    shop = shop,
                    isOpenNow = shop.isOpenAt(minuteOfDay)
                )
            }
            .filter { item ->
                val matchesArea = selectedArea == DhakaArea.ALL || item.shop.area == selectedArea
                val matchesOpen = !openNowOnly || item.isOpenNow
                matchesArea && matchesOpen
            }

        return NightCoffeeUiState(
            openNowOnly = openNowOnly,
            selectedArea = selectedArea,
            dhakaTimeDisplay = currentDhakaTimeFormatter(),
            allShopsCount = allShops.size,
            filteredShops = items
        )
    }

    companion object {
        private val dhakaTimeZone: TimeZone = TimeZone.getTimeZone("Asia/Dhaka")

        fun defaultDhakaMinuteOfDay(): Int {
            val calendar = Calendar.getInstance(dhakaTimeZone)
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            return hour * 60 + minute
        }

        fun defaultDhakaTimeFormatted(): String {
            val calendar = Calendar.getInstance(dhakaTimeZone)
            val formatter = SimpleDateFormat("h:mm a", Locale.US).apply {
                timeZone = dhakaTimeZone
            }
            return formatter.format(calendar.time)
        }
    }
}
