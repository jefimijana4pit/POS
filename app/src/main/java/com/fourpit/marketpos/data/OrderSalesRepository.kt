package com.fourpit.marketpos.data

import com.fourpit.marketpos.data.local.CatalogLocationDao
import com.fourpit.marketpos.data.local.PosOrderEntity
import com.fourpit.marketpos.data.local.PosOrderItemEntity
import com.fourpit.marketpos.data.local.SalesDao
import com.fourpit.marketpos.data.local.TableOrderDao
import com.fourpit.marketpos.data.local.WorkerLogEntity
import com.fourpit.marketpos.model.CartItem
import com.fourpit.marketpos.model.OrderStatus
import com.fourpit.marketpos.model.PaymentMethod
import com.fourpit.marketpos.model.PosOrder
import com.fourpit.marketpos.model.Product
import com.fourpit.marketpos.model.UserRole
import com.fourpit.marketpos.model.WorkerActivityLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrderSalesRepository(
    private val catalogDao: CatalogLocationDao,
    private val tableOrderDao: TableOrderDao,
    private val salesDao: SalesDao
) {

    fun observeOrdersForLocation(locationId: Long): Flow<List<PosOrder>> {
        return combine(
            salesDao.observeOrdersForLocation(locationId),
            catalogDao.observeLocationById(locationId)
        ) { orders, loc ->
            val locName = loc?.name.orEmpty()
            orders.map { it.toModel(locName) }
        }
    }

    fun observeOpenOrdersForLocation(locationId: Long): Flow<List<PosOrder>> {
        return combine(
            tableOrderDao.observeOpenOrdersForLocation(locationId),
            catalogDao.observeLocationById(locationId)
        ) { orders, loc ->
            val locName = loc?.name.orEmpty()
            orders.map { it.toModel(locName) }
        }
    }

    fun observePaidOrdersForLocation(locationId: Long): Flow<List<PosOrder>> {
        return combine(
            salesDao.observePaidOrdersForLocation(locationId),
            catalogDao.observeLocationById(locationId)
        ) { orders, loc ->
            val locName = loc?.name.orEmpty()
            orders.map { it.toModel(locName) }
        }
    }

    fun observeAllOrders(): Flow<List<PosOrder>> {
        return combine(
            salesDao.observeAllOrders(),
            catalogDao.observeAllLocations()
        ) { orders, locs ->
            val names = locs.associate { it.id to it.name }
            orders.map { it.toModel(names[it.order.locationId].orEmpty()) }
        }
    }

    fun observeOrderById(orderId: Long): Flow<PosOrder?> {
        return combine(
            tableOrderDao.observeOrderById(orderId),
            catalogDao.observeAllLocations()
        ) { entity, locs ->
            val names = locs.associate { it.id to it.name }
            entity?.toModel(names[entity.order.locationId].orEmpty())
        }
    }

    suspend fun getOrderById(orderId: Long): PosOrder? {
        val entity = tableOrderDao.getOrderById(orderId) ?: return null
        val locName = catalogDao.getLocationById(entity.order.locationId)?.name.orEmpty()
        return entity.toModel(locName)
    }

    fun observeWorkerLogsForLocation(locationId: Long): Flow<List<WorkerActivityLog>> =
        salesDao.observeWorkerLogsForLocation(locationId).map { list -> list.map { it.toModel() } }

    fun observeAllWorkerLogs(): Flow<List<WorkerActivityLog>> =
        salesDao.observeAllWorkerLogs().map { list -> list.map { it.toModel() } }

    suspend fun logWorkerAction(
        locationId: Long,
        workerName: String,
        role: UserRole,
        action: String,
        details: String
    ) {
        salesDao.insertWorkerLog(
            WorkerLogEntity(
                locationId = locationId,
                workerName = workerName,
                role = role.name,
                action = action,
                details = details,
                timestampMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun completeMarketCheckout(
        locationId: Long,
        workerName: String,
        items: List<CartItem>,
        paymentMethod: PaymentMethod,
        receivedRsd: Double,
        taxRatePercent: Double
    ): Long {
        require(items.isNotEmpty()) { "Cannot checkout an empty cart" }
        val now = System.currentTimeMillis()
        val total = items.sumOf { it.lineTotalRsd }
        val taxAmount = calculateIncludedTax(total, taxRatePercent)
        val effectiveReceived = if (paymentMethod == PaymentMethod.CARD) total else receivedRsd.coerceAtLeast(total)
        val change = (effectiveReceived - total).coerceAtLeast(0.0)
        val receiptNo = generateReceiptNumber("MKT", now)

        val orderId = tableOrderDao.insertOrder(
            PosOrderEntity(
                receiptNumber = receiptNo,
                locationId = locationId,
                workerName = workerName,
                status = OrderStatus.PAID.name,
                paymentMethod = paymentMethod.name,
                createdAtMillis = now,
                closedAtMillis = now,
                totalRsd = total,
                taxAmountRsd = taxAmount,
                receivedRsd = effectiveReceived,
                changeRsd = change
            )
        )

        val orderItems = items.map { cart ->
            PosOrderItemEntity(
                orderId = orderId,
                productId = cart.product.id,
                productName = cart.product.name,
                category = cart.product.category,
                unitPriceRsd = cart.product.priceRsd,
                quantity = cart.quantity
            )
        }
        tableOrderDao.insertOrderItems(orderItems)
        items.forEach { cart ->
            catalogDao.decrementStock(cart.product.id, cart.quantity)
        }

        logWorkerAction(
            locationId = locationId,
            workerName = workerName,
            role = UserRole.MARKET_WORKER,
            action = "SALE_COMPLETED",
            details = "$receiptNo · ${items.sumOf { it.quantity }} items · ${paymentMethod.name}"
        )
        return orderId
    }

    suspend fun openOrGetTableOrder(
        locationId: Long,
        tableId: Long,
        workerName: String
    ): Long {
        val table = tableOrderDao.getTableById(tableId)
            ?: throw IllegalArgumentException("Table not found: $tableId")
        table.activeOrderId?.let { existingId ->
            val existing = tableOrderDao.getOrderById(existingId)
            if (existing != null && existing.order.status == OrderStatus.OPEN.name) {
                return existingId
            }
        }
        val now = System.currentTimeMillis()
        val receiptNo = generateReceiptNumber("BAR", now)
        val orderId = tableOrderDao.insertOrder(
            PosOrderEntity(
                receiptNumber = receiptNo,
                locationId = locationId,
                tableId = table.id,
                tableLabel = table.label,
                workerName = workerName,
                status = OrderStatus.OPEN.name,
                createdAtMillis = now,
                totalRsd = 0.0
            )
        )
        tableOrderDao.setTableActiveOrder(table.id, orderId)
        logWorkerAction(
            locationId = locationId,
            workerName = workerName,
            role = UserRole.BAR_WORKER,
            action = "TABLE_OPENED",
            details = "${table.label} ($receiptNo)"
        )
        return orderId
    }

    suspend fun addProductToOpenOrder(orderId: Long, product: Product, deltaQty: Int = 1) {
        val wrapper = tableOrderDao.getOrderById(orderId) ?: return
        if (wrapper.order.status != OrderStatus.OPEN.name) return

        val existingItem = tableOrderDao.findOrderItem(orderId, product.id)
        if (existingItem != null) {
            val nextQty = existingItem.quantity + deltaQty
            if (nextQty <= 0) {
                tableOrderDao.deleteOrderItemById(existingItem.id)
            } else {
                tableOrderDao.updateOrderItemQuantity(existingItem.id, nextQty)
            }
        } else if (deltaQty > 0) {
            tableOrderDao.insertOrderItem(
                PosOrderItemEntity(
                    orderId = orderId,
                    productId = product.id,
                    productName = product.name,
                    category = product.category,
                    unitPriceRsd = product.priceRsd,
                    quantity = deltaQty
                )
            )
        }
        recalculateOpenOrderTotal(orderId)
    }

    suspend fun updateOrderItemQuantity(orderId: Long, itemId: Long, newQuantity: Int) {
        val wrapper = tableOrderDao.getOrderById(orderId) ?: return
        if (wrapper.order.status != OrderStatus.OPEN.name) return
        if (newQuantity <= 0) {
            tableOrderDao.deleteOrderItemById(itemId)
        } else {
            tableOrderDao.updateOrderItemQuantity(itemId, newQuantity)
        }
        recalculateOpenOrderTotal(orderId)
    }

    private suspend fun recalculateOpenOrderTotal(orderId: Long) {
        val wrapper = tableOrderDao.getOrderById(orderId) ?: return
        val items = tableOrderDao.getItemsForOrder(orderId)
        val total = items.sumOf { it.unitPriceRsd * it.quantity }
        tableOrderDao.updateOrder(wrapper.order.copy(totalRsd = total))
    }

    suspend fun settleOpenOrder(
        orderId: Long,
        paymentMethod: PaymentMethod,
        receivedRsd: Double,
        taxRatePercent: Double,
        workerName: String
    ): Long {
        val wrapper = tableOrderDao.getOrderById(orderId)
            ?: throw IllegalArgumentException("Order not found: $orderId")
        val items = tableOrderDao.getItemsForOrder(orderId)
        val total = items.sumOf { it.unitPriceRsd * it.quantity }
        val taxAmount = calculateIncludedTax(total, taxRatePercent)
        val effectiveReceived = if (paymentMethod == PaymentMethod.CARD) total else receivedRsd.coerceAtLeast(total)
        val change = (effectiveReceived - total).coerceAtLeast(0.0)
        val now = System.currentTimeMillis()

        tableOrderDao.updateOrder(
            wrapper.order.copy(
                status = OrderStatus.PAID.name,
                paymentMethod = paymentMethod.name,
                closedAtMillis = now,
                totalRsd = total,
                taxAmountRsd = taxAmount,
                receivedRsd = effectiveReceived,
                changeRsd = change
            )
        )

        wrapper.order.tableId?.let { tableId ->
            tableOrderDao.setTableActiveOrder(tableId, null)
        }

        items.forEach { item ->
            catalogDao.decrementStock(item.productId, item.quantity)
        }

        logWorkerAction(
            locationId = wrapper.order.locationId,
            workerName = workerName,
            role = UserRole.BAR_WORKER,
            action = "TABLE_SETTLED",
            details = "${wrapper.order.tableLabel ?: wrapper.order.receiptNumber} · ${paymentMethod.name}"
        )
        return orderId
    }

    suspend fun cancelOpenOrder(orderId: Long, workerName: String) {
        val wrapper = tableOrderDao.getOrderById(orderId) ?: return
        val now = System.currentTimeMillis()
        tableOrderDao.updateOrder(
            wrapper.order.copy(
                status = OrderStatus.CANCELLED.name,
                closedAtMillis = now
            )
        )
        wrapper.order.tableId?.let { tableId ->
            tableOrderDao.setTableActiveOrder(tableId, null)
        }
        logWorkerAction(
            locationId = wrapper.order.locationId,
            workerName = workerName,
            role = UserRole.BAR_WORKER,
            action = "ORDER_CANCELLED",
            details = wrapper.order.tableLabel ?: wrapper.order.receiptNumber
        )
    }

    private fun calculateIncludedTax(grossTotal: Double, taxRatePercent: Double): Double {
        if (taxRatePercent <= 0.0 || grossTotal <= 0.0) return 0.0
        return grossTotal - (grossTotal / (1.0 + taxRatePercent / 100.0))
    }

    private fun generateReceiptNumber(prefix: String, millis: Long): String {
        val fmt = SimpleDateFormat("MMdd-HHmmss", Locale.US)
        return "$prefix-${fmt.format(Date(millis))}"
    }
}
