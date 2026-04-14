package com.example.d_linkmobilymanagement.ui.screens.logs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.d_linkmobilymanagement.R
import com.example.d_linkmobilymanagement.ui.components.EmptyState
import com.example.d_linkmobilymanagement.ui.model.LogFilter
import com.example.d_linkmobilymanagement.ui.model.LogType
import com.example.d_linkmobilymanagement.ui.model.LogUiModel
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun LogsScreen(
    paddingValues: PaddingValues,
    logs: List<LogUiModel>,
    selectedFilter: LogFilter,
    onFilterSelected: (LogFilter) -> Unit
) {
    val filteredLogs = remember(logs, selectedFilter) {
        when (selectedFilter) {
            LogFilter.ALL -> logs
            LogFilter.ERRORS -> logs.filter { !it.isSuccess }
            else -> logs.filter { selectedFilter.types?.contains(it.type) == true }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        LogFilterChips(
            selectedFilter = selectedFilter,
            onFilterSelected = onFilterSelected
        )

        if (filteredLogs.isEmpty()) {
            EmptyState(
                message = if (logs.isEmpty()) stringResource(R.string.no_logs) else "لا توجد نتائج لهذا التصنيف",
                icon = Icons.Outlined.History
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredLogs, key = { it.id }) { log ->
                    LogItem(log)
                }
            }
        }
    }
}

@Composable
fun LogFilterChips(
    selectedFilter: LogFilter,
    onFilterSelected: (LogFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LogFilter.entries.forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(stringResource(filter.labelResId)) },
                leadingIcon = if (selectedFilter == filter) {
                    { Icon(Icons.Outlined.Check, null, modifier = Modifier.size(16.dp)) }
                } else null
            )
        }
    }
}

@Composable
fun LogItem(log: LogUiModel) {
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val color = if (log.isSuccess) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (log.isSuccess) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.widthIn(min = 64.dp).padding(end = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = timeFormat.format(log.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Icon(
                        imageVector = when (log.type) {
                            LogType.LOGIN -> Icons.AutoMirrored.Outlined.Login
                            LogType.REFRESH -> Icons.Outlined.Refresh
                            LogType.BLOCK -> Icons.Outlined.Block
                            LogType.UNBLOCK -> Icons.Outlined.CheckCircle
                            LogType.UPDATE_META -> Icons.Outlined.Edit
                            LogType.SYSTEM -> Icons.Outlined.Settings
                            LogType.ERRORS -> Icons.Outlined.ErrorOutline
                            LogType.INFO -> Icons.Outlined.Info
                        },
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (log.isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }
            
            Spacer(Modifier.width(8.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                val typeLabel = when (log.type) {
                    LogType.LOGIN -> stringResource(R.string.filter_login)
                    LogType.REFRESH -> stringResource(R.string.filter_refresh)
                    LogType.BLOCK -> stringResource(R.string.log_type_block)
                    LogType.UNBLOCK -> stringResource(R.string.log_type_unblock)
                    LogType.UPDATE_META -> stringResource(R.string.log_type_update_meta)
                    LogType.SYSTEM -> stringResource(R.string.log_type_system)
                    LogType.ERRORS -> stringResource(R.string.filter_errors)
                    LogType.INFO -> stringResource(R.string.log_type_info)
                }
                Text(
                    text = typeLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = stringResource(log.messageRes, *(log.messageArgs?.toTypedArray() ?: emptyArray())),
                    style = MaterialTheme.typography.bodyMedium,
                    color = color
                )
            }
        }
    }
}
