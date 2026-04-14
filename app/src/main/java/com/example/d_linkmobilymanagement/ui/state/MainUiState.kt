package com.example.d_linkmobilymanagement.ui.state

import com.example.d_linkmobilymanagement.data.model.AppLanguageSettings
import com.example.d_linkmobilymanagement.data.model.AppSettings
import com.example.d_linkmobilymanagement.data.model.RouterSavedInfo
import com.example.d_linkmobilymanagement.data.network.NetworkStatus
import com.example.d_linkmobilymanagement.ui.model.DeviceUiModel
import com.example.d_linkmobilymanagement.ui.model.LogFilter
import com.example.d_linkmobilymanagement.ui.model.LogUiModel

data class MainUiState(
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val username: String = "",
    val routerInfo: RouterSavedInfo = RouterSavedInfo(),
    val devices: List<DeviceUiModel> = emptyList(),
    val logs: List<LogUiModel> = emptyList(),
    val errorMessage: Int? = null,
    val activeDeviceActionIds: Set<String> = emptySet(),
    val devicesError: Int? = null,
    val consecutiveRefreshFailures: Int = 0,
    val selectedLogFilter: LogFilter = LogFilter.ALL,
    val isInitialLoadDone: Boolean = false,
    val appSettings: AppSettings = AppSettings(),
    val appLanguageSettings: AppLanguageSettings = AppLanguageSettings(),
    val networkStatus: NetworkStatus = NetworkStatus.CONNECTED,
    val showNetworkRecoveryDialog: Boolean = false,
    val isWaitingForExpectedReconnect: Boolean = false,
    val expectedReconnectDeadlineMillis: Long? = null,
    val pendingOperationLabel: Int? = null,
    val pendingBlockDevice: DeviceUiModel? = null,
    val wifiFilter24: WifiFilterUiState = WifiFilterUiState(),
    val wifiFilter5: WifiFilterUiState = WifiFilterUiState(),
    val selectedWifiFilterBand: com.example.d_linkmobilymanagement.data.model.WifiBand = com.example.d_linkmobilymanagement.data.model.WifiBand.BAND_2_4GHZ,
    val showWifiFilterSelfWarning: Boolean = false,
    val lastAttemptedFilterBand: com.example.d_linkmobilymanagement.data.model.WifiBand? = null,
    val pendingDeleteMac: String? = null
)
