package com.autodroid.trader.network

import com.google.gson.annotations.SerializedName

/**
 * 设备调试权限、安装的apps等检查响应模型
 */
data class DeviceCheckResponse(
    val success: Boolean,
    val message: String,
    @SerializedName("serialno")
    val serialNo: String,
    @SerializedName("udid")
    val udid: String? = null,
    @SerializedName("usb_debug_enabled")
    val usbDebugEnabled: Boolean,
    @SerializedName("wifi_debug_enabled")
    val wifiDebugEnabled: Boolean,
    @SerializedName("check_time")
    val checkTime: String? = null,
    @SerializedName("installed_apps")
    val installedApps: List<AppInfo>? = null,
    @SerializedName("device_info")
    val deviceInfo: DeviceInfoResponse? = null
)