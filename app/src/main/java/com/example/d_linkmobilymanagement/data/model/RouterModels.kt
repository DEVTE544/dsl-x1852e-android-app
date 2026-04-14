package com.example.d_linkmobilymanagement.data.model

data class RouterSavedInfo(
    val ip: String = "192.168.1.1",
    val username: String = "SuperAdmin",
    val password: String = ""
)

data class DeviceLocalMeta(
    val customName: String = "",
    val deviceType: String = "",
    val notes: String = "",
    val firstSeen: String = "",
    val lastSeen: String = ""
)

data class ConnectedClientRaw(
    val mac: String,
    val ip: String = "N/A",
    val hostname: String = "N/A",
    val isActive: Boolean = false,
    val sourceLinkType: String = "",
    val networkType: String = "Unknown"
)

data class WifiClientRaw(
    val mac: String,
    val sourcePort: String = "",
    val sourceContype: String = "",
    val isActive: Boolean = false,
    val networkType: String = "Unknown"
)

data class WifiMacListRaw(
    val clients: Map<String, WifiClientRaw> = emptyMap(),
    val mode24: String = "0",
    val mode5: String = "0",
    val blocked24: List<String> = emptyList(),
    val blocked5: List<String> = emptyList(),
    val slots24: List<String> = emptyList(),
    val slots5: List<String> = emptyList(),
    val sessionKey: String = ""
)
