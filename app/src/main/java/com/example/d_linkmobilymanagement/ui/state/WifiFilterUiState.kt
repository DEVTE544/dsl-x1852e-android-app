package com.example.d_linkmobilymanagement.ui.state

import com.example.d_linkmobilymanagement.data.model.WifiFilterMode

data class WifiFilterDevice(
    val mac: String,
    val name: String = "",
    val ip: String = "",
    val isSaved: Boolean = false
)

data class WifiFilterUiState(
    val mode: WifiFilterMode = WifiFilterMode.BLOCK,
    val macs: List<String> = emptyList(),
    val devices: List<WifiFilterDevice> = emptyList(),
    val originalMacs: List<String> = emptyList(),
    val originalMode: WifiFilterMode = WifiFilterMode.BLOCK,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: Int? = null,
    val pendingDeleteMac: String? = null
) {
    val hasChanges: Boolean
        get() {
            val normalizedMacs = macs.map { it.uppercase().trim() }
            val normalizedOriginalMacs = originalMacs.map { it.uppercase().trim() }
            
            return mode != originalMode || 
                    normalizedMacs.size != normalizedOriginalMacs.size ||
                    !normalizedMacs.containsAll(normalizedOriginalMacs) ||
                    !normalizedOriginalMacs.containsAll(normalizedMacs)
        }
}
