package com.example.data

import com.example.R
import com.example.model.CoffeeShop
import com.example.model.DhakaArea

interface CoffeeShopRepository {
    fun getCoffeeShops(): List<CoffeeShop>
    fun getCoffeeShopById(id: String): CoffeeShop?
}

class DhakaCoffeeShopRepository : CoffeeShopRepository {

    private val shops = listOf(
        CoffeeShop(
            id = "north-end-banani",
            name = "North End Coffee Roasters",
            area = DhakaArea.BANANI,
            fullAddress = "House 41, Road 11, Block F, Banani, Dhaka 1213",
            hoursDisplay = "8:00 AM - 1:00 AM",
            openMinuteOfDay = 8 * 60,
            closeMinuteOfDay = 1 * 60,
            rating = 4.8,
            isFamilyFriendly = true,
            phoneNumber = "+8801711002411",
            latitude = 23.7937,
            longitude = 90.4066,
            ambianceNote = "In-house small-batch roastery with late-night seating",
            imageRes = R.drawable.img_cafe_roastery,
            popularMenu = listOf(
                MenuItem("Sylhet Reserve Pour-Over", "Single-origin washed pour-over with bright citrus notes", 380, true),
                MenuItem("Caramel Macchiato", "Double espresso layered with steamed milk and caramel", 340, false),
                MenuItem("Iced Spanish Latte", "Chilled sweetened condensed milk latte over ice", 360, true),
                MenuItem("Classic Cappuccino", "Balanced 1:1:1 espresso, steamed milk, and microfoam", 310, false),
                MenuItem("Dark Chocolate Brownie", "Warm fudgy brownie served alongside espresso", 240, false)
            )
        ),
        CoffeeShop(
            id = "tabaq-dhanmondi",
            name = "Tabaq Coffee",
            area = DhakaArea.DHANMONDI,
            fullAddress = "House 45, Satmasjid Road (Road 27), Dhanmondi, Dhaka 1209",
            hoursDisplay = "10:00 AM - 2:00 AM",
            openMinuteOfDay = 10 * 60,
            closeMinuteOfDay = 2 * 60,
            rating = 4.7,
            isFamilyFriendly = true,
            phoneNumber = "+8801841233344",
            latitude = 23.7461,
            longitude = 90.3742,
            ambianceNote = "Quiet late-night lounge popular with readers and night owls",
            imageRes = R.drawable.img_cafe_lounge,
            popularMenu = listOf(
                MenuItem("Tabaq Signature Cold Brew", "Slow-steeped 18-hour cold brew with smooth cocoa finish", 350, true),
                MenuItem("Hazelnut Flat White", "Velvety microfoam over ristretto shots with roasted hazelnut", 330, true),
                MenuItem("Iced Caramel Frappe", "Blended iced coffee with caramel swirl and whipped cream", 370, false),
                MenuItem("Butter Croissant", "Flaky warm French pastry for late-night snacking", 220, false)
            )
        ),
        CoffeeShop(
            id = "kiva-han-khilgaon",
            name = "Kiva Han Coffee",
            area = DhakaArea.KHILGAON,
            fullAddress = "624/C, Taltola Market Lane, Block C, Khilgaon, Dhaka 1219",
            hoursDisplay = "4:00 PM - 3:00 AM",
            openMinuteOfDay = 16 * 60,
            closeMinuteOfDay = 3 * 60,
            rating = 4.6,
            isFamilyFriendly = true,
            phoneNumber = "+8801912556677",
            latitude = 23.7519,
            longitude = 90.4241,
            ambianceNote = "Open-air lantern-lit terrace built for midnight hangouts",
            imageRes = R.drawable.img_cafe_terrace,
            popularMenu = listOf(
                MenuItem("Turkish Cardamom Coffee", "Traditional finely ground coffee infused with green cardamom", 280, true),
                MenuItem("Nutella Espresso Shake", "Thick chilled espresso milkshake blended with hazelnut cocoa", 390, true),
                MenuItem("Iced Americano", "Bold double espresso poured over chilled water and ice", 230, false),
                MenuItem("Lotus Biscoff Cheesecake", "Creamy baked slice topped with spiced speculoos crumb", 340, false)
            )
        ),
        CoffeeShop(
            id = "beans-aroma-mirpur",
            name = "Beans & Aroma",
            area = DhakaArea.MIRPUR,
            fullAddress = "House 18, Begum Rokeya Sarani, Mirpur 10, Dhaka 1216",
            hoursDisplay = "11:00 AM - 11:00 PM",
            openMinuteOfDay = 11 * 60,
            closeMinuteOfDay = 23 * 60,
            rating = 4.5,
            isFamilyFriendly = true,
            phoneNumber = "+8801677334455",
            latitude = 23.8069,
            longitude = 90.3687,
            ambianceNote = "Warm neighborhood cafe near Mirpur 10 for evening meetups",
            imageRes = R.drawable.img_cafe_espresso,
            popularMenu = listOf(
                MenuItem("Classic Cafe Latte", "Smooth steamed milk over medium-roast espresso", 270, true),
                MenuItem("Brown Sugar Shaken Espresso", "Espresso shaken with caramelized brown sugar", 320, true),
                MenuItem("Vanilla Cold Coffee", "Creamy vanilla coffee blend served chilled", 290, false),
                MenuItem("Hot Chocolate", "Velvety steamed cocoa topped with cream", 260, false),
                MenuItem("Garlic Cheese Toast", "Toasted brioche with herb garlic butter", 220, false)
            )
        ),
        CoffeeShop(
            id = "gloria-jeans-dhanmondi",
            name = "Gloria Jean's Coffees",
            area = DhakaArea.DHANMONDI,
            fullAddress = "House 742, Road 27 (Old), Dhanmondi, Dhaka 1209",
            hoursDisplay = "9:00 AM - 4:00 AM",
            openMinuteOfDay = 9 * 60,
            closeMinuteOfDay = 4 * 60,
            rating = 4.6,
            isFamilyFriendly = true,
            phoneNumber = "+8801713445566",
            latitude = 23.7523,
            longitude = 90.3775,
            ambianceNote = "Dhaka's classic late-night espresso destination welcoming families till 4 AM",
            imageRes = R.drawable.img_cafe_roastery,
            popularMenu = listOf(
                MenuItem("Voltage Chiller", "Espresso, dark chocolate & cream blended icy", 450, true),
                MenuItem("Creme Brulee Latte", "Caramelized vanilla custard espresso", 390, true),
                MenuItem("Very Vanilla Chiller", "Smooth chilled coffee with Madagascar vanilla", 420, false),
                MenuItem("Cafe Mocha", "Rich espresso folded with chocolate & milk", 340, false),
                MenuItem("Blueberry Muffin", "Soft baked muffin bursting with berries", 250, false)
            )
        )
    )

    override fun getCoffeeShops(): List<CoffeeShop> = shops

    override fun getCoffeeShopById(id: String): CoffeeShop? = shops.find { it.id == id }
}
