package com.example.d_linkmobilymanagement.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.d_linkmobilymanagement.R
import com.example.d_linkmobilymanagement.data.model.SystemMenuItem
import com.example.d_linkmobilymanagement.data.model.WanInterfaceStatus
import com.example.d_linkmobilymanagement.data.repository.RouterRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SystemUiState(
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val systemSummary: String = "",
    val menuItems: List<SystemMenuItem> = emptyList(),
    val internetStatusData: List<WanInterfaceStatus>? = null,
    val errorMessage: String? = null
)

sealed class SystemUiEvent {
    data class OnSearchQueryChanged(val query: String) : SystemUiEvent()
    data class NavigateTo(val route: String) : SystemUiEvent()
    data object FetchInternetStatus : SystemUiEvent()
    data object ClearError : SystemUiEvent()
}

class SystemViewModel(
    private val repository: RouterRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SystemUiState())
    val uiState: StateFlow<SystemUiState> = _uiState.asStateFlow()

    init {
        try {
            loadMenuItems()
            // Don't auto-generate summary to avoid crash when not logged in
            _uiState.update { it.copy(systemSummary = "System Status: Ready") }
        } catch (e: Exception) {
            // Safe initialization
            _uiState.update { 
                it.copy(
                    systemSummary = "System Status: Initializing...",
                    menuItems = emptyList()
                )
            }
        }
    }

    fun onEvent(event: SystemUiEvent) {
        when (event) {
            is SystemUiEvent.OnSearchQueryChanged -> {
                _uiState.update { it.copy(searchQuery = event.query) }
            }
            is SystemUiEvent.NavigateTo -> {
                // Navigation is handled by the UI component
            }
            is SystemUiEvent.FetchInternetStatus -> {
                fetchInternetStatus()
            }
            is SystemUiEvent.ClearError -> {
                _uiState.update { it.copy(errorMessage = null) }
            }
        }
    }

    private fun loadMenuItems() {
        try {
            val menuItems = listOf(
                SystemMenuItem(
                    titleResId = R.string.system_internet_status,
                    iconResId = R.drawable.ic_internet_status,
                    route = "internet_status"
                )
                // Add more system menu items here in the future
            )
            _uiState.update { it.copy(menuItems = menuItems) }
        } catch (e: Exception) {
            // Fallback to empty menu if resource loading fails
            _uiState.update { it.copy(menuItems = emptyList()) }
        }
    }

    private fun generateSystemSummary() {
        // Generate a summary based on current system state
        viewModelScope.launch {
            try {
                val internetStatus = repository.getInternetStatus()
                val connectedCount = internetStatus.count { it.statusIpv4.equals("Connected", ignoreCase = true) }
                val summary = when {
                    connectedCount > 0 -> "System Online - $connectedCount WAN Connection(s) Active"
                    else -> "System Offline - No Active Connections"
                }
                _uiState.update { it.copy(systemSummary = summary) }
            } catch (e: Exception) {
                // Set a default summary when not logged in or error occurs
                _uiState.update { it.copy(systemSummary = "System Status: Login required") }
            }
        }
    }

    private fun fetchInternetStatus() {
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
                generateSystemSummary() // Update summary with fresh data
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = "Please login to router to view internet status",
                        internetStatusData = null
                    )
                }
            }
        }
    }

    fun refreshSystemData() {
        fetchInternetStatus()
    }

    fun getFilteredMenuItems(): List<SystemMenuItem> {
        val query = _uiState.value.searchQuery.trim().lowercase()
        if (query.isEmpty()) {
            return _uiState.value.menuItems
        }
        
        return _uiState.value.menuItems.filter { item ->
            // For now, we'll filter by a simple approach - in a real app,
            // you might want to resolve the string resource and filter by that
            item.route.lowercase().contains(query)
        }
    }
}
