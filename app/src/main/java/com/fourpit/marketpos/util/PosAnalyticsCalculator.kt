package com.fourpit.marketpos.util

import com.fourpit.marketpos.model.OrderStatus
import com.fourpit.marketpos.model.PaymentMethod
import com.fourpit.marketpos.model.PosOrder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DailyRevenuePoint(
    val dayKey: String,
    val label: String,
    val revenueRsd: Double,
    val orderCount: Int
)

data class CategoryBreakdownItem(
    val category: String,
    val revenueRsd: Double,
    val quantitySold: Int,
    val share: Float
)

data class PaymentBreakdownItem(
    val method: PaymentMethod,
    val revenueRsd: Double,
    val orderCount: Int,
    val share: Float
)

data class TopProductStat(
    val productName: String,
    val category: String,
    val quantitySold: Int,
    val revenueRsd: Double
)

data class BranchRevenueStat(
    val locationId: Long,
    val locationName: String,
    val revenueRsd: Double,
    val orderCount: Int,
    val share: Float
)

object PosAnalyticsCalculator {

    fun paidOrders(orders: List<PosOrder>): List<PosOrder> =
        orders.filter { it.status == OrderStatus.PAID }

    fun totalRevenueRsd(orders: List<PosOrder>): Double =
        paidOrders(orders).sumOf { it.totalRsd }

    fun averageTicketRsd(orders: List<PosOrder>): Double {
        val paid = paidOrders(orders)
        return if (paid.isEmpty()) 0.0 else paid.sumOf { it.totalRsd } / paid.size
    }

    fun revenueLastDays(
        orders: List<PosOrder>,
        days: Int = 7,
        nowMillis: Long = System.currentTimeMillis()
    ): List<DailyRevenuePoint> {
        val paid = paidOrders(orders)
        val keyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val labelFmt = SimpleDateFormat("dd.MM", Locale.US)
        val grouped = paid.groupBy { keyFmt.format(Date(it.closedAtMillis ?: it.createdAtMillis)) }

        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            add(Calendar.DAY_OF_YEAR, -(days - 1))
        }

        return (0 until days).map {
            val date = cal.time
            val key = keyFmt.format(date)
            val dayOrders = grouped[key].orEmpty()
            val point = DailyRevenuePoint(
                dayKey = key,
                label = labelFmt.format(date),
                revenueRsd = dayOrders.sumOf { it.totalRsd },
                orderCount = dayOrders.size
            )
            cal.add(Calendar.DAY_OF_YEAR, 1)
            point
        }
    }

    fun categoryBreakdown(orders: List<PosOrder>): List<CategoryBreakdownItem> {
        val items = paidOrders(orders).flatMap { it.items }
        val total = items.sumOf { it.lineTotalRsd }
        if (total <= 0.0) return emptyList()

        return items.groupBy { it.category.ifBlank { "Other" } }
            .map { (cat, catItems) ->
                val rev = catItems.sumOf { it.lineTotalRsd }
                CategoryBreakdownItem(
                    category = cat,
                    revenueRsd = rev,
                    quantitySold = catItems.sumOf { it.quantity },
                    share = (rev / total).toFloat().coerceIn(0f, 1f)
                )
            }
            .sortedByDescending { it.revenueRsd }
    }

    fun paymentBreakdown(orders: List<PosOrder>): List<PaymentBreakdownItem> {
        val paid = paidOrders(orders)
        val total = paid.sumOf { it.totalRsd }
        if (total <= 0.0) return emptyList()

        return paid.groupBy { it.paymentMethod ?: PaymentMethod.CASH }
            .map { (method, list) ->
                val rev = list.sumOf { it.totalRsd }
                PaymentBreakdownItem(
                    method = method,
                    revenueRsd = rev,
                    orderCount = list.size,
                    share = (rev / total).toFloat().coerceIn(0f, 1f)
                )
            }
            .sortedByDescending { it.revenueRsd }
    }

    fun topProducts(orders: List<PosOrder>, limit: Int = 6): List<TopProductStat> {
        val items = paidOrders(orders).flatMap { it.items }
        return items.groupBy { it.productName to it.category }
            .map { (key, list) ->
                TopProductStat(
                    productName = key.first,
                    category = key.second,
                    quantitySold = list.sumOf { it.quantity },
                    revenueRsd = list.sumOf { it.lineTotalRsd }
                )
            }
            .sortedByDescending { it.revenueRsd }
            .take(limit)
    }

    fun branchComparison(
        orders: List<PosOrder>,
        locationNames: Map<Long, String>
    ): List<BranchRevenueStat> {
        val paid = paidOrders(orders)
        val total = paid.sumOf { it.totalRsd }
        if (total <= 0.0) return emptyList()

        return paid.groupBy { it.locationId }
            .map { (locId, list) ->
                val rev = list.sumOf { it.totalRsd }
                BranchRevenueStat(
                    locationId = locId,
                    locationName = locationNames[locId] ?: list.firstOrNull()?.locationName.orEmpty().ifBlank { "Branch #$locId" },
                    revenueRsd = rev,
                    orderCount = list.size,
                    share = (rev / total).toFloat().coerceIn(0f, 1f)
                )
            }
            .sortedByDescending { it.revenueRsd }
    }
}
