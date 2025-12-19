package com.autodroid.trader.network

/**
 * Response model for device information
 */
data class DeviceInfoResponse(
    val udid: String,
    val name: String? = null,
    val model: String? = null,
    val manufacturer: String? = null,
    val android_version: String? = null,
    val api_level: Int? = null,
    val screen_width: Int? = null,
    val screen_height: Int? = null,
    val registered_at: String? = null,
    val last_seen: String? = null,
    val status: String? = null
)