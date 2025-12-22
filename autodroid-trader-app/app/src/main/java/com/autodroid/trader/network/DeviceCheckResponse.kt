package com.autodroid.trader.network

/**
 * 设备调试权限、安装的apps等检查响应模型
 */
data class DeviceCheckResponse(
    val success: Boolean,
    val message: String,
    val serialNo: String,
    val usb_debug_enabled: Boolean,
    val wifi_debug_enabled: Boolean,

    val check_time: String? = null
)