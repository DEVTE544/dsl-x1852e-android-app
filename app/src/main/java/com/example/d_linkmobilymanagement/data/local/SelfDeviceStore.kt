package com.example.d_linkmobilymanagement.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.selfDeviceDataStore by preferencesDataStore(name = "self_device_prefs")

data class SelfDeviceHints(
    val lastConfirmedIp: String? = null,
    val lastConfirmedRouterDeviceId: String? = null,
    val lastConfirmedRouterMac: String? = null,
    val lastConfirmedSsid: String? = null
)

class SelfDeviceStore(private val context: Context) {
    
    val hints: Flow<SelfDeviceHints> = context.selfDeviceDataStore.data.map { prefs ->
        SelfDeviceHints(
            lastConfirmedIp = prefs[LAST_IP],
            lastConfirmedRouterDeviceId = prefs[LAST_DEVICE_ID],
            lastConfirmedRouterMac = prefs[LAST_MAC],
            lastConfirmedSsid = prefs[LAST_SSID]
        )
    }

    suspend fun saveHints(hints: SelfDeviceHints) {
        context.selfDeviceDataStore.edit { prefs ->
            hints.lastConfirmedIp?.let { prefs[LAST_IP] = it }
            hints.lastConfirmedRouterDeviceId?.let { prefs[LAST_DEVICE_ID] = it }
            hints.lastConfirmedRouterMac?.let { prefs[LAST_MAC] = it }
            hints.lastConfirmedSsid?.let { prefs[LAST_SSID] = it }
        }
    }

    companion object {
        private val LAST_IP = stringPreferencesKey("last_confirmed_ip")
        private val LAST_DEVICE_ID = stringPreferencesKey("last_confirmed_device_id")
        private val LAST_MAC = stringPreferencesKey("last_confirmed_mac")
        private val LAST_SSID = stringPreferencesKey("last_confirmed_ssid")
    }
}
