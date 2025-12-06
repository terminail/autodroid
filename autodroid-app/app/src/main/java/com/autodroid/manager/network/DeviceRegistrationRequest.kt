package com.autodroid.manager.network

import com.autodroid.manager.model.Wifi
import com.autodroid.manager.model.Network
import com.autodroid.manager.model.Device

/**
 * Data class representing device registration request
 */
data class DeviceRegistrationRequest(
    val udid: String,
    val name: String? = null,
    val model: String? = null,
    val manufacturer: String? = null,
    val android_version: String? = null,
    val api_level: Int? = null,
    val screen_width: Int? = null,
    val screen_height: Int? = null,
    val wifi_info: Wifi? = null,
    val network_info: Network? = null
) {
    companion object {
        /**
         * 创建空设备注册信息
         */
        fun empty(): DeviceRegistrationRequest = DeviceRegistrationRequest("")
        
        /**
         * 从Device创建设备注册信息
         */
        fun fromDevice(device: Device, udid: String? = null): DeviceRegistrationRequest = DeviceRegistrationRequest(
            udid = udid ?: "",
            name = device.name,
            model = device.model,
            manufacturer = device.manufacturer,
            android_version = device.androidVersion
        )
    }
}