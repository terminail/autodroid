package com.autodroid.manager.apk

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.Base64
import android.util.Log
import com.autodroid.manager.network.ApiClient
import com.autodroid.manager.utils.DeviceUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Manager class for scanning installed APKs on the Android device
 */
class ApkScannerManager(private val context: Context) {
    
    companion object {
        private const val TAG = "ApkScannerManager"
    }
    
    /**
     * Scan all installed APKs on the device
     */
    suspend fun scanInstalledApks(): List<ApkInfo> = withContext(Dispatchers.IO) {
        val apkList = mutableListOf<ApkInfo>()
        
        try {
            val packageManager = context.packageManager
            val packages = packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
            
            for (packageInfo in packages) {
                try {
                    val apkInfo = extractApkInfo(packageInfo, packageManager)
                    apkList.add(apkInfo)
                } catch (e: Exception) {
                    Log.e(TAG, "Error extracting APK info for ${packageInfo.packageName}: ${e.message}")
                }
            }
            
            Log.d(TAG, "Scanned ${apkList.size} APKs")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning APKs: ${e.message}")
        }
        
        return@withContext apkList
    }
    
    /**
     * Extract APK information from PackageInfo
     */
    private fun extractApkInfo(packageInfo: PackageInfo, packageManager: PackageManager): ApkInfo {
        val applicationInfo = packageInfo.applicationInfo
        
        // Get app icon as base64 string
        val iconBase64 = applicationInfo?.let { getAppIconBase64(it, packageManager) } ?: ""
        
        // Determine if it's a system app
        val isSystemApp = applicationInfo?.let { 
            (it.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0 
        } ?: false
        
        return ApkInfo(
            apkid = packageInfo.packageName,
            packageName = packageInfo.packageName,
            appName = applicationInfo?.let { packageManager.getApplicationLabel(it).toString() } ?: packageInfo.packageName,
            version = packageInfo.versionName ?: "1.0",
            versionCode = packageInfo.versionCode,
            installedTime = packageInfo.firstInstallTime,
            isSystem = isSystemApp,
            iconPath = iconBase64
        )
    }
    
    /**
     * Convert app icon to base64 string for transmission
     */
    private fun getAppIconBase64(applicationInfo: android.content.pm.ApplicationInfo, packageManager: PackageManager): String {
        return try {
            val icon: Drawable = packageManager.getApplicationIcon(applicationInfo)
            val bitmap = when (icon) {
                is android.graphics.drawable.BitmapDrawable -> icon.bitmap
                else -> {
                    val bitmap = android.graphics.Bitmap.createBitmap(icon.intrinsicWidth, icon.intrinsicHeight, android.graphics.Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(bitmap)
                    icon.setBounds(0, 0, canvas.width, canvas.height)
                    icon.draw(canvas)
                    bitmap
                }
            }
            
            val stream = ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
            val iconBytes = stream.toByteArray()
            Base64.encodeToString(iconBytes, Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e(TAG, "Error converting icon to base64: ${e.message}")
            ""
        }
    }
    
    /**
     * Register scanned APKs with the server for a specific device
     */
    suspend fun registerApksWithServer(apkList: List<ApkInfo>, deviceUdid: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val apiClient = ApiClient.getInstance()
            
            // Convert APK list to data format for bulk registration
            val apkDataList = apkList.map { apkInfo ->
                mapOf(
                    "apkid" to apkInfo.apkid,
                    "package_name" to apkInfo.packageName,
                    "app_name" to apkInfo.appName,
                    "version" to apkInfo.version,
                    "version_code" to apkInfo.versionCode,
                    "installed_time" to apkInfo.installedTime,
                    "is_system" to apkInfo.isSystem,
                    "icon_path" to apkInfo.iconPath
                )
            }
            
            // Use bulk registration endpoint for better performance
            val response = apiClient.registerApksBulkForDevice(deviceUdid, apkDataList)
            
            if (response.isSuccessful) {
                Log.d(TAG, "Registered ${apkList.size} APKs for device $deviceUdid")
                return@withContext true
            } else {
                Log.e(TAG, "Failed to register APKs in bulk for device $deviceUdid")
                
                // Fallback to individual registration if bulk fails
                var successCount = 0
                for (apkData in apkDataList) {
                    try {
                        val individualResponse = apiClient.registerApkForDevice(deviceUdid, apkData)
                        if (individualResponse.isSuccessful) {
                            successCount++
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to register APK individually: ${e.message}")
                    }
                }
                
                Log.d(TAG, "Fallback registration: $successCount/${apkList.size} APKs registered")
                return@withContext successCount > 0
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error registering APKs with server: ${e.message}")
            return@withContext false
        }
    }
}

/**
 * Data class representing APK information
 */
data class ApkInfo(
    val apkid: String,
    val packageName: String,
    val appName: String,
    val version: String,
    val versionCode: Int,
    val installedTime: Long,
    val isSystem: Boolean,
    val iconPath: String
)