package com.autodroid.trader.network

/**
 * Data class representing device create request
 * Only contains essential fields that cannot be retrieved via ADB on server side
 */
data class DeviceCreateRequest(
    val serialNo: String,
    val name: String
) {
    companion object {
        /**
         * 创建空设备创建信息
         */
        fun empty(): DeviceCreateRequest = DeviceCreateRequest("", "Unknown Device")
    }
}