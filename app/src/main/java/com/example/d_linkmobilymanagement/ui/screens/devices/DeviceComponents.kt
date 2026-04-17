package com.example.d_linkmobilymanagement.ui.screens.devices

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.d_linkmobilymanagement.R
import com.example.d_linkmobilymanagement.ui.model.DeviceUiModel
import com.example.d_linkmobilymanagement.ui.model.NetworkType
import com.example.d_linkmobilymanagement.ui.model.SelfDetectionConfidence

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterSection(
    selectedFilters: Set<DeviceFilter>,
    onFilterToggle: (DeviceFilter) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = false) {}, // Prevent clicks from passing through
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.filter_devices),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (selectedFilters.isNotEmpty()) {
                        TextButton(onClick = { 
                            DeviceFilter.entries.forEach { if (it in selectedFilters) onFilterToggle(it) }
                        }) {
                            Text(stringResource(R.string.wifi_filter_reset))
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DeviceFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = filter in selectedFilters,
                            onClick = { onFilterToggle(filter) },
                            label = { 
                                Text(
                                    text = stringResource(filter.labelRes),
                                    style = MaterialTheme.typography.bodyMedium
                                ) 
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = filter.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = if (filter in selectedFilters) {
                                {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            modifier = Modifier.heightIn(min = 40.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTrailingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = filter in selectedFilters,
                                borderColor = MaterialTheme.colorScheme.outlineVariant,
                                selectedBorderColor = MaterialTheme.colorScheme.primary,
                                borderWidth = 1.dp,
                                selectedBorderWidth = 1.dp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SelfDeviceWarningDialog(
    device: DeviceUiModel?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (device != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = { Icon(Icons.Outlined.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.wifi_filter_self_warning_title)) },
            text = { 
                Text(
                    stringResource(
                        R.string.wifi_filter_self_warning_message
                    )
                ) 
            },
            confirmButton = {
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.wifi_filter_confirm_save))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun DeviceCard(
    device: DeviceUiModel,
    actionText: String,
    isProcessing: Boolean,
    onAction: () -> Unit,
    onSettings: (() -> Unit)?,
    onClick: () -> Unit
) {
    val isConnected = device.isOnline
    val neonColor = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val neonAlpha = if (isConnected) 0.4f else 0.15f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(
            width = 1.5.dp,
            color = neonColor.copy(alpha = neonAlpha)
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = if (isConnected) 0.95f else 0.85f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header row with device icon and name
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Device icon based on type
                val deviceIcon = if (device.isCurrentPhone) Icons.Outlined.Smartphone
                                else Icons.Outlined.Devices

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = deviceIcon,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = device.customName.ifBlank { device.visibleName },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (device.deviceTypeNote.isNotBlank()) {
                        Text(
                            text = device.deviceTypeNote,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Badges row - all badges below device name (RTL: aligned to end)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Network type chip
                    Surface(
                        color = when (device.networkType) {
                            NetworkType.WIFI_2_4G -> MaterialTheme.colorScheme.tertiaryContainer
                            NetworkType.WIFI_5G -> MaterialTheme.colorScheme.secondaryContainer
                            NetworkType.ETHERNET -> MaterialTheme.colorScheme.primaryContainer
                            NetworkType.UNKNOWN -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = when (device.networkType) {
                                NetworkType.WIFI_2_4G -> "2.4G"
                                NetworkType.WIFI_5G -> "5G"
                                NetworkType.ETHERNET -> "LAN"
                                NetworkType.UNKNOWN -> "Unknown"
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = when (device.networkType) {
                                NetworkType.WIFI_2_4G -> MaterialTheme.colorScheme.onTertiaryContainer
                                NetworkType.WIFI_5G -> MaterialTheme.colorScheme.onSecondaryContainer
                                NetworkType.ETHERNET -> MaterialTheme.colorScheme.onPrimaryContainer
                                NetworkType.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    
                    // "This device" badge
                    if (device.isCurrentPhone) {
                        SelfDeviceBadge(confidence = device.selfDetectionConfidence)
                    }
                    
                    // Status indicator with dot
                    Surface(
                        color = if (isConnected) 
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else 
                            MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                        shape = CircleShape
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(
                                        if (isConnected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.error,
                                        CircleShape
                                    )
                            )
                            Text(
                                text = if (isConnected) stringResource(R.string.connected) 
                                      else stringResource(R.string.disconnected),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isConnected) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // Divider
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                thickness = 0.5.dp,
                color = neonColor.copy(alpha = neonAlpha * 0.5f)
            )

            // Network info row (MAC & IP)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // MAC Address
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Outlined.Fingerprint,
                        null,
                        modifier = Modifier.size(16.dp),
                        tint = neonColor
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "MAC",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            device.mac,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // IP Address
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Outlined.Public,
                        null,
                        modifier = Modifier.size(16.dp),
                        tint = neonColor
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "IP",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            device.ip,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onAction,
                    enabled = !isProcessing,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            text = actionText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (onSettings != null) {
                    OutlinedButton(
                        onClick = onSettings,
                        enabled = !isProcessing,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, neonColor.copy(alpha = neonAlpha)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.settings),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SelfDeviceBadge(confidence: SelfDetectionConfidence) {
    val alpha = when (confidence) {
        SelfDetectionConfidence.HIGH -> 1.0f
        SelfDetectionConfidence.MEDIUM -> 0.7f
        SelfDetectionConfidence.LOW -> 0.4f
        else -> 0f
    }
    
    if (alpha > 0) {
        Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            shape = CircleShape,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = alpha))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Smartphone,
                    null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = alpha)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.this_device),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun EmptySearchState(query: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
            )
            Text(
                text = stringResource(R.string.system_no_results, query),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.system_no_results_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
