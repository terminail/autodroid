package com.autodroid.trader.network

import com.google.gson.annotations.SerializedName

/**
 * Data class representing app information
 */
data class AppInfo(
    @SerializedName("package_name")
    val packageName: String,
    @SerializedName("app_name")
    val appName: String,
    val version: String? = null,
    @SerializedName("version_code")
    val versionCode: Int? = null,
    @SerializedName("installed_time")
    val installedTime: Long? = null,
    @SerializedName("is_system")
    val isSystem: Boolean? = false,
    @SerializedName("icon_path")
    val iconPath: String? = null
)

/**
 * Data class representing device information response from FastAPI
 * This is the type-safe response model for device API calls
 */
data class DeviceInfoResponse(
    @SerializedName("serialno")
    val serialNo: String,
    val name: String? = null,
    val model: String? = null,
    val manufacturer: String? = null,
    @SerializedName("android_version")
    val androidVersion: String? = null,
    @SerializedName("api_level")
    val apiLevel: Int? = null,
    val platform: String? = null,
    val brand: String? = null,
    val device: String? = null,
    val product: String? = null,
    val ip: String? = null,
    @SerializedName("screen_width")
    val screenWidth: Int? = null,
    @SerializedName("screen_height")
    val screenHeight: Int? = null,
    @SerializedName("registered_at")
    val registeredAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null,
    val status: String? = null,
    @SerializedName("usb_debug_enabled")
    val usbDebugEnabled: Boolean? = false,
    @SerializedName("wifi_debug_enabled")
    val wifiDebugEnabled: Boolean? = false,
    @SerializedName("check_status")
    val checkStatus: String? = "UNKNOWN",
    @SerializedName("check_message")
    val checkMessage: String? = null,
    @SerializedName("check_time")
    val checkTime: String? = null,
    val apps: List<AppInfo>? = null
) {
    /**
     * Check if device is online
     */
    fun isOnline(): Boolean {
        return status?.equals("online", ignoreCase = true) ?: false
    }
    
    /**
     * Check if device is registered
     */
    fun isRegistered(): Boolean {
        return registeredAt != null
    }
    
    /**
     * Get device platform with fallback
     */
    fun getPlatformString(): String {
        return platform ?: "Android"
    }
    
    /**
     * Get device full name with model
     */
    fun getFullName(): String {
        return when {
            !name.isNullOrEmpty() && !model.isNullOrEmpty() -> "$name ($model)"
            !name.isNullOrEmpty() -> name!!
            !model.isNullOrEmpty() -> model!!
            else -> "Unknown Device"
        }
    }

    override fun toString(): String {
        return "DeviceInfoResponse(serialNo='$serialNo', name='$name', " +
                "model='$model', manufacturer='$manufacturer', " +
                "androidVersion='$androidVersion', platform='$platform', " +
                "ip='$ip', status='$status')"
    }
}