package com.example.d_linkmobilymanagement.data.local

import com.example.d_linkmobilymanagement.data.model.DeviceLocalMeta
import kotlinx.coroutines.flow.Flow

interface DeviceMetaStore {
    fun observeMeta(mac: String): Flow<DeviceLocalMeta>
    suspend fun getMeta(mac: String): DeviceLocalMeta
    suspend fun setCustomName(mac: String, name: String)
    suspend fun setDeviceType(mac: String, deviceType: String)
    suspend fun setNotes(mac: String, notes: String)
    suspend fun touchDevice(mac: String)
    suspend fun clearAll()
}
