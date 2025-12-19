package com.autodroid.trader.network

/**
 * Response model for APK registration
 */
data class ApkRegistrationResponse(
    val success: Boolean,
    val message: String
)

/**
 * Model for failed APK registration details
 */
data class FailedApkRegistration(
    val error: String
)