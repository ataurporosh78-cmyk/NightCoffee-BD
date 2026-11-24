package com.example

import com.example.data.CoffeeRepository
import com.example.data.InMemoryCoffeeAssetLoader
import com.example.data.InMemoryCommunityDataSource
import com.example.data.InMemoryFavoritesStorage
import com.example.data.IndexedCoffeeCatalog
import com.example.data.NewCoffeeShopInput
import com.example.model.CoffeeShop
import com.example.model.CoffeeShopJson
import com.example.model.DhakaArea
import com.example.model.ShopTimeParser
import com.example.viewmodel.CoffeeViewModel
import com.example.viewmodel.HomeTab
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExampleUnitTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `DhakaArea includes all 14 requested filter entries`() {
        val names = DhakaArea.entries.map { it.displayName }
        assertEquals(14, names.size)
        assertEquals(
            listOf(
                "All", "Dhanmondi", "Gulshan", "Banani", "Uttara",
                "Bashundhara", "Baridhara", "Mirpur", "Mohammadpur",
                "Khilgaon", "Bailey Road", "Old Dhaka", "Keraniganj", "Other"
            ),
            names
        )
        assertEquals(13, DhakaArea.selectableAreas.size)
        assertFalse(DhakaArea.selectableAreas.contains(DhakaArea.ALL))
    }

    @Test
    fun `ShopTimeParser supports both 12h and 24h formats and past-midnight closing`() {
        val open12h = ShopTimeParser.parseMinuteOfDay("10:00 AM")
        val close12h = ShopTimeParser.parseMinuteOfDay("02:00 AM")
        assertEquals(10 * 60, open12h)
        assertEquals(2 * 60, close12h)

        val open24h = ShopTimeParser.parseMinuteOfDay("16:00")
        val close24h = ShopTimeParser.parseMinuteOfDay("03:00")
        assertEquals(16 * 60, open24h)
        assertEquals(3 * 60, close24h)

        // 1:30 AM is open for 10:00 AM - 02:00 AM and 16:00 - 03:00
        assertTrue(ShopTimeParser.isOpenAt(open12h, close12h, 90))
        assertTrue(ShopTimeParser.isOpenAt(open24h, close24h, 90))

        // 4:00 AM is closed for both
        assertFalse(ShopTimeParser.isOpenAt(open12h, close12h, 4 * 60))
        assertFalse(ShopTimeParser.isOpenAt(open24h, close24h, 4 * 60))
    }

    @Test
    fun `coffee_shops asset parses cleanly via Moshi with mixed 12h and 24h times`() {
        val candidatePaths = listOf(
            "src/main/assets/coffee_shops.json",
            "app/src/main/assets/coffee_shops.json"
        )
        val assetFile = candidatePaths.map(::File).first { it.exists() }
        val moshi = Moshi.Builder().build()
        val listType = Types.newParameterizedType(List::class.java, CoffeeShopJson::class.java)
        val adapter = moshi.adapter<List<CoffeeShopJson>>(listType)

        val parsed = adapter.fromJson(assetFile.readText())
        assertNotNull(parsed)
        assertEquals(3, parsed!!.size)

        val domainShops = parsed.map { it.toDomain() }
        assertEquals("North End Coffee Roasters", domainShops[0].name)
        assertEquals("Tabaq Coffee", domainShops[1].name)
        assertEquals("Kiva Han Coffee", domainShops[2].name)
        // Kiva Han uses 24h "16:00" - "03:00" in JSON, formatted cleanly to 12h range
        assertEquals("4:00 PM - 3:00 AM", domainShops[2].hoursDisplay)
        assertTrue(domainShops[2].isOpenAt(90))
    }

    @Test
    fun `IndexedCoffeeCatalog handles 2500 shops with instant area slicing`() {
        val areas = DhakaArea.selectableAreas
        val largeCatalog = (1..2500).map { idx ->
            val area = areas[idx % areas.size]
            CoffeeShop(
                id = "shop-$idx",
                name = "Dhaka Night Cafe #$idx",
                area = area,
                areaLabel = area.displayName,
                address = "Road $idx, ${area.displayName}, Dhaka",
                openTime = "10:00 AM",
                closeTime = "02:00 AM",
                hoursDisplay = "10:00 AM - 2:00 AM",
                openMinuteOfDay = 10 * 60,
                closeMinuteOfDay = 2 * 60,
                popularItems = listOf("Cappuccino", "Spanish Latte"),
                priceRange = "৳250 - ৳500",
                rating = 4.5,
                phone = "+8801700000000",
                isFamilyFriendly = idx % 2 == 0
            )
        }
        val indexed = IndexedCoffeeCatalog.fromShops(largeCatalog)
        assertEquals(2500, indexed.allShops.size)
        assertNotNull(indexed.byId["shop-2499"])
        val bananiSlice = indexed.sliceForArea(DhakaArea.BANANI)
        assertTrue(bananiSlice.isNotEmpty())
        assertTrue(bananiSlice.all { it.area == DhakaArea.BANANI })
    }

    @Test
    fun `CoffeeViewModel supports filters, favorites, community shop creation, and reviews`() = runTest {
        val initialShop = CoffeeShop(
            id = "north-end-banani",
            name = "North End Coffee Roasters",
            area = DhakaArea.BANANI,
            areaLabel = "Banani",
            address = "House 41, Road 11, Banani, Dhaka",
            openTime = "08:00 AM",
            closeTime = "01:00 AM",
            hoursDisplay = "8:00 AM - 1:00 AM",
            openMinuteOfDay = 8 * 60,
            closeMinuteOfDay = 1 * 60,
            popularItems = listOf("Classic Cappuccino", "Spanish Latte"),
            priceRange = "৳250 - ৳550",
            rating = 4.8,
            phone = "+8801711002411",
            isFamilyFriendly = true
        )

        val viewModel = CoffeeViewModel(
            repository = CoffeeRepository(
                assetLoader = InMemoryCoffeeAssetLoader(listOf(initialShop)),
                communityDataSource = InMemoryCommunityDataSource()
            ),
            favoritesStorage = InMemoryFavoritesStorage(),
            currentMinuteProvider = { 90 }, // 1:30 AM (Banani shop closing at 1:00 AM is closed)
            currentTimeFormatter = { "1:30 AM" },
            computeDispatcher = testDispatcher,
            searchDebounceMs = 0L
        )

        val collectJob = launch(testDispatcher) { viewModel.uiState.collect() }

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(1, viewModel.uiState.value.shops.size)
        assertFalse(viewModel.uiState.value.shops.first().isOpenNow)

        // Add a community shop open until 03:00 AM in Bashundhara
        val addResult = viewModel.addShop(
            NewCoffeeShopInput(
                name = "Midnight Brew Hub",
                area = DhakaArea.BASHUNDHARA,
                address = "Block C, Bashundhara R/A",
                openTime = "16:00",
                closeTime = "03:00",
                popularItems = listOf("Hazelnut Latte", "Cold Brew"),
                priceRange = "৳260 - ৳520",
                phone = "+8801711223344",
                familyFriendly = false
            )
        )
        assertTrue(addResult.isSuccess)
        assertEquals(2, viewModel.uiState.value.shops.size)

        // Open Now filter keeps only Midnight Brew Hub at 1:30 AM
        viewModel.setOpenNowOnly(true)
        assertEquals(1, viewModel.uiState.value.shops.size)
        val openShop = viewModel.uiState.value.shops.first()
        assertEquals("Midnight Brew Hub", openShop.shop.name)
        assertTrue(openShop.isOpenNow)

        // Family Friendly filter hides Midnight Brew Hub (familyFriendly = false)
        viewModel.setFamilyFriendlyOnly(true)
        assertEquals(0, viewModel.uiState.value.shops.size)

        // Reset filters and test search + favorites
        viewModel.resetFilters()
        viewModel.onSearchQueryChange("Cappuccino")
        assertEquals(1, viewModel.uiState.value.shops.size)
        assertEquals("north-end-banani", viewModel.uiState.value.shops.first().shop.id)

        viewModel.clearSearch()
        viewModel.toggleFavorite("north-end-banani")
        viewModel.selectTab(HomeTab.FAVORITES)
        assertEquals(1, viewModel.uiState.value.shops.size)

        // Submit and verify review updates detail state and rating
        val reviewRes = viewModel.submitReview("north-end-banani", 5, "Great midnight espresso!")
        assertTrue(reviewRes.isSuccess)
        val detail = viewModel.observeShopDetail("north-end-banani").first()
        assertNotNull(detail)
        assertEquals(1, detail!!.reviews.size)
        assertEquals("Great midnight espresso!", detail.reviews.first().comment)

        collectJob.cancel()
    }
}
