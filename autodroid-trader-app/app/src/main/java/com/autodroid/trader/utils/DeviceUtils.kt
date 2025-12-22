package com.autodroid.trader.utils

import android.provider.Settings
import android.util.Log

/**
 * Utility class for device-related operations
 */
object DeviceUtils {
    private const val TAG = "DeviceUtils"
    
    /**
     * Get device UDID (unique identifier)
     * 
     * @param context The application context
     * @return The device UDID as a string, URL-safe and without problematic characters
     */
    fun getDeviceUDID(context: android.content.Context): String {
        return try {
            // 使用设备属性组合创建唯一标识符，避免ANDROID_ID隐私问题
            val deviceId = "${Build.MANUFACTURER}_${Build.MODEL}_${Build.VERSION.RELEASE}_${Build.ID}"
            
            // Ensure the UDID is URL-safe by removing problematic characters
            makeSerialNoUrlSafe(deviceId)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting device UDID", e)
            "Unknown-UDID"
        }
    }
    
    /**
     * Make serial number URL-safe by removing problematic characters
     */
    private fun makeSerialNoUrlSafe(serialNo: String): String {
        // Remove characters that can cause issues in URLs
        return serialNo.replace(":", "")
                  .replace("/", "")
                  .replace("\\", "")
                  .replace("?", "")
                  .replace("&", "")
                  .replace("=", "")
                  .replace("#", "")
                  .replace("%", "")
                  .replace(" ", "")
    }
}