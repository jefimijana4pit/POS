package com.fourpit.marketpos.di

import android.content.Context
import com.fourpit.marketpos.data.CatalogLocationRepository
import com.fourpit.marketpos.data.OrderSalesRepository
import com.fourpit.marketpos.data.SettingsDataStore
import com.fourpit.marketpos.data.local.PosDatabase
import com.fourpit.marketpos.data.seed.PosDatabaseSeeder
import com.fourpit.marketpos.printing.PrinterCoordinator
import com.fourpit.marketpos.session.PosSessionManager
import com.fourpit.marketpos.util.PosReportPdfGenerator

class PosAppContainer(context: Context) {
    val database: PosDatabase = PosDatabase.getInstance(context)
    val seeder: PosDatabaseSeeder = PosDatabaseSeeder(database)
    val settingsDataStore: SettingsDataStore = SettingsDataStore(context)
    val catalogLocationRepository: CatalogLocationRepository = CatalogLocationRepository(
        catalogDao = database.catalogLocationDao(),
        tableOrderDao = database.tableOrderDao()
    )
    val orderSalesRepository: OrderSalesRepository = OrderSalesRepository(
        catalogDao = database.catalogLocationDao(),
        tableOrderDao = database.tableOrderDao(),
        salesDao = database.salesDao()
    )
    val sessionManager: PosSessionManager = PosSessionManager()
    val printerCoordinator: PrinterCoordinator = PrinterCoordinator()
    val pdfGenerator: PosReportPdfGenerator = PosReportPdfGenerator()
}
