package com.autodroid.manager.utils

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
            // Try to get Android ID as UDID
            val androidId = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            ) ?: "Unknown-UDID"
            
            // Ensure the UDID is URL-safe by removing problematic characters
            makeUdidUrlSafe(androidId)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting device UDID", e)
            "Unknown-UDID"
        }
    }
    
    /**
     * Make UDID URL-safe by removing problematic characters
     */
    private fun makeUdidUrlSafe(udid: String): String {
        // Remove characters that can cause issues in URLs
        return udid.replace(":", "")
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