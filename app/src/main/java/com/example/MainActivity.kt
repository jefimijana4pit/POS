package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.marketpos.MarketPosApplication
import com.example.marketpos.i18n.LocalStrings
import com.example.marketpos.i18n.stringsFor
import com.example.marketpos.model.UserRole
import com.example.marketpos.model.VenueMode
import com.example.marketpos.navigation.AdminGlobalRoute
import com.example.marketpos.navigation.AdminRoute
import com.example.marketpos.navigation.AdminVenueSelectRoute
import com.example.marketpos.navigation.LoginRoute
import com.example.marketpos.navigation.ReceiptRoute
import com.example.marketpos.navigation.SettingsRoute
import com.example.marketpos.navigation.WorkerPosRoute
import com.example.marketpos.ui.screens.AdminGlobalScreen
import com.example.marketpos.ui.screens.AdminScreen
import com.example.marketpos.ui.screens.AdminVenueSelectScreen
import com.example.marketpos.ui.screens.BarWorkerScreen
import com.example.marketpos.ui.screens.LoginScreen
import com.example.marketpos.ui.screens.ReceiptScreen
import com.example.marketpos.ui.screens.SettingsScreen
import com.example.marketpos.ui.screens.WorkerPosScreen
import com.example.marketpos.ui.theme.MarketPosTheme
import com.example.marketpos.viewmodel.AdminBranchViewModel
import com.example.marketpos.viewmodel.AdminGlobalViewModel
import com.example.marketpos.viewmodel.AdminVenueSelectViewModel
import com.example.marketpos.viewmodel.BarWorkerViewModel
import com.example.marketpos.viewmodel.LoginViewModel
import com.example.marketpos.viewmodel.MarketWorkerViewModel
import com.example.marketpos.viewmodel.ReceiptViewModel
import com.example.marketpos.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MarketPosRoot()
        }
    }
}

@Composable
fun MarketPosRoot() {
    val context = LocalContext.current
    val appContainer = remember(context) {
        (context.applicationContext as MarketPosApplication).appContainer
    }
    val settings by appContainer.sessionManager.effectiveSettingsState.collectAsStateWithLifecycle()
    val localizedStrings = stringsFor(settings.language)
    val navController = rememberNavController()

    MarketPosTheme(
        themeMode = settings.themeMode,
        dynamicColor = settings.dynamicColor,
        fontScaleOption = settings.fontScale
    ) {
        CompositionLocalProvider(LocalStrings provides localizedStrings) {
            NavHost(
                navController = navController,
                startDestination = LoginRoute
            ) {
                composable<LoginRoute> { backStackEntry ->
                    val loginVm: LoginViewModel = viewModel(
                        viewModelStoreOwner = backStackEntry,
                        factory = LoginViewModel.factory(appContainer)
                    )
                    val loginState by loginVm.uiState.collectAsStateWithLifecycle()
                    LoginScreen(
                        uiState = loginState,
                        onToggleLanguage = loginVm::updateLanguage,
                        onLoginWithPin = { role, workerId, pin ->
                            val ok = loginVm.authenticate(role, workerId, pin)
                            if (ok) {
                                val target = if (role == UserRole.ADMIN) {
                                    AdminVenueSelectRoute
                                } else {
                                    WorkerPosRoute
                                }
                                navController.navigate(target) {
                                    popUpTo(LoginRoute) { inclusive = true }
                                }
                            }
                            ok
                        },
                        onQuickDemoLogin = { role ->
                            loginVm.quickDemoLogin(role)
                            val target = if (role == UserRole.ADMIN) {
                                AdminVenueSelectRoute
                            } else {
                                WorkerPosRoute
                            }
                            navController.navigate(target) {
                                popUpTo(LoginRoute) { inclusive = true }
                            }
                        }
                    )
                }

                composable<AdminVenueSelectRoute> { backStackEntry ->
                    val venueVm: AdminVenueSelectViewModel = viewModel(
                        viewModelStoreOwner = backStackEntry,
                        factory = AdminVenueSelectViewModel.factory(appContainer)
                    )
                    val venueState by venueVm.uiState.collectAsStateWithLifecycle()
                    AdminVenueSelectScreen(
                        uiState = venueState,
                        onOpenGlobalView = {
                            navController.navigate(AdminGlobalRoute)
                        },
                        onSelectLocation = { location ->
                            venueVm.selectAdminLocation(location)
                            navController.navigate(AdminRoute)
                        },
                        onSaveLocation = venueVm::saveLocation,
                        onDeleteLocation = venueVm::deleteLocation,
                        onDismissLocationError = venueVm::dismissLocationErrorNotice,
                        onOpenSettings = {
                            navController.navigate(SettingsRoute)
                        },
                        onLogout = {
                            venueVm.logout()
                            navController.navigate(LoginRoute) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }

                composable<AdminGlobalRoute> { backStackEntry ->
                    val globalVm: AdminGlobalViewModel = viewModel(
                        viewModelStoreOwner = backStackEntry,
                        factory = AdminGlobalViewModel.factory(appContainer)
                    )
                    val globalState by globalVm.uiState.collectAsStateWithLifecycle()
                    AdminGlobalScreen(
                        uiState = globalState,
                        onOpenLocationAdmin = { location ->
                            globalVm.selectAdminLocation(location)
                            navController.navigate(AdminRoute)
                        },
                        onAdjustStock = globalVm::adjustStock,
                        onBuildReportSummary = globalVm::buildReportSummary,
                        onExportPdfReport = { summary ->
                            globalVm.exportPdfReport(context, summary, localizedStrings)
                        },
                        onDismissPdfResult = globalVm::dismissPdfExportResult,
                        onOpenSettings = {
                            navController.navigate(SettingsRoute)
                        },
                        onBack = {
                            navController.popBackStack()
                        },
                        onLogout = {
                            globalVm.logout()
                            navController.navigate(LoginRoute) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }

                composable<WorkerPosRoute> { backStackEntry ->
                    if (settings.venueMode == VenueMode.BAR_RESTAURANT) {
                        val barVm: BarWorkerViewModel = viewModel(
                            viewModelStoreOwner = backStackEntry,
                            key = "bar_worker",
                            factory = BarWorkerViewModel.factory(appContainer)
                        )
                        val barState by barVm.uiState.collectAsStateWithLifecycle()
                        BarWorkerScreen(
                            uiState = barState,
                            onOpenOrLockTable = barVm::openOrLockTable,
                            onCloseTable = barVm::closeActiveBarTable,
                            onUnlockTable = barVm::unlockTableAfterCleanup,
                            onStartNewOrderOnTable = barVm::startNewOrderOnTable,
                            onSearchQueryChange = barVm::updateSearchQuery,
                            onSelectCategory = barVm::selectCategory,
                            onToggleOutOfStockFilter = barVm::toggleOutOfStockFilter,
                            onAddProductToTable = barVm::addProductToActiveTable,
                            onDecrementTableItem = barVm::decrementActiveTableItem,
                            onRemoveTableItem = barVm::removeActiveTableItem,
                            onClearTableOrder = barVm::clearActiveTableOrder,
                            onCompleteTableSale = { method, cashRsd ->
                                barVm.completeActiveTableSale(context, method, cashRsd) { tx ->
                                    navController.navigate(ReceiptRoute(tx.id))
                                }
                            },
                            onOpenOrderDetails = barVm::openOrderDetails,
                            onCloseOrderDetails = barVm::closeOrderDetails,
                            onEditOrder = barVm::startEditingOrder,
                            onIncrementOrderItem = barVm::incrementOrderItem,
                            onDecrementOrderItem = barVm::decrementOrderItem,
                            onCancelOrderItem = barVm::cancelOrderItem,
                            onCancelFullOrder = barVm::cancelFullOrder,
                            onCompleteOrderPayment = { order, method, cashRsd ->
                                barVm.completeOrderPayment(context, order, method, cashRsd) { tx ->
                                    navController.navigate(ReceiptRoute(tx.id))
                                }
                            },
                            onOpenBill = { txId ->
                                navController.navigate(ReceiptRoute(txId))
                            },
                            onDismissStockWarning = barVm::dismissStockWarning,
                            onDismissTableLockFeedback = barVm::dismissTableLockFeedback,
                            onKeepPendingOrder = barVm::keepActiveTableOrderPending,
                            onDismissOrderSavedNotice = barVm::dismissOrderSavedNotice,
                            onOpenSettings = {
                                navController.navigate(SettingsRoute)
                            },
                            onLogout = {
                                barVm.logout()
                                navController.navigate(LoginRoute) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    } else {
                        val marketVm: MarketWorkerViewModel = viewModel(
                            viewModelStoreOwner = backStackEntry,
                            key = "market_worker",
                            factory = MarketWorkerViewModel.factory(appContainer)
                        )
                        val marketState by marketVm.uiState.collectAsStateWithLifecycle()
                        WorkerPosScreen(
                            uiState = marketState,
                            onSearchQueryChange = marketVm::updateSearchQuery,
                            onSelectCategory = marketVm::selectCategory,
                            onToggleOutOfStockFilter = marketVm::toggleOutOfStockFilter,
                            onAddProduct = marketVm::addToCart,
                            onDecrementProduct = marketVm::decrementCartItem,
                            onRemoveCartItem = marketVm::removeFromCart,
                            onClearCart = marketVm::clearCart,
                            onOpenBill = { txId ->
                                navController.navigate(ReceiptRoute(txId))
                            },
                            onDismissStockWarning = marketVm::dismissStockWarning,
                            onCompleteSale = { method, cashRsd ->
                                marketVm.completeSale(method, cashRsd) { tx ->
                                    navController.navigate(ReceiptRoute(tx.id))
                                }
                            },
                            onOpenSettings = {
                                navController.navigate(SettingsRoute)
                            },
                            onLogout = {
                                marketVm.logout()
                                navController.navigate(LoginRoute) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }
                }

                composable<AdminRoute> { backStackEntry ->
                    val branchVm: AdminBranchViewModel = viewModel(
                        viewModelStoreOwner = backStackEntry,
                        factory = AdminBranchViewModel.factory(appContainer)
                    )
                    val branchState by branchVm.uiState.collectAsStateWithLifecycle()
                    AdminScreen(
                        uiState = branchState,
                        onSelectWorkerFilter = branchVm::selectAdminWorkerFilter,
                        onSelectDashboardDay = branchVm::selectDashboardDay,
                        onReprintReceipt = { txId ->
                            navController.navigate(ReceiptRoute(txId))
                        },
                        onOpenOrderDetails = branchVm::openOrderDetails,
                        onCloseOrderDetails = branchVm::closeOrderDetails,
                        onSaveProduct = branchVm::saveProduct,
                        onAdjustStock = branchVm::adjustStock,
                        onDeleteProduct = branchVm::deleteProduct,
                        onSaveTable = branchVm::saveTable,
                        onDeleteTable = branchVm::deleteTable,
                        onForceUnlockTable = branchVm::forceUnlockTable,
                        onBuildReportSummary = branchVm::buildReportSummary,
                        onExportPdfReport = { summary ->
                            branchVm.exportPdfReport(context, summary, localizedStrings)
                        },
                        onDismissPdfResult = branchVm::dismissPdfExportResult,
                        onOpenSettings = {
                            navController.navigate(SettingsRoute)
                        },
                        onBackToVenuePicker = {
                            navController.popBackStack()
                        },
                        onLogout = {
                            branchVm.logout()
                            navController.navigate(LoginRoute) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }

                composable<ReceiptRoute> { backStackEntry ->
                    val route = backStackEntry.toRoute<ReceiptRoute>()
                    val receiptVm: ReceiptViewModel = viewModel(
                        viewModelStoreOwner = backStackEntry,
                        factory = ReceiptViewModel.factory(appContainer, route.transactionId)
                    )
                    val receiptState by receiptVm.uiState.collectAsStateWithLifecycle()
                    ReceiptScreen(
                        transaction = receiptState.transaction,
                        settings = receiptState.settings,
                        printFeedback = receiptState.printFeedback,
                        onPrintReceipt = { receiptVm.printReceipt(context) },
                        onDismissPrintFeedback = receiptVm::dismissPrintFeedback,
                        onBluetoothPermissionsResult = { granted ->
                            if (granted) {
                                receiptVm.refreshBluetoothPrinters(context)
                            }
                        },
                        onNewSale = {
                            navController.popBackStack()
                        }
                    )
                }

                composable<SettingsRoute> { backStackEntry ->
                    val settingsVm: SettingsViewModel = viewModel(
                        viewModelStoreOwner = backStackEntry,
                        factory = SettingsViewModel.factory(appContainer)
                    )
                    val settingsState by settingsVm.uiState.collectAsStateWithLifecycle()
                    SettingsScreen(
                        uiState = settingsState,
                        onBack = { navController.popBackStack() },
                        onUpdateVenueMode = settingsVm::updateVenueMode,
                        onUpdateLanguage = settingsVm::updateLanguage,
                        onUpdateExchangeRate = settingsVm::updateExchangeRate,
                        onUpdateThemeMode = settingsVm::updateThemeMode,
                        onUpdateDynamicColor = settingsVm::updateDynamicColor,
                        onUpdateFontScale = settingsVm::updateFontScale,
                        onUpdatePrinterType = settingsVm::updatePrinterType,
                        onSelectPrinterDevice = settingsVm::updateSelectedPrinter,
                        onRefreshPrinters = { settingsVm.refreshBluetoothPrinters(context) },
                        onTestPrint = { settingsVm.runTestPrint(context) },
                        onDismissPrintFeedback = settingsVm::dismissPrintFeedback
                    )
                }
            }
        }
    }
}
