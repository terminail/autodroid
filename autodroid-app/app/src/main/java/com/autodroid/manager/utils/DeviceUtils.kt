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
     * @return The device UDID as a string, or "Unknown-UDID" if not available
     */
    fun getDeviceUDID(context: android.content.Context): String {
        return try {
            // Try to get Android ID as UDID
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            ) ?: "Unknown-UDID"
        } catch (e: Exception) {
            Log.e(TAG, "Error getting device UDID", e)
            "Unknown-UDID"
        }
    }
}