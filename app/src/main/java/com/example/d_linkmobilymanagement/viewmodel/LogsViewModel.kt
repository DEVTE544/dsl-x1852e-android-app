package com.example.d_linkmobilymanagement.viewmodel

import androidx.lifecycle.ViewModel
import com.example.d_linkmobilymanagement.ui.model.LogFilter
import com.example.d_linkmobilymanagement.ui.model.LogType
import com.example.d_linkmobilymanagement.ui.model.LogUiModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Date
import java.util.UUID

data class LogsUiState(
    val logs: List<LogUiModel> = emptyList(),
    val selectedLogFilter: LogFilter = LogFilter.ALL
)

/**
 * LogsViewModel - Handles log management and filtering
 */
class LogsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LogsUiState())
    val uiState: StateFlow<LogsUiState> = _uiState.asStateFlow()

    fun addLog(type: LogType, messageRes: Int, isSuccess: Boolean = true, args: Array<Any>? = null) {
        val newLog = LogUiModel(
            id = UUID.randomUUID().toString(),
            timestamp = Date(),
            type = type,
            messageRes = messageRes,
            messageArgs = args?.toList(),
            isSuccess = isSuccess
        )
        _uiState.update {
            it.copy(logs = (listOf(newLog) + it.logs).take(100))
        }
    }

    fun clearLogs() {
        _uiState.update { it.copy(logs = emptyList()) }
    }

    fun setLogFilter(filter: LogFilter) {
        _uiState.update { it.copy(selectedLogFilter = filter) }
    }
}
