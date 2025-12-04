// DeviceInfoItemManager.kt
package com.autodroid.manager.ui.dashboard

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import com.autodroid.manager.model.DashboardItem
import com.autodroid.manager.viewmodel.AppViewModel
import com.autodroid.manager.utils.NetworkUtils

/**
 * Manager class for handling Device Information dashboard item functionality
 */
class DeviceInfoItemManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val viewModel: AppViewModel,
    private val onItemUpdate: (DashboardItem) -> Unit
) {
    
    companion object {
        private const val TAG = "DeviceInfoItemManager"
    }
    
    /**
     * Initialize the DeviceInfoItemManager
     */
    fun initialize() {
        setupDeviceInfoObservers()
        updateDeviceInfo()
    }
    
    /**
     * Set up observers for device information changes
     */
    private fun setupDeviceInfoObservers() {
        // Observe device information changes from ViewModel
        viewModel.deviceInfo.observe(lifecycleOwner) { deviceInfo ->
            deviceInfo?.let {
                val filteredMap = it.filter { (k, v) -> k != null && v != null }
                    .mapKeys { it.key!! }
                    .mapValues { it.value!! }
                updateDeviceItem(filteredMap)
            }
        }
        
        // Observe network connectivity changes
        viewModel.networkInfo.observe(lifecycleOwner) { networkInfo ->
            networkInfo?.let {
                val filteredMap = it.filter { (k, v) -> k != null && v != null }
                    .mapKeys { it.key!! }
                    .mapValues { it.value!! }
                updateDeviceItemWithNetworkInfo(filteredMap)
            }
        }
    }
    
    /**
     * Update device information and refresh the dashboard item
     */
    fun updateDeviceInfo() {
        try {
            val deviceName = Build.MODEL
            val androidVersion = Build.VERSION.RELEASE
            val localIp = NetworkUtils.getLocalIpAddress() ?: "Not Available"
            
            val deviceInfo = mutableMapOf<String?, Any?>()
            deviceInfo["deviceName"] = deviceName
            deviceInfo["androidVersion"] = androidVersion
            deviceInfo["localIp"] = localIp
            deviceInfo["buildManufacturer"] = Build.MANUFACTURER
            deviceInfo["buildBrand"] = Build.BRAND
            deviceInfo["buildDevice"] = Build.DEVICE
            deviceInfo["buildProduct"] = Build.PRODUCT
            
            // Update ViewModel with device info
            viewModel.setDeviceInfo(deviceInfo)
            
            // Create and update dashboard item
            val deviceItem = DashboardItem.DeviceInfoItem(
                udid = "KNT-AL10-1234567890",
                userId = "user001",
                name = deviceName,
                platform = "Android",
                deviceModel = Build.MODEL,
                deviceStatus = "在线",
                connectionTime = "2024-01-01 00:00:00"
            )
            
            onItemUpdate(deviceItem)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating device info: ${e.message}")
        }
    }
    
    /**
     * Update device item with network information
     */
    private fun updateDeviceItemWithNetworkInfo(networkInfo: Map<String, Any>) {
        try {
            val currentDeviceInfo = viewModel.deviceInfo.value?.filter { (k, v) -> k != null && v != null }
                ?.mapKeys { it.key!! }
                ?.mapValues { it.value!! } ?: emptyMap()
            
            val deviceItem = DashboardItem.DeviceInfoItem(
                udid = "KNT-AL10-1234567890",
                userId = "user001",
                name = currentDeviceInfo["deviceName"] as? String ?: Build.MODEL,
                platform = "Android",
                deviceModel = currentDeviceInfo["model"] as? String ?: Build.MODEL,
                deviceStatus = "在线",
                connectionTime = "2024-01-01 00:00:00"
            )
            
            onItemUpdate(deviceItem)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating device item with network info: ${e.message}")
        }
    }
    
    /**
     * Update device item with device information
     */
    private fun updateDeviceItem(deviceInfo: Map<String, Any>) {
        try {
            val deviceItem = DashboardItem.DeviceInfoItem(
                udid = "KNT-AL10-1234567890",
                userId = "user001",
                name = deviceInfo["deviceName"] as? String ?: Build.MODEL,
                platform = "Android",
                deviceModel = deviceInfo["model"] as? String ?: Build.MODEL,
                deviceStatus = "在线",
                connectionTime = "2024-01-01 00:00:00"
            )
            
            onItemUpdate(deviceItem)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating device item: ${e.message}")
        }
    }
    

    
    /**
     * Refresh device information manually
     */
    fun refresh() {
        updateDeviceInfo()
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        // Clean up any observers or resources if needed
    }
}