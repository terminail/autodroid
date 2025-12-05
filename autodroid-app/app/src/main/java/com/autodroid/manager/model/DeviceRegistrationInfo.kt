package com.autodroid.manager.model

/**
 * 设备注册信息封装类
 */
data class DeviceRegistrationInfo(
    val udid: String? = null,
    val name: String? = null,
    val model: String? = null,
    val manufacturer: String? = null,
    val androidVersion: String? = null,
    val ipAddress: String? = null,
    val isConnected: Boolean = false,
    val capabilities: Map<String, Any>? = null
) {
    companion object {
        /**
         * 创建空设备注册信息
         */
        fun empty(): DeviceRegistrationInfo = DeviceRegistrationInfo()
        
        /**
         * 从DeviceInfo创建设备注册信息
         */
        fun fromDeviceInfo(deviceInfo: DeviceInfo, udid: String? = null): DeviceRegistrationInfo = DeviceRegistrationInfo(
            udid = udid,
            name = deviceInfo.name,
            model = deviceInfo.model,
            manufacturer = deviceInfo.manufacturer,
            androidVersion = deviceInfo.androidVersion,
            ipAddress = deviceInfo.ip,
            isConnected = deviceInfo.isConnected
        )
    }
}