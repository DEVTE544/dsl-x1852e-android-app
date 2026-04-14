package com.example.d_linkmobilymanagement.ui.screens.devices

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.d_linkmobilymanagement.R
import com.example.d_linkmobilymanagement.ui.model.DeviceUiModel

@Composable
fun DeviceSettingsScreen(
    paddingValues: PaddingValues,
    device: DeviceUiModel,
    onSave: (String, String) -> Unit
) {
    var customName by remember { mutableStateOf(device.customName) }
    var typeNote by remember { mutableStateOf(device.deviceTypeNote) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.basic_device_info), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.current_name, device.visibleName))
                Text("MAC: ${device.mac}")
                Text("IP: ${device.ip}")
            }
        }

        OutlinedTextField(
            value = customName,
            onValueChange = { customName = it },
            label = { Text(stringResource(R.string.custom_name)) },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        )

        OutlinedTextField(
            value = typeNote,
            onValueChange = { typeNote = it },
            label = { Text(stringResource(R.string.device_type_note)) },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        )

        Spacer(Modifier.weight(1f))
        
        Button(
            onClick = { onSave(customName, typeNote) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(stringResource(R.string.save_changes), fontSize = 16.sp)
        }
    }
}
