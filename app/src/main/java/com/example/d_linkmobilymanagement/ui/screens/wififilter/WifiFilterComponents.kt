package com.example.d_linkmobilymanagement.ui.screens.wififilter

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.d_linkmobilymanagement.R
import com.example.d_linkmobilymanagement.data.model.WifiBand
import com.example.d_linkmobilymanagement.data.model.WifiFilterMode
import com.example.d_linkmobilymanagement.ui.components.EmptyState
import com.example.d_linkmobilymanagement.ui.state.WifiFilterUiState

@Composable
fun WifiFilterContent(
    paddingValues: PaddingValues,
    state: WifiFilterUiState,
    band: WifiBand,
    onBandChange: (WifiBand) -> Unit,
    onModeChange: (WifiBand, WifiFilterMode) -> Unit,
    onAddMac: (WifiBand, String) -> Unit,
    onRemoveMac: (WifiBand, String) -> Unit,
    onSave: (WifiBand) -> Unit,
    onRefresh: (WifiBand) -> Unit,
    onConfirmDelete: () -> Unit = {},
    onCancelDelete: () -> Unit = {}
) {
    val showAddDialog = remember { mutableStateOf(false) }

    if (state.pendingDeleteMac != null) {
        ConfirmDeleteMacDialog(
            mac = state.pendingDeleteMac,
            onConfirm = { onConfirmDelete() },
            onDismiss = { onCancelDelete() }
        )
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = paddingValues.calculateBottomPadding() + 80.dp // Space for bottom buttons
            )
        ) {
            // Band Selection (Part of scroll now)
            item {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SegmentedButton(
                        selected = band == WifiBand.BAND_2_4GHZ,
                        onClick = { onBandChange(WifiBand.BAND_2_4GHZ) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.1f))
                    ) {
                        Text(stringResource(R.string.wifi_filter_24ghz))
                    }
                    SegmentedButton(
                        selected = band == WifiBand.BAND_5GHZ,
                        onClick = { onBandChange(WifiBand.BAND_5GHZ) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.1f))
                    ) {
                        Text(stringResource(R.string.wifi_filter_5ghz))
                    }
                }
            }

            // Mode Selection Card (Part of scroll)
            item {
                WifiFilterModeCard(
                    mode = state.mode,
                    onModeChange = { onModeChange(band, it) }
                )
            }

            // List Header Row (Part of scroll)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.wifi_filter_list_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f, fill = false),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.extraLarge
                        ) {
                            Text(
                                text = "${state.macs.size}/16",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { onRefresh(band) },
                            enabled = !state.isLoading && !state.isSaving
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    Icons.Outlined.Refresh,
                                    contentDescription = stringResource(R.string.refresh),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        FilledTonalButton(
                            onClick = { showAddDialog.value = true },
                            enabled = state.macs.size < 16,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier.heightIn(min = 36.dp)
                        ) {
                            Icon(Icons.Outlined.Add, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.add), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            // List Content
            if (state.macs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyState(
                            message = stringResource(R.string.wifi_filter_empty_list),
                            icon = Icons.Outlined.Shield
                        )
                    }
                }
            } else {
                items(state.devices, key = { it.mac }) { device ->
                    MacListItem(
                        device = device,
                        onRemove = { onRemoveMac(band, device.mac) }
                    )
                }
            }
        }

        // Action Buttons at bottom - Fixed as requested
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .padding(bottom = paddingValues.calculateBottomPadding()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { onSave(band) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    enabled = state.hasChanges && !state.isSaving,
                    shape = MaterialTheme.shapes.medium,
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Outlined.CloudUpload, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.wifi_filter_save_changes),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog.value) {
        AddMacDialog(
            onDismiss = { showAddDialog.value = false },
            onAdd = { mac ->
                onAddMac(band, mac)
                showAddDialog.value = false
            }
        )
    }
}

@Composable
fun WifiFilterModeCard(
    mode: WifiFilterMode,
    onModeChange: (WifiFilterMode) -> Unit
) {
    val containerColor by animateColorAsState(
        targetValue = if (mode == WifiFilterMode.BLOCK) 
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
        else 
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        label = "containerColor"
    )

    val statusColor by animateColorAsState(
        targetValue = if (mode == WifiFilterMode.BLOCK) 
            MaterialTheme.colorScheme.error 
        else 
            MaterialTheme.colorScheme.primary,
        label = "statusColor"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.wifi_filter_mode),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Mode Badge with pulsing dot
                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = CircleShape,
                    modifier = Modifier.heightIn(min = 28.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(statusColor, CircleShape)
                        )
                        Text(
                            text = if (mode == WifiFilterMode.BLOCK) 
                                stringResource(R.string.wifi_filter_mode_block) 
                            else 
                                stringResource(R.string.wifi_filter_mode_allow),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }
            }

            AnimatedContent(
                targetState = mode,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "modeContent"
            ) { targetMode ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = if (targetMode == WifiFilterMode.BLOCK) 
                            stringResource(R.string.wifi_filter_mode_block) 
                        else 
                            stringResource(R.string.wifi_filter_mode_allow),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Text(
                        text = if (targetMode == WifiFilterMode.BLOCK) 
                            stringResource(R.string.wifi_filter_block_desc) 
                        else 
                            stringResource(R.string.wifi_filter_allow_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Quick Toggle Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onModeChange(WifiFilterMode.BLOCK) },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium,
                    colors = if (mode == WifiFilterMode.BLOCK) 
                        ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                            contentColor = MaterialTheme.colorScheme.error
                        ) 
                    else ButtonDefaults.outlinedButtonColors(),
                    border = BorderStroke(
                        1.dp, 
                        if (mode == WifiFilterMode.BLOCK) MaterialTheme.colorScheme.error 
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                ) {
                    Text(stringResource(R.string.wifi_filter_mode_block))
                }
                
                OutlinedButton(
                    onClick = { onModeChange(WifiFilterMode.ALLOW) },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium,
                    colors = if (mode == WifiFilterMode.ALLOW) 
                        ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            contentColor = MaterialTheme.colorScheme.primary
                        ) 
                    else ButtonDefaults.outlinedButtonColors(),
                    border = BorderStroke(
                        1.dp, 
                        if (mode == WifiFilterMode.ALLOW) MaterialTheme.colorScheme.primary 
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                ) {
                    Text(stringResource(R.string.wifi_filter_mode_allow))
                }
            }
        }
    }
}


@Composable
fun MacListItem(
    device: com.example.d_linkmobilymanagement.ui.state.WifiFilterDevice,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (device.isSaved)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.primary.copy(alpha = 0.03f)
        ),
        border = if (!device.isSaved)
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        else
            null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (device.isSaved) 1.dp else 0.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            // Top row: Icon + Name + Remove button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon matching device list style
                Surface(
                    color = if (device.isSaved)
                        MaterialTheme.colorScheme.secondaryContainer
                    else
                        MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (device.isSaved) Icons.Outlined.DevicesOther else Icons.Outlined.AppRegistration,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (device.isSaved)
                                MaterialTheme.colorScheme.onSecondaryContainer
                            else
                                MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Always show name (fallback to MAC if blank)
                    val displayName = device.name.ifBlank { device.mac.uppercase() }
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = if (device.name.isBlank())
                            FontFamily.Monospace
                        else
                            FontFamily.Default,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Outlined.DeleteOutline,
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Second row: MAC address (always shown)
            Spacer(Modifier.height(4.dp))
            Text(
                text = device.mac.uppercase(),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Third row: Draft/Saved label (always shown)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!device.isSaved) {
                    Icon(
                        Icons.Outlined.History,
                        null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.wifi_filter_draft_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        Icons.Outlined.CheckCircle,
                        null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.wifi_filter_saved_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
fun AddMacDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var mac by remember { mutableStateOf("") }
    val isValid = remember(mac) { 
        Regex("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$").matches(mac) 
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.wifi_filter_add_mac)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = mac,
                    onValueChange = { mac = it.uppercase() },
                    placeholder = { Text("AA:BB:CC:11:22:33") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    isError = mac.isNotBlank() && !isValid
                )
                if (mac.isNotBlank() && !isValid) {
                    Text(stringResource(R.string.wifi_filter_invalid_mac), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = { onAdd(mac) }, enabled = isValid) {
                Text(stringResource(R.string.add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun ConfirmDeleteMacDialog(
    mac: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
        title = { Text(stringResource(R.string.delete_confirmation_title)) },
        text = { 
            Column {
                Text(stringResource(R.string.delete_confirmation_message))
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = mac,
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun WifiFilterSelfWarningDialog(
    show: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onAddSelf: () -> Unit
) {
    if (show) {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = { Icon(Icons.Outlined.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.wifi_filter_self_warning_title)) },
            text = { Text(stringResource(R.string.wifi_filter_self_warning_message)) },
            confirmButton = {
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.wifi_filter_confirm_save))
                }
            },
            dismissButton = {
                Column(horizontalAlignment = Alignment.End) {
                    TextButton(onClick = onAddSelf) {
                        Text(stringResource(R.string.wifi_filter_add_self))
                    }
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
        )
    }
}
