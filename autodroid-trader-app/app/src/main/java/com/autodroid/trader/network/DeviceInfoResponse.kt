package com.autodroid.trader.network

/**
 * Data class representing device information response from FastAPI
 * This is the type-safe response model for device API calls
 */
data class DeviceInfoResponse(
    val udid: String,
    val name: String? = null,
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
    val screen_height: Int? = null,
    val registered_at: String? = null,
    val updated_at: String? = null,
    val status: String? = null,
    val usb_debug_enabled: Boolean? = false,
    val wifi_debug_enabled: Boolean? = false,
    val debug_check_status: String? = "UNKNOWN",
    val debug_check_message: String? = null,
    val debug_check_time: String? = null
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
        return registered_at != null
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
        return "DeviceInfoResponse(udid='$udid', name='$name', " +
                "model='$model', manufacturer='$manufacturer', " +
                "android_version='$android_version', platform='$platform', " +
                "ip='$ip', status='$status')"
    }
}