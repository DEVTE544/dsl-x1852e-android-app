package com.example.d_linkmobilymanagement.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import timber.log.Timber

interface CurrentDeviceNetworkInfoProvider {
    suspend fun isOnWifi(): Boolean
    suspend fun getCurrentWifiIp(): String?
    suspend fun getCurrentSsid(): String?
}

class RealCurrentDeviceNetworkInfoProvider(context: Context) : CurrentDeviceNetworkInfoProvider {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    override suspend fun isOnWifi(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    override suspend fun getCurrentWifiIp(): String? {
        return try {
            val activeNetwork = connectivityManager.activeNetwork ?: return null
            val linkProperties = connectivityManager.getLinkProperties(activeNetwork) ?: return null
            val ip = linkProperties.linkAddresses.firstOrNull { 
                it.address is java.net.Inet4Address 
            }?.address?.hostAddress
            
            Timber.d("Local IP: $ip")
            ip
        } catch (e: Exception) {
            Timber.e(e, "Error getting IP")
            null
        }
    }

    override suspend fun getCurrentSsid(): String? {
        return try {
            val ssid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val activeNetwork = connectivityManager.activeNetwork ?: return null
                val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return null
                val wifiInfo = capabilities.transportInfo as? WifiInfo
                wifiInfo?.ssid?.removeSurrounding("\"")
            } else {
                @Suppress("DEPRECATION")
                wifiManager?.connectionInfo?.ssid?.removeSurrounding("\"")
            }

            Timber.d("Local SSID: $ssid")
            if (ssid == null || ssid == WifiManager.UNKNOWN_SSID) null else ssid
        } catch (e: Exception) {
            Timber.e(e, "Error getting SSID")
            null
        }
    }
}
