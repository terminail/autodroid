package com.autodroid.trader.network

import com.google.gson.annotations.SerializedName

/**
 * Response model for device creation
 */
data class DeviceCreateResponse(
    val success: Boolean,
    val message: String,
    val device_id: String? = null,
    @SerializedName("serialno")
    val serialNo: String? = null,
    val registered_at: String? = null,
    val device: DeviceInfoResponse? = null
)