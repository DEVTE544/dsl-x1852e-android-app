package com.example.d_linkmobilymanagement.ui.screens.devices

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.d_linkmobilymanagement.R

import com.example.d_linkmobilymanagement.ui.model.DeviceUiModel
import com.example.d_linkmobilymanagement.ui.model.NetworkType
import com.example.d_linkmobilymanagement.ui.utils.normalizeForSearch

enum class DeviceFilter(val labelRes: Int, val icon: ImageVector) {
    ONLINE(R.string.filter_online, Icons.Outlined.CloudDone),
    OFFLINE(R.string.filter_offline, Icons.Outlined.CloudOff),
    WIFI_5G(R.string.filter_5g, Icons.Outlined.Wifi),
    WIFI_4G(R.string.filter_4g, Icons.Outlined.Wifi1Bar),
    ETHERNET(R.string.filter_ethernet, Icons.Outlined.SettingsEthernet),
    NOT_REGISTERED(R.string.filter_not_registered, Icons.Outlined.AppRegistration)
}

fun List<DeviceUiModel>.filterDevices(query: String, selectedFilters: Set<DeviceFilter>): List<DeviceUiModel> {
    val normalizedQuery = query.normalizeForSearch()
    return filter { device ->
        val matchesSearch = if (normalizedQuery.isBlank()) {
            true
        } else {
            device.visibleName.normalizeForSearch().contains(normalizedQuery) ||
            device.customName.normalizeForSearch().contains(normalizedQuery) ||
            device.mac.normalizeForSearch().contains(normalizedQuery) ||
            device.ip.normalizeForSearch().contains(normalizedQuery) ||
            device.deviceTypeNote.normalizeForSearch().contains(normalizedQuery)
        }

        val matchesFilters = if (selectedFilters.isEmpty()) {
            true
        } else {
            val statusFilters = selectedFilters.intersect(setOf(DeviceFilter.ONLINE, DeviceFilter.OFFLINE))
            val typeFilters = selectedFilters.intersect(setOf(DeviceFilter.WIFI_5G, DeviceFilter.WIFI_4G, DeviceFilter.ETHERNET))
            val miscFilters = selectedFilters.intersect(setOf(DeviceFilter.NOT_REGISTERED))

            val statusMatch = statusFilters.isEmpty() || statusFilters.any {
                if (it == DeviceFilter.ONLINE) device.isOnline else !device.isOnline
            }
            val typeMatch = typeFilters.isEmpty() || typeFilters.any {
                when (it) {
                    DeviceFilter.WIFI_5G -> device.networkType == NetworkType.WIFI_5G
                    DeviceFilter.WIFI_4G -> device.networkType == NetworkType.WIFI_2_4G
                    DeviceFilter.ETHERNET -> device.networkType == NetworkType.ETHERNET
                    else -> false
                }
            }
            val miscMatch = miscFilters.isEmpty() || miscFilters.any {
                device.customName.isBlank() && device.deviceTypeNote.isBlank()
            }

            statusMatch && typeMatch && miscMatch
        }

        matchesSearch && matchesFilters
    }
}
