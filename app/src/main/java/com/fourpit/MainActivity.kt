package com.fourpit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.fourpit.marketpos.MarketPosApplication
import com.fourpit.marketpos.di.PosAppContainer
import com.fourpit.marketpos.i18n.LocalAppStrings
import com.fourpit.marketpos.i18n.stringsFor
import com.fourpit.marketpos.model.PosSettings
import com.fourpit.marketpos.model.UserRole
import com.fourpit.marketpos.navigation.AdminBranchRoute
import com.fourpit.marketpos.navigation.AdminGlobalRoute
import com.fourpit.marketpos.navigation.AdminVenueSelectRoute
import com.fourpit.marketpos.navigation.BarTableOrderRoute
import com.fourpit.marketpos.navigation.BarWorkerRoute
import com.fourpit.marketpos.navigation.LoginRoute
import com.fourpit.marketpos.navigation.OrderDetailsRoute
import com.fourpit.marketpos.navigation.ReceiptRoute
import com.fourpit.marketpos.navigation.SettingsRoute
import com.fourpit.marketpos.navigation.WorkerPosRoute
import com.fourpit.marketpos.ui.screens.AdminGlobalScreen
import com.fourpit.marketpos.ui.screens.AdminScreen
import com.fourpit.marketpos.ui.screens.AdminVenueSelectScreen
import com.fourpit.marketpos.ui.screens.BarWorkerScreen
import com.fourpit.marketpos.ui.screens.LoginScreen
import com.fourpit.marketpos.ui.screens.ReceiptScreen
import com.fourpit.marketpos.ui.screens.SettingsScreen
import com.fourpit.marketpos.ui.screens.WorkerPosScreen
import com.fourpit.marketpos.ui.screens.bar.BarTableOrderScreen
import com.fourpit.marketpos.ui.screens.orders.OrderDetailsScreen
import com.fourpit.marketpos.ui.theme.MarketPosTheme
import com.fourpit.marketpos.viewmodel.AdminBranchViewModel
import com.fourpit.marketpos.viewmodel.AdminGlobalViewModel
import com.fourpit.marketpos.viewmodel.AdminVenueSelectViewModel
import com.fourpit.marketpos.viewmodel.BarWorkerViewModel
import com.fourpit.marketpos.viewmodel.LoginViewModel
import com.fourpit.marketpos.viewmodel.MarketWorkerViewModel
import com.fourpit.marketpos.viewmodel.ReceiptViewModel
import com.fourpit.marketpos.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as MarketPosApplication).container

        setContent {
            MarketPosApp(container = container)
        }
    }
}

@Composable
fun MarketPosApp(container: PosAppContainer) {
    val settings by container.settingsDataStore.settingsFlow
        .collectAsStateWithLifecycle(initialValue = PosSettings())
    val session by container.sessionManager.session.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    fun logoutAndReturnToLogin() {
        container.sessionManager.logout()
        navController.navigate(LoginRoute) {
            popUpTo(0) { inclusive = true }
        }
    }

    MarketPosTheme(
        themeMode = settings.themeMode,
        dynamicColor = settings.dynamicColor,
        fontScaleOption = settings.fontScaleOption
    ) {
        CompositionLocalProvider(LocalAppStrings provides stringsFor(settings.language)) {
            NavHost(
                navController = navController,
                startDestination = LoginRoute
            ) {
                composable<LoginRoute> {
                    val vm: LoginViewModel = viewModel(
                        factory = LoginViewModel.Factory(
                            catalogRepo = container.catalogLocationRepository,
                            orderSalesRepo = container.orderSalesRepository,
                            sessionManager = container.sessionManager,
                            settingsDataStore = container.settingsDataStore
                        )
                    )
                    LoginScreen(
                        viewModel = vm,
                        currentLanguage = settings.language,
                        onOpenSettings = { navController.navigate(SettingsRoute) },
                        onLoginSuccess = { role, locationId ->
                            when (role) {
                                UserRole.ADMIN -> navController.navigate(AdminVenueSelectRoute) {
                                    popUpTo(LoginRoute) { inclusive = true }
                                }
                                UserRole.MARKET_WORKER -> {
                                    val locId = locationId ?: return@LoginScreen
                                    navController.navigate(WorkerPosRoute(locId)) {
                                        popUpTo(LoginRoute) { inclusive = true }
                                    }
                                }
                                UserRole.BAR_WORKER -> {
                                    val locId = locationId ?: return@LoginScreen
                                    navController.navigate(BarWorkerRoute(locId)) {
                                        popUpTo(LoginRoute) { inclusive = true }
                                    }
                                }
                            }
                        }
                    )
                }

                composable<WorkerPosRoute> { entry ->
                    val route = entry.toRoute<WorkerPosRoute>()
                    val vm: MarketWorkerViewModel = viewModel(
                        key = "market_worker_${route.locationId}",
                        factory = MarketWorkerViewModel.Factory(
                            locationId = route.locationId,
                            catalogRepo = container.catalogLocationRepository,
                            orderSalesRepo = container.orderSalesRepository
                        )
                    )
                    WorkerPosScreen(
                        viewModel = vm,
                        workerName = session?.workerName ?: "Kasir",
                        settings = settings,
                        onOrderCompleted = { orderId ->
                            navController.navigate(ReceiptRoute(orderId))
                        },
                        onOpenOrderDetails = { orderId ->
                            navController.navigate(OrderDetailsRoute(orderId))
                        },
                        onOpenSettings = { navController.navigate(SettingsRoute) },
                        onLogout = { logoutAndReturnToLogin() }
                    )
                }

                composable<BarWorkerRoute> { entry ->
                    val route = entry.toRoute<BarWorkerRoute>()
                    val vm: BarWorkerViewModel = viewModel(
                        key = "bar_worker_${route.locationId}",
                        factory = BarWorkerViewModel.Factory(
                            locationId = route.locationId,
                            catalogRepo = container.catalogLocationRepository,
                            orderSalesRepo = container.orderSalesRepository
                        )
                    )
                    BarWorkerScreen(
                        viewModel = vm,
                        workerName = session?.workerName ?: "Konobar",
                        settings = settings,
                        onOpenTableOrder = { tableId, orderId ->
                            navController.navigate(BarTableOrderRoute(route.locationId, tableId, orderId))
                        },
                        onOpenOrderDetails = { orderId ->
                            navController.navigate(OrderDetailsRoute(orderId))
                        },
                        onOpenSettings = { navController.navigate(SettingsRoute) },
                        onLogout = { logoutAndReturnToLogin() }
                    )
                }

                composable<BarTableOrderRoute> { entry ->
                    val route = entry.toRoute<BarTableOrderRoute>()
                    val vm: BarWorkerViewModel = viewModel(
                        key = "bar_order_${route.locationId}",
                        factory = BarWorkerViewModel.Factory(
                            locationId = route.locationId,
                            catalogRepo = container.catalogLocationRepository,
                            orderSalesRepo = container.orderSalesRepository
                        )
                    )
                    BarTableOrderScreen(
                        orderId = route.orderId,
                        viewModel = vm,
                        workerName = session?.workerName ?: "Konobar",
                        settings = settings,
                        onSettled = { settledOrderId ->
                            navController.navigate(ReceiptRoute(settledOrderId)) {
                                popUpTo(BarTableOrderRoute(route.locationId, route.tableId, route.orderId)) {
                                    inclusive = true
                                }
                            }
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable<OrderDetailsRoute> { entry ->
                    val route = entry.toRoute<OrderDetailsRoute>()
                    OrderDetailsScreen(
                        orderFlow = container.orderSalesRepository.observeOrderById(route.orderId),
                        settings = settings,
                        onViewReceipt = { orderId ->
                            navController.navigate(ReceiptRoute(orderId))
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable<ReceiptRoute> { entry ->
                    val route = entry.toRoute<ReceiptRoute>()
                    val vm: ReceiptViewModel = viewModel(
                        key = "receipt_${route.orderId}",
                        factory = ReceiptViewModel.Factory(
                            orderId = route.orderId,
                            orderSalesRepo = container.orderSalesRepository,
                            printerCoordinator = container.printerCoordinator
                        )
                    )
                    ReceiptScreen(
                        viewModel = vm,
                        settings = settings,
                        onDone = { navController.popBackStack() }
                    )
                }

                composable<AdminVenueSelectRoute> {
                    val vm: AdminVenueSelectViewModel = viewModel(
                        factory = AdminVenueSelectViewModel.Factory(
                            catalogRepo = container.catalogLocationRepository,
                            orderSalesRepo = container.orderSalesRepository
                        )
                    )
                    AdminVenueSelectScreen(
                        viewModel = vm,
                        settings = settings,
                        onOpenGlobal = { navController.navigate(AdminGlobalRoute) },
                        onOpenBranch = { locId -> navController.navigate(AdminBranchRoute(locId)) },
                        onOpenSettings = { navController.navigate(SettingsRoute) },
                        onLogout = { logoutAndReturnToLogin() }
                    )
                }

                composable<AdminBranchRoute> { entry ->
                    val route = entry.toRoute<AdminBranchRoute>()
                    val vm: AdminBranchViewModel = viewModel(
                        key = "admin_branch_${route.locationId}",
                        factory = AdminBranchViewModel.Factory(
                            locationId = route.locationId,
                            catalogRepo = container.catalogLocationRepository,
                            orderSalesRepo = container.orderSalesRepository
                        )
                    )
                    AdminScreen(
                        locationId = route.locationId,
                        viewModel = vm,
                        settings = settings,
                        pdfGenerator = container.pdfGenerator,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable<AdminGlobalRoute> {
                    val vm: AdminGlobalViewModel = viewModel(
                        factory = AdminGlobalViewModel.Factory(
                            catalogRepo = container.catalogLocationRepository,
                            orderSalesRepo = container.orderSalesRepository
                        )
                    )
                    AdminGlobalScreen(
                        viewModel = vm,
                        settings = settings,
                        pdfGenerator = container.pdfGenerator,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable<SettingsRoute> {
                    val vm: SettingsViewModel = viewModel(
                        factory = SettingsViewModel.Factory(
                            settingsDataStore = container.settingsDataStore,
                            bluetoothPrinter = container.printerCoordinator.bluetoothPrinter
                        )
                    )
                    SettingsScreen(
                        viewModel = vm,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
