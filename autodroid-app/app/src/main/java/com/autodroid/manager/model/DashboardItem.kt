package com.autodroid.manager.model

import com.autodroid.manager.ui.adapters.DashboardAdapter

sealed class DashboardItem(val type: Int) {
    data class ServerConnectionItem(
        val status: String = "Discovering servers...",
        val serverIp: String = "Searching...",
        val serverPort: String = "-",
        val serverStatus: String = "Disconnected",
        val apiEndpoint: String = "-",
        val showQrButton: Boolean = true,
        val isQrButtonEnabled: Boolean = true
    ) : DashboardItem(DashboardAdapter.TYPE_SERVER_CONNECTION)
    
    data class WiFiInfoItem(
        val ssid: String = "Not connected",
        val bssid: String = "Unknown",
        val signalStrength: Int = 0,
        val frequency: Int = 0,
        val ipAddress: String = "-",
        val linkSpeed: Int = 0,
        val isConnected: Boolean = false
    ) : DashboardItem(DashboardAdapter.TYPE_WIFI)
    
    data class DeviceInfoItem(
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
        val apkInfo: ApkInfo
    ) : DashboardItem(DashboardAdapter.TYPE_APK)
    
    data class ApkInfo(
        val packageName: String,
        val appName: String,
        val version: String,
        val versionCode: Int,
        val installTime: String = "Unknown",
        val updateTime: String = "Unknown"
    ) : DashboardItem(DashboardAdapter.TYPE_APK_INFO)
}