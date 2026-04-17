package com.example.d_linkmobilymanagement.ui.screens.devices

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.d_linkmobilymanagement.R
import com.example.d_linkmobilymanagement.ui.components.*
import com.example.d_linkmobilymanagement.ui.model.DeviceUiModel
import com.example.d_linkmobilymanagement.ui.screens.devices.components.DeviceDetailsSheet
import com.example.d_linkmobilymanagement.ui.screens.system.SystemSearchBar

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DevicesScreen(
    paddingValues: PaddingValues,
    devices: List<DeviceUiModel>,
    isRefreshing: Boolean,
    consecutiveFailures: Int,
    errorMessageRes: Int?,
    activeDeviceActionIds: Set<String>,
    onToggleBlock: (DeviceUiModel) -> Unit,
    onOpenSettings: (String) -> Unit,
    onRetry: () -> Unit
) {
    val selectedDevice = remember { mutableStateOf<DeviceUiModel?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    var isFilterVisible by remember { mutableStateOf(false) }
    var selectedFilters by remember { mutableStateOf(setOf<DeviceFilter>()) }

    val filteredDevices = remember(devices, searchQuery, selectedFilters) {
        devices.filterDevices(searchQuery, selectedFilters)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = paddingValues.calculateTopPadding())
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(
                bottom = paddingValues.calculateBottomPadding() + 16.dp
            )
        ) {
            item {
                if (isRefreshing) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

            item {
                DeviceSummaryCard(
                    isLoading = isRefreshing,
                    onlineCount = devices.count { it.isOnline },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            stickyHeader {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SystemSearchBar(
                            query = searchQuery,
                            onQueryChange = {
                                searchQuery = it
                                if (it.isEmpty()) {
                                    keyboardController?.hide()
                                }
                            },
                            onClearClick = {
                                searchQuery = ""
                                keyboardController?.hide()
                            },
                            hint = stringResource(R.string.device_search_list),
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = {
                                isFilterVisible = !isFilterVisible
                                if (isFilterVisible) keyboardController?.hide()
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (isFilterVisible) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                contentColor = if (isFilterVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Icon(
                                imageVector = if (isFilterVisible) Icons.Outlined.Close else Icons.Outlined.FilterList,
                                contentDescription = "Filter"
                            )
                        }
                    }
                }
            }

            if (isFilterVisible) {
                item {
                    FilterSection(
                        selectedFilters = selectedFilters,
                        onFilterToggle = { filter ->
                            selectedFilters = if (filter in selectedFilters) {
                                selectedFilters - filter
                            } else {
                                selectedFilters + filter
                            }
                        },
                        onDismiss = { isFilterVisible = false },
                        modifier = Modifier.fillParentMaxHeight(0.8f)
                    )
                }
            } else if (consecutiveFailures >= 3 && errorMessageRes != null) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        ErrorCard(
                            messageRes = errorMessageRes, 
                            onRetry = onRetry
                        )
                    }
                }
            } else if (searchQuery.isNotEmpty() && filteredDevices.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillParentMaxHeight(0.6f)
                            .padding(horizontal = 16.dp)
                    ) {
                        EmptySearchState(query = searchQuery)
                    }
                }
            } else if (devices.isEmpty() && !isRefreshing && consecutiveFailures < 3) {
                item {
                    Box(
                        modifier = Modifier
                            .fillParentMaxHeight(0.6f)
                            .padding(horizontal = 16.dp)
                    ) {
                        EmptyState(
                            message = stringResource(R.string.no_connected_devices),
                            icon = Icons.Outlined.Devices
                        )
                    }
                }
            } else {
                items(filteredDevices, key = { it.id }) { device ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        DeviceCard(
                            device = device,
                            actionText = stringResource(R.string.add_to_list),
                            isProcessing = device.id in activeDeviceActionIds,
                            onAction = { onToggleBlock(device) },
                            onSettings = { onOpenSettings(device.id) },
                            onClick = { selectedDevice.value = device }
                        )
                    }
                }
            }
        }
    }

    selectedDevice.value?.let { device ->
        DeviceDetailsSheet(
            device = device,
            onDismiss = { selectedDevice.value = null }
        )
    }
}
