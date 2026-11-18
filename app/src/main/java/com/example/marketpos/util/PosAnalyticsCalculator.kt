package com.example.marketpos.util

import com.example.marketpos.model.AppLanguage
import com.example.marketpos.model.BarTable
import com.example.marketpos.model.LocationStats
import com.example.marketpos.model.OrderStatus
import com.example.marketpos.model.PaymentMethod
import com.example.marketpos.model.PosOrder
import com.example.marketpos.model.Product
import com.example.marketpos.model.ProductCategory
import com.example.marketpos.model.SaleTransaction
import com.example.marketpos.model.UserRole
import com.example.marketpos.model.VenueLocation
import com.example.marketpos.model.VenueMode
import com.example.marketpos.model.WorkerProfile
import java.text.Collator
import java.util.Locale

data class DailyRevenuePoint(
    val dayTimestampMillis: Long,
    val revenueRsd: Double,
    val transactionCount: Int
)

data class ProductSalesStat(
    val productId: String,
    val nameSr: String,
    val nameEn: String,
    val unitsSold: Int,
    val revenueRsd: Double
) {
    fun displayName(language: AppLanguage): String = when (language) {
        AppLanguage.SERBIAN_LATIN -> nameSr
        AppLanguage.ENGLISH -> nameEn
    }
}

data class WorkerSalesStat(
    val workerId: String,
    val workerName: String,
    val transactionCount: Int,
    val revenueRsd: Double
)

object PosAnalyticsCalculator {

    fun alphabeticalProductComparator(language: AppLanguage): Comparator<Product> {
        val locale = when (language) {
            AppLanguage.SERBIAN_LATIN -> Locale.forLanguageTag("sr-Latn")
            AppLanguage.ENGLISH -> Locale.ENGLISH
        }
        val collator = Collator.getInstance(locale).apply {
            strength = Collator.SECONDARY
        }
        return Comparator { a, b ->
            val nameA = a.displayName(language)
            val nameB = b.displayName(language)
            val cmp = collator.compare(nameA, nameB)
            if (cmp != 0) cmp else nameA.compareTo(nameB, ignoreCase = true)
        }
    }

    fun filterCatalogProducts(
        products: List<Product>,
        searchQuery: String,
        selectedCategory: ProductCategory?,
        showOutOfStockOnly: Boolean,
        language: AppLanguage
    ): List<Product> {
        val q = searchQuery.trim().lowercase()
        return products
            .filter { product ->
                val matchesStockFilter = !showOutOfStockOnly || !product.isInStock
                val matchesCategory = showOutOfStockOnly || selectedCategory == null || product.category == selectedCategory
                val matchesQuery = q.isEmpty() ||
                    product.nameSr.lowercase().contains(q) ||
                    product.nameEn.lowercase().contains(q) ||
                    product.sku.lowercase().contains(q)
                matchesStockFilter && matchesCategory && matchesQuery
            }
            .sortedWith(alphabeticalProductComparator(language))
    }

    fun outOfStockProducts(products: List<Product>, language: AppLanguage): List<Product> =
        products
            .filter { !it.isInStock }
            .sortedWith(alphabeticalProductComparator(language))

    fun lowOrOutOfStockProducts(products: List<Product>): List<Product> =
        products
            .filter { !it.isInStock || it.isLowStock }
            .sortedBy { it.stockQuantity }

    fun filterWorkerOrders(orders: List<PosOrder>, currentUser: WorkerProfile?): List<PosOrder> {
        return if (currentUser == null || currentUser.role == UserRole.ADMIN) {
            orders
        } else {
            orders.filter { it.workerId == currentUser.id }
        }
    }

    fun filterWorkerTransactions(
        transactions: List<SaleTransaction>,
        currentUser: WorkerProfile?
    ): List<SaleTransaction> {
        return if (currentUser == null || currentUser.role == UserRole.ADMIN) {
            transactions
        } else {
            transactions.filter { it.workerId == currentUser.id }
        }
    }

    fun venueWorkers(
        workers: List<WorkerProfile>,
        venueMode: VenueMode,
        activeLocationId: String
    ): List<WorkerProfile> {
        val roleWorkers = workers.filter { it.role.assignedVenueMode() == venueMode }
        val assigned = roleWorkers.filter { it.locationId == activeLocationId }
        return assigned.ifEmpty { roleWorkers }
    }

    fun computeDailyRevenue(
        transactions: List<SaleTransaction>,
        limitDays: Int = 7
    ): List<DailyRevenuePoint> {
        if (transactions.isEmpty()) return emptyList()
        val buckets = mutableMapOf<Long, Pair<Double, Int>>()
        transactions.forEach { tx ->
            val dayKey = CurrencyFormatter.localDayStartMillis(tx.timestampMillis)
            val prev = buckets[dayKey] ?: (0.0 to 0)
            buckets[dayKey] = (prev.first + tx.totalRsd) to (prev.second + 1)
        }
        return buckets.entries
            .sortedBy { it.key }
            .takeLast(limitDays)
            .map { (dayTs, pair) ->
                DailyRevenuePoint(
                    dayTimestampMillis = dayTs,
                    revenueRsd = pair.first,
                    transactionCount = pair.second
                )
            }
    }

    fun computeTopProducts(
        transactions: List<SaleTransaction>,
        limit: Int = 5
    ): List<ProductSalesStat> {
        val map = mutableMapOf<String, ProductSalesStat>()
        transactions.forEach { tx ->
            tx.items.forEach { item ->
                val prev = map[item.productId]
                val lineRev = item.lineTotalRsd * (1.0 + CurrencyFormatter.VAT_RATE)
                map[item.productId] = if (prev == null) {
                    ProductSalesStat(
                        productId = item.productId,
                        nameSr = item.productNameSr,
                        nameEn = item.productNameEn,
                        unitsSold = item.quantity,
                        revenueRsd = lineRev
                    )
                } else {
                    prev.copy(
                        unitsSold = prev.unitsSold + item.quantity,
                        revenueRsd = prev.revenueRsd + lineRev
                    )
                }
            }
        }
        return map.values.sortedByDescending { it.unitsSold }.take(limit)
    }

    fun computeSalesByWorker(transactions: List<SaleTransaction>): List<WorkerSalesStat> {
        val map = mutableMapOf<String, WorkerSalesStat>()
        transactions.forEach { tx ->
            val prev = map[tx.workerId]
            map[tx.workerId] = if (prev == null) {
                WorkerSalesStat(
                    workerId = tx.workerId,
                    workerName = tx.workerName,
                    transactionCount = 1,
                    revenueRsd = tx.totalRsd
                )
            } else {
                prev.copy(
                    transactionCount = prev.transactionCount + 1,
                    revenueRsd = prev.revenueRsd + tx.totalRsd
                )
            }
        }
        return map.values.sortedByDescending { it.revenueRsd }
    }

    fun computeLocationStats(
        locations: List<VenueLocation>,
        products: List<Product>,
        transactions: List<SaleTransaction>,
        orders: List<PosOrder>,
        tables: List<BarTable>
    ): List<LocationStats> = locations.map { loc ->
        val locTx = transactions.filter { it.locationId == loc.id }
        val locOrders = orders.filter { it.locationId == loc.id }
        val locProducts = products.filter { it.locationId == loc.id }
        val locTables = tables.filter { it.locationId == loc.id }
        LocationStats(
            location = loc,
            revenueRsd = locTx.sumOf { it.totalRsd },
            cashRevenueRsd = locTx.filter { it.paymentMethod == PaymentMethod.CASH }.sumOf { it.totalRsd },
            cardRevenueRsd = locTx.filter { it.paymentMethod == PaymentMethod.CARD }.sumOf { it.totalRsd },
            transactionCount = locTx.size,
            pendingOrdersCount = locOrders.count { it.status == OrderStatus.PENDING },
            cancelledOrdersCount = locOrders.count { it.status == OrderStatus.CANCELLED },
            productsCount = locProducts.size,
            totalStockUnits = locProducts.sumOf { it.stockQuantity },
            lowOrOutStockCount = locProducts.count { !it.isInStock || it.isLowStock },
            stockValueRsd = locProducts.sumOf { it.priceRsd * it.stockQuantity },
            freeTablesCount = locTables.count { !it.isLocked },
            totalTablesCount = locTables.size
        )
    }
}
