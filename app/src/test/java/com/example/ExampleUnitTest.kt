package com.example

import com.example.data.DhakaCoffeeShopRepository
import com.example.model.DhakaArea
import com.example.ui.NightCoffeeViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `initial repository contains 6 Dhaka coffee shops across requested areas`() {
        val shops = DhakaCoffeeShopRepository().getCoffeeShops()
        assertEquals(6, shops.size)
        val areas = shops.map { it.area }.toSet()
        assertTrue(areas.contains(DhakaArea.BANANI))
        assertTrue(areas.contains(DhakaArea.DHANMONDI))
        assertTrue(areas.contains(DhakaArea.UTTARA))
        assertTrue(areas.contains(DhakaArea.MIRPUR))
        assertTrue(areas.contains(DhakaArea.KHILGAON))
        assertTrue(shops.all { it.isFamilyFriendly })
        assertTrue(shops.all { it.popularMenu.isNotEmpty() })
    }

    @Test
    fun `cross midnight opening hours evaluate accurately`() {
        val repo = DhakaCoffeeShopRepository()
        val tabaq = repo.getCoffeeShopById("tabaq-dhanmondi")!! // 10:00 AM - 2:00 AM
        assertTrue(tabaq.isOpenAt(10 * 60))       // 10:00 AM -> Open
        assertTrue(tabaq.isOpenAt(23 * 60 + 30))  // 11:30 PM -> Open
        assertTrue(tabaq.isOpenAt(1 * 60 + 30))   // 1:30 AM -> Open
        assertFalse(tabaq.isOpenAt(2 * 60))       // 2:00 AM -> Closed
        assertFalse(tabaq.isOpenAt(9 * 60))       // 9:00 AM -> Closed

        val beansAndAroma = repo.getCoffeeShopById("beans-aroma-mirpur")!! // 11:00 AM - 11:00 PM
        assertTrue(beansAndAroma.isOpenAt(15 * 60))   // 3:00 PM -> Open
        assertFalse(beansAndAroma.isOpenAt(23 * 60))  // 11:00 PM -> Closed
        assertFalse(beansAndAroma.isOpenAt(1 * 60))   // 1:00 AM -> Closed
    }

    @Test
    fun `viewModel filters by Open Now and Area at 1_30 AM Dhaka time`() {
        val viewModel = NightCoffeeViewModel(
            currentDhakaMinuteProvider = { 90 }, // 1:30 AM
            currentDhakaTimeFormatter = { "1:30 AM" }
        )

        assertEquals(6, viewModel.uiState.value.filteredShops.size)

        viewModel.setOpenNowOnly(true)
        val openShops = viewModel.uiState.value.filteredShops
        assertEquals(3, openShops.size)
        assertTrue(openShops.all { it.isOpenNow })

        viewModel.selectArea(DhakaArea.KHILGAON)
        assertEquals(1, viewModel.uiState.value.filteredShops.size)
        assertEquals("Kiva Han Coffee", viewModel.uiState.value.filteredShops.first().shop.name)

        viewModel.selectArea(DhakaArea.MIRPUR)
        assertTrue(viewModel.uiState.value.filteredShops.isEmpty())

        viewModel.resetFilters()
        assertEquals(6, viewModel.uiState.value.filteredShops.size)
        assertNotNull(viewModel.getShopUiItem("north-end-banani"))
    }
}
