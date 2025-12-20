package com.autodroid.trader.ui.dashboard

import com.autodroid.trader.ui.dashboard.DashboardAdapter

sealed class DashboardItem(val type: Int) {
    data class ItemServer(
        val status: String = "Discovering servers...",
        val serverStatus: String = "Disconnected",
        val apiEndpoint: String = "-",
        val discoveryMethod: String = "Auto Discovery",
        val serverName: String = "-",
        val hostname: String = "-",
        val platform: String = "-",
        val serverDiscoveryStatus: String = "等待扫描..."
    ) : DashboardItem(DashboardAdapter.Companion.TYPE_SERVER)

    data class ItemWiFi(
        val ssid: String = "Not connected",
        val bssid: String = "Unknown",
        val signalStrength: Int = 0,
        val frequency: Int = 0,
        val ipAddress: String = "-",
        val linkSpeed: Int = 0,
        val isConnected: Boolean = false
    ) : DashboardItem(DashboardAdapter.Companion.TYPE_WIFI)

    data class ItemDevice(
        val udid: String = "KNT-AL10-1234567890",
        val userId: String = "user001",
        val name: String = "KNT-AL10",
        val platform: String = "Android",
        val deviceModel: String = "KNT-AL10",
        val deviceStatus: String = "在线",
        val connectionTime: String = "2024-01-01 00:00:00"
    ) : DashboardItem(DashboardAdapter.Companion.TYPE_DEVICE)

    data class ItemPortRange(
        val portStart: Int = 8000,
        val portEnd: Int = 9000
    ) : DashboardItem(DashboardAdapter.Companion.TYPE_PORT_RANGE)
}