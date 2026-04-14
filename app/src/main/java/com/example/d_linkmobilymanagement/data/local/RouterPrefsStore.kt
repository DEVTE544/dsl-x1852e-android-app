package com.example.d_linkmobilymanagement.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.d_linkmobilymanagement.data.model.RouterSavedInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.routerPrefsDataStore by preferencesDataStore(name = "router_prefs")

class RouterPrefsStore(private val context: Context) {

    private object Keys {
        val ROUTER_IP = stringPreferencesKey("router_ip")
        val USERNAME = stringPreferencesKey("router_username")
        val PASSWORD = stringPreferencesKey("router_password")
    }

    val routerInfo: Flow<RouterSavedInfo> =
        context.routerPrefsDataStore.data.map { prefs ->
            RouterSavedInfo(
                ip = prefs[Keys.ROUTER_IP] ?: "192.168.1.1",
                username = prefs[Keys.USERNAME] ?: "SuperAdmin",
                password = prefs[Keys.PASSWORD] ?: ""
            )
        }

    suspend fun saveRouterInfo(ip: String, username: String, password: String = "") {
        context.routerPrefsDataStore.edit { prefs ->
            prefs[Keys.ROUTER_IP] = ip.trim()
            prefs[Keys.USERNAME] = username.trim()
            prefs[Keys.PASSWORD] = password.trim()
        }
    }
}
