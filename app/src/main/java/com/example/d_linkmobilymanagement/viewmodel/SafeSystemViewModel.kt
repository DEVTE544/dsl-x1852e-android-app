package com.example.d_linkmobilymanagement.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.d_linkmobilymanagement.data.model.SystemMenuItem
import com.example.d_linkmobilymanagement.data.model.WanInterfaceStatus
import com.example.d_linkmobilymanagement.data.repository.RouterRepository
import com.example.d_linkmobilymanagement.data.network.NetworkMonitor
import com.example.d_linkmobilymanagement.data.network.NetworkStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

data class SafeSystemUiState(
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val systemSummary: String = "System Status: Ready",
    val activeWanCount: Int = 0,
    val menuItems: List<SystemMenuItem> = emptyList(),
    val internetStatusData: List<WanInterfaceStatus>? = null,
    val errorMessage: String? = null,
    val selectedWanInterface: WanInterfaceStatus? = null
)

sealed class SafeSystemUiEvent {
    data class OnSearchQueryChanged(val query: String) : SafeSystemUiEvent()
    data class NavigateTo(val route: String) : SafeSystemUiEvent()
    data class OnWanInterfaceClick(val wan: WanInterfaceStatus) : SafeSystemUiEvent()
    data object FetchInternetStatus : SafeSystemUiEvent()
    data object ClearError : SafeSystemUiEvent()
    data object DismissWanDetails : SafeSystemUiEvent()
}

class SafeSystemViewModel(
    private val repository: RouterRepository?,
    private val networkMonitor: NetworkMonitor? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(SafeSystemUiState())
    val uiState: StateFlow<SafeSystemUiState> = _uiState.asStateFlow()

    init {
        loadMenuItems()
        fetchInternetStatus()
        observeNetworkStatus()
    }

    private fun observeNetworkStatus() {
        if (networkMonitor == null) return
        viewModelScope.launch {
            var lastStatus: NetworkStatus? = null
            networkMonitor.isOnline.collectLatest { status ->
                if (lastStatus == NetworkStatus.DISCONNECTED && status == NetworkStatus.CONNECTED) {
                    // Network restored, refresh data
                    fetchInternetStatus()
                }
                lastStatus = status
            }
        }
    }

    fun onEvent(event: SafeSystemUiEvent) {
        when (event) {
            is SafeSystemUiEvent.OnSearchQueryChanged -> {
                _uiState.update { it.copy(searchQuery = event.query) }
            }
            is SafeSystemUiEvent.NavigateTo -> {
                // Navigation is handled by the UI component
            }
            is SafeSystemUiEvent.FetchInternetStatus -> {
                fetchInternetStatus()
            }
            is SafeSystemUiEvent.ClearError -> {
                _uiState.update { it.copy(errorMessage = null) }
            }
            is SafeSystemUiEvent.OnWanInterfaceClick -> {
                _uiState.update { it.copy(selectedWanInterface = event.wan) }
            }
            is SafeSystemUiEvent.DismissWanDetails -> {
                _uiState.update { it.copy(selectedWanInterface = null) }
            }
        }
    }

    private fun loadMenuItems() {
        try {
            val menuItems = listOf(
                SystemMenuItem(
                    titleResId = com.example.d_linkmobilymanagement.R.string.system_internet_status,
                    iconResId = com.example.d_linkmobilymanagement.R.drawable.ic_internet_status,
                    route = "internet_status",
                    relatedKeywordsResIds = listOf(
                        com.example.d_linkmobilymanagement.R.string.wan_ip_version,
                        com.example.d_linkmobilymanagement.R.string.wan_ip_address,
                        com.example.d_linkmobilymanagement.R.string.wan_gateway,
                        com.example.d_linkmobilymanagement.R.string.wan_dns_servers,
                        com.example.d_linkmobilymanagement.R.string.wan_mac_address,
                        com.example.d_linkmobilymanagement.R.string.wan_type
                    )
                )
            )
            _uiState.update { it.copy(menuItems = menuItems) }
        } catch (_: Exception) {
            _uiState.update { it.copy(menuItems = emptyList()) }
        }
    }

    private fun fetchInternetStatus() {
        if (repository == null) {
            _uiState.update { 
                it.copy(
                    errorMessage = "Repository not available",
                    systemSummary = "System Status: Error"
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            try {
                val internetStatus = repository.getInternetStatus()
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        internetStatusData = internetStatus
                    )
                }
                updateSummary(internetStatus)
            } catch (e: Exception) {
                Timber.e(e, "Error fetching internet status")
                Timber.e("Error details: ${e.message}")
                Timber.e("Error type: ${e.javaClass.simpleName}")
                
                val errorMessage = when {
                    e.message?.contains("401", ignoreCase = true) == true -> "Authentication failed - Please login again"
                    e.message?.contains("403", ignoreCase = true) == true -> "Access denied - Please check permissions"
                    e.message?.contains("404", ignoreCase = true) == true -> "Page not found - Router may not support this feature"
                    e.message?.contains("timeout", ignoreCase = true) == true -> "Connection timeout - Please check network connection"
                    e.message?.contains("UnknownHost", ignoreCase = true) == true -> "Router not found - Please check IP address"
                    e.message?.contains("NetworkOnMainThreadException", ignoreCase = true) == true -> "Network error - Please try again"
                    else -> "Please login to router to view internet status"
                }
                
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = errorMessage,
                        internetStatusData = null
                    )
                }
            }
        }
    }

    private fun updateSummary(internetStatus: List<WanInterfaceStatus>) {
        try {
            val connectedCount = internetStatus.count { 
                it.statusIpv4.equals("Connected", ignoreCase = true) || 
                it.statusIpv4.equals("up", ignoreCase = true) 
            }
            val summary = when {
                connectedCount > 0 -> "System Online - $connectedCount WAN Connection(s) Active"
                else -> "System Offline - No Active Connections"
            }
            _uiState.update { it.copy(systemSummary = summary, activeWanCount = connectedCount) }
        } catch (_: Exception) {
            _uiState.update { it.copy(systemSummary = "System Status: Error", activeWanCount = 0) }
        }
    }

    fun refreshSystemData() {
        fetchInternetStatus()
    }

    companion object {
        fun provideFactory(
            repository: RouterRepository?,
            networkMonitor: NetworkMonitor?
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SafeSystemViewModel(repository, networkMonitor)
            }
        }
    }

}
