package com.autodroid.trader.network

/**
 * Response model for device creation
 */
data class DeviceCreateResponse(
    val success: Boolean,
    val message: String,
    val device_id: String? = null,
    val serialNo: String? = null,
    val registered_at: String? = null,
    val device: DeviceInfoResponse? = null
)