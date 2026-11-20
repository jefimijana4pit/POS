package com.example.marketpos.i18n

import androidx.compose.runtime.staticCompositionLocalOf
import com.example.marketpos.model.AppLanguage
import com.example.marketpos.model.PaymentMethod
import com.example.marketpos.model.ProductCategory
import com.example.marketpos.model.ReportPeriodType
import com.example.marketpos.model.UserRole
import com.example.marketpos.model.VenueMode

interface AppStrings {
    val appName: String
    val loginSubtitle: String
    val selectVenueMode: String
    val venueMarketLabel: String
    val venueBarLabel: String
    val selectRole: String
    val workerRole: String
    val marketWorkerRole: String
    val restaurantWorkerRole: String
    val adminRole: String
    val selectWorker: String
    val enterPin: String
    val pinHintWorker: String
    val pinHintAdmin: String
    val signInButton: String
    val invalidPinError: String
    val quickDemoTitle: String
    val demoWorkerBtn: String
    val demoMarketWorkerBtn: String
    val demoRestaurantWorkerBtn: String
    val demoAdminBtn: String
    val adminChooseWorkspaceTitle: String
    val adminChooseWorkspaceSubtitle: String
    val openMarketModuleBtn: String
    val openRestaurantModuleBtn: String
    val freeTablesSummary: (Int, Int) -> String
    val pendingOrdersSummary: (Int) -> String
    val activeProductsSummary: (Int) -> String

    // Common & Navigation
    val settings: String
    val logout: String
    val back: String
    val cancel: String
    val add: String
    val save: String
    val delete: String
    val edit: String
    val close: String
    val all: String
    val searchProducts: String
    val noProductsFound: String
    val activeCurrencyLabel: String

    // Worker Flow
    val posRegister: String
    val cartTitle: String
    val cartEmptyTitle: String
    val cartEmptyHint: String
    val clearCart: String
    val itemsCount: (Int) -> String
    val subtotalLabel: String
    val pdvLabel: String
    val totalLabel: String
    val openCartBtn: String
    val checkoutBtn: String
    val swipeToRemoveHint: String
    val addOne: String
    val removeOne: String
    val outOfStock: String
    val outOfStockBannerTitle: (Int) -> String
    val viewLabel: String
    val showAllLabel: String
    val inStockCount: (Int, String) -> String
    val lowStockCount: (Int, String) -> String
    val maxStockReached: (String, Int) -> String

    // Bar & Restaurant Tables Flow
    val barFloorTitle: String
    val navTables: String
    val tableFilterFree: String
    val tableFilterMine: String
    val tableFilterBusy: String
    val tableSeatsCount: (Int) -> String
    val tableStatusFree: String
    val tableStatusLockedByYou: String
    val tableStatusLockedByOther: (String) -> String
    val tableStatusAwaitingCleanup: String
    val tableBusyItemsSummary: (Int) -> String
    val tableEmptyOrderLabel: String
    val tableLockedCannotInteract: (String) -> String
    val tableAutoUnlockIn: (String) -> String
    val tableCleanupBannerTitle: String
    val tableCleanupBannerSubtitle: (String) -> String
    val tableCleanedUnlockBtn: String
    val newGuestsOrderBtn: String
    val unlockEmptyTableBtn: String
    val tableOrderTitle: (String) -> String
    val payAndPrintBillBtn: String
    val receiptTableLabel: String
    val backToTableBtn: String
    val addTableTitle: String
    val editTableTitle: String
    val tableNameLabel: String
    val tableZoneLabel: String
    val tableCapacityLabel: String
    val deleteTableTitle: String
    val deleteTableConfirm: (String) -> String
    val forceUnlockBtn: String
    val noTablesDefined: String
    val tableBusyStatusLabel: String

    // Orders & Bills Lifecycle
    val navOrders: String
    val navBills: String
    val ordersTitle: String
    val billsTitle: String
    val orderDetailsTitle: (String) -> String
    val orderStatusPending: String
    val orderStatusFinished: String
    val orderStatusCancelled: String
    val cancelledItemBadge: String
    val activeItemsSection: String
    val cancelledItemsSection: String
    val savePendingOrderBtn: String
    val takeOrderBtn: String
    val editingOrderBanner: (String) -> String
    val doneEditingBtn: String
    val editOrderBtn: String
    val addItemsBtn: String
    val cancelOrderBtn: String
    val cancelItemBtn: String
    val cancelOrderConfirmTitle: String
    val cancelOrderConfirmMessage: (String) -> String
    val confirmCancelBtn: String
    val payAndFinishOrderBtn: String
    val viewBillBtn: String
    val counterOrderLabel: String
    val noOrdersFound: String
    val noBillsFound: String
    val canceledOrdersSummary: (Int) -> String
    val orderSavedMessage: String

    // Payment Modal
    val paymentTitle: String
    val paymentMethodLabel: String
    val cashLabel: String
    val cardLabel: String
    val amountDue: String
    val cashReceivedLabel: String
    val exactAmount: String
    val changeDueLabel: String
    val insufficientCashError: String
    val cardReadyNote: String
    val completeSaleBtn: String

    // Receipt Screen
    val receiptTitle: String
    val storeNameHeader: String
    val storeAddressHeader: String
    val receiptNumberLabel: String
    val dateTimeLabel: String
    val cashierLabel: String
    val paymentUsedLabel: String
    val printReceiptBtn: String
    val newSaleBtn: String
    val thankYouFooter: String
    val printSuccessSystem: String
    val printSuccessThermal: String
    val printSimulatedTitle: String
    val bluetoothPermissionNeeded: String
    val grantPermissionBtn: String

    // Admin Flow
    val adminPanelTitle: String
    val navDashboard: String
    val navWorkerLog: String
    val navProducts: String
    val kpiRevenue: String
    val kpiTransactions: String
    val kpiAvgBasket: String
    val kpiActiveItems: String
    val chartRevenueOverTime: String
    val chartTopProducts: String
    val chartSalesByWorker: String
    val tapDotHint: String
    val pickDateLabel: String
    val allDaysBtn: String
    val dateOverviewTitle: (String) -> String
    val cashVsCardLabel: String
    val dayReceiptsTitle: String
    val noSalesOnDate: String
    val soldUnitsSuffix: (Int) -> String
    val noChartData: String
    val filterByWorker: String
    val allWorkers: String
    val workerSummaryTitle: String
    val transactionItemsTitle: String
    val reprintReceiptBtn: String
    val noTransactionsFound: String
    val addProductTitle: String
    val editProductTitle: String
    val productNameSrLabel: String
    val productNameEnLabel: String
    val productSkuLabel: String
    val productCategoryLabel: String
    val productPriceLabel: (String) -> String
    val productUnitSrLabel: String
    val productUnitEnLabel: String
    val productStockLabel: String
    val productLowStockThresholdLabel: String
    val productAvailableLabel: String
    val kpiLowStock: String
    val lowStockAlertsTitle: String
    val lowStockFilter: String
    val outOfStockFilter: String
    val quickRestockBtn: String
    val deleteProductTitle: String
    val deleteProductConfirm: (String) -> String
    val validationRequiredError: String
    val validationPriceError: String
    val validationStockError: String

    // Settings Screen
    val settingsTitle: String
    val venueModeSection: String
    val venueModeLabel: String
    val venueModeMarketDesc: String
    val venueModeBarDesc: String
    val languageCurrencySection: String
    val languageLabel: String
    val serbianOption: String
    val englishOption: String
    val exchangeRateLabel: String
    val exchangeRateApplied: String
    val exchangeRateInvalid: String
    val applyRateBtn: String
    val appearanceSection: String
    val themeModeLabel: String
    val themeSystem: String
    val themeLight: String
    val themeDark: String
    val dynamicColorLabel: String
    val dynamicColorSubtitle: String
    val fontScaleLabel: String
    val fontNormal: String
    val fontLarge: String
    val fontExtraLarge: String
    val printerSection: String
    val printerTypeLabel: String
    val printerSystemTitle: String
    val printerSystemSubtitle: String
    val printerThermalTitle: String
    val printerThermalSubtitle: String
    val scanPrintersBtn: String
    val selectPrinterLabel: String
    val testPrintBtn: String
    val bluetoothGrantedStatus: String
    val bluetoothMissingStatus: String
    val testPrintSuccess: String

    // Multi-Location, Global View & PDF Reports
    val globalOverviewTitle: String
    val globalOverviewSubtitle: String
    val openGlobalViewBtn: String
    val marketsSectionTitle: String
    val barsSectionTitle: String
    val addMarketBtn: String
    val addBarBtn: String
    val addLocationTitle: (VenueMode) -> String
    val editLocationTitle: String
    val locationNameLabel: String
    val locationAddressLabel: String
    val copyStarterCatalogLabel: String
    val deleteLocationTitle: String
    val deleteLocationConfirm: (String) -> String
    val cannotDeleteLocationMsg: String
    val globalTabEarnings: String
    val globalTabStock: String
    val globalFilterAllLocations: String
    val globalFilterMarketsOnly: String
    val globalFilterBarsOnly: String
    val earningsByLocationTitle: String
    val globalStockValueLabel: String
    val globalTotalUnitsLabel: String
    val globalSkusLabel: String
    val openLocationAdminBtn: String
    val pdfReportTitle: String
    val exportPdfReportBtn: String
    val reportPeriodDaily: String
    val reportPeriodMonthly: String
    val reportPeriodDateRange: String
    val reportPeriodTypeName: (ReportPeriodType) -> String
    val reportSelectLocationLabel: String
    val reportAllLocationsOption: String
    val reportPickDateBtn: String
    val reportStartDateLabel: String
    val reportEndDateLabel: String
    val reportGeneratedAtLabel: String
    val reportFinishedBillsLabel: String
    val reportCancelledBillsLabel: String
    val reportTablesHandledLabel: String
    val reportUnitsSoldLabel: String
    val reportWorkerStatsTitle: String
    val reportColWorker: String
    val reportColBills: String
    val reportColTables: String
    val reportColProducts: String
    val reportColRevenue: String
    val reportFinShort: String
    val reportCancShort: String
    val reportItemsShort: String
    val reportUnitsShort: String
    val reportNoWorkerData: String
    val reportDownloadPdfBtn: String
    val reportSharePdfBtn: String
    val reportSavedToDownloadsMsg: (String) -> String
    val reportPageLabel: (Int) -> String
    val globalReportSubtitle: String

    // Category & Enum names
    val categoryName: (ProductCategory) -> String
    val paymentMethodName: (PaymentMethod) -> String
    val roleName: (UserRole) -> String
    val venueName: (VenueMode) -> String
}

fun stringsFor(language: AppLanguage): AppStrings = when (language) {
    AppLanguage.SERBIAN_LATIN -> SerbianLatinStrings
    AppLanguage.ENGLISH -> EnglishStrings
}

val LocalStrings = staticCompositionLocalOf<AppStrings> { SerbianLatinStrings }
