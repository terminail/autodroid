// ApkInfoItemManager.kt
package com.autodroid.manager.ui.dashboard

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import com.autodroid.manager.viewmodel.AppViewModel
import com.autodroid.manager.model.DashboardItem
import com.autodroid.manager.apk.ApkScannerManager
import com.autodroid.manager.utils.DeviceUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Manager class for handling APK Information dashboard item functionality
 */
class ApkInfoItemManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val viewModel: AppViewModel,
    private val onItemUpdate: (DashboardItem) -> Unit
) {
    
    companion object {
        private const val TAG = "ApkInfoItemManager"
    }
    
    /**
     * Initialize the ApkInfoItemManager
     */
    fun initialize() {
        setupApkObservers()
        updateApkInfo()
    }
    
    /**
     * Set up observers for APK information changes
     */
    private fun setupApkObservers() {
        // Observe APK information changes from ViewModel
        viewModel.apkInfo.observe(lifecycleOwner) { apkInfo ->
            apkInfo?.let {
                val safeMap = it.mapKeys { (key, _) -> key ?: "" }.mapValues { (_, value) -> value ?: "" }
                updateApkItem(safeMap)
            }
        }
        
        // Observe APK scan status changes
        viewModel.apkScanStatus.observe(lifecycleOwner) { scanStatus ->
            scanStatus?.let {
                updateApkItemWithScanStatus(mapOf("status" to it))
            }
        }
    }
    
    /**
     * Update APK information and refresh the dashboard item
     */
    fun updateApkInfo() {
        try {
            val apkInfo = getCurrentApkInfo()
            
            // Update ViewModel with APK info
            viewModel.setApkInfo(apkInfo.toMutableMap())
            
            // Create and update dashboard item
            val apkItem = DashboardItem.ApkInfoItem(
                packageName = apkInfo["packageName"] as? String ?: "",
                version = apkInfo["version"] as? String ?: ""
            )
            
            onItemUpdate(apkItem)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating APK info: ${e.message}")
        }
    }
    
    /**
     * Update APK item with APK information
     */
    private fun updateApkItem(apkInfo: Map<String, Any>) {
        try {
            val apkItem = DashboardItem.ApkInfoItem(
                packageName = apkInfo["packageName"] as? String ?: "",
                version = apkInfo["version"] as? String ?: ""
            )
            
            onItemUpdate(apkItem)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating APK item: ${e.message}")
        }
    }
    
    /**
     * Update APK item with scan status
     */
    private fun updateApkItemWithScanStatus(scanStatus: Map<String, Any>) {
        try {
            val currentApkInfo = viewModel.apkInfo.value ?: emptyMap()
            
            val apkItem = DashboardItem.ApkInfoItem(
                packageName = currentApkInfo["packageName"] as? String ?: "",
                version = currentApkInfo["version"] as? String ?: ""
            )
            
            onItemUpdate(apkItem)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating APK item with scan status: ${e.message}")
        }
    }
    
    /**
     * Get current APK information
     */
    private fun getCurrentApkInfo(): Map<String, Any> {
        return try {
            // This would typically scan the device for APKs
            // For now, return mock data
            val apkInfoMap = mutableMapOf<String, Any>()
            
            apkInfoMap["packageName"] = "com.example.app"
            apkInfoMap["version"] = "1.0.0"
            
            apkInfoMap
        } catch (e: Exception) {
            Log.e(TAG, "Error getting APK info: ${e.message}")
            emptyMap()
        }
    }
    
    /**
     * Handle scan APKs button click
     */
    fun handleScanApksClick() {
        try {
            // Update scan status
            viewModel.setApkScanStatus("Scanning APKs...")
            
            // Start scanning in background
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Get device UDID
                    val deviceUdid = DeviceUtils.getDeviceUDID(context)
                    
                    // Create ApkScannerManager instance
                    val apkScannerManager = ApkScannerManager(context)
                    
                    // Scan installed APKs
                    val apkList = apkScannerManager.scanInstalledApks()
                    
                    // Register APKs with server
                    apkScannerManager.registerApksWithServer(apkList, deviceUdid)
                    
                    // Update UI on main thread
                    CoroutineScope(Dispatchers.Main).launch {
                        viewModel.setApkScanStatus("Scan completed: ${apkList.size} APKs found")
                        
                        // Update APK info with scan results
                        val apkInfo = mutableMapOf<String?, Any?>()
                        apkInfo["packageName"] = "Multiple APKs"
                        apkInfo["version"] = "${apkList.size} packages"
                        viewModel.setApkInfo(apkInfo)
                        
                        // Update dashboard item
                        val apkItem = DashboardItem.ApkInfoItem(
                            packageName = "Multiple APKs",
                            version = "${apkList.size} packages"
                        )
                        onItemUpdate(apkItem)
                    }
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error scanning APKs: ${e.message}")
                    CoroutineScope(Dispatchers.Main).launch {
                        viewModel.setApkScanStatus("Scan failed: ${e.message}")
                    }
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling APK scan click: ${e.message}")
            viewModel.setApkScanStatus("Scan failed: ${e.message}")
        }
    }
    
    /**
     * Refresh APK information manually
     */
    fun refresh() {
        updateApkInfo()
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        // Clean up any observers or resources if needed
    }
}