package com.example.d_linkmobilymanagement.ui.screens.wififilter

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.d_linkmobilymanagement.data.model.WifiBand
import com.example.d_linkmobilymanagement.data.model.WifiFilterMode
import com.example.d_linkmobilymanagement.ui.state.WifiFilterUiState

@Composable
fun WifiFilterScreen(
    paddingValues: PaddingValues,
    state24: WifiFilterUiState,
    state5: WifiFilterUiState,
    selectedBand: WifiBand,
    onBandChange: (WifiBand) -> Unit,
    onModeChange: (WifiBand, WifiFilterMode) -> Unit,
    onAddMac: (WifiBand, String) -> Unit,
    onRemoveMac: (WifiBand, String) -> Unit,
    onSave: (WifiBand) -> Unit,
    onRefresh: (WifiBand) -> Unit,
    onConfirmDelete: () -> Unit = {},
    onCancelDelete: () -> Unit = {}
) {
    val currentState = if (selectedBand == WifiBand.BAND_2_4GHZ) state24 else state5

    LaunchedEffect(selectedBand) {
        if (currentState.macs.isEmpty() && !currentState.isLoading) {
            onBandChange(selectedBand)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = paddingValues.calculateTopPadding())
    ) {
        if (currentState.isLoading && currentState.macs.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            WifiFilterContent(
                paddingValues = paddingValues,
                state = currentState,
                band = selectedBand,
                onBandChange = onBandChange,
                onModeChange = onModeChange,
                onAddMac = onAddMac,
                onRemoveMac = onRemoveMac,
                onSave = onSave,
                onRefresh = onRefresh,
                onConfirmDelete = onConfirmDelete,
                onCancelDelete = onCancelDelete
            )
        }
    }
}
