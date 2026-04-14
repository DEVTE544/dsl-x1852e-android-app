package com.example.d_linkmobilymanagement.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.d_linkmobilymanagement.R
import com.example.d_linkmobilymanagement.data.model.AppSettings
import com.example.d_linkmobilymanagement.utils.AppConfig

@Composable
fun AppSettingsScreen(
    paddingValues: PaddingValues,
    settings: AppSettings,
    languageSettings: com.example.d_linkmobilymanagement.data.model.AppLanguageSettings,
    isCheckingForUpdate: Boolean,
    updateInfo: com.example.d_linkmobilymanagement.data.model.remote.UpdateInfo?,
    onAutoRefreshChange: (Boolean) -> Unit,
    onRefreshIntervalChange: (Int) -> Unit,
    onLanguageChange: (String) -> Unit,
    onClearDeviceMeta: () -> Unit,
    onClearLogs: () -> Unit,
    onCheckForUpdates: () -> Unit
) {
    val showClearMetaDialog = remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ... (بقيمة الأقسام السابقة كما هي)
        SettingsSection(title = stringResource(R.string.router_settings)) {
            SettingsItem(
                icon = Icons.Outlined.Devices,
                title = stringResource(R.string.clear_device_data),
                subtitle = stringResource(R.string.clear_device_data_summary),
                onClick = { showClearMetaDialog.value = true },
                color = MaterialTheme.colorScheme.error
            )
        }

        SettingsSection(title = stringResource(R.string.app_language)) {
            LanguageSelectionItem(
                currentLanguageTag = languageSettings.selectedLanguageTag,
                isAuto = !languageSettings.hasUserChosenLanguage,
                onLanguageChange = onLanguageChange
            )
        }

        SettingsSection(title = stringResource(R.string.auto_refresh)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.enable_auto_refresh), style = MaterialTheme.typography.bodyLarge)
                    Text(stringResource(R.string.auto_refresh_summary), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                }
                Switch(
                    checked = settings.autoRefreshEnabled,
                    onCheckedChange = onAutoRefreshChange
                )
            }
            
            if (settings.autoRefreshEnabled) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.refresh_interval), style = MaterialTheme.typography.bodyMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(2, 5, 10).forEach { seconds ->
                            FilterChip(
                                selected = settings.refreshIntervalSeconds == seconds,
                                onClick = { onRefreshIntervalChange(seconds) },
                                label = { Text(stringResource(R.string.seconds_unit, seconds)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        SettingsSection(title = stringResource(R.string.data_management)) {
            SettingsItem(
                icon = Icons.Outlined.DeleteSweep,
                title = stringResource(R.string.clear_logs),
                subtitle = stringResource(R.string.clear_logs_summary),
                onClick = onClearLogs,
                color = MaterialTheme.colorScheme.error
            )
        }

        SettingsSection(title = stringResource(R.string.about_app)) {
            SettingsItem(
                icon = Icons.Outlined.Info,
                title = stringResource(R.string.app_version),
                subtitle = AppConfig.APP_FULL_VERSION,
                onClick = { }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)

            val currentContext = androidx.compose.ui.platform.LocalContext.current
            
            SettingsItem(
                icon = if (isCheckingForUpdate) Icons.Outlined.Sync else Icons.Outlined.SystemUpdate,
                title = when {
                    updateInfo?.isUpdateAvailable == true -> stringResource(R.string.update_available, updateInfo.latestTag)
                    else -> stringResource(R.string.check_for_updates)
                },
                subtitle = when {
                    isCheckingForUpdate -> stringResource(R.string.checking_updates)
                    updateInfo?.isUpdateAvailable == true -> stringResource(R.string.download_update)
                    updateInfo != null -> stringResource(R.string.update_not_available)
                    else -> ""
                },
                onClick = {
                    if (updateInfo?.isUpdateAvailable == true) {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(updateInfo.downloadUrl))
                        currentContext.startActivity(intent)
                    } else if (!isCheckingForUpdate) {
                        onCheckForUpdates()
                    }
                },
                color = if (updateInfo?.isUpdateAvailable == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }

    if (showClearMetaDialog.value) {
        AlertDialog(
            onDismissRequest = { showClearMetaDialog.value = false },
            title = { Text(stringResource(R.string.clear_device_data)) },
            text = { Text(stringResource(R.string.clear_device_data_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearDeviceMeta()
                        showClearMetaDialog.value = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearMetaDialog.value = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
