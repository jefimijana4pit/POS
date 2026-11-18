package com.example.marketpos.data.seed

import com.example.marketpos.data.local.BarTableEntity
import com.example.marketpos.data.local.CatalogLocationDao
import com.example.marketpos.data.local.OrderEntity
import com.example.marketpos.data.local.OrderItemEntity
import com.example.marketpos.data.local.SaleLineItemEntity
import com.example.marketpos.data.local.SaleTransactionEntity
import com.example.marketpos.data.local.SalesDao
import com.example.marketpos.data.local.TableOrderDao
import com.example.marketpos.data.local.TableOrderItemEntity
import com.example.marketpos.data.local.toDomain
import com.example.marketpos.data.local.toEntity
import com.example.marketpos.model.BarTable
import com.example.marketpos.model.DefaultLocations
import com.example.marketpos.model.OrderItemStatus
import com.example.marketpos.model.OrderStatus
import com.example.marketpos.model.PaymentMethod
import com.example.marketpos.model.Product
import com.example.marketpos.model.ProductCategory
import com.example.marketpos.model.VenueMode
import com.example.marketpos.model.WorkerProfile
import com.example.marketpos.util.CurrencyFormatter
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class PosDatabaseSeeder(
    private val catalogLocationDao: CatalogLocationDao,
    private val salesDao: SalesDao,
    private val tableOrderDao: TableOrderDao
) {
    private val workers = CatalogSeedData.workers

    suspend fun ensureSeedData() {
        if (catalogLocationDao.countLocations() == 0) {
            catalogLocationDao.insertLocations(CatalogSeedData.defaultLocations.map { it.toEntity() })
        }

        if (catalogLocationDao.countProductsByLocation(DefaultLocations.MARKET_CENTRAL_ID) == 0) {
            val seedMarketProducts = CatalogSeedData.buildMarketSeedProducts(DefaultLocations.MARKET_CENTRAL_ID)
            catalogLocationDao.insertProducts(seedMarketProducts.map { it.toEntity() })
            if (salesDao.countTransactionsByLocation(DefaultLocations.MARKET_CENTRAL_ID) == 0) {
                seedInitialMarketTransactions(seedMarketProducts, DefaultLocations.MARKET_CENTRAL_ID)
            }
        }

        if (catalogLocationDao.countProductsByLocation(DefaultLocations.MARKET_NBG_ID) == 0) {
            val seedNbgProducts = CatalogSeedData.buildSecondMarketSeedProducts(DefaultLocations.MARKET_NBG_ID)
            catalogLocationDao.insertProducts(seedNbgProducts.map { it.toEntity() })
            if (salesDao.countTransactionsByLocation(DefaultLocations.MARKET_NBG_ID) == 0) {
                seedSecondMarketTransactions(seedNbgProducts, DefaultLocations.MARKET_NBG_ID)
            }
        }

        if (catalogLocationDao.countProductsByLocation(DefaultLocations.BAR_CENTER_ID) == 0) {
            val seedBarProducts = CatalogSeedData.buildBarSeedProducts(DefaultLocations.BAR_CENTER_ID)
            catalogLocationDao.insertProducts(seedBarProducts.map { it.toEntity() })
            if (salesDao.countTransactionsByLocation(DefaultLocations.BAR_CENTER_ID) == 0) {
                seedInitialBarTransactions(seedBarProducts, DefaultLocations.BAR_CENTER_ID)
            }
        }

        if (catalogLocationDao.countProductsByLocation(DefaultLocations.BAR_RIVER_ID) == 0) {
            val seedRiverProducts = CatalogSeedData.buildSecondBarSeedProducts(DefaultLocations.BAR_RIVER_ID)
            catalogLocationDao.insertProducts(seedRiverProducts.map { it.toEntity() })
            if (salesDao.countTransactionsByLocation(DefaultLocations.BAR_RIVER_ID) == 0) {
                seedSecondBarTransactions(seedRiverProducts, DefaultLocations.BAR_RIVER_ID)
            }
        }

        if (tableOrderDao.countTablesByLocation(DefaultLocations.BAR_CENTER_ID) == 0) {
            seedInitialBarTables()
        }

        if (tableOrderDao.countTablesByLocation(DefaultLocations.BAR_RIVER_ID) == 0) {
            seedSecondBarTables()
        }

        if (tableOrderDao.countOrders() == 0) {
            seedInitialOrders()
        }

        val cutoff = System.currentTimeMillis() - BarTable.TABLE_CLEANUP_AUTO_UNLOCK_MILLIS
        tableOrderDao.autoUnlockExpiredTables(cutoff)
    }

    private suspend fun seedInitialBarTables() {
        val now = System.currentTimeMillis()
        val korzoWorkers = workers.filter { it.locationId == DefaultLocations.BAR_CENTER_ID }
        val wJelena = korzoWorkers.getOrElse(0) { workers.first() }
        val wMilica = korzoWorkers.getOrElse(1) { wJelena }

        val tables = listOf(
            BarTableEntity(id = "t1", name = "Sto 1", zone = "Sala", capacity = 4, sortOrder = 1, locationId = DefaultLocations.BAR_CENTER_ID),
            BarTableEntity(
                id = "t2", name = "Sto 2", zone = "Sala", capacity = 4,
                lockedByWorkerId = wJelena.id, lockedByWorkerName = wJelena.name,
                lockedAtMillis = now - TimeUnit.MINUTES.toMillis(18),
                sortOrder = 2, locationId = DefaultLocations.BAR_CENTER_ID
            ),
            BarTableEntity(id = "t3", name = "Sto 3", zone = "Sala", capacity = 2, sortOrder = 3, locationId = DefaultLocations.BAR_CENTER_ID),
            BarTableEntity(
                id = "t4", name = "Sto 4", zone = "Bašta", capacity = 6,
                lockedByWorkerId = wMilica.id, lockedByWorkerName = wMilica.name,
                lockedAtMillis = now - TimeUnit.MINUTES.toMillis(7),
                sortOrder = 4, locationId = DefaultLocations.BAR_CENTER_ID
            ),
            BarTableEntity(id = "t5", name = "Sto 5", zone = "Bašta", capacity = 4, sortOrder = 5, locationId = DefaultLocations.BAR_CENTER_ID),
            BarTableEntity(id = "t6", name = "Šank 1", zone = "Šank", capacity = 2, sortOrder = 6, locationId = DefaultLocations.BAR_CENTER_ID),
            BarTableEntity(id = "t7", name = "Šank 2", zone = "Šank", capacity = 2, sortOrder = 7, locationId = DefaultLocations.BAR_CENTER_ID)
        )
        tableOrderDao.insertTables(tables)

        val initialOrderItems = listOf(
            TableOrderItemEntity(tableId = "t2", productId = "b2", productNameSr = "Kapućino", productNameEn = "Cappuccino", category = ProductCategory.COFFEE_TEA.name, unitPriceRsd = 240.0, quantity = 2),
            TableOrderItemEntity(tableId = "t2", productId = "b8", productNameSr = "Klub sendvič", productNameEn = "Club Sandwich", category = ProductCategory.MAIN_DISHES.name, unitPriceRsd = 690.0, quantity = 1),
            TableOrderItemEntity(tableId = "t4", productId = "b4", productNameSr = "Točeno pivo 0.5L", productNameEn = "Draft Lager Beer 0.5L", category = ProductCategory.BAR_DRINKS.name, unitPriceRsd = 340.0, quantity = 3),
            TableOrderItemEntity(tableId = "t4", productId = "b10", productNameSr = "Goveđi burger i pomfrit", productNameEn = "Beef Burger & Fries", category = ProductCategory.MAIN_DISHES.name, unitPriceRsd = 980.0, quantity = 1)
        )
        tableOrderDao.insertTableOrderItems(initialOrderItems)
    }

    private suspend fun seedSecondBarTables() {
        val now = System.currentTimeMillis()
        val wLuka = workers.firstOrNull { it.locationId == DefaultLocations.BAR_RIVER_ID } ?: workers.first()

        val tables = listOf(
            BarTableEntity(id = "rt1", name = "Terasa 1", zone = "Terasa", capacity = 4, sortOrder = 1, locationId = DefaultLocations.BAR_RIVER_ID),
            BarTableEntity(
                id = "rt2", name = "Terasa 2", zone = "Terasa", capacity = 6,
                lockedByWorkerId = wLuka.id, lockedByWorkerName = wLuka.name,
                lockedAtMillis = now - TimeUnit.MINUTES.toMillis(11),
                sortOrder = 2, locationId = DefaultLocations.BAR_RIVER_ID
            ),
            BarTableEntity(id = "rt3", name = "Sala 1", zone = "Sala", capacity = 4, sortOrder = 3, locationId = DefaultLocations.BAR_RIVER_ID),
            BarTableEntity(id = "rt4", name = "Sala 2", zone = "Sala", capacity = 4, sortOrder = 4, locationId = DefaultLocations.BAR_RIVER_ID),
            BarTableEntity(id = "rt5", name = "Šank Reka", zone = "Šank", capacity = 2, sortOrder = 5, locationId = DefaultLocations.BAR_RIVER_ID)
        )
        tableOrderDao.insertTables(tables)

        val initialItems = listOf(
            TableOrderItemEntity(tableId = "rt2", productId = "br2_b6", productNameSr = "Grilovani losos", productNameEn = "Grilled Salmon Fillet", category = ProductCategory.MAIN_DISHES.name, unitPriceRsd = 1590.0, quantity = 1),
            TableOrderItemEntity(tableId = "rt2", productId = "br2_b4", productNameSr = "Aperol Spritz", productNameEn = "Aperol Spritz", category = ProductCategory.BAR_DRINKS.name, unitPriceRsd = 560.0, quantity = 2)
        )
        tableOrderDao.insertTableOrderItems(initialItems)
    }

    private suspend fun seedInitialMarketTransactions(products: List<Product>, locationId: String) {
        val now = System.currentTimeMillis()
        val pMap = products.associateBy { it.id }

        data class SeedSale(
            val daysAgo: Long,
            val hoursOffset: Long,
            val worker: WorkerProfile,
            val method: PaymentMethod,
            val lines: List<Pair<String, Int>>
        )

        val marketWorkers = workers.filter { it.locationId == locationId }
        val w0 = marketWorkers.getOrElse(0) { workers[0] }
        val w1 = marketWorkers.getOrElse(1) { w0 }

        val seedSales = listOf(
            SeedSale(6, 2, w0, PaymentMethod.CASH, listOf("p1" to 2, "p4" to 1, "p8" to 2)),
            SeedSale(6, 5, w1, PaymentMethod.CARD, listOf("p10" to 1, "p14" to 2, "p5" to 1)),
            SeedSale(5, 3, w0, PaymentMethod.CASH, listOf("p6" to 1, "p7" to 1, "p1" to 1, "p11" to 2)),
            SeedSale(5, 7, w0, PaymentMethod.CARD, listOf("p9" to 2, "p12" to 2, "p15" to 3)),
            SeedSale(4, 1, w1, PaymentMethod.CASH, listOf("p2" to 4, "p4" to 2, "p10" to 1)),
            SeedSale(4, 6, w0, PaymentMethod.CARD, listOf("p16" to 2, "p17" to 1, "p18" to 1)),
            SeedSale(3, 4, w0, PaymentMethod.CASH, listOf("p1" to 3, "p3" to 4, "p8" to 3, "p13" to 2)),
            SeedSale(2, 2, w0, PaymentMethod.CARD, listOf("p9" to 2, "p10" to 2, "p14" to 2)),
            SeedSale(2, 5, w1, PaymentMethod.CASH, listOf("p4" to 3, "p5" to 2, "p7" to 1, "p12" to 2)),
            SeedSale(1, 3, w1, PaymentMethod.CARD, listOf("p6" to 2, "p11" to 2, "p16" to 1, "p18" to 2)),
            SeedSale(1, 6, w0, PaymentMethod.CASH, listOf("p2" to 3, "p9" to 1, "p15" to 2)),
            SeedSale(0, 2, w0, PaymentMethod.CASH, listOf("p1" to 2, "p4" to 1, "p10" to 1, "p13" to 1)),
            SeedSale(0, 1, w0, PaymentMethod.CARD, listOf("p7" to 2, "p9" to 1, "p14" to 3, "p17" to 1))
        )

        seedSales.forEachIndexed { idx, seed ->
            insertSeedSale(
                txId = "seed_tx_${idx + 1}",
                receiptNum = "MP-${1001 + idx}",
                timestampMillis = now - TimeUnit.DAYS.toMillis(seed.daysAgo) - TimeUnit.HOURS.toMillis(seed.hoursOffset),
                worker = seed.worker,
                method = seed.method,
                venueMode = VenueMode.MARKET,
                locationId = locationId,
                tableId = null,
                tableName = null,
                lines = seed.lines,
                pMap = pMap
            )
        }
    }

    private suspend fun seedSecondMarketTransactions(products: List<Product>, locationId: String) {
        val now = System.currentTimeMillis()
        val pMap = products.associateBy { it.id }
        val wStefan = workers.firstOrNull { it.locationId == locationId } ?: workers.first()

        val seeds = listOf(
            Triple(3L, PaymentMethod.CARD, listOf("nbg_p1" to 2, "nbg_p4" to 1, "nbg_p6" to 1)),
            Triple(2L, PaymentMethod.CASH, listOf("nbg_p2" to 3, "nbg_p3" to 2, "nbg_p5" to 2)),
            Triple(1L, PaymentMethod.CARD, listOf("nbg_p6" to 1, "nbg_p8" to 2, "nbg_p9" to 3)),
            Triple(0L, PaymentMethod.CASH, listOf("nbg_p1" to 2, "nbg_p2" to 2, "nbg_p10" to 1))
        )

        seeds.forEachIndexed { idx, (daysAgo, method, lines) ->
            insertSeedSale(
                txId = "seed_nbg_tx_${idx + 1}",
                receiptNum = "MP-${1501 + idx}",
                timestampMillis = now - TimeUnit.DAYS.toMillis(daysAgo) - TimeUnit.HOURS.toMillis(2),
                worker = wStefan,
                method = method,
                venueMode = VenueMode.MARKET,
                locationId = locationId,
                tableId = null,
                tableName = null,
                lines = lines,
                pMap = pMap
            )
        }
    }

    private suspend fun seedInitialBarTransactions(products: List<Product>, locationId: String) {
        val now = System.currentTimeMillis()
        val pMap = products.associateBy { it.id }

        data class BarSeedSale(
            val daysAgo: Long,
            val hoursOffset: Long,
            val worker: WorkerProfile,
            val tableId: String,
            val tableName: String,
            val method: PaymentMethod,
            val lines: List<Pair<String, Int>>
        )

        val restaurantWorkers = workers.filter { it.locationId == locationId }
        val w0 = restaurantWorkers.getOrElse(0) { workers.first() }
        val w1 = restaurantWorkers.getOrElse(1) { w0 }

        val seedSales = listOf(
            BarSeedSale(2, 3, w0, "t1", "Sto 1", PaymentMethod.CASH, listOf("b1" to 2, "b2" to 2, "b12" to 1)),
            BarSeedSale(1, 5, w1, "t3", "Sto 3", PaymentMethod.CARD, listOf("b4" to 2, "b10" to 2)),
            BarSeedSale(1, 2, w0, "t6", "Šank 1", PaymentMethod.CASH, listOf("b4" to 3, "b6" to 2)),
            BarSeedSale(0, 4, w1, "t5", "Sto 5", PaymentMethod.CARD, listOf("b5" to 2, "b9" to 1, "b11" to 1)),
            BarSeedSale(0, 2, w0, "t1", "Sto 1", PaymentMethod.CASH, listOf("b2" to 2, "b8" to 2, "b12" to 2))
        )

        seedSales.forEachIndexed { idx, seed ->
            insertSeedSale(
                txId = "seed_bar_tx_${idx + 1}",
                receiptNum = "BR-${2001 + idx}",
                timestampMillis = now - TimeUnit.DAYS.toMillis(seed.daysAgo) - TimeUnit.HOURS.toMillis(seed.hoursOffset),
                worker = seed.worker,
                method = seed.method,
                venueMode = VenueMode.BAR_RESTAURANT,
                locationId = locationId,
                tableId = seed.tableId,
                tableName = seed.tableName,
                lines = seed.lines,
                pMap = pMap
            )
        }
    }

    private suspend fun seedSecondBarTransactions(products: List<Product>, locationId: String) {
        val now = System.currentTimeMillis()
        val pMap = products.associateBy { it.id }
        val wLuka = workers.firstOrNull { it.locationId == locationId } ?: workers.first()

        data class RiverSeedSale(
            val daysAgo: Long,
            val tableId: String,
            val tableName: String,
            val method: PaymentMethod,
            val lines: List<Pair<String, Int>>
        )

        val seeds = listOf(
            RiverSeedSale(2, "rt1", "Terasa 1", PaymentMethod.CARD, listOf("br2_b1" to 2, "br2_b6" to 2, "br2_b5" to 2)),
            RiverSeedSale(1, "rt3", "Sala 1", PaymentMethod.CASH, listOf("br2_b3" to 3, "br2_b7" to 2, "br2_b8" to 2)),
            RiverSeedSale(0, "rt4", "Sala 2", PaymentMethod.CARD, listOf("br2_b2" to 2, "br2_b4" to 2, "br2_b9" to 2))
        )

        seeds.forEachIndexed { idx, seed ->
            insertSeedSale(
                txId = "seed_river_tx_${idx + 1}",
                receiptNum = "BR-${2501 + idx}",
                timestampMillis = now - TimeUnit.DAYS.toMillis(seed.daysAgo) - TimeUnit.HOURS.toMillis(3),
                worker = wLuka,
                method = seed.method,
                venueMode = VenueMode.BAR_RESTAURANT,
                locationId = locationId,
                tableId = seed.tableId,
                tableName = seed.tableName,
                lines = seed.lines,
                pMap = pMap
            )
        }
    }

    private suspend fun insertSeedSale(
        txId: String,
        receiptNum: String,
        timestampMillis: Long,
        worker: WorkerProfile,
        method: PaymentMethod,
        venueMode: VenueMode,
        locationId: String,
        tableId: String?,
        tableName: String?,
        lines: List<Pair<String, Int>>,
        pMap: Map<String, Product>
    ) {
        val items = lines.mapNotNull { (pid, qty) ->
            pMap[pid]?.let { prod ->
                SaleLineItemEntity(
                    transactionId = txId,
                    productId = prod.id,
                    productNameSr = prod.nameSr,
                    productNameEn = prod.nameEn,
                    category = prod.category.name,
                    unitPriceRsd = prod.priceRsd,
                    quantity = qty
                )
            }
        }
        val subtotal = items.sumOf { it.unitPriceRsd * it.quantity }
        val tax = subtotal * CurrencyFormatter.VAT_RATE
        val total = subtotal + tax
        val cashReceived = if (method == PaymentMethod.CASH) kotlin.math.ceil(total / 500.0) * 500.0 else null
        val change = cashReceived?.let { (it - total).coerceAtLeast(0.0) }
        salesDao.insertFullSale(
            SaleTransactionEntity(
                id = txId,
                receiptNumber = receiptNum,
                workerId = worker.id,
                workerName = worker.name,
                timestampMillis = timestampMillis,
                subtotalRsd = subtotal,
                taxRsd = tax,
                totalRsd = total,
                paymentMethod = method.name,
                cashReceivedRsd = cashReceived,
                changeRsd = change,
                venueMode = venueMode.name,
                tableId = tableId,
                tableName = tableName,
                locationId = locationId
            ),
            items = items,
            deductStock = false
        )
    }

    private suspend fun seedInitialOrders() {
        val now = System.currentTimeMillis()
        var seq = 101

        val currentTables = tableOrderDao.observeTablesWithItems().first().map { it.toDomain() }
        currentTables.filter { it.orderItems.isNotEmpty() }.forEach { table ->
            val ordId = "seed_ord_tbl_${table.id}"
            val created = table.lockedAtMillis ?: (now - TimeUnit.MINUTES.toMillis(10))
            val orderEntity = OrderEntity(
                id = ordId,
                orderNumber = "BR-${seq++}",
                venueMode = VenueMode.BAR_RESTAURANT.name,
                tableId = table.id,
                tableName = table.name,
                workerId = table.lockedByWorkerId ?: workers[0].id,
                workerName = table.lockedByWorkerName ?: workers[0].name,
                status = OrderStatus.PENDING.name,
                createdAtMillis = created,
                updatedAtMillis = created,
                locationId = table.locationId
            )
            val orderItems = table.orderItems.map { item ->
                OrderItemEntity(
                    orderId = ordId,
                    productId = item.productId,
                    productNameSr = item.productNameSr,
                    productNameEn = item.productNameEn,
                    category = item.category.name,
                    unitPriceRsd = item.unitPriceRsd,
                    quantity = item.quantity,
                    status = OrderItemStatus.ACTIVE.name
                )
            }.toMutableList()

            if (table.id == "t4") {
                orderItems.add(
                    OrderItemEntity(
                        orderId = ordId,
                        productId = "b3",
                        productNameSr = "Biljni čaj sa medom",
                        productNameEn = "Herbal Tea & Honey",
                        category = ProductCategory.COFFEE_TEA.name,
                        unitPriceRsd = 210.0,
                        quantity = 1,
                        status = OrderItemStatus.CANCELLED.name
                    )
                )
            }
            tableOrderDao.createOrderWithItems(orderEntity, orderItems)
        }

        val existingTx = salesDao.observeTransactionsWithItems().first().map { it.toDomain() }
        existingTx.take(12).forEach { tx ->
            val ordId = "seed_ord_${tx.id}"
            val prefix = if (tx.venueMode == VenueMode.BAR_RESTAURANT) "BR" else "MP"
            val orderEntity = OrderEntity(
                id = ordId,
                orderNumber = "$prefix-${seq++}",
                venueMode = tx.venueMode.name,
                tableId = tx.tableId,
                tableName = tx.tableName,
                workerId = tx.workerId,
                workerName = tx.workerName,
                status = OrderStatus.FINISHED.name,
                createdAtMillis = tx.timestampMillis - TimeUnit.MINUTES.toMillis(12),
                updatedAtMillis = tx.timestampMillis,
                completedAtMillis = tx.timestampMillis,
                transactionId = tx.id,
                locationId = tx.locationId
            )
            val items = tx.items.map { line ->
                OrderItemEntity(
                    orderId = ordId,
                    productId = line.productId,
                    productNameSr = line.productNameSr,
                    productNameEn = line.productNameEn,
                    category = line.category.name,
                    unitPriceRsd = line.unitPriceRsd,
                    quantity = line.quantity,
                    status = OrderItemStatus.ACTIVE.name
                )
            }
            tableOrderDao.createOrderWithItems(orderEntity, items)
        }

        val korzoWorker = workers.firstOrNull { it.locationId == DefaultLocations.BAR_CENTER_ID } ?: workers.first()
        val riverWorker = workers.firstOrNull { it.locationId == DefaultLocations.BAR_RIVER_ID } ?: korzoWorker
        val marketCentralWorker = workers.firstOrNull { it.locationId == DefaultLocations.MARKET_CENTRAL_ID } ?: workers.first()

        val canceledBarId = "seed_ord_bar_cancelled_1"
        val canceledBarTime = now - TimeUnit.HOURS.toMillis(3)
        tableOrderDao.createOrderWithItems(
            OrderEntity(
                id = canceledBarId, orderNumber = "BR-${seq++}", venueMode = VenueMode.BAR_RESTAURANT.name,
                tableId = "t3", tableName = "Sto 3", workerId = korzoWorker.id, workerName = korzoWorker.name,
                status = OrderStatus.CANCELLED.name, createdAtMillis = canceledBarTime - TimeUnit.MINUTES.toMillis(5),
                updatedAtMillis = canceledBarTime, locationId = DefaultLocations.BAR_CENTER_ID
            ),
            listOf(
                OrderItemEntity(orderId = canceledBarId, productId = "b2", productNameSr = "Kapućino", productNameEn = "Cappuccino", category = ProductCategory.COFFEE_TEA.name, unitPriceRsd = 240.0, quantity = 2, status = OrderItemStatus.CANCELLED.name),
                OrderItemEntity(orderId = canceledBarId, productId = "b12", productNameSr = "Čizkejk šumsko voće", productNameEn = "Berry Cheesecake", category = ProductCategory.DESSERTS.name, unitPriceRsd = 430.0, quantity = 1, status = OrderItemStatus.CANCELLED.name)
            )
        )

        val canceledRiverId = "seed_ord_river_cancelled_1"
        val canceledRiverTime = now - TimeUnit.HOURS.toMillis(4)
        tableOrderDao.createOrderWithItems(
            OrderEntity(
                id = canceledRiverId, orderNumber = "BR-${seq++}", venueMode = VenueMode.BAR_RESTAURANT.name,
                tableId = "rt3", tableName = "Sala 1", workerId = riverWorker.id, workerName = riverWorker.name,
                status = OrderStatus.CANCELLED.name, createdAtMillis = canceledRiverTime - TimeUnit.MINUTES.toMillis(5),
                updatedAtMillis = canceledRiverTime, locationId = DefaultLocations.BAR_RIVER_ID
            ),
            listOf(
                OrderItemEntity(orderId = canceledRiverId, productId = "br2_b4", productNameSr = "Aperol Spritz", productNameEn = "Aperol Spritz", category = ProductCategory.BAR_DRINKS.name, unitPriceRsd = 560.0, quantity = 1, status = OrderItemStatus.CANCELLED.name)
            )
        )

        val canceledMarketId = "seed_ord_mkt_cancelled_1"
        val canceledMarketTime = now - TimeUnit.HOURS.toMillis(2)
        tableOrderDao.createOrderWithItems(
            OrderEntity(
                id = canceledMarketId, orderNumber = "MP-${seq++}", venueMode = VenueMode.MARKET.name,
                tableId = null, tableName = null, workerId = marketCentralWorker.id, workerName = marketCentralWorker.name,
                status = OrderStatus.CANCELLED.name, createdAtMillis = canceledMarketTime - TimeUnit.MINUTES.toMillis(4),
                updatedAtMillis = canceledMarketTime, locationId = DefaultLocations.MARKET_CENTRAL_ID
            ),
            listOf(
                OrderItemEntity(orderId = canceledMarketId, productId = "p10", productNameSr = "Domaća kafa 200g", productNameEn = "Roasted Ground Coffee 200g", category = ProductCategory.BEVERAGES.name, unitPriceRsd = 380.0, quantity = 1, status = OrderItemStatus.CANCELLED.name),
                OrderItemEntity(orderId = canceledMarketId, productId = "p14", productNameSr = "Tamna čokolada 75% 100g", productNameEn = "Dark Chocolate 75% 100g", category = ProductCategory.SNACKS.name, unitPriceRsd = 210.0, quantity = 2, status = OrderItemStatus.CANCELLED.name)
            )
        )
    }
}
