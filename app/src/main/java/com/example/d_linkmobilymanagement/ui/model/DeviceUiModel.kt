package com.example.d_linkmobilymanagement.ui.model

enum class NetworkType {
    WIFI_2_4G, WIFI_5G, ETHERNET, UNKNOWN
}

enum class SelfDetectionConfidence {
    NONE, LOW, MEDIUM, HIGH
}

data class DeviceUiModel(
    val id: String,
    val mac: String,
    val ip: String,
    val visibleName: String,
    val customName: String = "",
    val deviceTypeNote: String = "",
    val networkType: NetworkType = NetworkType.UNKNOWN,
    val isBlocked: Boolean = false,
    val isOnline: Boolean = true,
    val lastSeenText: String = "الآن",
    val isCurrentPhone: Boolean = false,
    val selfDetectionConfidence: SelfDetectionConfidence = SelfDetectionConfidence.NONE
)
