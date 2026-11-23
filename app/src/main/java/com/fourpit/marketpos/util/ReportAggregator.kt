package com.fourpit.marketpos.util

import com.fourpit.marketpos.model.OrderStatus
import com.fourpit.marketpos.model.PaymentMethod
import com.fourpit.marketpos.model.PosOrder
import com.fourpit.marketpos.model.ReportPeriod
import java.util.Calendar

data class AggregatedReport(
    val title: String,
    val scopeLabel: String,
    val period: ReportPeriod,
    val generatedAtMillis: Long,
    val fromMillis: Long?,
    val toMillis: Long,
    val totalRevenueRsd: Double,
    val totalTaxRsd: Double,
    val cashRevenueRsd: Double,
    val cardRevenueRsd: Double,
    val paidOrderCount: Int,
    val cancelledOrderCount: Int,
    val totalItemsSold: Int,
    val averageTicketRsd: Double,
    val categoryBreakdown: List<CategoryBreakdownItem>,
    val topProducts: List<TopProductStat>,
    val branchBreakdown: List<BranchRevenueStat>,
    val orders: List<PosOrder>
)

object ReportAggregator {

    fun periodStartMillis(period: ReportPeriod, nowMillis: Long = System.currentTimeMillis()): Long? {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        return when (period) {
            ReportPeriod.TODAY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            ReportPeriod.LAST_7_DAYS -> {
                cal.add(Calendar.DAY_OF_YEAR, -6)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            ReportPeriod.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            ReportPeriod.ALL_TIME -> null
        }
    }

    fun aggregate(
        orders: List<PosOrder>,
        period: ReportPeriod,
        title: String,
        scopeLabel: String,
        locationNames: Map<Long, String> = emptyMap(),
        nowMillis: Long = System.currentTimeMillis()
    ): AggregatedReport {
        val startMillis = periodStartMillis(period, nowMillis)
        val filtered = orders.filter { order ->
            val ts = order.closedAtMillis ?: order.createdAtMillis
            (startMillis == null || ts >= startMillis) && ts <= nowMillis
        }

        val paid = filtered.filter { it.status == OrderStatus.PAID }
        val cancelled = filtered.filter { it.status == OrderStatus.CANCELLED }

        val totalRev = paid.sumOf { it.totalRsd }
        val totalTax = paid.sumOf { it.taxAmountRsd }
        val cashRev = paid.filter { it.paymentMethod == PaymentMethod.CASH }.sumOf { it.totalRsd }
        val cardRev = paid.filter { it.paymentMethod == PaymentMethod.CARD }.sumOf { it.totalRsd }
        val itemsSold = paid.sumOf { it.itemCount }
        val avgTicket = if (paid.isEmpty()) 0.0 else totalRev / paid.size

        return AggregatedReport(
            title = title,
            scopeLabel = scopeLabel,
            period = period,
            generatedAtMillis = nowMillis,
            fromMillis = startMillis,
            toMillis = nowMillis,
            totalRevenueRsd = totalRev,
            totalTaxRsd = totalTax,
            cashRevenueRsd = cashRev,
            cardRevenueRsd = cardRev,
            paidOrderCount = paid.size,
            cancelledOrderCount = cancelled.size,
            totalItemsSold = itemsSold,
            averageTicketRsd = avgTicket,
            categoryBreakdown = PosAnalyticsCalculator.categoryBreakdown(paid),
            topProducts = PosAnalyticsCalculator.topProducts(paid, limit = 10),
            branchBreakdown = PosAnalyticsCalculator.branchComparison(paid, locationNames),
            orders = paid.sortedByDescending { it.closedAtMillis ?: it.createdAtMillis }
        )
    }
}
