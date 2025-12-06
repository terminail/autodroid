package com.autodroid.manager.network

import com.autodroid.manager.apk.ApkScannerManager

/**
 * Response model for APK registration
 */
data class ApkRegistrationResponse(
    val success: Boolean,
    val message: String,
    val registered_apks: List<com.autodroid.manager.model.Apk> = emptyList(),
    val failed_apks: List<FailedApkRegistration> = emptyList()
)

/**
 * Model for failed APK registration details
 */
data class FailedApkRegistration(
    val apk_info: com.autodroid.manager.model.Apk,
    val error: String
)