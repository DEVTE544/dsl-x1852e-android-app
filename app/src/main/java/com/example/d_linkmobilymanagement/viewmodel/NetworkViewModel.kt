package com.example.d_linkmobilymanagement.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.d_linkmobilymanagement.R
import com.example.d_linkmobilymanagement.data.network.NetworkMonitor
import com.example.d_linkmobilymanagement.data.network.NetworkStatus
import com.example.d_linkmobilymanagement.ui.model.LogType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
data class NetworkUiState(
    val networkStatus: NetworkStatus = NetworkStatus.CONNECTED,
    val showNetworkRecoveryDialog: Boolean = false,
    val isWaitingForExpectedReconnect: Boolean = false,
    val expectedReconnectDeadlineMillis: Long? = null,
    val pendingOperationLabel: Int? = null
)

/**
 * NetworkViewModel - Handles network status monitoring and recovery
 */
class NetworkViewModel(
    private val networkMonitor: NetworkMonitor,
    private val logsViewModel: LogsViewModel
) : ViewModel() {

    private val _uiState = MutableStateFlow(NetworkUiState())
    val uiState: StateFlow<NetworkUiState> = _uiState.asStateFlow()

    private val _networkRestoredEvent = MutableSharedFlow<Unit>()
    val networkRestoredEvent: SharedFlow<Unit> = _networkRestoredEvent.asSharedFlow()

    init {
        observeNetworkStatus()
    }

    private fun observeNetworkStatus() {
        viewModelScope.launch {
            networkMonitor.isOnline.collectLatest { status ->
                val prevStatus = _uiState.value.networkStatus
                _uiState.update { it.copy(networkStatus = status) }

                if (prevStatus == NetworkStatus.DISCONNECTED && status == NetworkStatus.CONNECTED) {
                    handleNetworkRestored()
                } else if (status == NetworkStatus.DISCONNECTED) {
                    handleNetworkDisconnected()
                }
            }
        }
    }

    private fun handleNetworkDisconnected() {
        logsViewModel.addLog(LogType.ERRORS, R.string.network_disconnected, false)
        _uiState.update { it.copy(showNetworkRecoveryDialog = true) }
    }

    private fun handleNetworkRestored() {
        logsViewModel.addLog(LogType.INFO, R.string.network_restored)

        viewModelScope.launch {
            // ننتظر ثانية واحدة لضمان استقرار مسارات الشبكة (Routing) في أندرويد
            delay(1000)
            _networkRestoredEvent.emit(Unit)
            
            // ننتظر ثانيتين إضافيتين قبل إخفاء رسالة الاستعادة لإعطاء وقت للبيانات لتظهر
            delay(2000)

            if (_uiState.value.showNetworkRecoveryDialog) {
                _uiState.update {
                    it.copy(
                        showNetworkRecoveryDialog = false,
                        isWaitingForExpectedReconnect = false,
                        expectedReconnectDeadlineMillis = null
                    )
                }
            }
        }
    }
}
