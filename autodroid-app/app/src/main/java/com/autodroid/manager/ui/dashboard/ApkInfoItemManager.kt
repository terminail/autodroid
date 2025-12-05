// ApkInfoItemManager.kt
package com.autodroid.manager.ui.dashboard

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import com.autodroid.manager.viewmodel.AppViewModel
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
class ApkInfoItemManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val viewModel: AppViewModel,
    private val onItemUpdate: (DashboardItem) -> Unit
) {
    
    companion object {
        private const val TAG = "ApkInfoItemManager"
    }
    
    // selectedApkIndex is now managed by AppViewModel
    
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
        // Observe APK list changes from ViewModel
        viewModel.apkList.observe(lifecycleOwner) { apkList ->
            apkList?.let {
                // Update APK information based on selected APK
                updateApkInfo()
            }
        }
        
        // Observe APK information changes from ViewModel
        viewModel.apkInfo.observe(lifecycleOwner) { apkInfo ->
            apkInfo?.let {
                val safeMap = it.mapKeys { (key, _) -> key ?: "" }.mapValues { (_, value) -> value ?: "" }
                updateApkItem(safeMap)
            }
        }
        
        viewModel.selectedApkIndex.observeForever { selectedIndex ->
            updateApkInfo()
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
            
            // Only create and update dashboard item if we have valid APK information
            if (apkInfo.isNotEmpty()) {
                val apkInfoItem = DashboardItem.ApkInfo(
                    packageName = apkInfo["packageName"] as? String ?: "Unknown",
                    appName = apkInfo["appName"] as? String ?: "Unknown App",
                    version = apkInfo["version"] as? String ?: "Unknown",
                    versionCode = apkInfo["versionCode"] as? Int ?: 0,
                    installTime = apkInfo["installTime"] as? String ?: "Unknown",
                    updateTime = apkInfo["updateTime"] as? String ?: "Unknown"
                )
                
                onItemUpdate(apkInfoItem)
            } else {
                // If no APK info available, send a special empty APK item to indicate removal
                // This is a workaround since onItemUpdate expects non-null DashboardItem
                val emptyApkInfoItem = DashboardItem.ApkInfo(
                    packageName = "",
                    appName = "",
                    version = "",
                    versionCode = 0,
                    installTime = "",
                    updateTime = ""
                )
                onItemUpdate(emptyApkInfoItem)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating APK info: ${e.message}")
        }
    }
    
    /**
     * Update APK item with APK information
     */
    private fun updateApkItem(apkInfo: Map<String, Any>) {
        try {
            val apkInfoItem = DashboardItem.ApkInfo(
                packageName = apkInfo["packageName"] as? String ?: "Unknown",
                appName = apkInfo["appName"] as? String ?: "Unknown App",
                version = apkInfo["version"] as? String ?: "Unknown",
                versionCode = apkInfo["versionCode"] as? Int ?: 0,
                installTime = apkInfo["installTime"] as? String ?: "Unknown",
                updateTime = apkInfo["updateTime"] as? String ?: "Unknown"
            )
            
            onItemUpdate(apkInfoItem)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating APK item: ${e.message}")
        }
    }
    

    
    /**
     * Get current APK information based on selected APK
     */
    private fun getCurrentApkInfo(): Map<String, Any> {
        return try {
            val apkInfoMap = mutableMapOf<String, Any>()
            
            // Get current APK list from ViewModel
            val currentApkList = viewModel.apkList.value
            val selectedIndex = viewModel.selectedApkIndex.value ?: -1
            
            if (currentApkList != null && currentApkList.isNotEmpty() && selectedIndex >= 0 && selectedIndex < currentApkList.size) {
                // Get real APK data from the selected APK
                val selectedApk = currentApkList[selectedIndex]
                apkInfoMap["packageName"] = selectedApk.packageName ?: "Unknown"
                apkInfoMap["appName"] = selectedApk.appName ?: "Unknown App"
                apkInfoMap["version"] = selectedApk.version ?: "Unknown"
                apkInfoMap["versionCode"] = selectedApk.versionCode ?: 0
            } else {
                // No APK selected or APK list is empty - return empty map to hide APK info section
                return emptyMap()
            }
            
            apkInfoMap
        } catch (e: Exception) {
            Log.e(TAG, "Error getting APK info: ${e.message}")
            emptyMap()
        }
    }
    
    /**
     * Handle APK item selection
     */
    fun selectApk(apkIndex: Int) {
        try {
            viewModel.setSelectedApkIndex(apkIndex)
        } catch (e: Exception) {
            Log.e(TAG, "Error selecting APK: ${e.message}")
        }
    }
    
    /**
     * Handle APK information display - scan functionality is handled by ApkScannerItemManager
     */
    fun handleApkInfoDisplay() {
        // This method is intentionally left empty as scanning is handled by ApkScannerItemManager
        // ApkInfoItemManager only displays APK information provided by the scanning process
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
            if (item is DashboardItem.ApkInfo) {
                // Only add/update APK info item if we have valid APK information
                if (item.packageName.isNotEmpty()) {
                    // Find existing APK info item in the list
                    val existingIndex = dashboardItems.indexOfFirst { it is DashboardItem.ApkInfo }
                    
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
                    val existingIndex = dashboardItems.indexOfFirst { it is DashboardItem.ApkInfo }
                    if (existingIndex != -1) {
                        dashboardItems.removeAt(existingIndex)
                    }
                }
                
                // Update adapter
                dashboardAdapter?.notifyDataSetChanged()
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