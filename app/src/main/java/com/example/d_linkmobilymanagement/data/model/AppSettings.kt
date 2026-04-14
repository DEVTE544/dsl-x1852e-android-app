package com.example.d_linkmobilymanagement.data.model

data class AppSettings(
    val autoRefreshEnabled: Boolean = true,
    val refreshIntervalSeconds: Int = 2
)
