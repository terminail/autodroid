// ApkScannerItemManager.kt
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
 * Manager class for handling APK Scanner dashboard item functionality
 */
class ApkScannerItemManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val viewModel: AppViewModel,
    private val onItemUpdate: (DashboardItem) -> Unit
) {
    
    companion object {
        private const val TAG = "ApkScannerItemManager"
    }
    
    /**
     * Initialize the ApkScannerItemManager
     */
    fun initialize() {
        setupApkObservers()
        updateApkScanner()
    }
    
    /**
     * Set up observers for APK scanner status changes
     */
    private fun setupApkObservers() {
        // Observe APK scan status changes
        viewModel.apkScanStatus.observe(lifecycleOwner) { scanStatus ->
            scanStatus?.let {
                updateApkScannerItem(scanStatus)
            }
        }
    }
    
    /**
     * Update APK scanner status
     */
    fun updateApkScanner() {
        try {
            val apkScannerItem = DashboardItem.ApkScannerItem(
                scanStatus = "SCAN INSTALLED APKS",
                statusMessage = "Ready to scan",
                showButton = true
            )
            
            onItemUpdate(apkScannerItem)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating APK scanner: ${e.message}")
        }
    }
    
    /**
     * Update APK scanner item with scan status
     */
    private fun updateApkScannerItem(scanStatus: String) {
        try {
            val apkScannerItem = DashboardItem.ApkScannerItem(
                scanStatus = "SCAN INSTALLED APKS", // 按钮文本保持固定
                statusMessage = scanStatus, // 状态信息显示在专门的TextView中
                showButton = true
            )
            
            onItemUpdate(apkScannerItem)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating APK scanner item: ${e.message}")
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
                    Log.d(TAG, "Device UDID: '$deviceUdid' (length: ${deviceUdid.length})")
                    
                    // Create ApkScannerManager instance
                    val apkScannerManager = ApkScannerManager(context)
                    
                    // First, register the device with the server
                    val deviceRegistrationSuccess = apkScannerManager.registerDeviceWithServer(deviceUdid)
                    
                    if (!deviceRegistrationSuccess) {
                        Log.e(TAG, "Failed to register device with server")
                        CoroutineScope(Dispatchers.Main).launch {
                            viewModel.setApkScanStatus("设备注册失败: 请检查服务器连接")
                        }
                        return@launch
                    }
                    
                    // Scan installed APKs (this is a suspend function)
                    val apkList = apkScannerManager.scanInstalledApks()
                    
                    // Register APKs with server (this is also a suspend function)
                    val registrationSuccess = apkScannerManager.registerApksWithServer(apkList, deviceUdid)
                    
                    // Update UI on main thread
                        CoroutineScope(Dispatchers.Main).launch {
                            if (registrationSuccess) {
                                viewModel.setApkScanStatus("Scan completed: ${apkList.size} APKs found and registered")
                            } else {
                                viewModel.setApkScanStatus("Scan completed: ${apkList.size} APKs found, but registration failed")
                            }
                            
                            // Update APK info with scan results
                            val apkInfoList = apkList.map { apkInfo ->
                                DashboardItem.ApkInfo(
                                    packageName = apkInfo.packageName ?: "Unknown",
                                    appName = apkInfo.appName ?: "Unknown App",
                                    version = apkInfo.version ?: "Unknown",
                                    versionCode = apkInfo.versionCode ?: 0
                                )
                            }
                            
                            // Update ViewModel with APK list for display
                            viewModel.setApkList(apkInfoList)
                            
                            // Auto-select the first APK to display its information
                            if (apkInfoList.isNotEmpty()) {
                                // Trigger APK info display by selecting the first APK
                                // This will be handled by the DashboardAdapter or Fragment
                                // that manages both ApkScannerItemManager and ApkInfoItemManager
                                Log.d(TAG, "Scan completed: ${apkInfoList.size} APKs ready for display")
                                
                                // Auto-select the first APK to display its information
                                // This triggers the ApkInfoItemManager to show the first APK details
                                viewModel.setSelectedApkIndex(0)
                            }
                        }
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error scanning APKs: ${e.message}")
                    CoroutineScope(Dispatchers.Main).launch {
                        // 检查是否是服务器连接错误
                        val errorMessage = if (e.message?.contains("Failed to connect") == true || 
                                               e.message?.contains("Network request failed") == true ||
                                               e.message?.contains("Connection refused") == true) {
                            "服务器连接失败: 请检查服务器是否启动 (http://192.168.1.59:8004)"
                        } else if (e.message?.contains("404") == true || 
                                  e.message?.contains("Not Found") == true) {
                            "API路径错误: 服务器端点不存在，请检查服务器配置"
                        } else {
                            "扫描失败: ${e.message}"
                        }
                        viewModel.setApkScanStatus(errorMessage)
                    }
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling APK scan click: ${e.message}")
            viewModel.setApkScanStatus("Scan failed: ${e.message}")
        }
    }
    
    /**
     * Refresh APK scanner status manually
     */
    fun refresh() {
        updateApkScanner()
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        // Clean up any observers or resources if needed
    }
    
    /**
     * Handle list update logic for APK scanner item
     */
    fun handleListUpdate(item: DashboardItem, dashboardItems: MutableList<DashboardItem>, dashboardAdapter: DashboardAdapter?): Boolean {
        return try {
            if (item is DashboardItem.ApkScannerItem) {
                // Find existing APK scanner item in the list
                val existingIndex = dashboardItems.indexOfFirst { it is DashboardItem.ApkScannerItem }
                
                if (existingIndex != -1) {
                    // Update existing item
                    dashboardItems[existingIndex] = item
                } else {
                    // Add new item after device info item
                    val deviceInfoIndex = dashboardItems.indexOfFirst { it is DashboardItem.DeviceInfoItem }
                    if (deviceInfoIndex != -1) {
                        dashboardItems.add(deviceInfoIndex + 1, item)
                    } else {
                        // Fallback: add to the end
                        dashboardItems.add(item)
                    }
                }
                
                // Update adapter
                dashboardAdapter?.notifyDataSetChanged()
                true
            } else {
                Log.e(TAG, "Invalid item type for APK scanner: ${item::class.simpleName}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating APK scanner item in list: ${e.message}", e)
            false
        }
    }
}