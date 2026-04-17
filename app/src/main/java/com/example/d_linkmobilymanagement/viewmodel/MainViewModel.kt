package com.example.d_linkmobilymanagement.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.d_linkmobilymanagement.R
import com.example.d_linkmobilymanagement.data.local.SelfDeviceStore
import com.example.d_linkmobilymanagement.data.network.CurrentDeviceNetworkInfoProvider
import com.example.d_linkmobilymanagement.data.network.NetworkMonitor
import com.example.d_linkmobilymanagement.data.repository.RouterRepository
import com.example.d_linkmobilymanagement.data.model.WifiBand
import com.example.d_linkmobilymanagement.data.model.WifiFilterMode
import com.example.d_linkmobilymanagement.data.model.PreLoginInfo
import com.example.d_linkmobilymanagement.ui.model.DeviceUiModel
import com.example.d_linkmobilymanagement.ui.model.LogFilter
import com.example.d_linkmobilymanagement.ui.model.LogType
import com.example.d_linkmobilymanagement.ui.state.MainUiState
import com.example.d_linkmobilymanagement.ui.state.UiEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: RouterRepository,
    private val updateRepository: com.example.d_linkmobilymanagement.data.repository.update.UpdateRepository,
    val networkMonitor: NetworkMonitor,
    val selfDeviceStore: SelfDeviceStore,
    networkInfoProvider: CurrentDeviceNetworkInfoProvider
) : ViewModel() {

    // Feature ViewModels (delegated)
    val logsViewModel = LogsViewModel()
    val loginViewModel = LoginViewModel(repository)
    val devicesViewModel = DevicesViewModel(repository, selfDeviceStore, networkInfoProvider, logsViewModel, networkMonitor)
    val wifiFilterViewModel = WifiFilterViewModel(repository, logsViewModel, networkMonitor)
    val settingsViewModel = SettingsViewModel(repository, updateRepository)
    val networkViewModel = NetworkViewModel(networkMonitor, logsViewModel)

    // Main UI State (aggregated)
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _preLoginInfo = MutableStateFlow<PreLoginInfo?>(null)
    val preLoginInfo = _preLoginInfo.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    private var refreshJob: Job? = null

    // Expose repository for SystemViewModel
    fun getRepository(): RouterRepository = repository

    init {
        observeAllStates()
        forwardChildEvents()
    }

    private fun forwardChildEvents() {
        // Forward events from child ViewModels to main events
        viewModelScope.launch {
            loginViewModel.events.collect { event ->
                _events.emit(event)
            }
        }
        viewModelScope.launch {
            devicesViewModel.events.collect { event ->
                _events.emit(event)
            }
        }
        viewModelScope.launch {
            wifiFilterViewModel.events.collect { event ->
                _events.emit(event)
            }
        }
        viewModelScope.launch {
            settingsViewModel.events.collect { event ->
                _events.emit(event)
            }
        }
    }

    private fun observeAllStates() {
        // Observe router info (for login screen)
        viewModelScope.launch {
            repository.observeRouterInfo().collectLatest { routerInfo ->
                _uiState.update {
                    it.copy(routerInfo = routerInfo)
                }
            }
        }

        // Observe login state
        viewModelScope.launch {
            loginViewModel.uiState.collectLatest { loginState ->
                _uiState.update {
                    it.copy(
                        isLoading = loginState.isLoading,
                        errorMessage = loginState.errorMessage
                    )
                }
            }
        }

        // Observe devices state
        viewModelScope.launch {
            devicesViewModel.uiState.collectLatest { devicesState ->
                _uiState.update {
                    it.copy(
                        devices = devicesState.devices,
                        isRefreshing = devicesState.isRefreshing,
                        isInitialLoadDone = devicesState.isInitialLoadDone,
                        devicesError = devicesState.devicesError,
                        consecutiveRefreshFailures = devicesState.consecutiveRefreshFailures,
                        activeDeviceActionIds = devicesState.activeDeviceActionIds,
                        pendingBlockDevice = devicesState.pendingBlockDevice
                    )
                }
                // Sync devices to WiFi filter ViewModel
                wifiFilterViewModel.setAllDevices(devicesState.devices)
            }
        }

        // Observe WiFi filter state
        viewModelScope.launch {
            wifiFilterViewModel.uiState.collectLatest { wifiFilterState ->
                _uiState.update {
                    it.copy(
                        wifiFilter24 = wifiFilterState.wifiFilter24,
                        wifiFilter5 = wifiFilterState.wifiFilter5,
                        selectedWifiFilterBand = wifiFilterState.selectedWifiFilterBand,
                        showWifiFilterSelfWarning = wifiFilterState.showWifiFilterSelfWarning,
                        lastAttemptedFilterBand = wifiFilterState.lastAttemptedFilterBand,
                        pendingBlockDevice = wifiFilterState.pendingBlockDevice,
                        pendingDeleteMac = wifiFilterState.pendingDeleteMac
                    )
                }
            }
        }

        // Observe logs state
        viewModelScope.launch {
            logsViewModel.uiState.collectLatest { logsState ->
                _uiState.update {
                    it.copy(
                        logs = logsState.logs,
                        selectedLogFilter = logsState.selectedLogFilter
                    )
                }
            }
        }

        // Observe settings state
        viewModelScope.launch {
            settingsViewModel.uiState.collectLatest { settingsState ->
                _uiState.update {
                    it.copy(
                        appSettings = settingsState.settings,
                        appLanguageSettings = settingsState.languageSettings
                    )
                }
            }
        }

        // Observe network state
        viewModelScope.launch {
            networkViewModel.uiState.collectLatest { networkState ->
                _uiState.update {
                    it.copy(
                        networkStatus = networkState.networkStatus,
                        showNetworkRecoveryDialog = networkState.showNetworkRecoveryDialog,
                        isWaitingForExpectedReconnect = networkState.isWaitingForExpectedReconnect,
                        expectedReconnectDeadlineMillis = networkState.expectedReconnectDeadlineMillis,
                        pendingOperationLabel = networkState.pendingOperationLabel
                    )
                }
            }
        }

        // Observe network restoration to auto-refresh current data
        viewModelScope.launch {
            networkViewModel.networkRestoredEvent.collect {
                if (_uiState.value.isLoggedIn) {
                    // Update devices first as WiFi filter depends on them for names/IPs
                    devicesViewModel.refreshDevices(silent = true)
                    
                    // Small delay to ensure devices list is updated in memory before filter reload
                    delay(500)
                    
                    // Reload both bands to be safe, or at least the current one
                    loadWifiFilter(WifiBand.BAND_2_4GHZ)
                    loadWifiFilter(WifiBand.BAND_5GHZ)
                }
            }
        }

        // Observe pre-login info
        viewModelScope.launch {
            loginViewModel.preLoginInfo.collectLatest { info ->
                _preLoginInfo.value = info
            }
        }

        // Observe app settings for auto-refresh
        viewModelScope.launch {
            repository.observeAppSettings().collect { settings ->
                val prevSettings = _uiState.value.appSettings
                _uiState.update { it.copy(appSettings = settings) }

                if (settings.autoRefreshEnabled && _uiState.value.isLoggedIn) {
                    if (!prevSettings.autoRefreshEnabled || settings.refreshIntervalSeconds != prevSettings.refreshIntervalSeconds) {
                        startAutoRefresh()
                    }
                } else {
                    refreshJob?.cancel()
                    refreshJob = null
                }
            }
        }

        // Observe language settings
        viewModelScope.launch {
            repository.observeLanguageSettings().collect { settings ->
                _uiState.update { it.copy(appLanguageSettings = settings) }
            }
        }
    }

    fun login(
        routerIp: String,
        username: String,
        password: String,
        rememberMe: Boolean,
        onSuccess: () -> Unit
    ) {
        loginViewModel.login(routerIp, username, password, rememberMe) {
            _uiState.update { it.copy(logs = emptyList()) }
            logsViewModel.addLog(LogType.LOGIN, R.string.login_success)
            
            // Refresh devices immediately after login to get fresh data with correct IPs
            refreshDevices(silent = true)
            
            if (_uiState.value.appSettings.autoRefreshEnabled) {
                startAutoRefresh()
            }
            onSuccess()
        }
    }

    fun fetchPreLoginInfo(ip: String) {
        loginViewModel.fetchPreLoginInfo(ip)
    }

    fun refreshDevices(silent: Boolean = false) {
        devicesViewModel.refreshDevices(silent)
    }

    fun updateDeviceInfo(deviceId: String, customName: String, deviceType: String) {
        devicesViewModel.updateDeviceInfo(deviceId, customName, deviceType)
    }

    suspend fun getDevice(deviceId: String): DeviceUiModel? {
        return devicesViewModel.getDevice(deviceId)
    }

    fun clearLogs() {
        logsViewModel.clearLogs()
    }

    fun setLogFilter(filter: LogFilter) {
        logsViewModel.setLogFilter(filter)
    }

    fun updateLanguage(languageTag: String) {
        settingsViewModel.updateLanguage(languageTag)
    }

    fun updateAutoRefresh(enabled: Boolean) {
        settingsViewModel.updateAutoRefresh(enabled)
        viewModelScope.launch {
            val res = if (enabled) R.string.auto_refresh_enabled else R.string.auto_refresh_disabled
            logsViewModel.addLog(LogType.REFRESH, res, true)
        }
    }

    fun updateRefreshInterval(seconds: Int) {
        settingsViewModel.updateRefreshInterval(seconds)
        logsViewModel.addLog(
            type = LogType.REFRESH,
            messageRes = R.plurals.refresh_interval_changed,
            args = arrayOf(seconds),
            isSuccess = true,
            isPlural = true,
            pluralQuantity = seconds
        )
    }

    fun clearAllDeviceMeta() {
        settingsViewModel.clearAllDeviceMeta()
    }

    fun setWifiFilterBand(band: WifiBand) {
        wifiFilterViewModel.setWifiFilterBand(band)
    }

    fun loadWifiFilter(band: WifiBand) {
        wifiFilterViewModel.loadWifiFilter(band)
    }

    fun updateWifiFilterMode(band: WifiBand, mode: WifiFilterMode) {
        wifiFilterViewModel.updateWifiFilterMode(band, mode)
    }

    fun addMacToFilter(band: WifiBand, mac: String, deviceName: String? = null) {
        wifiFilterViewModel.addMacToFilter(band, mac, deviceName)
    }

    fun removeMacFromFilter(band: WifiBand, mac: String) {
        wifiFilterViewModel.removeMacFromFilter(band, mac)
    }

    fun saveWifiFilter(band: WifiBand, ignoreWarning: Boolean = false) {
        wifiFilterViewModel.saveWifiFilter(band, ignoreWarning)
    }

    fun dismissWifiFilterSelfWarning() {
        wifiFilterViewModel.dismissWifiFilterSelfWarning()
    }

    fun addDeviceToFilterDraft(device: DeviceUiModel) {
        wifiFilterViewModel.addDeviceToFilterDraft(device)
    }

    fun addSelfMacToFilter(band: WifiBand) {
        wifiFilterViewModel.addSelfMacToFilter(band)
    }

    private fun startAutoRefresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (true) {
                if (_uiState.value.isLoggedIn && _uiState.value.appSettings.autoRefreshEnabled) {
                    devicesViewModel.refreshDevices(silent = true)
                }
                delay(_uiState.value.appSettings.refreshIntervalSeconds * 1000L)
            }
        }
    }

    override fun onCleared() {
        refreshJob?.cancel()
        super.onCleared()
    }
}
