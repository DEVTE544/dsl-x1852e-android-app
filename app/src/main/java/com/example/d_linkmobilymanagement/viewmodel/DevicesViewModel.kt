package com.example.d_linkmobilymanagement.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.d_linkmobilymanagement.R
import com.example.d_linkmobilymanagement.data.local.SelfDeviceHints
import com.example.d_linkmobilymanagement.data.local.SelfDeviceStore
import com.example.d_linkmobilymanagement.data.network.CurrentDeviceNetworkInfoProvider
import com.example.d_linkmobilymanagement.data.network.NetworkMonitor
import com.example.d_linkmobilymanagement.data.network.NetworkStatus
import com.example.d_linkmobilymanagement.data.repository.RouterRepository
import com.example.d_linkmobilymanagement.ui.model.DeviceUiModel
import com.example.d_linkmobilymanagement.ui.model.LogType
import com.example.d_linkmobilymanagement.ui.model.SelfDetectionConfidence
import com.example.d_linkmobilymanagement.ui.state.UiEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DevicesUiState(
    val devices: List<DeviceUiModel> = emptyList(),
    val isRefreshing: Boolean = false,
    val isInitialLoadDone: Boolean = false,
    val devicesError: Int? = null,
    val consecutiveRefreshFailures: Int = 0,
    val activeDeviceActionIds: Set<String> = emptySet(),
    val pendingBlockDevice: DeviceUiModel? = null
)

/**
 * DevicesViewModel - Handles device management (block/unblock, settings, detection)
 */
class DevicesViewModel(
    private val repository: RouterRepository,
    private val selfDeviceStore: SelfDeviceStore,
    private val networkInfoProvider: CurrentDeviceNetworkInfoProvider,
    private val logsViewModel: LogsViewModel,
    private val networkMonitor: NetworkMonitor? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(DevicesUiState())
    val uiState: StateFlow<DevicesUiState> = _uiState.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events: Flow<UiEvent> = _events.receiveAsFlow()

    init {
        observeDevices()
        observeNetworkStatus()
    }

    private fun observeNetworkStatus() {
        if (networkMonitor == null) return
        viewModelScope.launch {
            var lastStatus: NetworkStatus? = null
            networkMonitor.isOnline.collectLatest { status ->
                if (lastStatus == NetworkStatus.DISCONNECTED && status == NetworkStatus.CONNECTED) {
                    // محاكاة الضغط على زر التحديث عند عودة الشبكة
                    delay(1500)
                    refreshDevices(silent = true)
                }
                lastStatus = status
            }
        }
    }

    private fun observeDevices() {
        viewModelScope.launch {
            repository.observeDevices().collectLatest { devices ->
                val enriched = enrichDevicesList(devices)
                _uiState.update {
                    it.copy(
                        devices = enriched,
                        isInitialLoadDone = true
                    )
                }
            }
        }
    }

    fun refreshDevices(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) {
                _uiState.update {
                    it.copy(
                        isRefreshing = true,
                        devicesError = null
                    )
                }
            }

            try {
                repository.refreshDevices()

                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        devicesError = null,
                        consecutiveRefreshFailures = 0
                    )
                }

                if (!silent) {
                    logsViewModel.addLog(LogType.REFRESH, R.string.devices_updated)
                    _events.send(UiEvent.ShowMessage(R.string.devices_updated))
                }
            } catch (e: Exception) {
                val isRecovering = _uiState.value.devicesError == null

                val errorRes = if (isRecovering) null else mapErrorMessage(e)
                val currentFailures = _uiState.value.consecutiveRefreshFailures + 1

                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        devicesError = errorRes,
                        consecutiveRefreshFailures = currentFailures
                    )
                }

                if (!isRecovering && !silent) {
                    _events.send(UiEvent.ShowMessage(errorRes ?: R.string.error_generic))
                }
            }
        }
    }

    fun updateDeviceInfo(deviceId: String, customName: String, deviceType: String) {
        if (deviceId in _uiState.value.activeDeviceActionIds) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    activeDeviceActionIds = it.activeDeviceActionIds + deviceId,
                    isRefreshing = true
                )
            }

            try {
                repository.updateDeviceMeta(deviceId, customName, deviceType)

                _uiState.update {
                    it.copy(
                        activeDeviceActionIds = it.activeDeviceActionIds - deviceId,
                        isRefreshing = false
                    )
                }

                logsViewModel.addLog(LogType.UPDATE_META, R.string.update_device_meta_success, args = arrayOf(customName))
                _events.send(UiEvent.ShowMessage(R.string.save_settings_success))
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        activeDeviceActionIds = it.activeDeviceActionIds - deviceId
                    )
                }
                logsViewModel.addLog(LogType.ERRORS, R.string.error_unknown, false)
                _events.send(UiEvent.ShowMessage(R.string.error_unknown))
            }
        }
    }

    suspend fun getDevice(deviceId: String): DeviceUiModel? {
        return repository.getDevice(deviceId)
    }

    private suspend fun enrichDevicesList(devices: List<DeviceUiModel>): List<DeviceUiModel> {
        val isOnWifi = networkInfoProvider.isOnWifi()
        val currentIp = networkInfoProvider.getCurrentWifiIp()
        val currentSsid = networkInfoProvider.getCurrentSsid()
        val hints = selfDeviceStore.hints.first()

        return devices.map { device ->
            var confidence = SelfDetectionConfidence.NONE
            var isCurrent = false

            if (isOnWifi && currentIp != null && device.ip == currentIp) {
                confidence = SelfDetectionConfidence.HIGH
                isCurrent = true

                if (hints.lastConfirmedRouterMac != device.mac || hints.lastConfirmedSsid != currentSsid) {
                    viewModelScope.launch {
                        selfDeviceStore.saveHints(SelfDeviceHints(
                            lastConfirmedIp = currentIp,
                            lastConfirmedRouterDeviceId = device.id,
                            lastConfirmedRouterMac = device.mac,
                            lastConfirmedSsid = currentSsid
                        ))
                    }
                }
            } else if (currentSsid != null && hints.lastConfirmedSsid == currentSsid) {
                if (device.mac == hints.lastConfirmedRouterMac || device.id == hints.lastConfirmedRouterDeviceId) {
                    confidence = SelfDetectionConfidence.MEDIUM
                    isCurrent = true
                }
            }

            device.copy(
                isCurrentPhone = isCurrent,
                selfDetectionConfidence = confidence
            )
        }
    }

    private fun mapErrorMessage(e: Exception): Int {
        val message = e.message ?: ""
        return when {
            message.contains("ConnectException", true) || message.contains("Unable to resolve host", true) ->
                R.string.error_router_unreachable
            message.contains("Timeout", true) ->
                R.string.error_timeout
            message.contains("401", true) ->
                R.string.error_session_expired
            else -> R.string.error_unknown
        }
    }
}
