package com.example.d_linkmobilymanagement.data.network

import android.content.Context
import android.net.ConnectivityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

enum class NetworkStatus {
    CONNECTED,
    DISCONNECTED
}

interface NetworkMonitor {
    val isOnline: Flow<NetworkStatus>
}

class ActiveNetworkMonitor(
    context: Context
) : NetworkMonitor {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    override val isOnline: Flow<NetworkStatus> = flow {
        while (true) {
            val isCurrentlyOnline = checkRealConnection()
            emit(if (isCurrentlyOnline) NetworkStatus.CONNECTED else NetworkStatus.DISCONNECTED)
            delay(3000) // Active check every 3 seconds as requested
        }
    }.distinctUntilChanged()

    private suspend fun checkRealConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Get the gateway IP (Router IP)
            val gateway = getGatewayIp() ?: return@withContext false
            
            // Try to connect to the router's web management port (80)
            // This ensures we have a path to the router specifically.
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(gateway, 80), 2000)
                    true
                }
            } catch (_: Exception) {
                // Fallback: try port 53 (DNS) on the router if port 80 is closed/filtered
                try {
                    Socket().use { socket ->
                        socket.connect(InetSocketAddress(gateway, 53), 1000)
                        true
                    }
                } catch (_: Exception) {
                    false
                }
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun getGatewayIp(): String? {
        return try {
            val activeNetwork = connectivityManager.activeNetwork ?: return null
            val linkProperties = connectivityManager.getLinkProperties(activeNetwork) ?: return null
            linkProperties.routes.firstOrNull { it.isDefaultRoute && it.gateway != null }?.gateway?.hostAddress
        } catch (_: Exception) {
            null
        }
    }
}
