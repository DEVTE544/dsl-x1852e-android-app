package com.example.d_linkmobilymanagement.data.remote

import androidx.core.text.HtmlCompat
import com.example.d_linkmobilymanagement.data.model.ConnectedClientRaw
import com.example.d_linkmobilymanagement.data.model.WifiClientRaw
import com.example.d_linkmobilymanagement.data.model.WifiMacListRaw
import com.example.d_linkmobilymanagement.data.model.PreLoginInfo
import com.example.d_linkmobilymanagement.data.model.WanInterfaceStatus
import timber.log.Timber

object RouterParser {

    private const val MAX_FILTER_SLOTS = 16

    private val varPattern = Regex(
        pattern = """(?:var\s+)?([A-Za-z0-9_]+)\s*=\s*(?:'([^']*)'|"([^"]*)")\s*;""",
        options = setOf(RegexOption.MULTILINE)
    )

    fun parseJsVariables(text: String): Map<String, String> {
        val data = linkedMapOf<String, String>()

        varPattern.findAll(text).forEach { match ->
            val key = match.groupValues.getOrElse(1) { "" }
            val val1 = match.groupValues.getOrElse(2) { "" }
            val val2 = match.groupValues.getOrElse(3) { "" }
            data[key] = val1.ifEmpty { val2 }
        }

        return data
    }

    fun normalizeMac(mac: String?): String {
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

    fun decodeHostname(value: String?): String {
        if (value.isNullOrBlank()) return "N/A"
        if (value.trim().lowercase() == "n/a") return "N/A"

        val decoded = HtmlCompat.fromHtml(
            value,
            HtmlCompat.FROM_HTML_MODE_LEGACY
        ).toString().trim()

        return decoded.ifBlank { "N/A" }
    }

    fun detectNetworkType(
        linktype: String = "",
        port: String = "",
        contype: String = ""
    ): String {
        var resolvedPort = port.trim()
        val resolvedContype = contype.trim()
        val resolvedLinktype = linktype.trim()

        if (resolvedPort.isBlank() && resolvedLinktype.isNotBlank()) {
            val match = Regex("""\.(\d+)$""").find(resolvedLinktype)
            if (match != null) {
                resolvedPort = match.groupValues.getOrElse(1) { "" }
            }
        }

        if (resolvedContype == "0") return "LAN"
        if (resolvedContype == "1") {
            if (resolvedPort == "5") return "2.4GHz"
            if (resolvedPort == "13") return "5GHz"
            return "WiFi"
        }

        return when (resolvedPort) {
            "1", "2", "3", "4" -> "LAN"
            "5" -> "2.4GHz"
            "13" -> "5GHz"
            else -> "Unknown"
        }
    }

    fun parseConnectedDevices(text: String): List<ConnectedClientRaw> {
        val raw = parseJsVariables(text)
        val leaseNum = raw["LeaseNum"] ?: "0"
        val maxIndex = maxOf(leaseNum.toIntOrNull() ?: 0, 29)

        val devices = mutableListOf<ConnectedClientRaw>()

        for (i in 0..maxIndex) {
            val prefix = "dhcpClient${i}_"
            val mac = normalizeMac(raw[prefix + "mac"])

            if (mac.isBlank()) continue

            val ipRaw = raw[prefix + "ip"]?.trim().orEmpty()
            val ip = ipRaw.ifBlank { "N/A" }
            val hostname = decodeHostname(raw[prefix + "hostname"])
            val active = raw[prefix + "active"]?.trim().orEmpty()
            val linktype = raw[prefix + "linktype"]?.trim().orEmpty()

            Timber.d("[$prefix] mac=$mac, ip_raw='$ipRaw', ip=$ip, hostname=$hostname, active=$active")

            devices += ConnectedClientRaw(
                mac = mac,
                ip = ip,
                hostname = hostname,
                isActive = active == "1",
                sourceLinkType = linktype,
                networkType = detectNetworkType(linktype = linktype)
            )
        }

        return devices
    }

    fun parseWifiMacList(macListText: String, pageText: String = ""): WifiMacListRaw {
        val macRaw = parseJsVariables(macListText)
        val pageRaw = if (pageText.isNotEmpty()) parseJsVariables(pageText) else emptyMap()

        val clients = linkedMapOf<String, WifiClientRaw>()

        for (i in 0 until 16) {
            val prefix = "dhcpClient${i}_"
            val mac = normalizeMac(macRaw[prefix + "mac"])

            if (mac.isBlank()) continue

            val port = macRaw[prefix + "port"]?.trim().orEmpty()
            val contype = macRaw[prefix + "contype"]?.trim().orEmpty()
            val active = macRaw[prefix + "active"]?.trim().orEmpty()

            clients[mac] = WifiClientRaw(
                mac = mac,
                sourcePort = port,
                sourceContype = contype,
                isActive = active == "1",
                networkType = detectNetworkType(port = port, contype = contype)
            )
        }

        val blocked24 = mutableListOf<String>()
        val blocked5 = mutableListOf<String>()
        val slots24 = mutableListOf<String>()
        val slots5 = mutableListOf<String>()

        // Source of truth for modes: allowflag and allow11acflag from wifiFilter.asp
        // If pageText is not provided, fallback to WLAN_FltAction (though less reliable)
        val mode24 = pageRaw["allowflag"] ?: macRaw["WLAN_FltAction"] ?: "0"
        val mode5 = pageRaw["allow11acflag"] ?: macRaw["WLAN11AC_FltAction"] ?: "0"
        val sessionKey = pageRaw["sessionKey"] ?: ""

        for (i in 0 until MAX_FILTER_SLOTS) {
            val mac24 = normalizeMac(macRaw["wlanflt_mac$i"])
            val mac5 = normalizeMac(macRaw["wlan11acflt_mac$i"])

            slots24 += mac24
            slots5 += mac5

            if (mac24.isNotBlank()) blocked24 += mac24
            if (mac5.isNotBlank()) blocked5 += mac5
        }

        return WifiMacListRaw(
            clients = clients,
            mode24 = mode24,
            mode5 = mode5,
            blocked24 = blocked24,
            blocked5 = blocked5,
            slots24 = slots24,
            slots5 = slots5,
            sessionKey = sessionKey
        )
    }

    fun parsePreLoginInfo(html: String): PreLoginInfo? {
        if (html.isBlank()) return null

        // 1 extract model name with high flexibility
        var modelName = "Unknown"
        val modelPatterns = listOf(
            Regex("""<label[^>]*id="modelName"[^>]*>([^<]+)</label>""", RegexOption.IGNORE_CASE),
            Regex("""var\s+m_wizard_modelname\s*=\s*['"]([^'"]+)['"]"""), // from JS variables
            Regex("""<div[^>]*class="model"[^>]*>([^<]+)</div>""", RegexOption.IGNORE_CASE)
        )
        for (pattern in modelPatterns) {
            val match = pattern.find(html)?.groupValues?.get(1)?.trim()
            if (!match.isNullOrBlank()) {
                // 🟢 decode HTML entities to convert &#45; to normal dash -
                modelName = HtmlCompat.fromHtml(match, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
                break
            }
        }

        // 2 extract firmware version with high flexibility
        var firmwareVersion = "Unknown"
        val fwPatterns = listOf(
            Regex("""<label[^>]*id="FWversion"[^>]*>([^<]+)</label>""", RegexOption.IGNORE_CASE),
            Regex("""var\s+ini_ver\s*=\s*['"]([^'"]+)['"]"""), // common in D-Link JS variables
            Regex("""var\s+m_wizard_fw\s*=\s*['"]([^'"]+)['"]""")
        )
        for (pattern in fwPatterns) {
            val match = pattern.find(html)?.groupValues?.get(1)?.trim()
            if (!match.isNullOrBlank()) {
                firmwareVersion = match
                break
            }
        }

        val users = mutableListOf<String>()
        
        val selectBlockRegex = Regex("""<select[^>]*name="(?:userid|user|account)"[^>]*>(.*?)</select>""", RegexOption.DOT_MATCHES_ALL)
        val selectBlock = selectBlockRegex.find(html)?.groupValues?.get(1)
        
        if (selectBlock != null) {
            Regex("""<option\s+value="[^"]+"[^>]*>([^<]+)</option>""").findAll(selectBlock).forEach { matchResult ->
                // 🟢 we take the display name only (SuperAdmin) which is what the router actually accepts!
                users.add(matchResult.groupValues[1].trim())
            }
        }
        
        if (users.isEmpty()) {
            val accPattern = Regex("""var\s+acc\d+\s*=\s*['"]([^'"]+)['"]""")
            val jsUsers = accPattern.findAll(html).map { it.groupValues[1].trim() }.toList()
            if (jsUsers.isNotEmpty()) {
                users.addAll(jsUsers)
            }
        }

        val finalUsers = if (users.isNotEmpty()) users.distinct() else listOf("SuperAdmin", "admin")

        val result = PreLoginInfo(
            modelName = modelName,
            firmwareVersion = firmwareVersion,
            availableUsers = finalUsers
        )
        
        Timber.d("Dynamic PreLoginInfo Extracted: $result")
        return result
    }

    fun parseInternetStatus(html: String): List<WanInterfaceStatus> {
        // Helper function to extract array string based on the variable name it's assigned to
        fun extractArray(varName: String): List<String> {
            // Matches: vArrayStr = "value1,value2,"; \n var Wan_WanName = vArrayStr.split(',');
            val regex = Regex("""vArrayStr\s*=\s*"([^"]*)";\s*var\s+$varName\s*=""")
            val match = regex.find(html)
            val valueString = match?.groups?.get(1)?.value ?: return emptyList()
            return valueString.split(",").filter { it.isNotEmpty() }
        }

        val names = extractArray("Wan_WanName")
        val ipVersions = extractArray("Wan_IPVERSION")
        val status4 = extractArray("Wan_Status4")
        val ips = extractArray("Wan_IP4")
        val gateways = extractArray("Wan_GateWay4")
        val dns = extractArray("Wan_DNS4")
        val macs = extractArray("Wan_MAC")
        val types = extractArray("wanTypeIsp")

        val interfaces = mutableListOf<WanInterfaceStatus>()
        val count = names.size
        
        for (i in 0 until count) {
            interfaces.add(
                WanInterfaceStatus(
                    name = names.getOrNull(i) ?: "Unknown",
                    ipVersion = ipVersions.getOrNull(i) ?: "N/A",
                    statusIpv4 = status4.getOrNull(i) ?: "down",
                    ipIpv4 = ips.getOrNull(i) ?: "N/A",
                    gatewayIpv4 = gateways.getOrNull(i) ?: "N/A",
                    dnsIpv4 = dns.getOrNull(i) ?: "N/A",
                    mac = macs.getOrNull(i) ?: "N/A",
                    typeIsp = types.getOrNull(i) ?: "N/A"
                )
            )
        }
        
        Timber.d("Parsed ${interfaces.size} WAN interfaces")
        return interfaces
    }
}
