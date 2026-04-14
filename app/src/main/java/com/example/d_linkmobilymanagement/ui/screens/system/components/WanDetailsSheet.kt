package com.example.d_linkmobilymanagement.ui.screens.system.components

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
import com.example.d_linkmobilymanagement.data.model.WanInterfaceStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WanDetailsSheet(
    wanInterface: WanInterfaceStatus,
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
                    text = wanInterface.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Status indicator
            val isConnected = wanInterface.statusIpv4.equals("Connected", ignoreCase = true) ||
                              wanInterface.statusIpv4.equals("up", ignoreCase = true)
            val statusColor = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            
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
                        text = if (isConnected) stringResource(R.string.wan_status_connected) 
                               else stringResource(R.string.wan_status_disconnected),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = statusColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Details section
            Text(
                text = "Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Copyable info rows
            CopyableDetailRowWithIcon(
                icon = Icons.Outlined.Lan,
                label = stringResource(R.string.wan_ip_version),
                value = wanInterface.ipVersion
            )
            
            CopyableDetailRowWithIcon(
                icon = Icons.Outlined.Router,
                label = stringResource(R.string.wan_ip_address),
                value = wanInterface.ipIpv4
            )
            
            CopyableDetailRowWithIcon(
                icon = Icons.Outlined.Router,
                label = stringResource(R.string.wan_gateway),
                value = wanInterface.gatewayIpv4
            )
            
            CopyableDetailRowWithIcon(
                icon = Icons.Outlined.Dns,
                label = stringResource(R.string.wan_dns_servers),
                value = wanInterface.dnsIpv4
            )
            
            CopyableDetailRowWithIcon(
                icon = Icons.Outlined.Computer,
                label = stringResource(R.string.wan_mac_address),
                value = wanInterface.mac
            )
            
            CopyableDetailRowWithIcon(
                icon = Icons.Outlined.Router,
                label = stringResource(R.string.wan_type),
                value = wanInterface.typeIsp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Copy All button
            val wanInterfacesLabel = stringResource(R.string.wan_interfaces)
            val ipVersionLabel = stringResource(R.string.wan_ip_version)
            val ipAddressLabel = stringResource(R.string.wan_ip_address)
            val gatewayLabel = stringResource(R.string.wan_gateway)
            val dnsLabel = stringResource(R.string.wan_dns_servers)
            val macLabel = stringResource(R.string.wan_mac_address)
            val typeLabel = stringResource(R.string.wan_type)
            
            OutlinedButton(
                onClick = {
                    val allInfo = buildString {
                        appendLine("${wanInterfacesLabel}: ${wanInterface.name}")
                        appendLine("${ipVersionLabel}: ${wanInterface.ipVersion}")
                        appendLine("${ipAddressLabel}: ${wanInterface.ipIpv4}")
                        appendLine("${gatewayLabel}: ${wanInterface.gatewayIpv4}")
                        appendLine("${dnsLabel}: ${wanInterface.dnsIpv4}")
                        appendLine("${macLabel}: ${wanInterface.mac}")
                        appendLine("${typeLabel}: ${wanInterface.typeIsp}")
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
