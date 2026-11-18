package com.example.marketpos.di

import android.content.Context
import com.example.marketpos.data.CatalogLocationRepository
import com.example.marketpos.data.OrderSalesRepository
import com.example.marketpos.data.SettingsDataStore
import com.example.marketpos.data.local.PosDatabase
import com.example.marketpos.data.seed.PosDatabaseSeeder
import com.example.marketpos.printing.PrinterCoordinator
import com.example.marketpos.session.PosSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class PosAppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val database: PosDatabase = PosDatabase.getInstance(appContext)
    val settingsDataStore: SettingsDataStore = SettingsDataStore(appContext)

    private val databaseSeeder: PosDatabaseSeeder = PosDatabaseSeeder(
        catalogLocationDao = database.catalogLocationDao(),
        salesDao = database.salesDao(),
        tableOrderDao = database.tableOrderDao()
    )

    val catalogLocationRepository: CatalogLocationRepository = CatalogLocationRepository(
        catalogLocationDao = database.catalogLocationDao(),
        tableOrderDao = database.tableOrderDao()
    )

    val orderSalesRepository: OrderSalesRepository = OrderSalesRepository(
        database = database,
        salesDao = database.salesDao(),
        tableOrderDao = database.tableOrderDao()
    )

    val sessionManager: PosSessionManager = PosSessionManager(
        appScope = appScope,
        settingsDataStore = settingsDataStore,
        catalogLocationRepository = catalogLocationRepository,
        orderSalesRepository = orderSalesRepository,
        databaseSeeder = databaseSeeder
    )

    val printerCoordinator: PrinterCoordinator = PrinterCoordinator(
        appContext = appContext,
        appScope = appScope
    )
}
