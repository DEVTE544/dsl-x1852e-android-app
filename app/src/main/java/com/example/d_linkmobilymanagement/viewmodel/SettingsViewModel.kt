package com.example.d_linkmobilymanagement.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.d_linkmobilymanagement.R
import com.example.d_linkmobilymanagement.data.model.AppLanguageSettings
import com.example.d_linkmobilymanagement.data.model.AppSettings
import com.example.d_linkmobilymanagement.data.model.remote.UpdateInfo
import com.example.d_linkmobilymanagement.data.repository.RouterRepository
import com.example.d_linkmobilymanagement.data.repository.update.UpdateRepository
import com.example.d_linkmobilymanagement.ui.state.UiEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val languageSettings: AppLanguageSettings = AppLanguageSettings(),
    val isCheckingForUpdate: Boolean = false,
    val updateInfo: UpdateInfo? = null,
    val updateError: String? = null
)

/**
 * SettingsViewModel - Handles app settings management
 */
class SettingsViewModel(
    private val repository: RouterRepository,
    private val updateRepository: UpdateRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events: Flow<UiEvent> = _events.receiveAsFlow()

    init {
        observeSettings()
        observeLanguageSettings()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            repository.observeAppSettings().collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
    }

    private fun observeLanguageSettings() {
        viewModelScope.launch {
            repository.observeLanguageSettings().collect { settings ->
                _uiState.update { it.copy(languageSettings = settings) }
            }
        }
    }

    fun updateLanguage(languageTag: String) {
        viewModelScope.launch {
            repository.updateLanguage(languageTag, true)
        }
    }

    fun updateAutoRefresh(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateAutoRefreshEnabled(enabled)
        }
    }

    fun updateRefreshInterval(seconds: Int) {
        viewModelScope.launch {
            repository.updateRefreshInterval(seconds)
        }
    }

    fun clearAllDeviceMeta() {
        viewModelScope.launch {
            try {
                repository.clearAllDeviceMeta()
                _events.send(UiEvent.ShowMessage(R.string.clear_meta_success))
            } catch (_: Exception) {
                _events.send(UiEvent.ShowMessage(R.string.clear_meta_fail))
            }
        }
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingForUpdate = true, updateError = null) }
            updateRepository.checkForUpdate()
                .onSuccess { info ->
                    _uiState.update { it.copy(isCheckingForUpdate = false, updateInfo = info) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isCheckingForUpdate = false, updateError = error.message) }
                }
        }
    }
}
