package com.autodroid.manager.apk

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
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
        val apkMap = mutableMapOf<String, ApkInfo>() // Use packageName as key for deduplication
        
        try {
            val packageManager = context.packageManager
            val packages = packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
            
            for (packageInfo in packages) {
                try {
                    // Filter out system apps and services, only include user-usable apps
                    if (isUserUsableApp(packageInfo)) {
                        val apkInfo = extractApkInfo(packageInfo, packageManager)
                        // Use packageName as unique identifier to avoid duplicates
                        apkMap[apkInfo.packageName] = apkInfo
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error extracting APK info for ${packageInfo.packageName}: ${e.message}")
                }
            }
            
            val apkList = apkMap.values.toList()
            Log.d(TAG, "Scanned ${apkList.size} unique user-usable APKs (filtered from ${packages.size} total packages)")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning APKs: ${e.message}")
        }
        
        return@withContext apkMap.values.toList()
    }
    
    /**
     * Check if an app is user-usable (not a system service or background process)
     */
    private fun isUserUsableApp(packageInfo: PackageInfo): Boolean {
        val applicationInfo = packageInfo.applicationInfo ?: return false
        
        // Filter criteria for user-usable apps:
        // 1. Not a system app (or if system app, has launcher intent)
        // 2. Has launcher activity (can be launched by user)
        // 3. Not a background service-only app
        
        val packageManager = context.packageManager
        
        // Check if app has launcher intent (can be launched by user)
        val launchIntent = packageManager.getLaunchIntentForPackage(packageInfo.packageName)
        val hasLauncherIntent = launchIntent != null
        
        // Check if it's a system app
        val isSystemApp = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
        
        // Check if it's a system app that's updated (user-installed system app)
        val isUpdatedSystemApp = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        
        // Check if it's a persistent app (background service)
        val isPersistent = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_PERSISTENT) != 0
        
        // Common system service packages to exclude
        val systemServicePackages = setOf(
            "com.android.", "android.", "com.google.android.gms",
            "com.google.android.apps.", "com.qualcomm.", "com.qti.",
            "com.android.systemui", "com.android.phone", "com.android.providers.",
            "com.android.settings", "com.android.inputmethod", "com.android.keychain",
            "com.android.captiveportallogin", "com.android.carrierconfig",
            "com.android.deskclock", "com.android.dialer", "com.android.documentsui",
            "com.android.externalstorage", "com.android.htmlviewer", "com.android.managedprovisioning",
            "com.android.mms", "com.android.packageinstaller", "com.android.printspooler",
            "com.android.proxyhandler", "com.android.server.", "com.android.sharedstoragebackup",
            "com.android.shell", "com.android.statementservice", "com.android.stk",
            "com.android.vending", "com.android.voicemail", "com.android.wallpaper"
        )
        
        // Check if package name matches system service patterns
        val isSystemService = systemServicePackages.any { packageInfo.packageName.startsWith(it) }
        
        // Additional filtering: exclude Autodroid Manager itself to avoid duplicates
        // But allow the current Autodroid Manager app to be scanned
        val isAutodroidManager = packageInfo.packageName.contains("autodroid") || 
                                packageInfo.packageName.contains("com.autodroid")
        
        // User-usable apps should:
        // 1. Have launcher intent OR be a user-installed system app
        // 2. Not be a persistent background service
        // 3. Not be a core system service
        // 4. Allow Autodroid Manager app to be scanned (but exclude other autodroid packages)
        
        return (hasLauncherIntent || isUpdatedSystemApp) && 
               !isPersistent && 
               !isSystemService &&
               !packageInfo.packageName.contains(".test") &&
               !packageInfo.packageName.contains(".demo") &&
               !packageInfo.packageName.contains(".sample")
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
     * Register device with the server before scanning APKs
     */
    suspend fun registerDeviceWithServer(deviceUdid: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val apiClient = ApiClient.getInstance()
            
            // Collect device information
            val deviceInfo = mapOf(
                "udid" to deviceUdid,
                "device_name" to Build.MODEL,
                "android_version" to Build.VERSION.RELEASE,
                "battery_level" to 50, // Default value
                "connection_type" to "network"
            )
            
            // Register device with server
            val response = apiClient.registerDevice(deviceInfo)
            
            if (response.isSuccessful) {
                Log.d(TAG, "Device registered successfully with UDID: $deviceUdid")
                return@withContext true
            } else {
                Log.e(TAG, "Failed to register device with UDID: $deviceUdid, response code: ${response.code}")
                return@withContext false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error registering device with server: ${e.message}")
            return@withContext false
        }
    }
    
    /**
     * Register scanned APKs with the server for a specific device
     */
    suspend fun registerApksWithServer(apkList: List<ApkInfo>, deviceUdid: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val apiClient = ApiClient.getInstance()
            
            // Debug: Log the device UDID and URL being constructed
            Log.d(TAG, "Registering APKs for device UDID: '$deviceUdid'")
            Log.d(TAG, "Constructed URL: ${ApiClient.BASE_URL}/api/devices/$deviceUdid/apks")
            
            // Convert APK list to data format for bulk registration
            val apkDataList = apkList.map { apkInfo ->
                mapOf(
                    "package_name" to apkInfo.packageName,
                    "app_name" to apkInfo.appName,
                    "version" to apkInfo.version,
                    "version_code" to apkInfo.versionCode,
                    "installed_time" to apkInfo.installedTime,
                    "is_system" to apkInfo.isSystem,
                    "icon_path" to apkInfo.iconPath
                )
            }
            
            // Use unified registration endpoint that always expects a list
            val response = apiClient.registerApksForDevice(deviceUdid, apkDataList)
            
            if (response.isSuccessful) {
                Log.d(TAG, "Registered ${apkList.size} APKs for device $deviceUdid")
                return@withContext true
            } else {
                Log.e(TAG, "Failed to register APKs for device $deviceUdid")
                return@withContext false
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
        val packageName: String,
        val appName: String,
        val version: String,
        val versionCode: Int,
        val installedTime: Long,
        val isSystem: Boolean,
        val iconPath: String
    )