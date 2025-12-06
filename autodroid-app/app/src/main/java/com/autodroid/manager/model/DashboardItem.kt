package com.autodroid.manager.model

import com.autodroid.manager.apk.ApkScannerManager
import com.autodroid.manager.ui.adapters.DashboardAdapter

sealed class DashboardItem(val type: Int) {
    data class ServerItem(
        val status: String = "Discovering servers...",
        val serverStatus: String = "Disconnected",
        val apiEndpoint: String = "-",
        val discoveryMethod: String = "Auto Discovery",
        val isStartMdnsButtonEnabled: Boolean = true,
        val serverName: String = "-",
        val hostname: String = "-",
        val platform: String = "-"
    ) : DashboardItem(DashboardAdapter.TYPE_SERVER)
    
    data class WiFiItem(
        val ssid: String = "Not connected",
        val bssid: String = "Unknown",
        val signalStrength: Int = 0,
        val frequency: Int = 0,
        val ipAddress: String = "-",
        val linkSpeed: Int = 0,
        val isConnected: Boolean = false
    ) : DashboardItem(DashboardAdapter.TYPE_WIFI)
    
    data class DeviceItem(
        val udid: String = "KNT-AL10-1234567890",
        val userId: String = "user001",
        val name: String = "KNT-AL10",
        val platform: String = "Android",
        val deviceModel: String = "KNT-AL10",
        val deviceStatus: String = "在线",
        val connectionTime: String = "2024-01-01 00:00:00"
    ) : DashboardItem(DashboardAdapter.TYPE_DEVICE)
    
    data class ApkScannerItem(
        val scanStatus: String = "SCAN INSTALLED APKS",
        val statusMessage: String = "Ready to scan",
        val showButton: Boolean = true
    ) : DashboardItem(DashboardAdapter.TYPE_APK_SCANNER)
    
    data class ApkItem(
        val apkInfo: com.autodroid.manager.model.Apk
    ) : DashboardItem(DashboardAdapter.TYPE_APK)
}