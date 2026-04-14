package com.example.d_linkmobilymanagement.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.d_linkmobilymanagement.R
import com.example.d_linkmobilymanagement.data.model.PreLoginInfo
import com.example.d_linkmobilymanagement.data.remote.RouterParser
import com.example.d_linkmobilymanagement.data.repository.RouterRepository
import com.example.d_linkmobilymanagement.ui.state.UiEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val errorMessage: Int? = null,
    val username: String = ""
)

/**
 * LoginViewModel - Handles authentication and pre-login info
 */
class LoginViewModel(
    private val repository: RouterRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _preLoginInfo = MutableStateFlow<PreLoginInfo?>(null)
    val preLoginInfo: StateFlow<PreLoginInfo?> = _preLoginInfo.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events: Flow<UiEvent> = _events.receiveAsFlow()

    fun fetchPreLoginInfo(ip: String) {
        viewModelScope.launch {
            try {
                val html = repository.fetchLoginPageRaw(ip)
                val parsedInfo = RouterParser.parsePreLoginInfo(html)
                _preLoginInfo.value = parsedInfo
            } catch (_: Exception) {
                _preLoginInfo.value = PreLoginInfo(
                    modelName = "Router not reachable",
                    firmwareVersion = "Check IP address",
                    availableUsers = listOf("SuperAdmin", "admin")
                )
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
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null
                )
            }

            try {
                val savedPassword = if (rememberMe) password else ""
                repository.saveRouterInfo(routerIp, username, savedPassword)
                val ok = repository.login(username, password)

                _uiState.update {
                    it.copy(
                        isLoggedIn = ok,
                        isLoading = false,
                        username = username,
                        errorMessage = if (ok) null else R.string.error_wrong_password
                    )
                }

                if (ok) {
                    _events.send(UiEvent.ShowMessage(R.string.login_success))
                    onSuccess()
                } else {
                    _events.send(UiEvent.ShowMessage(R.string.login_fail))
                }
            } catch (e: Exception) {
                val errorRes = mapErrorMessage(e)
                _uiState.update {
                    it.copy(
                        isLoggedIn = false,
                        isLoading = false,
                        username = username,
                        errorMessage = errorRes
                    )
                }
                _events.send(UiEvent.ShowMessage(errorRes))
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
