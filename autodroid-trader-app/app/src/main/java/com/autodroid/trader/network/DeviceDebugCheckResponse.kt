package com.autodroid.trader.network

/**
 * 设备调试权限检查响应模型
 */
data class DeviceDebugCheckResponse(
    val success: Boolean,
    val message: String,
    val udid: String,
    val usb_debug_enabled: Boolean,
    val wifi_debug_enabled: Boolean,
    val check_time: String? = null
)