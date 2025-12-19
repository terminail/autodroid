package com.autodroid.trader.network

/**
 * Response model for device registration
 */
data class DeviceRegistrationResponse(
    val success: Boolean,
    val message: String,
    val device_id: String? = null,
    val udid: String? = null,
    val registered_at: String? = null
)