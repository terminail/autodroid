package com.autodroid.trader.network

import com.google.gson.annotations.SerializedName

/**
 * Data class representing device create request
 * Only contains serial number as the server will retrieve all device information via ADB
 */
data class DeviceCreateRequest(
    @SerializedName("serialno")
    val serialNo: String
) {
    companion object {
        /**
         * 创建空设备创建信息
         */
        fun empty(): DeviceCreateRequest = DeviceCreateRequest("")
    }
}