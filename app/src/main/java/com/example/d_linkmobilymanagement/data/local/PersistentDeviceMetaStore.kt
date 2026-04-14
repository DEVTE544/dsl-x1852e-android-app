package com.example.d_linkmobilymanagement.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.d_linkmobilymanagement.data.model.DeviceLocalMeta
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val DEVICE_META_STORE_NAME = "device_meta_store"

private val Context.deviceMetaDataStore by preferencesDataStore(
    name = DEVICE_META_STORE_NAME
)

class PersistentDeviceMetaStore(
    private val context: Context
) : DeviceMetaStore {

    override fun observeMeta(mac: String): Flow<DeviceLocalMeta> {
        val normalizedMac = normalizeMac(mac)
        if (normalizedMac.isBlank()) {
            return context.deviceMetaDataStore.data
                .catch { exception ->
                    if (exception is IOException) emit(emptyPreferences()) else throw exception
                }
                .map { DeviceLocalMeta() }
        }

        return context.deviceMetaDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { prefs ->
                prefs.toDeviceLocalMeta(normalizedMac)
            }
    }

    override suspend fun getMeta(mac: String): DeviceLocalMeta {
        val normalizedMac = normalizeMac(mac)
        if (normalizedMac.isBlank()) return DeviceLocalMeta()
        return observeMeta(normalizedMac).first()
    }

    override suspend fun setCustomName(mac: String, name: String) {
        val normalizedMac = normalizeMac(mac)
        if (normalizedMac.isBlank()) return

        ensureDeviceExists(normalizedMac)

        context.deviceMetaDataStore.edit { prefs ->
            prefs[keyCustomName(normalizedMac)] = name.trim()
            prefs[keyLastSeen(normalizedMac)] = now()
        }
    }

    override suspend fun setDeviceType(mac: String, deviceType: String) {
        val normalizedMac = normalizeMac(mac)
        if (normalizedMac.isBlank()) return

        ensureDeviceExists(normalizedMac)

        context.deviceMetaDataStore.edit { prefs ->
            prefs[keyDeviceType(normalizedMac)] = deviceType.trim()
            prefs[keyLastSeen(normalizedMac)] = now()
        }
    }

    override suspend fun setNotes(mac: String, notes: String) {
        val normalizedMac = normalizeMac(mac)
        if (normalizedMac.isBlank()) return

        ensureDeviceExists(normalizedMac)

        context.deviceMetaDataStore.edit { prefs ->
            prefs[keyNotes(normalizedMac)] = notes.trim()
            prefs[keyLastSeen(normalizedMac)] = now()
        }
    }

    override suspend fun touchDevice(mac: String) {
        val normalizedMac = normalizeMac(mac)
        if (normalizedMac.isBlank()) return

        ensureDeviceExists(normalizedMac)

        context.deviceMetaDataStore.edit { prefs ->
            prefs[keyLastSeen(normalizedMac)] = now()
        }
    }

    override suspend fun clearAll() {
        context.deviceMetaDataStore.edit { it.clear() }
    }

    private suspend fun ensureDeviceExists(mac: String) {
        context.deviceMetaDataStore.edit { prefs ->
            val currentFirstSeen = prefs[keyFirstSeen(mac)].orEmpty()
            val timestamp = now()

            if (currentFirstSeen.isBlank()) {
                prefs[keyFirstSeen(mac)] = timestamp
            }

            if (prefs[keyLastSeen(mac)].isNullOrBlank()) {
                prefs[keyLastSeen(mac)] = timestamp
            }

            if (prefs[keyCustomName(mac)] == null) {
                prefs[keyCustomName(mac)] = ""
            }

            if (prefs[keyDeviceType(mac)] == null) {
                prefs[keyDeviceType(mac)] = ""
            }

            if (prefs[keyNotes(mac)] == null) {
                prefs[keyNotes(mac)] = ""
            }
        }
    }

    private fun Preferences.toDeviceLocalMeta(mac: String): DeviceLocalMeta {
        return DeviceLocalMeta(
            customName = this[keyCustomName(mac)] ?: "",
            deviceType = this[keyDeviceType(mac)] ?: "",
            notes = this[keyNotes(mac)] ?: "",
            firstSeen = this[keyFirstSeen(mac)] ?: "",
            lastSeen = this[keyLastSeen(mac)] ?: ""
        )
    }

    private fun keyCustomName(mac: String) =
        stringPreferencesKey("device_${mac.toKeyPart()}_custom_name")

    private fun keyDeviceType(mac: String) =
        stringPreferencesKey("device_${mac.toKeyPart()}_device_type")

    private fun keyNotes(mac: String) =
        stringPreferencesKey("device_${mac.toKeyPart()}_notes")

    private fun keyFirstSeen(mac: String) =
        stringPreferencesKey("device_${mac.toKeyPart()}_first_seen")

    private fun keyLastSeen(mac: String) =
        stringPreferencesKey("device_${mac.toKeyPart()}_last_seen")

    private fun String.toKeyPart(): String = replace(":", "_")

    private fun now(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        return formatter.format(Date())
    }

    private fun normalizeMac(mac: String?): String {
        if (mac.isNullOrBlank()) return ""

        val cleaned = mac.trim()
            .lowercase()
            .replace("-", "")
            .replace(":", "")

        if (cleaned.isBlank() || cleaned == "n/a" || cleaned == "000000000000") return ""
        if (cleaned.length != 12) return ""
        if (!cleaned.matches(Regex("[0-9a-f]{12}"))) return ""

        return cleaned.chunked(2).joinToString(":")
    }
}
