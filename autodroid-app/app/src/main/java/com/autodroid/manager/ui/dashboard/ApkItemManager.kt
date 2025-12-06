// ApkItemManager.kt
package com.autodroid.manager.ui.dashboard

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import com.autodroid.manager.AppViewModel
import com.autodroid.manager.model.DashboardItem
import com.autodroid.manager.apk.ApkScannerManager
import com.autodroid.manager.utils.DeviceUtils
import com.autodroid.manager.ui.adapters.DashboardAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Manager class for handling APK Information dashboard item functionality
 */
class ApkItemManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val viewModel: AppViewModel,
    private val onItemUpdate: (DashboardItem) -> Unit
) {
    
    companion object {
        private const val TAG = "ApkItemManager"
    }
    
    // selectedApkIndex is now managed by AppViewModel
    
    /**
     * Initialize the ApkItemManager
     */
    fun initialize() {
        setupApkObservers()
        updateApkInfo()
    }
    
    /**
     * Set up observers for APK information changes
     */
    private fun setupApkObservers() {
        // Simplified observer - only observe APK information changes from ViewModel
        viewModel.apk.observe(lifecycleOwner) { apk ->
            apk?.let {
                // Only show APK info if it's a valid APK (not empty)
                if (apk.packageName.isNotEmpty() && apk.appName.isNotEmpty()) {
                    val apkItem = DashboardItem.ApkItem(
                apkInfo = com.autodroid.manager.model.Apk(
                    packageName = apk.packageName ?: "Unknown",
                    appName = apk.appName ?: "Unknown App",
                    version = apk.version ?: "Unknown",
                    versionCode = apk.versionCode ?: 0,
                    installedTime = apk.installedTime ?: 0L,
                    isSystem = false,
                    iconPath = ""
                )
            )
                    onItemUpdate(apkItem)
                } else {
                    // Hide APK info section if no valid APK
                    val emptyApkInfoItem = DashboardItem.ApkItem(
                apkInfo = com.autodroid.manager.model.Apk(
                    packageName = "",
                    appName = "",
                    version = "",
                    versionCode = 0,
                    installedTime = 0L,
                    isSystem = false,
                    iconPath = ""
                )
            )
                    onItemUpdate(emptyApkInfoItem)
                }
            }
        }
    }
    
    /**
     * Update APK information and refresh the dashboard item
     */
    fun updateApkInfo() {
        // This method is simplified - APK info is now handled directly through ViewModel
        // The actual APK information comes from APK scanning process
        try {
            // Clear any existing APK info to indicate no APK is selected
            // Note: This method is no longer needed as we're using ApkScannerManager.ApkInfo directly
            
            // Remove APK info item from dashboard
            val emptyApkInfoItem = DashboardItem.ApkItem(
                apkInfo = com.autodroid.manager.model.Apk(
                    packageName = "",
                    appName = "",
                    version = "",
                    versionCode = 0,
                    installedTime = 0L,
                    isSystem = false,
                    iconPath = ""
                )
            )
            onItemUpdate(emptyApkInfoItem)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating APK info: ${e.message}")
        }
    }
    

    
    /**
     * Handle APK information display - scan functionality is handled by ApkScannerItemManager
     */
    fun handleApkInfoDisplay() {
        // This method is intentionally left empty as scanning is handled by ApkScannerItemManager
        // ApkItemManager only displays APK information provided by the scanning process
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
    
    /**
     * Handle list update logic for APK info item
     */
    fun handleListUpdate(item: DashboardItem, dashboardItems: MutableList<DashboardItem>, dashboardAdapter: DashboardAdapter?): Boolean {
        return try {
            if (item is DashboardItem.ApkItem) {
                // Only add/update APK info item if we have valid APK information
                if (item.apkInfo.packageName.isNotEmpty()) {
                    // Find existing APK info item in the list
                    val existingIndex = dashboardItems.indexOfFirst { it is DashboardItem.ApkItem }
                    
                    if (existingIndex != -1) {
                        // Update existing item
                        dashboardItems[existingIndex] = item
                    } else {
                        // Add new item after APK scanner item
                        val apkScannerIndex = dashboardItems.indexOfFirst { it is DashboardItem.ApkScannerItem }
                        if (apkScannerIndex != -1) {
                            dashboardItems.add(apkScannerIndex + 1, item)
                        } else {
                            // Fallback: add to the end
                            dashboardItems.add(item)
                        }
                    }
                } else {
                    // Remove APK info item if no valid APK information
                    val existingIndex = dashboardItems.indexOfFirst { it is DashboardItem.ApkItem }
                    if (existingIndex != -1) {
                        dashboardItems.removeAt(existingIndex)
                    }
                }
                
                // Update adapter - use updateItems to sync both lists
                dashboardAdapter?.updateItems(dashboardItems)
                true
            } else {
                Log.e(TAG, "Invalid item type for APK info: ${item::class.simpleName}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating APK info item in list: ${e.message}", e)
            false
        }
    }
}