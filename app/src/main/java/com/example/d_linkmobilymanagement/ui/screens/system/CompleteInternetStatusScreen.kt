package com.example.d_linkmobilymanagement.ui.screens.system

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Lan
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.d_linkmobilymanagement.R
import com.example.d_linkmobilymanagement.data.model.WanInterfaceStatus
import com.example.d_linkmobilymanagement.ui.screens.system.components.WanDetailsSheet
import com.example.d_linkmobilymanagement.viewmodel.SafeSystemViewModel
import com.example.d_linkmobilymanagement.viewmodel.SafeSystemUiEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompleteInternetStatusScreen(
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(),
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit = {},
    viewModel: SafeSystemViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        // System Header with Back Button
        SystemHeader(
            title = stringResource(R.string.internet_status_title),
            showBackButton = true,
            onBackClick = onBackClick,
            onSettingsClick = onSettingsClick,
            onRefreshClick = { viewModel.refreshSystemData() }
        )

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = paddingValues.calculateBottomPadding() + 16.dp
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Safely extract the list first
            val data = uiState.internetStatusData

            // Content based on state
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.errorMessage != null -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.error_unknown),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = uiState.errorMessage ?: stringResource(R.string.error_unknown),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Button(
                                onClick = { viewModel.onEvent(SafeSystemUiEvent.ClearError) },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text(stringResource(R.string.cancel))
                            }
                        }
                    }
                }

                !data.isNullOrEmpty() -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Text(
                                text = stringResource(R.string.wan_interfaces),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        items(data) { wanInterface ->
                            WanInterfaceCard(
                                wanInterface = wanInterface,
                                onClick = { viewModel.onEvent(SafeSystemUiEvent.OnWanInterfaceClick(wanInterface)) }
                            )
                        }
                    }
                }

                else -> {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.NetworkCheck,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )

                            Text(
                                text = stringResource(R.string.internet_status_title),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = stringResource(R.string.internet_status_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }

    // Details Sheet (The Copy Feature)
    uiState.selectedWanInterface?.let { wan ->
        WanDetailsSheet(
            wanInterface = wan,
            onDismiss = { viewModel.onEvent(SafeSystemUiEvent.DismissWanDetails) }
        )
    }
}

@Composable
private fun WanInterfaceCard(
    wanInterface: WanInterfaceStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isConnected = wanInterface.statusIpv4.equals("Connected", ignoreCase = true) ||
                      wanInterface.statusIpv4.equals("up", ignoreCase = true)
    val statusColor = if (isConnected) Color.Green else Color.Red
    val statusText = if (isConnected) stringResource(R.string.wan_status_connected) else stringResource(R.string.wan_status_disconnected)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = wanInterface.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(statusColor, CircleShape)
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }

            // IP Version
            InfoRow(
                icon = Icons.Outlined.Lan,
                label = stringResource(R.string.wan_ip_version),
                value = wanInterface.ipVersion
            )

            // IP Address
            InfoRow(
                icon = Icons.Outlined.Router,
                label = stringResource(R.string.wan_ip_address),
                value = wanInterface.ipIpv4
            )

            // Gateway
            InfoRow(
                icon = Icons.Outlined.Router,
                label = stringResource(R.string.wan_gateway),
                value = wanInterface.gatewayIpv4
            )

            // DNS Servers
            InfoRow(
                icon = Icons.Outlined.Dns,
                label = stringResource(R.string.wan_dns_servers),
                value = wanInterface.dnsIpv4
            )

            // MAC Address
            InfoRow(
                icon = Icons.Outlined.Computer,
                label = stringResource(R.string.wan_mac_address),
                value = wanInterface.mac
            )

            // Type
            InfoRow(
                icon = Icons.Outlined.Router,
                label = stringResource(R.string.wan_type),
                value = wanInterface.typeIsp
            )
            
            // Tap hint
            Text(
                text = stringResource(R.string.tap_to_copy_details),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.End),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value.ifBlank { "---" },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
