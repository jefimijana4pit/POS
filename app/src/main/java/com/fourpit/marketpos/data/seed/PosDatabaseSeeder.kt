package com.fourpit.marketpos.data.seed

import com.fourpit.marketpos.data.local.PosDatabase
import com.fourpit.marketpos.data.local.PosOrderEntity
import com.fourpit.marketpos.data.local.PosOrderItemEntity
import com.fourpit.marketpos.data.local.WorkerLogEntity
import com.fourpit.marketpos.model.OrderStatus
import com.fourpit.marketpos.model.PaymentMethod
import com.fourpit.marketpos.model.UserRole
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PosDatabaseSeeder(private val database: PosDatabase) {
    private val seedMutex = Mutex()

    suspend fun seedIfEmpty() = seedMutex.withLock {
        val catalogDao = database.catalogLocationDao()
        if (catalogDao.getAllLocationsOnce().isNotEmpty()) return@withLock

        val tableDao = database.tableOrderDao()
        val salesDao = database.salesDao()

        val defaultLocs = CatalogSeedData.defaultLocations()
        val marketId = catalogDao.insertLocation(defaultLocs[0])
        val barId = catalogDao.insertLocation(defaultLocs[1])

        val marketSeedProducts = CatalogSeedData.marketProducts(marketId)
        val barSeedProducts = CatalogSeedData.barProducts(barId)
        val marketProductIds = marketSeedProducts.map { catalogDao.insertProduct(it) }
        val barProductIds = barSeedProducts.map { catalogDao.insertProduct(it) }
        tableDao.insertTables(CatalogSeedData.barTables(barId))

        // Seed realistic recent sales over the past 5 days so analytics dashboards have immediate signal
        val now = System.currentTimeMillis()
        val dayMs = 24L * 60L * 60L * 1000L

        suspend fun insertPaidOrder(
            receipt: String,
            locationId: Long,
            worker: String,
            method: PaymentMethod,
            daysAgo: Int,
            tableLabel: String?,
            lines: List<Triple<Long, String, Pair<String, Pair<Double, Int>>>>
        ) {
            val total = lines.sumOf { it.third.second.first * it.third.second.second }
            val tax = total - (total / 1.20)
            val ts = now - (daysAgo * dayMs) - (3600_000L * (daysAgo + 1))
            val orderId = tableDao.insertOrder(
                PosOrderEntity(
                    receiptNumber = receipt,
                    locationId = locationId,
                    tableLabel = tableLabel,
                    workerName = worker,
                    status = OrderStatus.PAID.name,
                    paymentMethod = method.name,
                    createdAtMillis = ts,
                    closedAtMillis = ts + 180_000L,
                    totalRsd = total,
                    taxAmountRsd = tax,
                    receivedRsd = total,
                    changeRsd = 0.0
                )
            )
            tableDao.insertOrderItems(
                lines.map { (prodId, name, meta) ->
                    PosOrderItemEntity(
                        orderId = orderId,
                        productId = prodId,
                        productName = name,
                        category = meta.first,
                        unitPriceRsd = meta.second.first,
                        quantity = meta.second.second
                    )
                }
            )
        }

        if (marketProductIds.size >= 8) {
            insertPaidOrder(
                receipt = "MKT-1001",
                locationId = marketId,
                worker = "Ana Petrović",
                method = PaymentMethod.CASH,
                daysAgo = 3,
                tableLabel = null,
                lines = listOf(
                    Triple(marketProductIds[0], marketSeedProducts[0].name, marketSeedProducts[0].category to (marketSeedProducts[0].priceRsd to 2)),
                    Triple(marketProductIds[2], marketSeedProducts[2].name, marketSeedProducts[2].category to (marketSeedProducts[2].priceRsd to 2)),
                    Triple(marketProductIds[7], marketSeedProducts[7].name, marketSeedProducts[7].category to (marketSeedProducts[7].priceRsd to 1))
                )
            )
            insertPaidOrder(
                receipt = "MKT-1002",
                locationId = marketId,
                worker = "Ana Petrović",
                method = PaymentMethod.CARD,
                daysAgo = 1,
                tableLabel = null,
                lines = listOf(
                    Triple(marketProductIds[4], marketSeedProducts[4].name, marketSeedProducts[4].category to (marketSeedProducts[4].priceRsd to 2)),
                    Triple(marketProductIds[6], marketSeedProducts[6].name, marketSeedProducts[6].category to (marketSeedProducts[6].priceRsd to 3)),
                    Triple(marketProductIds[8], marketSeedProducts[8].name, marketSeedProducts[8].category to (marketSeedProducts[8].priceRsd to 2))
                )
            )
            insertPaidOrder(
                receipt = "MKT-1003",
                locationId = marketId,
                worker = "Marko Ilić",
                method = PaymentMethod.CASH,
                daysAgo = 0,
                tableLabel = null,
                lines = listOf(
                    Triple(marketProductIds[0], marketSeedProducts[0].name, marketSeedProducts[0].category to (marketSeedProducts[0].priceRsd to 3)),
                    Triple(marketProductIds[3], marketSeedProducts[3].name, marketSeedProducts[3].category to (marketSeedProducts[3].priceRsd to 2)),
                    Triple(marketProductIds[5], marketSeedProducts[5].name, marketSeedProducts[5].category to (marketSeedProducts[5].priceRsd to 4))
                )
            )
        }

        if (barProductIds.size >= 10) {
            insertPaidOrder(
                receipt = "BAR-2001",
                locationId = barId,
                worker = "Nikola Jović",
                method = PaymentMethod.CARD,
                daysAgo = 2,
                tableLabel = "Sto 2",
                lines = listOf(
                    Triple(barProductIds[0], barSeedProducts[0].name, barSeedProducts[0].category to (barSeedProducts[0].priceRsd to 2)),
                    Triple(barProductIds[3], barSeedProducts[3].name, barSeedProducts[3].category to (barSeedProducts[3].priceRsd to 2)),
                    Triple(barProductIds[10], barSeedProducts[10].name, barSeedProducts[10].category to (barSeedProducts[10].priceRsd to 1))
                )
            )
            insertPaidOrder(
                receipt = "BAR-2002",
                locationId = barId,
                worker = "Jelena Kovač",
                method = PaymentMethod.CASH,
                daysAgo = 0,
                tableLabel = "Bašta 1",
                lines = listOf(
                    Triple(barProductIds[1], barSeedProducts[1].name, barSeedProducts[1].category to (barSeedProducts[1].priceRsd to 3)),
                    Triple(barProductIds[6], barSeedProducts[6].name, barSeedProducts[6].category to (barSeedProducts[6].priceRsd to 2)),
                    Triple(barProductIds[9], barSeedProducts[9].name, barSeedProducts[9].category to (barSeedProducts[9].priceRsd to 2))
                )
            )
        }

        salesDao.insertWorkerLog(
            WorkerLogEntity(
                locationId = marketId,
                workerName = "Ana Petrović",
                role = UserRole.MARKET_WORKER.name,
                action = "SHIFT_OPEN",
                details = "Initial cash drawer verified",
                timestampMillis = now - 7_200_000L
            )
        )
        salesDao.insertWorkerLog(
            WorkerLogEntity(
                locationId = barId,
                workerName = "Jelena Kovač",
                role = UserRole.BAR_WORKER.name,
                action = "SHIFT_OPEN",
                details = "Bar floor & tables ready",
                timestampMillis = now - 5_400_000L
            )
        )
    }
}
