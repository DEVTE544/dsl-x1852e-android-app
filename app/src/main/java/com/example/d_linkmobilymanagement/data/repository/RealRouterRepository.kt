package com.example.d_linkmobilymanagement.data.repository

import com.example.d_linkmobilymanagement.data.local.AppLanguageStore
import com.example.d_linkmobilymanagement.data.local.AppSettingsStore
import com.example.d_linkmobilymanagement.data.local.DeviceMetaStore
import com.example.d_linkmobilymanagement.data.local.RouterPrefsStore
import com.example.d_linkmobilymanagement.data.model.AppLanguageSettings
import com.example.d_linkmobilymanagement.data.model.AppSettings
import com.example.d_linkmobilymanagement.data.model.ConnectedClientRaw
import com.example.d_linkmobilymanagement.data.model.RouterSavedInfo
import com.example.d_linkmobilymanagement.data.model.WanInterfaceStatus
import com.example.d_linkmobilymanagement.data.model.WifiBand
import com.example.d_linkmobilymanagement.data.model.WifiBandFilterState
import com.example.d_linkmobilymanagement.data.model.WifiFilterMode
import com.example.d_linkmobilymanagement.data.model.WifiMacListRaw
import com.example.d_linkmobilymanagement.data.remote.RouterHttpClient
import timber.log.Timber
import com.example.d_linkmobilymanagement.data.remote.RouterParser
import com.example.d_linkmobilymanagement.ui.model.DeviceUiModel
import com.example.d_linkmobilymanagement.ui.model.NetworkType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

class RealRouterRepository(
    private val prefsStore: RouterPrefsStore,
    private val deviceMetaStore: DeviceMetaStore,
    private val appSettingsStore: AppSettingsStore,
    private val languageStore: AppLanguageStore,
    private val httpClient: RouterHttpClient = RouterHttpClient()
) : RouterRepository {

    companion object {
        private const val MAX_FILTER_SLOTS = 16
        private const val ZERO_MAC = "00:00:00:00:00:00"
    }

    private val devicesFlow = MutableStateFlow<List<DeviceUiModel>>(emptyList())
    private val runtimeStateByMac = linkedMapOf<String, RuntimeDeviceState>()

    override fun observeRouterInfo(): Flow<RouterSavedInfo> = prefsStore.routerInfo

    override fun observeDevices(): Flow<List<DeviceUiModel>> = devicesFlow.asStateFlow()

    override suspend fun saveRouterInfo(ip: String, username: String, password: String) {
        prefsStore.saveRouterInfo(ip, username, password)
    }

    override suspend fun login(username: String, password: String): Boolean {
        val routerInfo = prefsStore.routerInfo.first()
        httpClient.clearSession()

        val ok = httpClient.login(
            routerIp = routerInfo.ip,
            username = username,
            password = password
        )

        if (ok) {
            refreshDevices()
        }

        return ok
    }

    override suspend fun fetchLoginPageRaw(routerIp: String): String {
        Timber.d("fetchLoginPageRaw called with IP: $routerIp")
        return try {
            val result = httpClient.fetchLoginPageRaw(routerIp)
            Timber.d("HTML received: ${result.take(100)}...")
            result
        } catch (e: Exception) {
            Timber.e(e, "Error fetching login page")
            throw e
        }
    }

    override suspend fun refreshDevices() {
        val routerInfo = prefsStore.routerInfo.first()

        val connectedRawText = httpClient.fetchConnectedDevicesRaw(routerInfo.ip)
        val filterPageText = httpClient.fetchWifiFilterPageRaw(routerInfo.ip)
        val blockListsRawText = httpClient.fetchBlockListsRaw(routerInfo.ip)

        val connected = RouterParser.parseConnectedDevices(connectedRawText)
        val wifiData = RouterParser.parseWifiMacList(blockListsRawText, filterPageText)

        // Log IPs from connected devices
        connected.forEach { c ->
            Timber.d("Connected device: mac=${c.mac}, ip=${c.ip}, hostname=${c.hostname}, isActive=${c.isActive}")
        }

        val mergedStates = mergeDeviceData(connected, wifiData)

        // Log merged states
        mergedStates.forEach { s ->
            Timber.d("Merged device: mac=${s.mac}, ip=${s.ip}, hostname=${s.hostname}, isActive=${s.isActive}")
        }

        runtimeStateByMac.clear()
        mergedStates.forEach { state ->
            runtimeStateByMac[state.mac] = state
        }

        devicesFlow.value = mergedStates.map { toUiModel(it) }
    }

    override suspend fun toggleBlock(deviceId: String) {
        val mac = RouterParser.normalizeMac(deviceId)
        if (mac.isBlank()) return

        val current = runtimeStateByMac[mac] ?: return
        val routerInfo = prefsStore.routerInfo.first()
        val filterPageText = httpClient.fetchWifiFilterPageRaw(routerInfo.ip)
        val blockListsRawText = httpClient.fetchBlockListsRaw(routerInfo.ip)
        val latestWifiData = RouterParser.parseWifiMacList(blockListsRawText, filterPageText)

        if (current.isBlockedAny) {
            unblockDevice(routerInfo.ip, current, latestWifiData)
        } else {
            blockDevice(routerIp = routerInfo.ip, device = current, wifiData = latestWifiData)
        }

        delay(500)
        refreshDevices()
    }

    override suspend fun updateDeviceMeta(deviceId: String, customName: String, deviceType: String) {
        val mac = RouterParser.normalizeMac(deviceId)
        if (mac.isBlank()) return

        deviceMetaStore.setCustomName(mac, customName)
        deviceMetaStore.setDeviceType(mac, deviceType)

        // Force refresh UI models to reflect meta changes
        val currentStates = runtimeStateByMac.values.toList()
        devicesFlow.value = currentStates.map { toUiModel(it) }
    }

    override suspend fun clearAllDeviceMeta() {
        deviceMetaStore.clearAll()
        // Refresh devices after clearing meta to update UI
        refreshDevices()
    }

    override suspend fun getDevice(deviceId: String): DeviceUiModel? {
        val mac = RouterParser.normalizeMac(deviceId)
        val state = runtimeStateByMac[mac] ?: return null
        return toUiModel(state)
    }

    override fun observeAppSettings(): Flow<AppSettings> = appSettingsStore.appSettings

    override suspend fun updateAutoRefreshEnabled(enabled: Boolean) {
        appSettingsStore.updateAutoRefreshEnabled(enabled)
    }

    override suspend fun updateRefreshInterval(seconds: Int) {
        appSettingsStore.updateRefreshInterval(seconds)
    }

    override fun observeLanguageSettings(): Flow<AppLanguageSettings> = languageStore.languageSettings

    override suspend fun updateLanguage(languageTag: String, hasUserChosen: Boolean) {
        languageStore.saveLanguage(languageTag, hasUserChosen)
    }

    override suspend fun getInternetStatus(): List<WanInterfaceStatus> {
        val routerInfo = prefsStore.routerInfo.first()
        val htmlResponse = httpClient.getInternetStatus(routerInfo.ip)
        return RouterParser.parseInternetStatus(htmlResponse)
    }

    private suspend fun blockDevice(
        routerIp: String,
        device: RuntimeDeviceState,
        wifiData: WifiMacListRaw
    ) {
        val mac = device.mac
        val band = normalizeBandFromNetworkType(device.networkType)
            ?: throw IllegalStateException("Only 2.4GHz or 5GHz WiFi devices can be blocked.")

        val slots24 = sanitizeSlotList(wifiData.slots24)
        val slots5 = sanitizeSlotList(wifiData.slots5)

        if (band == "24") {
            if (slots24.contains(mac)) return
            if (slots24.size >= MAX_FILTER_SLOTS) {
                throw IllegalStateException("2.4GHz block list is full.")
            }

            val newSlots24 = slots24.toMutableList().apply { add(mac) }
            val sessionKey = httpClient.getSessionKey(routerIp)
            val payload = buildWifiFilterPayload(
                sessionKey = sessionKey,
                mode24 = mapToWifiFilterMode(wifiData.mode24),
                mode5 = mapToWifiFilterMode(wifiData.mode5),
                slots24 = newSlots24,
                slots5 = slots5,
                saveBand = "24",
                selectedMac = mac
            )
            httpClient.submitWifiFilter(routerIp, payload)
            return
        }

        if (slots5.contains(mac)) return
        if (slots5.size >= MAX_FILTER_SLOTS) {
            throw IllegalStateException("5GHz block list is full.")
        }

        val newSlots5 = slots5.toMutableList().apply { add(mac) }
        val sessionKey = httpClient.getSessionKey(routerIp)
        val payload = buildWifiFilterPayload(
            sessionKey = sessionKey,
            mode24 = mapToWifiFilterMode(wifiData.mode24),
            mode5 = mapToWifiFilterMode(wifiData.mode5),
            slots24 = slots24,
            slots5 = newSlots5,
            saveBand = "5",
            selectedMac = mac
        )
        httpClient.submitWifiFilter(routerIp, payload)
    }

    private suspend fun unblockDevice(
        routerIp: String,
        device: RuntimeDeviceState,
        wifiData: WifiMacListRaw
    ) {
        val mac = device.mac

        val band = when {
            device.isBlocked24 && !device.isBlocked5 -> "24"
            device.isBlocked5 && !device.isBlocked24 -> "5"
            else -> normalizeBandFromNetworkType(device.networkType)
        } ?: throw IllegalStateException("Unable to determine the blocked band for this device.")

        val rawSlots24 = ensureSlotPayload(wifiData.slots24)
        val rawSlots5 = ensureSlotPayload(wifiData.slots5)

        val targetSlots = if (band == "24") rawSlots24 else rawSlots5

        val deleteIndex = targetSlots.indexOfFirst {
            RouterParser.normalizeMac(it) == mac
        }

        if (deleteIndex == -1) return

        val sessionKey = httpClient.getSessionKey(routerIp)
        val payload = buildSlotDeletePayload(
            sessionKey = sessionKey,
            mode24 = mapToWifiFilterMode(wifiData.mode24),
            mode5 = mapToWifiFilterMode(wifiData.mode5),
            band = band,
            slots24 = rawSlots24,
            slots5 = rawSlots5,
            deleteIndex = deleteIndex
        )

        delay(300)
        httpClient.submitWifiFilter(routerIp, payload)
    }

    private suspend fun mergeDeviceData(
        connected: List<ConnectedClientRaw>,
        wifiData: WifiMacListRaw
    ): List<RuntimeDeviceState> {
        val combined = linkedMapOf<String, RuntimeDeviceState>()

        val list24 = wifiData.blocked24.toSet()
        val list5 = wifiData.blocked5.toSet()

        val mode24 = mapToWifiFilterMode(wifiData.mode24)
        val mode5 = mapToWifiFilterMode(wifiData.mode5)

        fun checkBlocked(mac: String, bandList: Set<String>, mode: WifiFilterMode): Boolean {
            val inList = mac in bandList
            return if (mode == WifiFilterMode.ALLOW) {
                !inList
            } else {
                inList
            }
        }

        // كل الأجهزة التي نعرفها (سواء من قائمة المتصلين أو من قوائم الفلترة)
        val allMacs = (connected.map { it.mac } + wifiData.clients.keys + list24 + list5).distinct()

        allMacs.forEach { mac ->
            val conn = connected.find { it.mac == mac }
            val client = wifiData.clients[mac]

            val isBlocked24 = checkBlocked(mac, list24, mode24)
            val isBlocked5 = checkBlocked(mac, list5, mode5)

            combined[mac] = RuntimeDeviceState(
                mac = mac,
                ip = conn?.ip ?: "N/A",
                hostname = conn?.hostname ?: client?.mac ?: "N/A",
                networkType = client?.networkType ?: conn?.networkType ?: "Unknown",
                isActive = conn?.isActive ?: client?.isActive ?: false,
                isBlocked24 = isBlocked24,
                isBlocked5 = isBlocked5,
                isBlockedAny = isBlocked24 || isBlocked5,
                sourceLinkType = conn?.sourceLinkType ?: "",
                sourcePort = client?.sourcePort ?: "",
                sourceContype = client?.sourceContype ?: ""
            )
        }

        val deviceList = combined.values.toList()
        deviceList.forEach { deviceMetaStore.touchDevice(it.mac) }

        val nameMap = deviceList.associate { it.mac to displayNameOf(it) }

        return deviceList.sortedWith(
            compareBy(
                { !it.isActive },
                { it.networkType },
                { nameMap[it.mac]?.lowercase() },
                { it.mac }
            )
        )
    }

    private suspend fun toUiModel(state: RuntimeDeviceState): DeviceUiModel {
        val meta = deviceMetaStore.getMeta(state.mac)

        val visibleName = state.hostname
            .takeUnless { it.isBlank() || it == "N/A" }
            ?: state.mac

        val lastSeen = meta.lastSeen.ifBlank {
            if (state.isActive) "الآن" else ""
        }

        return DeviceUiModel(
            id = state.mac,
            mac = state.mac,
            ip = state.ip.ifBlank { "N/A" },
            visibleName = visibleName,
            customName = meta.customName,
            deviceTypeNote = meta.deviceType,
            networkType = toNetworkTypeEnum(state.networkType),
            isBlocked = state.isBlockedAny,
            isOnline = state.isActive,
            lastSeenText = lastSeen
        )
    }

    override suspend fun getWifiFilterState(band: WifiBand): WifiBandFilterState {
        val routerInfo = prefsStore.routerInfo.first()
        val pageText = httpClient.fetchWifiFilterPageRaw(routerInfo.ip)
        val macListText = httpClient.fetchBlockListsRaw(routerInfo.ip)
        val wifiData = RouterParser.parseWifiMacList(macListText, pageText)

        return if (band == WifiBand.BAND_2_4GHZ) {
            WifiBandFilterState(
                mode = mapToWifiFilterMode(wifiData.mode24),
                macs = wifiData.blocked24,
                fullSlots = wifiData.slots24
            )
        } else {
            WifiBandFilterState(
                mode = mapToWifiFilterMode(wifiData.mode5),
                macs = wifiData.blocked5,
                fullSlots = wifiData.slots5
            )
        }
    }

    override suspend fun saveWifiFilterState(band: WifiBand, state: WifiBandFilterState) {
        val routerInfo = prefsStore.routerInfo.first()
        
        // 1. جلب الحالة الحقيقية الحالية من الراوتر للحصول على الخانات الأصلية (Slots)
        val pageText = httpClient.fetchWifiFilterPageRaw(routerInfo.ip)
        val macListText = httpClient.fetchBlockListsRaw(routerInfo.ip)
        val wifiData = RouterParser.parseWifiMacList(macListText, pageText)
        
        val originalSlots = if (band == WifiBand.BAND_2_4GHZ) wifiData.slots24.toMutableList() else wifiData.slots5.toMutableList()
        val normalizedOriginal = originalSlots.map { RouterParser.normalizeMac(it) }
        val normalizedNew = state.macs.map { RouterParser.normalizeMac(it) }

        // 2. تحديد نوع العملية (إضافة، حذف، أو تغيير وضع فقط)
        val deletedMac = normalizedOriginal.find { it.isNotBlank() && it !in normalizedNew }
        val addedMac = normalizedNew.find { it.isNotBlank() && it !in normalizedOriginal }

        val payload = when {
            deletedMac != null -> {
                // عملية حذف: نضع ZERO_MAC في نفس مكان الجهاز المحذوف بالضبط
                val deleteIndex = normalizedOriginal.indexOf(deletedMac)
                buildSlotDeletePayload(
                    sessionKey = httpClient.getSessionKey(routerInfo.ip),
                    mode24 = if (band == WifiBand.BAND_2_4GHZ) state.mode else mapToWifiFilterMode(wifiData.mode24),
                    mode5 = if (band == WifiBand.BAND_5GHZ) state.mode else mapToWifiFilterMode(wifiData.mode5),
                    band = if (band == WifiBand.BAND_2_4GHZ) "24" else "5",
                    slots24 = if (band == WifiBand.BAND_2_4GHZ) originalSlots else wifiData.slots24,
                    slots5 = if (band == WifiBand.BAND_5GHZ) originalSlots else wifiData.slots5,
                    deleteIndex = deleteIndex
                )
            }
            addedMac != null -> {
                // عملية إضافة: نجد أول خانة فارغة في الراوتر ونضع الماك الجديد فيها
                val firstEmptyIndex = normalizedOriginal.indexOfFirst { it.isBlank() }.coerceAtLeast(0)
                buildSlotAddPayload(
                    sessionKey = httpClient.getSessionKey(routerInfo.ip),
                    mode24 = if (band == WifiBand.BAND_2_4GHZ) state.mode else mapToWifiFilterMode(wifiData.mode24),
                    mode5 = if (band == WifiBand.BAND_5GHZ) state.mode else mapToWifiFilterMode(wifiData.mode5),
                    band = if (band == WifiBand.BAND_2_4GHZ) "24" else "5",
                    slots24 = if (band == WifiBand.BAND_2_4GHZ) originalSlots else wifiData.slots24,
                    slots5 = if (band == WifiBand.BAND_5GHZ) originalSlots else wifiData.slots5,
                    newMac = addedMac,
                    addIndex = firstEmptyIndex
                )
            }
            else -> {
                // تغيير وضع الفلترة فقط أو لا يوجد تغيير في الأجهزة
                buildFullFilterPayload(
                    sessionKey = httpClient.getSessionKey(routerInfo.ip),
                    mode24 = if (band == WifiBand.BAND_2_4GHZ) state.mode else mapToWifiFilterMode(wifiData.mode24),
                    slots24 = if (band == WifiBand.BAND_2_4GHZ) originalSlots else wifiData.slots24,
                    mode5 = if (band == WifiBand.BAND_5GHZ) state.mode else mapToWifiFilterMode(wifiData.mode5),
                    slots5 = if (band == WifiBand.BAND_5GHZ) originalSlots else wifiData.slots5,
                    saveBand = if (band == WifiBand.BAND_2_4GHZ) "24" else "5"
                )
            }
        }

        httpClient.submitWifiFilter(routerInfo.ip, payload)
        delay(500) // تأخير بسيط لضمان معالجة الراوتر للطلب
        refreshDevices()
    }

    private fun buildSlotAddPayload(
        sessionKey: String,
        mode24: WifiFilterMode,
        mode5: WifiFilterMode,
        band: String,
        slots24: List<String>,
        slots5: List<String>,
        newMac: String,
        addIndex: Int
    ): List<Pair<String, String>> {
        val safe24 = ensureSlotPayload(slots24)
        val safe5 = ensureSlotPayload(slots5)

        if (band == "24") {
            safe24[addIndex] = newMac
        } else {
            safe5[addIndex] = newMac
        }

        val action24 = if (mode24 == WifiFilterMode.ALLOW) "1" else "2"
        val action5 = if (mode5 == WifiFilterMode.ALLOW) "1" else "2"

        val payload = mutableListOf(
            "sessionKey" to sessionKey,
            "savecfg" to if (band == "24") "1" else "",
            "savecfg_ac" to if (band == "5") "1" else "",
            "WLAN_FltAction" to action24,
            "WLAN11AC_FltAction" to action5,
            "flag" to if (band == "24") addIndex.toString() else "0",
            "flag11ac" to if (band == "5") addIndex.toString() else "0"
        )

        for (i in 0 until MAX_FILTER_SLOTS) {
            payload += "wlanflt_mac$i" to safe24[i]
        }
        for (i in 0 until MAX_FILTER_SLOTS) {
            payload += "wlan11acflt_mac$i" to safe5[i]
        }

        payload += "wlanflt_mac" to if (band == "24") newMac else "Manual"
        payload += "wlanflt_manual" to ""
        payload += "wlan11acflt_mac" to if (band == "5") newMac else "Manual"
        payload += "wlan11acflt_manual" to ""

        if (action24 == "1") payload.add("filterrule_ck" to "on")
        if (action5 == "1") payload.add("filterrule11ac_ck" to "on")

        return payload
    }

    private fun mapToWifiFilterMode(value: String?): WifiFilterMode {
        return when (value?.trim()) {
            "1" -> WifiFilterMode.ALLOW
            "0", "2" -> WifiFilterMode.BLOCK
            else -> WifiFilterMode.BLOCK
        }
    }

    private fun buildFullFilterPayload(
        sessionKey: String,
        mode24: WifiFilterMode,
        slots24: List<String>,
        mode5: WifiFilterMode,
        slots5: List<String>,
        saveBand: String
    ): List<Pair<String, String>> {
        val safe24 = ensureSlotPayload(slots24)
        val safe5 = ensureSlotPayload(slots5)
        
        // Mapping: ALLOW -> 1, BLOCK/DENY -> 2
        val action24 = if (mode24 == WifiFilterMode.ALLOW) "1" else "2"
        val action5 = if (mode5 == WifiFilterMode.ALLOW) "1" else "2"

        // الاكتشاف: الـ flag يمثل الـ index الخاص بآخر ماك تمت إضافته.
        val lastIdx24 = slots24.indexOfLast { it.isNotBlank() }.coerceAtLeast(0)
        val lastIdx5 = slots5.indexOfLast { it.isNotBlank() }.coerceAtLeast(0)
        
        val lastMac24 = slots24.getOrNull(lastIdx24) ?: ""
        val lastMac5 = slots5.getOrNull(lastIdx5) ?: ""

        val payload = mutableListOf(
            "sessionKey" to sessionKey,
            "savecfg" to if (saveBand == "24") "1" else "",
            "savecfg_ac" to if (saveBand == "5") "1" else "",
            "WLAN_FltAction" to action24,
            "WLAN11AC_FltAction" to action5,
            "flag" to if (saveBand == "24") lastIdx24.toString() else "0",
            "flag11ac" to if (saveBand == "5") lastIdx5.toString() else "0"
        )
        
        // تعبئة الـ 16 خانة
        for (i in 0 until MAX_FILTER_SLOTS) {
            val mac24 = safe24.getOrNull(i)
            payload.add("wlanflt_mac$i" to (mac24 ?: ""))
        }
        for (i in 0 until MAX_FILTER_SLOTS) {
            val mac5 = safe5.getOrNull(i)
            payload.add("wlan11acflt_mac$i" to (mac5 ?: ""))
        }
        
        // هنا التغيير المهم بناءً على الـ Log الخاص بك
        payload.add("wlanflt_mac" to if (saveBand == "24") lastMac24 else "Manual")
        payload.add("wlanflt_manual" to "")
        payload.add("wlan11acflt_mac" to if (saveBand == "5") lastMac5 else "Manual")
        payload.add("wlan11acflt_manual" to "")

        if (action24 == "1") payload.add("filterrule_ck" to "on")
        if (action5 == "1") payload.add("filterrule11ac_ck" to "on")

        return payload
    }

    private suspend fun displayNameOf(state: RuntimeDeviceState): String {
        val meta = deviceMetaStore.getMeta(state.mac)
        val customName = meta.customName.trim()
        if (customName.isNotBlank()) return customName

        val host = state.hostname.trim()
        if (host.isNotBlank() && host != "N/A") return host

        return state.mac
    }

    private fun toNetworkTypeEnum(value: String): NetworkType {
        return when (value.trim()) {
            "2.4GHz" -> NetworkType.WIFI_2_4G
            "5GHz" -> NetworkType.WIFI_5G
            "LAN" -> NetworkType.ETHERNET
            else -> NetworkType.UNKNOWN
        }
    }

    private fun normalizeBandFromNetworkType(value: String): String? {
        return when (value.trim()) {
            "2.4GHz" -> "24"
            "5GHz" -> "5"
            else -> null
        }
    }

    private fun sanitizeSlotList(macs: List<String>): List<String> {
        val seen = linkedSetOf<String>()

        macs.forEach { mac ->
            val normalized = RouterParser.normalizeMac(mac)
            if (normalized.isNotBlank()) {
                seen += normalized
            }
        }

        return seen.take(MAX_FILTER_SLOTS).toList()
    }

    private fun ensureSlotPayload(macs: List<String>): MutableList<String> {
        val values = macs.take(MAX_FILTER_SLOTS)
            .map { RouterParser.normalizeMac(it) }
            .toMutableList()

        while (values.size < MAX_FILTER_SLOTS) {
            values += ""
        }

        return values
    }

    private fun buildWifiFilterPayload(
        sessionKey: String,
        mode24: WifiFilterMode,
        mode5: WifiFilterMode,
        slots24: List<String>,
        slots5: List<String>,
        saveBand: String,
        selectedMac: String = "",
        manualMode: Boolean = false
    ): List<Pair<String, String>> {
        val safe24 = ensureSlotPayload(slots24)
        val safe5 = ensureSlotPayload(slots5)

        val action24 = if (mode24 == WifiFilterMode.ALLOW) "1" else "2"
        val action5 = if (mode5 == WifiFilterMode.ALLOW) "1" else "2"

        // الاكتشاف: الـ flag يمثل الـ index الخاص بآخر ماك تمت إضافته.
        val lastIdx24 = slots24.indexOfLast { it.isNotBlank() }.coerceAtLeast(0)
        val lastIdx5 = slots5.indexOfLast { it.isNotBlank() }.coerceAtLeast(0)
        
        val lastMac24 = slots24.getOrNull(lastIdx24) ?: ""
        val lastMac5 = slots5.getOrNull(lastIdx5) ?: ""

        val payload = mutableListOf(
            "sessionKey" to sessionKey,
            "savecfg" to if (saveBand == "24") "1" else "",
            "savecfg_ac" to if (saveBand == "5") "1" else "",
            "WLAN_FltAction" to action24,
            "WLAN11AC_FltAction" to action5,
            "flag" to if (saveBand == "24") lastIdx24.toString() else "0",
            "flag11ac" to if (saveBand == "5") lastIdx5.toString() else "0"
        )

        for (i in 0 until MAX_FILTER_SLOTS) {
            payload += "wlanflt_mac$i" to safe24[i]
        }

        for (i in 0 until MAX_FILTER_SLOTS) {
            payload += "wlan11acflt_mac$i" to safe5[i]
        }

        // بناءً على الـ Log: الماك الجديد يجب أن يوضع في wlanflt_mac والـ flag يشير لمكانه
        payload += "wlanflt_mac" to if (saveBand == "24") lastMac24 else "Manual"
        payload += "wlanflt_manual" to ""
        payload += "wlan11acflt_mac" to if (saveBand == "5") lastMac5 else "Manual"
        payload += "wlan11acflt_manual" to ""

        // >>> Add ALLOW mode condition <<<
        if (action24 == "1") {
            payload.add("filterrule_ck" to "on")
        }
        if (action5 == "1") {
            payload.add("filterrule11ac_ck" to "on")
        }

        return payload
    }

    private fun buildSlotDeletePayload(
        sessionKey: String,
        mode24: WifiFilterMode,
        mode5: WifiFilterMode,
        band: String,
        slots24: List<String>,
        slots5: List<String>,
        deleteIndex: Int
    ): List<Pair<String, String>> {
        val safe24 = ensureSlotPayload(slots24)
        val safe5 = ensureSlotPayload(slots5)

        val savecfg: String
        val savecfgAc: String
        val flag: String
        val flag11ac: String

        if (band == "24") {
            safe24[deleteIndex] = ZERO_MAC
            savecfg = "1"
            savecfgAc = ""
            flag = deleteIndex.toString()
            flag11ac = ""
        } else {
            safe5[deleteIndex] = ZERO_MAC
            savecfg = ""
            savecfgAc = "1"
            flag = ""
            flag11ac = deleteIndex.toString()
        }

        val action24 = if (mode24 == WifiFilterMode.ALLOW) "1" else "2"
        val action5 = if (mode5 == WifiFilterMode.ALLOW) "1" else "2"

        val payload = mutableListOf(
            "sessionKey" to sessionKey,
            "savecfg" to savecfg,
            "savecfg_ac" to savecfgAc,
            "WLAN_FltAction" to action24,
            "WLAN11AC_FltAction" to action5,
            "flag" to flag,
            "flag11ac" to flag11ac
        )

        for (i in 0 until MAX_FILTER_SLOTS) {
            payload += "wlanflt_mac$i" to safe24[i]
        }

        for (i in 0 until MAX_FILTER_SLOTS) {
            payload += "wlan11acflt_mac$i" to safe5[i]
        }

        payload += "wlanflt_mac" to "Manual"
        payload += "wlanflt_manual" to ""
        payload += "wlan11acflt_mac" to "Manual"
        payload += "wlan11acflt_manual" to ""

        // >>> Add ALLOW mode condition <<<
        // Router requests this field only when mode is "allow" (1)
        if (action24 == "1") {
            payload.add("filterrule_ck" to "on")
        }
        if (action5 == "1") {
            payload.add("filterrule11ac_ck" to "on")
        }

        return payload
    }

    private data class RuntimeDeviceState(
        val mac: String,
        val ip: String,
        val hostname: String,
        val networkType: String,
        val isActive: Boolean,
        val isBlocked24: Boolean,
        val isBlocked5: Boolean,
        val isBlockedAny: Boolean,
        val sourceLinkType: String,
        val sourcePort: String,
        val sourceContype: String
    )
}
