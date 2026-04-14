package com.example.d_linkmobilymanagement.data.model

data class WanInterfaceStatus(
    val name: String,
    val ipVersion: String,
    val statusIpv4: String,
    val ipIpv4: String,
    val gatewayIpv4: String,
    val dnsIpv4: String,
    val mac: String,
    val typeIsp: String
)

data class SystemMenuItem(
    val titleResId: Int,
    val iconResId: Int,
    val route: String,
    val relatedKeywordsResIds: List<Int> = emptyList()
)
