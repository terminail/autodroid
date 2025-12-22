package com.autodroid.trader.network

/**
 * Data class representing device create request
 */
data class DeviceCreateRequest(
    val serialNo: String,
    val name: String,
    val model: String? = null,
    val manufacturer: String? = null,
    val android_version: String? = null,
    val api_level: Int? = null,
    val platform: String? = null,
    val brand: String? = null,
    val device: String? = null,
    val product: String? = null,
    val ip: String? = null,
    val screen_width: Int? = null,
    val screen_height: Int? = null
) {
    companion object {
        /**
         * 创建空设备创建信息
         */
        fun empty(): DeviceCreateRequest = DeviceCreateRequest("", "Unknown Device")
    }
}