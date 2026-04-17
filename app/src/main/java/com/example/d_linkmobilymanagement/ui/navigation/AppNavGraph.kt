package com.example.d_linkmobilymanagement.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.d_linkmobilymanagement.R
import com.example.d_linkmobilymanagement.ui.components.BottomNavItem
import com.example.d_linkmobilymanagement.ui.components.ModernBottomNavigationBar
import com.example.d_linkmobilymanagement.ui.model.DeviceUiModel
import com.example.d_linkmobilymanagement.ui.screens.auth.LoginScreen
import com.example.d_linkmobilymanagement.ui.screens.devices.DevicesScreen
import com.example.d_linkmobilymanagement.ui.screens.devices.DeviceSettingsScreen
import com.example.d_linkmobilymanagement.ui.screens.devices.SelfDeviceWarningDialog
import com.example.d_linkmobilymanagement.ui.screens.logs.LogsScreen
import com.example.d_linkmobilymanagement.ui.screens.settings.AppSettingsScreen
import com.example.d_linkmobilymanagement.ui.screens.wififilter.WifiFilterScreen
import com.example.d_linkmobilymanagement.ui.screens.system.CompleteSystemScreen
import com.example.d_linkmobilymanagement.viewmodel.SafeSystemViewModel
import com.example.d_linkmobilymanagement.ui.screens.system.CompleteInternetStatusScreen
import com.example.d_linkmobilymanagement.ui.screens.wififilter.WifiFilterSelfWarningDialog
import com.example.d_linkmobilymanagement.ui.components.NetworkRecoveryDialog
import com.example.d_linkmobilymanagement.ui.state.UiEvent
import com.example.d_linkmobilymanagement.utils.LogExportHelper
import com.example.d_linkmobilymanagement.viewmodel.MainViewModel
import com.example.d_linkmobilymanagement.viewmodel.MainViewModelFactory
import com.example.d_linkmobilymanagement.viewmodel.SettingsViewModel

private sealed class AppRoute(
    val route: String,
    val labelRes: Int = 0,
    val icon: ImageVector? = null
) {
    data object Login : AppRoute("login")
    data object Devices : AppRoute("devices", R.string.connected_devices, Icons.Outlined.Devices)
    data object Logs : AppRoute("logs", R.string.logs, Icons.Outlined.History)
    data object Settings : AppRoute("settings", R.string.app_settings, Icons.Outlined.Settings)
    data object WifiFilter : AppRoute("wifi_filter", R.string.wifi_filter_title, Icons.Outlined.Shield)
    data object DeviceSettings : AppRoute("device_settings/{deviceId}")
    data object System : AppRoute("system", R.string.system, Icons.Outlined.Info)

    companion object {
        const val DEVICES_GROUP = "devices_group"
        const val SYSTEM_GROUP = "system_group"
    }
}

@Composable
fun WifiFilterConfirmDeleteDialog(
    mac: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (mac != null) {
        com.example.d_linkmobilymanagement.ui.screens.wififilter.ConfirmDeleteMacDialog(
            mac = mac,
            onConfirm = onConfirm,
            onDismiss = onDismiss
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavGraph(vm: MainViewModel) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val currentResources by rememberUpdatedState(resources)
    val navController = rememberNavController()
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                is UiEvent.ShowMessage -> {
                    val message = if (event.isPlural) {
                        if (event.args != null) {
                            currentResources.getQuantityString(event.messageRes, event.pluralQuantity, *event.args.toTypedArray())
                        } else {
                            currentResources.getQuantityString(event.messageRes, event.pluralQuantity)
                        }
                    } else {
                        event.args?.let { args ->
                            currentResources.getString(event.messageRes, *args.toTypedArray())
                        } ?: currentResources.getString(event.messageRes)
                    }
                    snackbarHostState.showSnackbar(message)
                }
            }
        }
    }

    val backStack by navController.currentBackStackEntryAsState()
    val currentDestination = backStack?.destination
    val currentRoute = currentDestination?.route

    // تحديد القسم النشط بناءً على التسلسل الهرمي (Hierarchy) ديناميكياً
    val activeTabRoute = remember(currentDestination) {
        currentDestination?.hierarchy?.firstOrNull { 
            it.route == AppRoute.DEVICES_GROUP || 
            it.route == AppRoute.SYSTEM_GROUP || 
            it.route == AppRoute.Logs.route || 
            it.route == AppRoute.WifiFilter.route 
        }?.route
    }

    val isSettingsRoute = currentRoute?.startsWith("device_settings") == true
    val isSystemChildRoute = currentDestination?.hierarchy?.any { it.route == AppRoute.SYSTEM_GROUP } == true
    val showBottomBar = currentRoute != AppRoute.Login.route && !isSettingsRoute
    val showTopBar = currentRoute != AppRoute.Login.route && !isSettingsRoute && !isSystemChildRoute

    Scaffold(
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = {
                        val shouldShowTitle = currentRoute == AppRoute.Devices.route ||
                                             currentRoute == AppRoute.Logs.route ||
                                             currentRoute == AppRoute.Settings.route ||
                                             currentRoute == AppRoute.WifiFilter.route

                        AnimatedVisibility(
                            visible = shouldShowTitle,
                            enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 }),
                            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it / 2 })
                        ) {
                            Text(
                                when (currentRoute) {
                                    AppRoute.Devices.route -> stringResource(R.string.connected_devices)
                                    AppRoute.Logs.route -> stringResource(R.string.logs)
                                    AppRoute.Settings.route -> stringResource(R.string.app_settings)
                                    AppRoute.WifiFilter.route -> stringResource(R.string.wifi_filter_title)
                                    else -> "D-Link Mobily"
                                }
                            )
                        }
                    },
                    actions = {
                        if (currentRoute == AppRoute.Devices.route) {
                            IconButton(onClick = { vm.refreshDevices() }, enabled = !uiState.isRefreshing) {
                                Icon(Icons.Outlined.Refresh, contentDescription = stringResource(R.string.filter_refresh))
                            }
                        }
                        if (currentRoute == AppRoute.Logs.route) {
                            IconButton(onClick = { vm.clearLogs() }) {
                                Icon(Icons.Outlined.DeleteSweep, contentDescription = stringResource(R.string.clear_logs))
                            }
                            IconButton(onClick = { LogExportHelper.exportLogs(context, uiState.logs) }) {
                                Icon(Icons.Outlined.Share, contentDescription = stringResource(R.string.export_logs))
                            }
                        }
                        if (currentRoute != AppRoute.Settings.route) {
                            IconButton(onClick = { navController.navigate(AppRoute.Settings.route) }) {
                                Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.settings))
                            }
                        }
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                val bottomNavItems = remember {
                    listOf(
                        BottomNavItem(AppRoute.DEVICES_GROUP, AppRoute.Devices.labelRes, AppRoute.Devices.icon!!),
                        BottomNavItem(AppRoute.WifiFilter.route, AppRoute.WifiFilter.labelRes, AppRoute.WifiFilter.icon!!),
                        BottomNavItem(AppRoute.SYSTEM_GROUP, AppRoute.System.labelRes, AppRoute.System.icon!!),
                        BottomNavItem(AppRoute.Logs.route, AppRoute.Logs.labelRes, AppRoute.Logs.icon!!)
                    )
                }
                
                ModernBottomNavigationBar(
                    items = bottomNavItems,
                    currentRoute = activeTabRoute,
                    onItemSelected = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = AppRoute.Login.route
        ) {
            composable(AppRoute.Login.route) {
                LoginScreen(
                    defaultRouterIp = uiState.routerInfo.ip,
                    defaultUsername = uiState.routerInfo.username,
                    defaultPassword = uiState.routerInfo.password,
                    isLoading = uiState.isLoading,
                    errorMessageRes = uiState.errorMessage,
                    preLoginInfo = vm.preLoginInfo.collectAsStateWithLifecycle().value,
                    onIpChanged = { ip -> vm.fetchPreLoginInfo(ip) },
                    onLogin = { routerIp, username, password, rememberMe ->
                        vm.login(routerIp, username, password, rememberMe) {
                            navController.navigate(AppRoute.DEVICES_GROUP) {
                                popUpTo(AppRoute.Login.route) { inclusive = true }
                            }
                        }
                    }
                )
            }

            // مجموعة الأجهزة (تتضمن الأجهزة والإعدادات)
            navigation(startDestination = AppRoute.Devices.route, route = AppRoute.DEVICES_GROUP) {
                composable(AppRoute.Devices.route) {
                    DevicesScreen(
                        paddingValues = padding,
                        devices = uiState.devices,
                        isRefreshing = uiState.isRefreshing,
                        consecutiveFailures = uiState.consecutiveRefreshFailures,
                        errorMessageRes = uiState.errorMessage,
                        activeDeviceActionIds = uiState.activeDeviceActionIds,
                        onToggleBlock = vm::addDeviceToFilterDraft,
                        onOpenSettings = { deviceId -> navController.navigate("device_settings/$deviceId") },
                        onRetry = vm::refreshDevices
                    )
                }
                composable(
                    route = AppRoute.DeviceSettings.route,
                    arguments = listOf(navArgument("deviceId") { type = NavType.StringType })
                ) { entry ->
                    val deviceId = entry.arguments?.getString("deviceId").orEmpty()
                    var device by remember(deviceId) { mutableStateOf<DeviceUiModel?>(null) }
                    LaunchedEffect(deviceId) { device = vm.getDevice(deviceId) }
                    device?.let { currentDevice ->
                        DeviceSettingsScreen(
                            paddingValues = padding,
                            device = currentDevice,
                            onSave = { customName, typeNote ->
                                vm.updateDeviceInfo(deviceId, customName, typeNote)
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }

            composable(AppRoute.Logs.route) {
                LogsScreen(paddingValues = padding, logs = uiState.logs, selectedFilter = uiState.selectedLogFilter, onFilterSelected = vm::setLogFilter)
            }

            composable(AppRoute.Settings.route) {
                val settingsVm: SettingsViewModel = viewModel(
                    factory = MainViewModelFactory(
                        (LocalContext.current.applicationContext as com.example.d_linkmobilymanagement.DlinkApp).container.routerRepository,
                        (LocalContext.current.applicationContext as com.example.d_linkmobilymanagement.DlinkApp).container.updateRepository,
                        vm.networkMonitor,
                        vm.selfDeviceStore,
                        (LocalContext.current.applicationContext as com.example.d_linkmobilymanagement.DlinkApp).container.currentDeviceNetworkInfoProvider
                    )
                )
                val settingsUiState by settingsVm.uiState.collectAsStateWithLifecycle()

                AppSettingsScreen(
                    paddingValues = padding,
                    settings = settingsUiState.settings,
                    languageSettings = settingsUiState.languageSettings,
                    isCheckingForUpdate = settingsUiState.isCheckingForUpdate,
                    updateInfo = settingsUiState.updateInfo,
                    onAutoRefreshChange = settingsVm::updateAutoRefresh,
                    onRefreshIntervalChange = settingsVm::updateRefreshInterval,
                    onLanguageChange = settingsVm::updateLanguage,
                    onClearDeviceMeta = settingsVm::clearAllDeviceMeta,
                    onClearLogs = vm::clearLogs,
                    onCheckForUpdates = settingsVm::checkForUpdates
                )
            }

            composable(AppRoute.WifiFilter.route) {
                WifiFilterScreen(
                    paddingValues = padding,
                    state24 = uiState.wifiFilter24,
                    state5 = uiState.wifiFilter5,
                    selectedBand = uiState.selectedWifiFilterBand,
                    onBandChange = vm::setWifiFilterBand,
                    onModeChange = vm::updateWifiFilterMode,
                    onAddMac = vm::addMacToFilter,
                    onRemoveMac = vm::removeMacFromFilter,
                    onSave = vm::saveWifiFilter,
                    onRefresh = vm::loadWifiFilter,
                    onConfirmDelete = vm.wifiFilterViewModel::confirmDeleteMac,
                    onCancelDelete = vm.wifiFilterViewModel::cancelDeleteMac
                )
            }

            // مجموعة النظام (تتضمن النظام وحالة الإنترنت)
            navigation(startDestination = AppRoute.System.route, route = AppRoute.SYSTEM_GROUP) {
                composable(AppRoute.System.route) {
                    CompleteSystemScreen(
                        paddingValues = padding,
                        onNavigateToInternetStatus = { navController.navigate("internet_status") },
                        onSettingsClick = { navController.navigate(AppRoute.Settings.route) },
                        viewModel = viewModel(
                            factory = SafeSystemViewModel.provideFactory(
                                repository = try { vm.getRepository() } catch (_: Exception) { null },
                                networkMonitor = vm.networkMonitor
                            )
                        )
                    )
                }
                composable("internet_status") {
                    CompleteInternetStatusScreen(
                        paddingValues = padding,
                        onBackClick = { navController.popBackStack() },
                        onSettingsClick = { navController.navigate(AppRoute.Settings.route) },
                        viewModel = viewModel(
                            factory = SafeSystemViewModel.provideFactory(
                                repository = try { vm.getRepository() } catch (_: Exception) { null },
                                networkMonitor = vm.networkMonitor
                            )
                        )
                    )
                }
            }
        }
    }

    // Dialogs
    NetworkRecoveryDialog(show = uiState.showNetworkRecoveryDialog, pendingOperationLabelRes = uiState.pendingOperationLabel)
    
    SelfDeviceWarningDialog(
        device = uiState.pendingBlockDevice,
        onConfirm = { vm.wifiFilterViewModel.confirmAddSelfToDraft() },
        onDismiss = { vm.wifiFilterViewModel.cancelPendingBlock() }
    )

    WifiFilterSelfWarningDialog(
        show = uiState.showWifiFilterSelfWarning,
        onConfirm = { vm.saveWifiFilter(uiState.lastAttemptedFilterBand!!, ignoreWarning = true) },
        onDismiss = vm::dismissWifiFilterSelfWarning,
        onAddSelf = { vm.addSelfMacToFilter(uiState.lastAttemptedFilterBand!!) }
    )

    WifiFilterConfirmDeleteDialog(
        mac = uiState.pendingDeleteMac,
        onConfirm = vm.wifiFilterViewModel::confirmDeleteMac,
        onDismiss = vm.wifiFilterViewModel::cancelDeleteMac
    )
}
