// DeviceItemManager.kt
package com.autodroid.manager.ui.dashboard

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import com.autodroid.manager.model.DashboardItem
import com.autodroid.manager.AppViewModel
import com.autodroid.manager.utils.NetworkUtils
import com.autodroid.manager.ui.adapters.DashboardAdapter
import com.autodroid.manager.model.Network
import com.autodroid.manager.model.Device

/**
 * Manager class for handling Device dashboard item functionality
 */
class DeviceItemManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val appViewModel: AppViewModel,
    private val onItemUpdate: (DashboardItem) -> Unit
) {
    
    companion object {
        private const val TAG = "DeviceItemManager"
    }
    
    /**
     * Initialize the DeviceItemManager
     */
    fun initialize() {
        setupDeviceObservers()
        updateDevice()
    }
    
    /**
     * Set up observers for device changes
     */
    private fun setupDeviceObservers() {
        // Observe device information changes from ViewModel
        appViewModel.device.observe(lifecycleOwner) { device ->
            device?.let {
                updateDeviceItem(it)
            }
        }
        
        // Observe network connectivity changes
        appViewModel.network.observe(lifecycleOwner) { network ->
            network?.let {
                updateDeviceItemWithNetworkInfo(it)
            }
        }
    }
    
    /**
     * Update device and refresh the dashboard item
     */
    fun updateDevice() {
        try {
            val deviceName = Build.MODEL
            val androidVersion = Build.VERSION.RELEASE
            val localIp = NetworkUtils.getLocalIpAddress() ?: "Not Available"
            
            // Create Device object directly
            val device = Device(
                ip = localIp,
                name = deviceName,
                model = Build.MODEL,
                platform = "Android",
                androidVersion = androidVersion,
                manufacturer = Build.MANUFACTURER,
                brand = Build.BRAND,
                device = Build.DEVICE,
                product = Build.PRODUCT
            )
            
            // Update ViewModel with device info
            appViewModel.setDevice(device)
            
            // Create and update dashboard item
            val deviceItem = DashboardItem.DeviceItem(
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
    private fun updateDeviceItemWithNetworkInfo(network: Network) {
        try {
            val currentDevice = appViewModel.device.value
            
            val deviceItem = DashboardItem.DeviceItem(
                udid = "KNT-AL10-1234567890",
                userId = "user001",
                name = currentDevice?.name ?: Build.MODEL,
                platform = "Android",
                deviceModel = currentDevice?.model ?: Build.MODEL,
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
    private fun updateDeviceItem(deviceInfo: Device) {
        try {
            val deviceItem = DashboardItem.DeviceItem(
                udid = "KNT-AL10-1234567890",
                userId = "user001",
                name = deviceInfo.name ?: Build.MODEL,
                platform = "Android",
                deviceModel = deviceInfo.model ?: Build.MODEL,
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
        updateDevice()
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        // Clean up any observers or resources if needed
    }
    
    /**
     * Handle list update logic for device info item
     */
    fun handleListUpdate(item: DashboardItem, dashboardItems: MutableList<DashboardItem>, dashboardAdapter: DashboardAdapter?): Boolean {
        return try {
            if (item is DashboardItem.DeviceItem) {
                // Find existing device info item in the list
                val existingIndex = dashboardItems.indexOfFirst { it is DashboardItem.DeviceItem }
                
                if (existingIndex != -1) {
                    // Update existing item
                    dashboardItems[existingIndex] = item
                } else {
                    // Add new item after WiFi info item
                    val wifiInfoIndex = dashboardItems.indexOfFirst { it is DashboardItem.WiFiItem }
                    if (wifiInfoIndex != -1) {
                        dashboardItems.add(wifiInfoIndex + 1, item)
                    } else {
                        // Fallback: add to the end
                        dashboardItems.add(item)
                    }
                }
                
                // Update adapter - use updateItems to sync both lists
                dashboardAdapter?.updateItems(dashboardItems)
                true
            } else {
                Log.e(TAG, "Invalid item type for device info: ${item::class.simpleName}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating device info item in list: ${e.message}", e)
            false
        }
    }
}