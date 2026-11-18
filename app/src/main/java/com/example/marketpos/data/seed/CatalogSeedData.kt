package com.example.marketpos.data.seed

import com.example.marketpos.model.DefaultLocations
import com.example.marketpos.model.Product
import com.example.marketpos.model.ProductCategory
import com.example.marketpos.model.UserRole
import com.example.marketpos.model.VenueLocation
import com.example.marketpos.model.VenueMode
import com.example.marketpos.model.WorkerProfile

object CatalogSeedData {

    val workers: List<WorkerProfile> = listOf(
        WorkerProfile(
            id = "w_marko",
            name = "Marko Petrović",
            role = UserRole.MARKET_WORKER,
            pin = "1234",
            shiftLabelSr = "Market Centar • Prepodnevna",
            shiftLabelEn = "Market Centar • Morning",
            locationId = DefaultLocations.MARKET_CENTRAL_ID
        ),
        WorkerProfile(
            id = "w_nikola",
            name = "Nikola Đorđević",
            role = UserRole.MARKET_WORKER,
            pin = "1234",
            shiftLabelSr = "Market Centar • Popodnevna",
            shiftLabelEn = "Market Centar • Afternoon",
            locationId = DefaultLocations.MARKET_CENTRAL_ID
        ),
        WorkerProfile(
            id = "w_stefan",
            name = "Stefan Ilić",
            role = UserRole.MARKET_WORKER,
            pin = "1234",
            shiftLabelSr = "Market Novi Beograd • Smena",
            shiftLabelEn = "Market Novi Beograd • Shift",
            locationId = DefaultLocations.MARKET_NBG_ID
        ),
        WorkerProfile(
            id = "w_jelena",
            name = "Jelena Jovanović",
            role = UserRole.RESTAURANT_WORKER,
            pin = "1234",
            shiftLabelSr = "Kafe Bar Korzo • Prva smena",
            shiftLabelEn = "Cafe Bar Korzo • First Shift",
            locationId = DefaultLocations.BAR_CENTER_ID
        ),
        WorkerProfile(
            id = "w_milica",
            name = "Milica Ilić",
            role = UserRole.RESTAURANT_WORKER,
            pin = "1234",
            shiftLabelSr = "Kafe Bar Korzo • Druga smena",
            shiftLabelEn = "Cafe Bar Korzo • Second Shift",
            locationId = DefaultLocations.BAR_CENTER_ID
        ),
        WorkerProfile(
            id = "w_luka",
            name = "Luka Nikolić",
            role = UserRole.RESTAURANT_WORKER,
            pin = "1234",
            shiftLabelSr = "Restoran Reka • Smena",
            shiftLabelEn = "Restaurant Reka • Shift",
            locationId = DefaultLocations.BAR_RIVER_ID
        )
    )

    val adminProfile: WorkerProfile = WorkerProfile(
        id = "admin_ana",
        name = "Ana Kovačević (Admin)",
        role = UserRole.ADMIN,
        pin = "0000",
        shiftLabelSr = "Menadžer objekata",
        shiftLabelEn = "Operations Manager",
        locationId = DefaultLocations.MARKET_CENTRAL_ID
    )

    val defaultLocations: List<VenueLocation> = listOf(
        VenueLocation(
            id = DefaultLocations.MARKET_CENTRAL_ID,
            name = "Market Centar",
            address = "Knez Mihailova 14, Beograd",
            venueMode = VenueMode.MARKET,
            sortOrder = 1
        ),
        VenueLocation(
            id = DefaultLocations.MARKET_NBG_ID,
            name = "Market Novi Beograd",
            address = "Bul. Zorana Đinđića 44, Beograd",
            venueMode = VenueMode.MARKET,
            sortOrder = 2
        ),
        VenueLocation(
            id = DefaultLocations.BAR_CENTER_ID,
            name = "Kafe Bar Korzo",
            address = "Obilićev venac 21, Beograd",
            venueMode = VenueMode.BAR_RESTAURANT,
            sortOrder = 3
        ),
        VenueLocation(
            id = DefaultLocations.BAR_RIVER_ID,
            name = "Restoran Reka",
            address = "Kej oslobođenja 8, Zemun",
            venueMode = VenueMode.BAR_RESTAURANT,
            sortOrder = 4
        )
    )

    fun buildMarketSeedProducts(
        locationId: String = DefaultLocations.MARKET_CENTRAL_ID,
        idPrefix: String = ""
    ): List<Product> = listOf(
        Product("${idPrefix}p1", "101", "Domaći hleb Sava 500g", "Artisan White Bread 500g", ProductCategory.BAKERY, 75.0, "kom", "loaf", stockQuantity = 32, lowStockThreshold = 6, venueMode = VenueMode.MARKET, locationId = locationId),
        Product("${idPrefix}p2", "102", "Kroasan sa puterom", "Butter Croissant", ProductCategory.BAKERY, 110.0, "kom", "pcs", stockQuantity = 4, lowStockThreshold = 5, venueMode = VenueMode.MARKET, locationId = locationId),
        Product("${idPrefix}p3", "103", "Integralna kifla", "Wholegrain Roll", ProductCategory.BAKERY, 55.0, "kom", "pcs", stockQuantity = 24, lowStockThreshold = 5, venueMode = VenueMode.MARKET, locationId = locationId),
        Product("${idPrefix}p4", "201", "Sveže mleko 1L 2.8%", "Fresh Whole Milk 1L", ProductCategory.DAIRY, 145.0, "fl", "btl", stockQuantity = 28, lowStockThreshold = 6, venueMode = VenueMode.MARKET, locationId = locationId),
        Product("${idPrefix}p5", "202", "Grčki jogurt 400g", "Greek Yogurt 400g", ProductCategory.DAIRY, 185.0, "kom", "cup", stockQuantity = 16, lowStockThreshold = 5, venueMode = VenueMode.MARKET, locationId = locationId),
        Product("${idPrefix}p6", "203", "Mladi sir 250g", "Fresh Cottage Cheese 250g", ProductCategory.DAIRY, 240.0, "pak", "pack", stockQuantity = 3, lowStockThreshold = 5, venueMode = VenueMode.MARKET, locationId = locationId),
        Product("${idPrefix}p7", "204", "Domaća jaja M 10/1", "Free-Range Eggs 10pk", ProductCategory.DAIRY, 230.0, "pak", "box", stockQuantity = 18, lowStockThreshold = 5, venueMode = VenueMode.MARKET, locationId = locationId),
        Product("${idPrefix}p8", "301", "Negazirana voda 1.5L", "Still Mineral Water 1.5L", ProductCategory.BEVERAGES, 70.0, "fl", "btl", stockQuantity = 48, lowStockThreshold = 10, venueMode = VenueMode.MARKET, locationId = locationId),
        Product("${idPrefix}p9", "302", "Ceđeni sok pomorandža 1L", "Fresh Orange Juice 1L", ProductCategory.BEVERAGES, 290.0, "fl", "btl", stockQuantity = 12, lowStockThreshold = 4, venueMode = VenueMode.MARKET, locationId = locationId),
        Product("${idPrefix}p10", "303", "Domaća kafa 200g", "Roasted Ground Coffee 200g", ProductCategory.BEVERAGES, 380.0, "pak", "bag", stockQuantity = 22, lowStockThreshold = 5, venueMode = VenueMode.MARKET, locationId = locationId),
        Product("${idPrefix}p11", "401", "Jabuke Zlatni delišes 1kg", "Golden Apples 1kg", ProductCategory.PRODUCE, 160.0, "kg", "kg", stockQuantity = 19, lowStockThreshold = 5, venueMode = VenueMode.MARKET, locationId = locationId),
        Product("${idPrefix}p12", "402", "Banane 1kg", "Bananas 1kg", ProductCategory.PRODUCE, 195.0, "kg", "kg", stockQuantity = 5, lowStockThreshold = 5, venueMode = VenueMode.MARKET, locationId = locationId),
        Product("${idPrefix}p13", "403", "Paradajz čeri 250g", "Cherry Tomatoes 250g", ProductCategory.PRODUCE, 175.0, "pak", "pack", stockQuantity = 0, lowStockThreshold = 4, venueMode = VenueMode.MARKET, locationId = locationId),
        Product("${idPrefix}p14", "501", "Tamna čokolada 75% 100g", "Dark Chocolate 75% 100g", ProductCategory.SNACKS, 210.0, "kom", "bar", stockQuantity = 30, lowStockThreshold = 5, venueMode = VenueMode.MARKET, locationId = locationId),
        Product("${idPrefix}p15", "502", "Slani štapići sa susamom", "Sesame Pretzel Sticks", ProductCategory.SNACKS, 95.0, "pak", "bag", stockQuantity = 25, lowStockThreshold = 5, venueMode = VenueMode.MARKET, locationId = locationId),
        Product("${idPrefix}p16", "503", "Pečeni badem 150g", "Roasted Almonds 150g", ProductCategory.SNACKS, 320.0, "pak", "pack", stockQuantity = 2, lowStockThreshold = 4, venueMode = VenueMode.MARKET, locationId = locationId),
        Product("${idPrefix}p17", "601", "Deterdžent za sudove 500ml", "Dishwashing Liquid 500ml", ProductCategory.HOUSEHOLD, 260.0, "fl", "btl", stockQuantity = 14, lowStockThreshold = 4, venueMode = VenueMode.MARKET, locationId = locationId),
        Product("${idPrefix}p18", "602", "Papirni ubrusi 2/1", "Kitchen Paper Towels 2pk", ProductCategory.HOUSEHOLD, 215.0, "pak", "pack", stockQuantity = 20, lowStockThreshold = 5, venueMode = VenueMode.MARKET, locationId = locationId)
    )

    fun buildSecondMarketSeedProducts(locationId: String): List<Product> = listOf(
        Product("nbg_p1", "N101", "Baguette hrskavi", "Crispy Baguette", ProductCategory.BAKERY, 95.0, "kom", "loaf", stockQuantity = 22, lowStockThreshold = 5, venueMode = VenueMode.MARKET, locationId = locationId),
        Product("nbg_p2", "N102", "Burek sa sirom", "Cheese Pastry", ProductCategory.BAKERY, 180.0, "kom", "pcs", stockQuantity = 14, lowStockThreshold = 4, venueMode = VenueMode.MARKET, locationId = locationId),
        Product("nbg_p3", "N201", "Kefir 500ml", "Kefir Drink 500ml", ProductCategory.DAIRY, 130.0, "fl", "btl", stockQuantity = 19, lowStockThreshold = 5, venueMode = VenueMode.MARKET, locationId = locationId),
        Product("nbg_p4", "N202", "Feta sir 300g", "Feta Cheese 300g", ProductCategory.DAIRY, 340.0, "pak", "pack", stockQuantity = 3, lowStockThreshold = 5, venueMode = VenueMode.MARKET, locationId = locationId),
        Product("nbg_p5", "N301", "Gazirana voda 1.5L", "Sparkling Water 1.5L", ProductCategory.BEVERAGES, 75.0, "fl", "btl", stockQuantity = 36, lowStockThreshold = 8, venueMode = VenueMode.MARKET, locationId = locationId),
        Product("nbg_p6", "N302", "Espresso u zrnu 250g", "Espresso Beans 250g", ProductCategory.BEVERAGES, 640.0, "pak", "bag", stockQuantity = 11, lowStockThreshold = 4, venueMode = VenueMode.MARKET, locationId = locationId),
        Product("nbg_p7", "N401", "Avokado kom", "Avocado", ProductCategory.PRODUCE, 190.0, "kom", "pcs", stockQuantity = 0, lowStockThreshold = 4, venueMode = VenueMode.MARKET, locationId = locationId),
        Product("nbg_p8", "N402", "Borovnice 125g", "Blueberries 125g", ProductCategory.PRODUCE, 280.0, "pak", "pack", stockQuantity = 15, lowStockThreshold = 4, venueMode = VenueMode.MARKET, locationId = locationId),
        Product("nbg_p9", "N501", "Proteinska čokoladica", "Protein Bar", ProductCategory.SNACKS, 220.0, "kom", "bar", stockQuantity = 28, lowStockThreshold = 6, venueMode = VenueMode.MARKET, locationId = locationId),
        Product("nbg_p10", "N601", "Sredstvo za staklo 750ml", "Glass Cleaner 750ml", ProductCategory.HOUSEHOLD, 290.0, "fl", "btl", stockQuantity = 9, lowStockThreshold = 4, venueMode = VenueMode.MARKET, locationId = locationId)
    )

    fun buildBarSeedProducts(
        locationId: String = DefaultLocations.BAR_CENTER_ID,
        idPrefix: String = ""
    ): List<Product> = listOf(
        Product("${idPrefix}b1", "B101", "Espresso kafa", "Single Espresso", ProductCategory.COFFEE_TEA, 190.0, "šolj", "cup", stockQuantity = 80, lowStockThreshold = 10, venueMode = VenueMode.BAR_RESTAURANT, locationId = locationId),
        Product("${idPrefix}b2", "B102", "Kapućino", "Cappuccino", ProductCategory.COFFEE_TEA, 240.0, "šolj", "cup", stockQuantity = 60, lowStockThreshold = 10, venueMode = VenueMode.BAR_RESTAURANT, locationId = locationId),
        Product("${idPrefix}b3", "B103", "Biljni čaj sa medom", "Herbal Tea & Honey", ProductCategory.COFFEE_TEA, 210.0, "šolj", "cup", stockQuantity = 40, lowStockThreshold = 6, venueMode = VenueMode.BAR_RESTAURANT, locationId = locationId),
        Product("${idPrefix}b4", "B201", "Točeno pivo 0.5L", "Draft Lager Beer 0.5L", ProductCategory.BAR_DRINKS, 340.0, "čaša", "glass", stockQuantity = 50, lowStockThreshold = 8, venueMode = VenueMode.BAR_RESTAURANT, locationId = locationId),
        Product("${idPrefix}b5", "B202", "Ceđena limunada 0.3L", "Fresh Lemonade 0.3L", ProductCategory.BAR_DRINKS, 280.0, "čaša", "glass", stockQuantity = 35, lowStockThreshold = 6, venueMode = VenueMode.BAR_RESTAURANT, locationId = locationId),
        Product("${idPrefix}b6", "B203", "Mineralna voda 0.25L", "Sparkling Water 0.25L", ProductCategory.BAR_DRINKS, 180.0, "fl", "btl", stockQuantity = 45, lowStockThreshold = 8, venueMode = VenueMode.BAR_RESTAURANT, locationId = locationId),
        Product("${idPrefix}b7", "B204", "Crveno vino 0.15L", "House Red Wine 0.15L", ProductCategory.BAR_DRINKS, 420.0, "čaša", "glass", stockQuantity = 4, lowStockThreshold = 5, venueMode = VenueMode.BAR_RESTAURANT, locationId = locationId),
        Product("${idPrefix}b8", "B301", "Klub sendvič", "Club Sandwich", ProductCategory.MAIN_DISHES, 690.0, "por", "plate", stockQuantity = 18, lowStockThreshold = 4, venueMode = VenueMode.BAR_RESTAURANT, locationId = locationId),
        Product("${idPrefix}b9", "B302", "Cezar salata", "Caesar Salad", ProductCategory.MAIN_DISHES, 790.0, "por", "plate", stockQuantity = 14, lowStockThreshold = 4, venueMode = VenueMode.BAR_RESTAURANT, locationId = locationId),
        Product("${idPrefix}b10", "B303", "Goveđi burger i pomfrit", "Beef Burger & Fries", ProductCategory.MAIN_DISHES, 980.0, "por", "plate", stockQuantity = 16, lowStockThreshold = 4, venueMode = VenueMode.BAR_RESTAURANT, locationId = locationId),
        Product("${idPrefix}b11", "B304", "Pasta Karbonara", "Pasta Carbonara", ProductCategory.MAIN_DISHES, 860.0, "por", "plate", stockQuantity = 3, lowStockThreshold = 4, venueMode = VenueMode.BAR_RESTAURANT, locationId = locationId),
        Product("${idPrefix}b12", "B401", "Čizkejk šumsko voće", "Berry Cheesecake", ProductCategory.DESSERTS, 430.0, "parče", "slice", stockQuantity = 12, lowStockThreshold = 3, venueMode = VenueMode.BAR_RESTAURANT, locationId = locationId),
        Product("${idPrefix}b13", "B402", "Tiramisu", "Classic Tiramisu", ProductCategory.DESSERTS, 460.0, "parče", "slice", stockQuantity = 0, lowStockThreshold = 3, venueMode = VenueMode.BAR_RESTAURANT, locationId = locationId)
    )

    fun buildSecondBarSeedProducts(locationId: String): List<Product> = listOf(
        Product("br2_b1", "R101", "Dupli Espresso", "Double Espresso", ProductCategory.COFFEE_TEA, 240.0, "šolj", "cup", stockQuantity = 65, lowStockThreshold = 10, venueMode = VenueMode.BAR_RESTAURANT, locationId = locationId),
        Product("br2_b2", "R102", "Ledeni Latte", "Iced Latte", ProductCategory.COFFEE_TEA, 290.0, "čaša", "glass", stockQuantity = 38, lowStockThreshold = 8, venueMode = VenueMode.BAR_RESTAURANT, locationId = locationId),
        Product("br2_b3", "R201", "Craft IPA 0.5L", "Craft IPA Beer 0.5L", ProductCategory.BAR_DRINKS, 420.0, "čaša", "glass", stockQuantity = 29, lowStockThreshold = 6, venueMode = VenueMode.BAR_RESTAURANT, locationId = locationId),
        Product("br2_b4", "R202", "Aperol Spritz", "Aperol Spritz", ProductCategory.BAR_DRINKS, 560.0, "čaša", "glass", stockQuantity = 24, lowStockThreshold = 5, venueMode = VenueMode.BAR_RESTAURANT, locationId = locationId),
        Product("br2_b5", "R203", "Belo vino 0.15L", "House White Wine 0.15L", ProductCategory.BAR_DRINKS, 410.0, "čaša", "glass", stockQuantity = 4, lowStockThreshold = 6, venueMode = VenueMode.BAR_RESTAURANT, locationId = locationId),
        Product("br2_b6", "R301", "Grilovani losos", "Grilled Salmon Fillet", ProductCategory.MAIN_DISHES, 1590.0, "por", "plate", stockQuantity = 10, lowStockThreshold = 3, venueMode = VenueMode.BAR_RESTAURANT, locationId = locationId),
        Product("br2_b7", "R302", "Rižoto sa vrganjima", "Porcini Mushroom Risotto", ProductCategory.MAIN_DISHES, 940.0, "por", "plate", stockQuantity = 14, lowStockThreshold = 4, venueMode = VenueMode.BAR_RESTAURANT, locationId = locationId),
        Product("br2_b8", "R303", "Bruskete trio", "Bruschetta Trio", ProductCategory.MAIN_DISHES, 520.0, "por", "plate", stockQuantity = 19, lowStockThreshold = 4, venueMode = VenueMode.BAR_RESTAURANT, locationId = locationId),
        Product("br2_b9", "R401", "Palačinke sa nutelom", "Chocolate Crepes", ProductCategory.DESSERTS, 390.0, "por", "plate", stockQuantity = 16, lowStockThreshold = 4, venueMode = VenueMode.BAR_RESTAURANT, locationId = locationId)
    )
}
