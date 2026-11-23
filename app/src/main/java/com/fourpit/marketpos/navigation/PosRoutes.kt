package com.fourpit.marketpos.navigation

import kotlinx.serialization.Serializable

@Serializable
data object LoginRoute

@Serializable
data class WorkerPosRoute(val locationId: Long)

@Serializable
data class BarWorkerRoute(val locationId: Long)

@Serializable
data class BarTableOrderRoute(val locationId: Long, val tableId: Long, val orderId: Long)

@Serializable
data class OrderDetailsRoute(val orderId: Long)

@Serializable
data class ReceiptRoute(val orderId: Long)

@Serializable
data object AdminVenueSelectRoute

@Serializable
data class AdminBranchRoute(val locationId: Long)

@Serializable
data object AdminGlobalRoute

@Serializable
data object SettingsRoute
