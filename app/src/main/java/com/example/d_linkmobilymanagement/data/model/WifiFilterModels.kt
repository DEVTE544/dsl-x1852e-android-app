package com.example.d_linkmobilymanagement.data.model

enum class WifiFilterMode(val value: Int) {
    BLOCK(0),     // wlanflt_macX = محظورين
    ALLOW(1)      // wlanflt_macX = مسموحين فقط
}

enum class WifiBand {
    BAND_2_4GHZ,
    BAND_5GHZ
}

data class WifiBandFilterState(
    val mode: WifiFilterMode,
    val macs: List<String>,
    val fullSlots: List<String> = emptyList()
)

data class PreLoginInfo(
    val modelName: String,
    val firmwareVersion: String,
    val availableUsers: List<String> // 🟢 back to simple String list
)
