package com.example.marketpos.util

import com.example.marketpos.model.LocationReportSummary
import com.example.marketpos.model.OrderItemStatus
import com.example.marketpos.model.OrderStatus
import com.example.marketpos.model.PaymentMethod
import com.example.marketpos.model.PosOrder
import com.example.marketpos.model.ReportPeriodType
import com.example.marketpos.model.SaleTransaction
import com.example.marketpos.model.VenueLocation
import com.example.marketpos.model.WorkerProductSoldStat
import com.example.marketpos.model.WorkerProfile
import com.example.marketpos.model.WorkerReportRow
import java.util.Calendar

object ReportAggregator {

    fun computePeriodBounds(
        periodType: ReportPeriodType,
        anchorMillis: Long,
        rangeStartMillis: Long? = null,
        rangeEndMillis: Long? = null
    ): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        return when (periodType) {
            ReportPeriodType.DAILY -> {
                cal.timeInMillis = anchorMillis
                setDayStart(cal)
                val start = cal.timeInMillis
                setDayEnd(cal)
                start to cal.timeInMillis
            }
            ReportPeriodType.MONTHLY -> {
                cal.timeInMillis = anchorMillis
                cal.set(Calendar.DAY_OF_MONTH, 1)
                setDayStart(cal)
                val start = cal.timeInMillis
                val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                cal.set(Calendar.DAY_OF_MONTH, maxDay)
                setDayEnd(cal)
                start to cal.timeInMillis
            }
            ReportPeriodType.DATE_RANGE -> {
                val rawStart = rangeStartMillis ?: anchorMillis
                val rawEnd = rangeEndMillis ?: rawStart
                val minMillis = minOf(rawStart, rawEnd)
                val maxMillis = maxOf(rawStart, rawEnd)
                cal.timeInMillis = minMillis
                setDayStart(cal)
                val start = cal.timeInMillis
                cal.timeInMillis = maxMillis
                setDayEnd(cal)
                start to cal.timeInMillis
            }
        }
    }

    private fun setDayStart(cal: Calendar) {
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
    }

    private fun setDayEnd(cal: Calendar) {
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
    }

    fun buildLocationReportSummary(
        location: VenueLocation?,
        isGlobal: Boolean,
        periodType: ReportPeriodType,
        startMillis: Long,
        endMillis: Long,
        workers: List<WorkerProfile>,
        transactions: List<SaleTransaction>,
        orders: List<PosOrder>,
        generatedAtMillis: Long = System.currentTimeMillis()
    ): LocationReportSummary {
        val scopedTx = transactions.filter { tx ->
            (isGlobal || tx.locationId == location?.id) &&
                tx.timestampMillis in startMillis..endMillis
        }

        val scopedOrders = orders.filter { order ->
            val ts = order.completedAtMillis ?: order.updatedAtMillis
            (isGlobal || order.locationId == location?.id) &&
                ts in startMillis..endMillis
        }

        val knownWorkers = if (isGlobal || location == null) {
            workers
        } else {
            workers.filter { it.locationId == location.id }
        }

        val workerNamesById = linkedMapOf<String, String>()
        knownWorkers.forEach { workerNamesById[it.id] = it.name }
        scopedTx.forEach { tx -> workerNamesById.putIfAbsent(tx.workerId, tx.workerName) }
        scopedOrders.forEach { ord -> workerNamesById.putIfAbsent(ord.workerId, ord.workerName) }

        val workerRows = workerNamesById.entries.map { (workerId, workerName) ->
            val workerTx = scopedTx.filter { it.workerId == workerId }
            val workerOrders = scopedOrders.filter { it.workerId == workerId }

            val finishedBillsCount = workerTx.size
            val cancelledOrdersCount = workerOrders.count { it.status == OrderStatus.CANCELLED }
            val cancelledItemsCount = workerOrders.sumOf { ord ->
                ord.items.filter { it.status == OrderItemStatus.CANCELLED }.sumOf { it.quantity }
            }

            val distinctTables = (
                workerTx.mapNotNull { it.tableId } +
                    workerOrders.mapNotNull { it.tableId }
                ).distinct().size

            val productStatsMap = linkedMapOf<String, WorkerProductSoldStat>()
            workerTx.forEach { tx ->
                tx.items.forEach { item ->
                    val lineRev = item.lineTotalRsd * (1.0 + CurrencyFormatter.VAT_RATE)
                    val prev = productStatsMap[item.productId]
                    productStatsMap[item.productId] = if (prev == null) {
                        WorkerProductSoldStat(
                            productId = item.productId,
                            nameSr = item.productNameSr,
                            nameEn = item.productNameEn,
                            quantity = item.quantity,
                            revenueRsd = lineRev
                        )
                    } else {
                        prev.copy(
                            quantity = prev.quantity + item.quantity,
                            revenueRsd = prev.revenueRsd + lineRev
                        )
                    }
                }
            }

            val sortedProducts = productStatsMap.values.sortedWith(
                compareByDescending<WorkerProductSoldStat> { it.quantity }
                    .thenBy { it.nameSr.lowercase() }
            )

            WorkerReportRow(
                workerId = workerId,
                workerName = workerName,
                finishedBillsCount = finishedBillsCount,
                cancelledOrdersCount = cancelledOrdersCount,
                cancelledItemsCount = cancelledItemsCount,
                tablesHandledCount = distinctTables,
                totalUnitsSold = sortedProducts.sumOf { it.quantity },
                revenueRsd = workerTx.sumOf { it.totalRsd },
                productsSold = sortedProducts
            )
        }.sortedWith(
            compareByDescending<WorkerReportRow> { it.revenueRsd }
                .thenByDescending { it.finishedBillsCount }
                .thenBy { it.workerName.lowercase() }
        )

        val locationDistinctTables = (
            scopedTx.mapNotNull { it.tableId } +
                scopedOrders.mapNotNull { it.tableId }
            ).distinct().size

        return LocationReportSummary(
            location = location,
            isGlobal = isGlobal,
            periodType = periodType,
            startMillis = startMillis,
            endMillis = endMillis,
            generatedAtMillis = generatedAtMillis,
            totalRevenueRsd = scopedTx.sumOf { it.totalRsd },
            cashRevenueRsd = scopedTx.filter { it.paymentMethod == PaymentMethod.CASH }.sumOf { it.totalRsd },
            cardRevenueRsd = scopedTx.filter { it.paymentMethod == PaymentMethod.CARD }.sumOf { it.totalRsd },
            finishedBillsCount = scopedTx.size,
            cancelledOrdersCount = scopedOrders.count { it.status == OrderStatus.CANCELLED },
            tablesHandledCount = locationDistinctTables,
            totalUnitsSold = workerRows.sumOf { it.totalUnitsSold },
            workerRows = workerRows
        )
    }
}
