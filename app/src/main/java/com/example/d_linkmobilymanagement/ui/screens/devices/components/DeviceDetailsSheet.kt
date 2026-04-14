package com.example.d_linkmobilymanagement.ui.screens.devices.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.d_linkmobilymanagement.R
import com.example.d_linkmobilymanagement.ui.model.DeviceUiModel
import com.example.d_linkmobilymanagement.ui.model.NetworkType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailsSheet(
    device: DeviceUiModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = device.customName.ifBlank { device.visibleName },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.cancel))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Status indicator
            val statusColor = if (device.isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = statusColor.copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(statusColor, androidx.compose.foundation.shape.CircleShape)
                    )
                    Text(
                        text = if (device.isOnline) stringResource(R.string.connected) else stringResource(R.string.disconnected),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Details section
            Text(
                text = stringResource(R.string.device_details),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Copyable info rows
            CopyableDetailRowWithIcon(
                icon = Icons.Outlined.Person,
                label = stringResource(R.string.name_label),
                value = device.visibleName
            )

            if (device.customName.isNotBlank()) {
                CopyableDetailRowWithIcon(
                    icon = Icons.Outlined.Edit,
                    label = stringResource(R.string.custom_name_label),
                    value = device.customName
                )
            }

            CopyableDetailRowWithIcon(
                icon = Icons.Outlined.Wifi,
                label = stringResource(R.string.network_type_label),
                value = when (device.networkType) {
                    NetworkType.WIFI_2_4G -> "Wi-Fi 2.4GHz"
                    NetworkType.WIFI_5G -> "Wi-Fi 5GHz"
                    NetworkType.ETHERNET -> "Ethernet (LAN)"
                    NetworkType.UNKNOWN -> stringResource(R.string.unknown)
                }
            )

            CopyableDetailRowWithIcon(
                icon = Icons.Outlined.Fingerprint,
                label = stringResource(R.string.mac_label),
                value = device.mac
            )

            CopyableDetailRowWithIcon(
                icon = Icons.Outlined.Public,
                label = stringResource(R.string.ip_label),
                value = device.ip
            )

            if (device.deviceTypeNote.isNotBlank()) {
                CopyableDetailRowWithIcon(
                    icon = Icons.Outlined.Info,
                    label = stringResource(R.string.device_type_label),
                    value = device.deviceTypeNote
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Copy All button
            val labelDeviceName = stringResource(R.string.device_name)
            val labelCustomName = stringResource(R.string.custom_name)
            val labelStatus = stringResource(R.string.status_label)
            val labelDeviceType = stringResource(R.string.device_type_label)
            val statusConnected = stringResource(R.string.connected)
            val statusDisconnected = stringResource(R.string.disconnected)

            OutlinedButton(
                onClick = {
                    val allInfo = buildString {
                        appendLine("${labelDeviceName}: ${device.visibleName}")
                        if (device.customName.isNotBlank()) appendLine("${labelCustomName}: ${device.customName}")
                        appendLine("${labelStatus}: ${if (device.isOnline) statusConnected else statusDisconnected}")
                        val networkText = when (device.networkType) {
                            NetworkType.WIFI_2_4G -> "Wi-Fi 2.4GHz"
                            NetworkType.WIFI_5G -> "Wi-Fi 5GHz"
                            NetworkType.ETHERNET -> "Ethernet (LAN)"
                            NetworkType.UNKNOWN -> "Unknown"
                        }
                        appendLine("Network: $networkText")
                        appendLine("MAC: ${device.mac}")
                        appendLine("IP: ${device.ip}")
                        if (device.deviceTypeNote.isNotBlank()) appendLine("${labelDeviceType}: ${device.deviceTypeNote}")
                    }
                    clipboardManager.setText(AnnotatedString(allInfo))
                    Toast.makeText(context, R.string.copy_all_info, Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.ContentCopy, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.copy_all_info))
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CopyableDetailRowWithIcon(
    icon: ImageVector,
    label: String,
    value: String
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = {
                clipboardManager.setText(AnnotatedString(value))
                Toast.makeText(context, R.string.copy, Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.ContentCopy,
                contentDescription = stringResource(R.string.copy),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
