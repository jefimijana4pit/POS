package com.example.marketpos.navigation

import kotlinx.serialization.Serializable

@Serializable
object LoginRoute

@Serializable
object WorkerPosRoute

@Serializable
object AdminVenueSelectRoute

@Serializable
object AdminGlobalRoute

@Serializable
object AdminRoute

@Serializable
data class ReceiptRoute(val transactionId: String)

@Serializable
object SettingsRoute
