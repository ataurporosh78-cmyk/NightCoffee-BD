package com.example.data

import com.example.R
import com.example.model.CoffeeShop
import com.example.model.DhakaArea
import com.example.model.MenuItem

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
            ambianceNote = "In-house small-batch roasting with warm wooden booths and calm late-evening seating.",
            imageRes = R.drawable.img_cafe_roastery,
            popularMenu = listOf(
                MenuItem("Sylhet Reserve Pour-Over", "Single-origin hand-brewed filter coffee", 320, isHouseFavorite = true),
                MenuItem("Caramel Macchiato", "Double espresso, steamed milk, house caramel", 360),
                MenuItem("Iced Spanish Latte", "Espresso over chilled milk & condensed cream", 380, isHouseFavorite = true),
                MenuItem("Classic Cappuccino", "Balanced espresso with velvety microfoam", 290),
                MenuItem("Dark Chocolate Brownie", "Warm fudge square roasted in-house", 260)
            )
        ),
        CoffeeShop(
            id = "tabaq-dhanmondi",
            name = "Tabaq Coffee",
            area = DhakaArea.DHANMONDI,
            fullAddress = "House 45, Satmasjid Road (Road 9/A), Dhanmondi, Dhaka 1209",
            hoursDisplay = "10:00 AM - 2:00 AM",
            openMinuteOfDay = 10 * 60,
            closeMinuteOfDay = 2 * 60,
            rating = 4.7,
            isFamilyFriendly = true,
            phoneNumber = "+8801841223344",
            latitude = 23.7465,
            longitude = 90.3712,
            ambianceNote = "Spacious two-floor lounge popular for late-night family hangouts and specialty espresso.",
            imageRes = R.drawable.img_cafe_espresso,
            popularMenu = listOf(
                MenuItem("Hazelnut Mocha", "Rich Belgian cocoa, espresso & roasted hazelnut", 390, isHouseFavorite = true),
                MenuItem("Tabaq Signature Cold Brew", "18-hour cold steep over crystal ice", 310),
                MenuItem("Flat White", "Ristretto double shot with silky steamed milk", 300),
                MenuItem("Iced Caramel Frappe", "Blended espresso cream with whipped topping", 410, isHouseFavorite = true),
                MenuItem("Butter Croissant", "Flaky warm French-style pastry", 240)
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
            ambianceNote = "Open-air lantern-lit terrace crafted for Khilgaon's vibrant post-dinner coffee crowd.",
            imageRes = R.drawable.img_cafe_terrace,
            popularMenu = listOf(
                MenuItem("Turkish Cardamom Coffee", "Traditional ibrik brew with aromatic cardamom", 280, isHouseFavorite = true),
                MenuItem("Nutella Espresso Shake", "Creamy hazelnut chocolate blended with espresso", 370),
                MenuItem("Cinnamon Honey Latte", "Warm spiced milk & rich double shot", 330, isHouseFavorite = true),
                MenuItem("Iced Americano", "Bold double espresso chilled with spring water", 230),
                MenuItem("Lotus Biscoff Cheesecake", "Creamy cheesecake slice with spiced biscuit", 390)
            )
        ),
        CoffeeShop(
            id = "crimson-cup-uttara",
            name = "Crimson Cup Coffee",
            area = DhakaArea.UTTARA,
            fullAddress = "Plot 12, Gareeb-E-Newaz Avenue, Sector 11, Uttara, Dhaka 1230",
            hoursDisplay = "10:00 AM - 12:30 AM",
            openMinuteOfDay = 10 * 60,
            closeMinuteOfDay = 0 * 60 + 30,
            rating = 4.7,
            isFamilyFriendly = true,
            phoneNumber = "+8801755889900",
            latitude = 23.8759,
            longitude = 90.3895,
            ambianceNote = "Quiet, family-welcoming espresso house with plush leather seating and midnight desserts.",
            imageRes = R.drawable.img_cafe_lounge,
            popularMenu = listOf(
                MenuItem("Crimson Mocha", "Signature white & dark chocolate espresso blend", 395, isHouseFavorite = true),
                MenuItem("Roasted Almond Latte", "Steamed milk infused with toasted almond syrup", 350),
                MenuItem("Iced Salted Caramel", "Sweet-savory caramel over espresso and milk", 375, isHouseFavorite = true),
                MenuItem("Double Espresso", "Rich crema double-shot pull", 210),
                MenuItem("Red Velvet Pastry", "Classic cocoa sponge with cream cheese frosting", 340)
            )
        ),
        CoffeeShop(
            id = "beans-aroma-mirpur",
            name = "Beans & Aroma",
            area = DhakaArea.MIRPUR,
            fullAddress = "House 18, Begum Rokeya Sarani, Section 10, Mirpur, Dhaka 1216",
            hoursDisplay = "11:00 AM - 11:00 PM",
            openMinuteOfDay = 11 * 60,
            closeMinuteOfDay = 23 * 60,
            rating = 4.5,
            isFamilyFriendly = true,
            phoneNumber = "+8801677334455",
            latitude = 23.8069,
            longitude = 90.3687,
            ambianceNote = "Warm neighborhood cafe near Mirpur 10 circle ideal for families and evening study.",
            imageRes = R.drawable.img_cafe_espresso,
            popularMenu = listOf(
                MenuItem("Classic Cafe Latte", "Smooth steamed milk over medium-roast espresso", 270, isHouseFavorite = true),
                MenuItem("Iced Brown Sugar Shaken", "Espresso shaken with caramelized brown sugar", 320),
                MenuItem("Vanilla Cold Coffee", "Chilled creamy vanilla coffee blend", 290, isHouseFavorite = true),
                MenuItem("Hot Chocolate", "Velvety steamed cocoa topped with cream", 260),
                MenuItem("Garlic Cheese Toast", "Toasted brioche with herb garlic butter", 220)
            )
        ),
        CoffeeShop(
            id = "gloria-jeans-dhanmondi",
            name = "Gloria Jean's Coffees",
            area = DhakaArea.DHANMONDI,
            fullAddress = "House 28, Road 27 (Old), Dhanmondi, Dhaka 1209",
            hoursDisplay = "9:00 AM - 4:00 AM",
            openMinuteOfDay = 9 * 60,
            closeMinuteOfDay = 4 * 60,
            rating = 4.6,
            isFamilyFriendly = true,
            phoneNumber = "+8801713445566",
            latitude = 23.7523,
            longitude = 90.3775,
            ambianceNote = "Dhaka's classic late-night espresso destination welcoming families till 4 AM.",
            imageRes = R.drawable.img_cafe_roastery,
            popularMenu = listOf(
                MenuItem("Voltage Chiller", "Espresso, dark chocolate & cream blended icy", 450, isHouseFavorite = true),
                MenuItem("Creme Brulee Latte", "Caramelized vanilla custard espresso", 390),
                MenuItem("Very Vanilla Chiller", "Smooth chilled coffee with Madagascar vanilla", 420, isHouseFavorite = true),
                MenuItem("Cafe Mocha", "Rich espresso folded with chocolate & milk", 340),
                MenuItem("Blueberry Muffin", "Soft baked muffin bursting with berries", 250)
            )
        )
    )

    override fun getCoffeeShops(): List<CoffeeShop> = shops

    override fun getCoffeeShopById(id: String): CoffeeShop? = shops.find { it.id == id }
}
