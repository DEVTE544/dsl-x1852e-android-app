package com.example.d_linkmobilymanagement.data.repository

import com.example.d_linkmobilymanagement.data.model.AppLanguageSettings
import com.example.d_linkmobilymanagement.data.model.AppSettings
import com.example.d_linkmobilymanagement.data.model.RouterSavedInfo
import com.example.d_linkmobilymanagement.data.model.WanInterfaceStatus
import com.example.d_linkmobilymanagement.data.model.WifiBand
import com.example.d_linkmobilymanagement.data.model.WifiBandFilterState
import com.example.d_linkmobilymanagement.ui.model.DeviceUiModel
import kotlinx.coroutines.flow.Flow

interface RouterRepository {
    fun observeRouterInfo(): Flow<RouterSavedInfo>
    fun observeDevices(): Flow<List<DeviceUiModel>>

    suspend fun saveRouterInfo(ip: String, username: String, password: String = "")
    suspend fun login(username: String, password: String): Boolean
    suspend fun fetchLoginPageRaw(routerIp: String): String

    suspend fun refreshDevices()
    suspend fun toggleBlock(deviceId: String)
    suspend fun updateDeviceMeta(deviceId: String, customName: String, deviceType: String)
    suspend fun clearAllDeviceMeta()

    suspend fun getDevice(deviceId: String): DeviceUiModel?

    suspend fun getWifiFilterState(band: WifiBand): WifiBandFilterState
    suspend fun saveWifiFilterState(band: WifiBand, state: WifiBandFilterState)

    suspend fun getInternetStatus(): List<WanInterfaceStatus>

    fun observeAppSettings(): Flow<AppSettings>
    suspend fun updateAutoRefreshEnabled(enabled: Boolean)
    suspend fun updateRefreshInterval(seconds: Int)

    fun observeLanguageSettings(): Flow<AppLanguageSettings>
    suspend fun updateLanguage(languageTag: String, hasUserChosen: Boolean)
}
