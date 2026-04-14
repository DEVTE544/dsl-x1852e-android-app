package com.example.d_linkmobilymanagement.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.d_linkmobilymanagement.R
import com.example.d_linkmobilymanagement.data.model.WifiBand
import com.example.d_linkmobilymanagement.data.model.WifiBandFilterState
import com.example.d_linkmobilymanagement.data.model.WifiFilterMode
import com.example.d_linkmobilymanagement.data.repository.RouterRepository
import com.example.d_linkmobilymanagement.ui.model.DeviceUiModel
import com.example.d_linkmobilymanagement.ui.model.LogType
import com.example.d_linkmobilymanagement.ui.state.UiEvent
import com.example.d_linkmobilymanagement.ui.state.WifiFilterDevice
import com.example.d_linkmobilymanagement.ui.state.WifiFilterUiState
import com.example.d_linkmobilymanagement.data.network.NetworkMonitor
import com.example.d_linkmobilymanagement.data.network.NetworkStatus
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

data class WifiFilterViewModelState(
    val wifiFilter24: WifiFilterUiState = WifiFilterUiState(),
    val wifiFilter5: WifiFilterUiState = WifiFilterUiState(),
    val selectedWifiFilterBand: WifiBand = WifiBand.BAND_2_4GHZ,
    val showWifiFilterSelfWarning: Boolean = false,
    val lastAttemptedFilterBand: WifiBand? = null,
    val pendingBlockDevice: DeviceUiModel? = null,
    val allDevices: List<DeviceUiModel> = emptyList(),
    val pendingDeleteMac: String? = null,
    val pendingDeleteBand: WifiBand? = null
)

/**
 * WifiFilterViewModel - Handles WiFi MAC filtering
 */
class WifiFilterViewModel(
    private val repository: RouterRepository,
    private val logsViewModel: LogsViewModel,
    private val networkMonitor: NetworkMonitor? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(WifiFilterViewModelState())
    val uiState: StateFlow<WifiFilterViewModelState> = _uiState.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events: Flow<UiEvent> = _events.receiveAsFlow()

    init {
        observeNetworkStatus()
    }

    private fun observeNetworkStatus() {
        if (networkMonitor == null) return
        viewModelScope.launch {
            var lastStatus: NetworkStatus? = null
            networkMonitor.isOnline.collectLatest { status ->
                if (lastStatus == NetworkStatus.DISCONNECTED && status == NetworkStatus.CONNECTED) {
                    // محاكاة الضغط على زر التحديث عند عودة الشبكة
                    delay(1500) // وقت كافٍ لاستقرار الاتصال
                    loadWifiFilter(_uiState.value.selectedWifiFilterBand)
                }
                lastStatus = status
            }
        }
    }

    fun setAllDevices(devices: List<DeviceUiModel>) {
        _uiState.update { it.copy(allDevices = devices) }
    }

    fun setWifiFilterBand(band: WifiBand) {
        _uiState.update { it.copy(selectedWifiFilterBand = band) }
        loadWifiFilter(band)
    }

    fun loadWifiFilter(band: WifiBand) {
        viewModelScope.launch {
            updateWifiFilterState(band) { it.copy(isLoading = true, error = null) }
            try {
                val state = repository.getWifiFilterState(band)
                updateWifiFilterState(band) { current ->
                    val newOriginalMacs = state.macs.map { it.uppercase().trim() }.filter { it.isNotBlank() }
                    
                    // تحسين منطق الدمج لضمان عدم فقدان المسودات المحلية
                    val additions = current.macs.filter { it.uppercase() !in current.originalMacs.map { m -> m.uppercase() } }
                    val deletions = current.originalMacs.filter { it.uppercase() !in current.macs.map { m -> m.uppercase() } }

                    // القائمة الجديدة = (ما في الراوتر + إضافاتنا) - (ما حذفناه يدوياً)
                    val mergedMacs = (newOriginalMacs + additions)
                        .map { it.uppercase() }
                        .distinct()
                        .filter { it !in deletions.map { d -> d.uppercase() } }
                        .take(16)

                    val allKnownDevices = _uiState.value.allDevices
                    val existingDevicesByMac = current.devices.associateBy { it.mac.uppercase() }
                    
                    val filterDevices = mergedMacs.map { mac ->
                        val macUpper = mac.uppercase()
                        val isActuallySaved = newOriginalMacs.contains(macUpper)
                        val known = allKnownDevices.find { it.mac.equals(mac, ignoreCase = true) }
                        
                        val existingDevice = existingDevicesByMac[macUpper]
                        val resolvedName = existingDevice?.name?.ifBlank { null }
                            ?: known?.customName?.ifBlank { null }
                            ?: known?.visibleName?.ifBlank { null }
                            ?: mac
                            
                        WifiFilterDevice(
                            mac = mac,
                            name = resolvedName,
                            ip = known?.ip ?: existingDevice?.ip ?: "",
                            isSaved = isActuallySaved
                        )
                    }

                    current.copy(
                        mode = if (current.mode == current.originalMode) state.mode else current.mode,
                        macs = mergedMacs,
                        devices = filterDevices,
                        originalMacs = newOriginalMacs,
                        originalMode = state.mode,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                updateWifiFilterState(band) { it.copy(isLoading = false, error = mapErrorMessage(e)) }
            }
        }
    }

    fun updateWifiFilterMode(band: WifiBand, mode: WifiFilterMode) {
        updateWifiFilterState(band) { it.copy(mode = mode) }
    }

    fun addMacToFilter(band: WifiBand, mac: String, deviceName: String? = null): Boolean {
        val normalized = mac.uppercase().trim()
        if (normalized.isEmpty()) return false

        val currentDraft = if (band == WifiBand.BAND_2_4GHZ) _uiState.value.wifiFilter24 else _uiState.value.wifiFilter5

        // القيد: منع الإضافة في حال وجود أي تغييرات معلقة (إضافة أو حذف) لم تُحفظ بعد.
        val hasAdditions = currentDraft.macs.any { it.uppercase().trim() !in currentDraft.originalMacs.map { m -> m.uppercase().trim() } }
        val hasDeletions = currentDraft.originalMacs.any { it.uppercase().trim() !in currentDraft.macs.map { m -> m.uppercase().trim() } }

        if (hasAdditions || hasDeletions) {
            viewModelScope.launch {
                _events.send(UiEvent.ShowMessage(R.string.wifi_filter_pending_changes))
            }
            return false
        }

        if (currentDraft.macs.any { it.equals(normalized, ignoreCase = true) }) {
            viewModelScope.launch {
                _events.send(UiEvent.ShowMessage(R.string.mac_already_exists))
            }
            return false
        }

        val allKnownDevices = _uiState.value.allDevices
        val known = allKnownDevices.find { it.mac.equals(normalized, ignoreCase = true) }
        // Priority: passed name > customName > visibleName > MAC
        val resolvedName = deviceName?.ifBlank { null }
            ?: known?.customName?.ifBlank { null }
            ?: known?.visibleName?.ifBlank { null }
            ?: mac
        val newDevice = WifiFilterDevice(
            mac = normalized,
            name = resolvedName,
            ip = known?.ip ?: "",
            isSaved = false
        )

        updateWifiFilterState(band) { state ->
            if (state.macs.size < 16) {
                val updatedDevices = state.devices
                    .filter { !it.mac.equals(normalized, ignoreCase = true) }
                    .plus(newDevice)

                Timber.d("addMacToFilter: mac=$normalized, name=$resolvedName, devicesBefore=${state.devices.size}, devicesAfter=${updatedDevices.size}")
                
                state.copy(
                    macs = (state.macs + normalized).distinct(),
                    devices = updatedDevices
                )
            } else {
                viewModelScope.launch {
                    _events.send(UiEvent.ShowMessage(R.string.wifi_filter_max_reached))
                }
                state
            }
        }
        return true
    }

    fun saveWifiFilter(band: WifiBand, ignoreWarning: Boolean = false) {
        val state = if (band == WifiBand.BAND_2_4GHZ) _uiState.value.wifiFilter24 else _uiState.value.wifiFilter5

        if (!ignoreWarning && state.mode == WifiFilterMode.ALLOW) {
            val currentDevice = _uiState.value.allDevices.find { it.isCurrentPhone }
            val currentMac = currentDevice?.mac?.uppercase()

            if (currentMac != null && !state.macs.map { it.uppercase() }.contains(currentMac)) {
                _uiState.update { it.copy(showWifiFilterSelfWarning = true, lastAttemptedFilterBand = band) }
                return
            }
        }

        _uiState.update { it.copy(showWifiFilterSelfWarning = false, lastAttemptedFilterBand = null) }

        viewModelScope.launch {
            updateWifiFilterState(band) { it.copy(isSaving = true, error = null) }
            try {
                // نأخذ لقطة للحالة التي سنقوم بحفظها
                val macsToSave = state.macs.toList()
                val modeToSave = state.mode

                repository.saveWifiFilterState(band, WifiBandFilterState(modeToSave, macsToSave))

                // انتظر قليلاً ثم جلب الحالة للتأكد (مع العلم أن الراوتر قد يتأخر)
                delay(1000)
                val newState = repository.getWifiFilterState(band)

                updateWifiFilterState(band) { current ->
                    val newOriginalMacs = newState.macs.map { it.uppercase().trim() }.filter { it.isNotBlank() }
                    
                    // بعد الحفظ، نعتبر أن ما أرسلناه هو الـ originalMacs الجديد مؤقتاً 
                    // حتى لو لم يقم الراوتر بتحديث استجابته بعد.
                    // هذا يمنع ظهور زر "حفظ" مرة أخرى فوراً أو اختفاء الأجهزة.

                    val finalMacs = if (newOriginalMacs.containsAll(macsToSave.map { it.uppercase() })) {
                        newOriginalMacs
                    } else {
                        macsToSave.map { it.uppercase() }
                    }

                    current.copy(
                        isSaving = false,
                        originalMacs = finalMacs,
                        originalMode = modeToSave,
                        macs = finalMacs,
                        mode = modeToSave,
                        devices = finalMacs.map { mac ->
                            val macUpper = mac.uppercase()
                            val known = _uiState.value.allDevices.find { d -> d.mac.equals(mac, ignoreCase = true) }
                            val existingName = current.devices.find { d -> d.mac.uppercase() == macUpper }?.name?.ifBlank { null }
                            val resolvedName = existingName
                                ?: known?.customName?.ifBlank { null }
                                ?: known?.visibleName?.ifBlank { null }
                                ?: mac
                            WifiFilterDevice(
                                mac = mac,
                                name = resolvedName,
                                ip = known?.ip ?: "",
                                isSaved = true
                            )
                        }
                    )
                }
                logsViewModel.addLog(LogType.UPDATE_META, R.string.save_settings_success)
                _events.send(UiEvent.ShowMessage(R.string.save_settings_success))
            } catch (e: Exception) {
                updateWifiFilterState(band) { it.copy(isSaving = false, error = mapErrorMessage(e)) }
                logsViewModel.addLog(LogType.ERRORS, mapErrorMessage(e), false)
                _events.send(UiEvent.ShowMessage(mapErrorMessage(e)))
            }
        }
    }

    fun dismissWifiFilterSelfWarning() {
        _uiState.update { it.copy(showWifiFilterSelfWarning = false, lastAttemptedFilterBand = null) }
    }

    fun addSelfMacToFilter(band: WifiBand) {
        val currentDevice = _uiState.value.allDevices.find { it.isCurrentPhone }
        val mac = currentDevice?.mac ?: return
        addMacToFilter(band, mac)
        _uiState.update { it.copy(showWifiFilterSelfWarning = false) }
    }

    fun addDeviceToFilterDraft(device: DeviceUiModel) {
        viewModelScope.launch {
            Timber.d("addDeviceToFilterDraft: id=${device.id}, mac=${device.mac}, customName=${device.customName}, visibleName=${device.visibleName}, networkType=${device.networkType}")
            // Ensure the device is in allDevices list (sync for name resolution)
            val existingDevice = _uiState.value.allDevices.find { it.id.equals(device.id, ignoreCase = true) }
            if (existingDevice == null) {
                val updatedDevices = _uiState.value.allDevices + device
                _uiState.update { it.copy(allDevices = updatedDevices) }
            }

            // Use the device object directly (has correct name from DevicesScreen)
            val resolvedDevice = existingDevice ?: device
            val isCurrentPhone = resolvedDevice.isCurrentPhone

            val resolvedName = resolvedDevice.customName.ifBlank { resolvedDevice.visibleName }.ifBlank { resolvedDevice.mac }

            val band = when (resolvedDevice.networkType.toString().uppercase()) {
                "WIFI_5G" -> WifiBand.BAND_5GHZ
                else -> WifiBand.BAND_2_4GHZ
            }

            // 1. تحميل حالة الفلتر من الراوتر إذا لم تكن محملة
            val currentDraft = if (band == WifiBand.BAND_2_4GHZ) _uiState.value.wifiFilter24 else _uiState.value.wifiFilter5
            
            // إذا لم تكن محملة، قم بتحميلها مع الحفاظ على التغييرات المحلية
            if (currentDraft.originalMacs.isEmpty() && !currentDraft.isLoading) {
                try {
                    // حفظ التغييرات المحلية الحالية
                    val localMode = currentDraft.mode

                    val state = repository.getWifiFilterState(band)
                    updateWifiFilterState(band) { current ->
                        val newOriginalMacs = state.macs.map { it.uppercase() }
                        
                        val additions = current.macs.filter { it.uppercase() !in current.originalMacs.map { m -> m.uppercase() } }
                        val deletions = current.originalMacs.filter { it.uppercase() !in current.macs.map { m -> m.uppercase() } }

                        // دمج التغييرات المحلية مع القائمة من الراوتر
                        val mergedMacs = (newOriginalMacs + additions)
                            .map { it.uppercase() }
                            .distinct()
                            .filter { it !in deletions.map { d -> d.uppercase() } }
                            .take(16)

                        val allKnownDevices = _uiState.value.allDevices
                        // Preserve existing names from current draft first
                        val existingDevicesByMac = current.devices.associateBy { it.mac.uppercase() }
                        val filterDevices = mergedMacs.map { mac ->
                            val macUpper = mac.uppercase()
                            val known = allKnownDevices.find { it.mac.equals(mac, ignoreCase = true) }
                            // Priority: existing draft name > customName > visibleName > MAC
                            val resolvedName = existingDevicesByMac[macUpper]?.name?.ifBlank { null }
                                ?: known?.customName?.ifBlank { null }
                                ?: known?.visibleName?.ifBlank { null }
                                ?: mac
                            WifiFilterDevice(
                                mac = mac,
                                name = resolvedName,
                                ip = known?.ip ?: "",
                                isSaved = newOriginalMacs.any { it.equals(mac, ignoreCase = true) }
                            )
                        }
                        
                        current.copy(
                            mode = if (localMode == current.originalMode) state.mode else localMode,
                            macs = mergedMacs,
                            devices = filterDevices,
                            originalMacs = newOriginalMacs,
                            originalMode = state.mode,
                            isLoading = false
                        )
                    }
                } catch (e: Exception) {
                    updateWifiFilterState(band) { it.copy(isLoading = false, error = mapErrorMessage(e)) }
                    _events.send(UiEvent.ShowMessage(mapErrorMessage(e)))
                    return@launch
                }
            }

            // 2. التحقق مما إذا كان الجهاز هو الجهاز الحالي وتحذير المستخدم
            val latestState = if (band == WifiBand.BAND_2_4GHZ) _uiState.value.wifiFilter24 else _uiState.value.wifiFilter5

            if (isCurrentPhone && latestState.mode == WifiFilterMode.BLOCK) {
                // تحذير: المستخدم يحاول إضافة نفسه للقائمة السوداء
                _uiState.update { it.copy(pendingBlockDevice = resolvedDevice) }
                return@launch
            }

            // 3. التحقق مما إذا كان الجهاز موجوداً بالفعل في القائمة
            val normalizedMac = resolvedDevice.mac.uppercase().trim()
            if (latestState.macs.any { it.equals(normalizedMac, ignoreCase = true) }) {
                _events.send(UiEvent.ShowMessage(R.string.mac_already_exists))
                return@launch
            }

            // 4. إضافة الجهاز إلى المسودة والتحقق من النجاح
            val addedSuccessfully = addMacToFilter(band, resolvedDevice.mac, resolvedName)
            
            if (addedSuccessfully) {
                logsViewModel.addLog(
                    type = if (latestState.mode == WifiFilterMode.BLOCK) LogType.BLOCK else LogType.UNBLOCK,
                    messageRes = R.string.add_to_list_success,
                    args = arrayOf(resolvedName)
                )
                _events.send(UiEvent.ShowMessage(R.string.add_to_list_success, listOf(resolvedName)))
            }
        }
    }

    fun confirmAddSelfToDraft() {
        val device = _uiState.value.pendingBlockDevice ?: return
        val band = when (device.networkType.toString().uppercase()) {
            "WIFI_5G" -> WifiBand.BAND_5GHZ
            else -> WifiBand.BAND_2_4GHZ
        }
        val resolvedDeviceName = device.customName.ifBlank { device.visibleName }.ifBlank { device.mac }
        _uiState.update { it.copy(pendingBlockDevice = null) }
        addMacToFilter(band, device.mac, resolvedDeviceName)
        logsViewModel.addLog(
            type = LogType.BLOCK,
            messageRes = R.string.add_to_list_success,
            args = arrayOf(resolvedDeviceName)
        )
        viewModelScope.launch {
            _events.send(UiEvent.ShowMessage(R.string.add_to_list_success, listOf(resolvedDeviceName)))
        }
    }

    fun removeMacFromFilter(band: WifiBand, mac: String) {
        val currentDraft = if (band == WifiBand.BAND_2_4GHZ) _uiState.value.wifiFilter24 else _uiState.value.wifiFilter5
        val macUpper = mac.uppercase().trim()
        
        // تحديد ما إذا كان الجهاز "مسودة" (تمت إضافته ولم يُحفظ)
        val isDraftAddition = macUpper !in currentDraft.originalMacs.map { it.uppercase().trim() }

        if (!isDraftAddition) {
            // الجهاز محفوظ فعلياً في الراوتر، نطبق قيود الحذف
            val hasAdditions = currentDraft.macs.any { it.uppercase().trim() !in currentDraft.originalMacs.map { m -> m.uppercase().trim() } }
            val hasDeletions = currentDraft.originalMacs.any { it.uppercase().trim() !in currentDraft.macs.map { m -> m.uppercase().trim() } }

            if (hasAdditions || hasDeletions) {
                viewModelScope.launch {
                    _events.send(UiEvent.ShowMessage(R.string.wifi_filter_pending_changes))
                }
                return
            }

            // إظهار حوار التأكيد للحذف المحفوظ
            _uiState.update { it.copy(pendingDeleteMac = mac, pendingDeleteBand = band) }
            updateWifiFilterState(band) { it.copy(pendingDeleteMac = mac) }
            return
        }

        // تنفيذ الحذف فوراً للمسودات
        performDeletion(band, mac)
    }

    fun confirmDeleteMac() {
        val mac = _uiState.value.pendingDeleteMac ?: return
        val band = _uiState.value.pendingDeleteBand ?: return
        
        _uiState.update { it.copy(pendingDeleteMac = null, pendingDeleteBand = null) }
        updateWifiFilterState(band) { it.copy(pendingDeleteMac = null) }
        performDeletion(band, mac)
    }

    fun cancelDeleteMac() {
        val band = _uiState.value.pendingDeleteBand ?: return
        _uiState.update { it.copy(pendingDeleteMac = null, pendingDeleteBand = null) }
        updateWifiFilterState(band) { it.copy(pendingDeleteMac = null) }
    }

    private fun performDeletion(band: WifiBand, mac: String) {
        val macUpper = mac.uppercase().trim()
        val deviceName = _uiState.value.allDevices.find { it.mac.equals(mac, ignoreCase = true) }
            ?.let { it.customName.ifEmpty { it.visibleName } } ?: mac
            
        updateWifiFilterState(band) { state ->
            state.copy(
                macs = state.macs.filter { it.uppercase().trim() != macUpper },
                devices = state.devices.filter { it.mac.uppercase().trim() != macUpper }
            )
        }
        
        logsViewModel.addLog(
            type = LogType.UPDATE_META,
            messageRes = R.string.save_settings_success,
            args = arrayOf(deviceName)
        )
    }

    fun cancelPendingBlock() {
        _uiState.update { it.copy(pendingBlockDevice = null) }
    }

    private fun updateWifiFilterState(band: WifiBand, transform: (WifiFilterUiState) -> WifiFilterUiState) {
        _uiState.update {
            if (band == WifiBand.BAND_2_4GHZ) {
                it.copy(wifiFilter24 = transform(it.wifiFilter24))
            } else {
                it.copy(wifiFilter5 = transform(it.wifiFilter5))
            }
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
