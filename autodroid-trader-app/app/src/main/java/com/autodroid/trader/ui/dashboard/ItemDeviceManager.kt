// ItemDeviceManager.kt
package com.autodroid.trader.ui.dashboard

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import com.autodroid.trader.AppViewModel
import com.autodroid.trader.utils.NetworkUtils
import com.autodroid.trader.model.Network
import com.autodroid.trader.data.dao.DeviceEntity

/**
 * Manager class for handling Device dashboard item functionality
 */
class ItemDeviceManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val appViewModel: AppViewModel,
    private val onItemUpdate: (DashboardItem) -> Unit
) {
    
    companion object {
        private const val TAG = "ItemDeviceManager"
    }
    
    /**
     * Initialize the ItemDeviceManager
     */
    fun initialize() {
        setupDeviceObservers()
    }
    
    /**
     * Set up observers for device changes
     */
    private fun setupDeviceObservers() {
        // Observe device information changes from ViewModel
        appViewModel.device.observe(lifecycleOwner) { device: DeviceEntity? ->
            device?.let { deviceInfo: DeviceEntity ->
                updateItemDevice(deviceInfo)
            }
        }

    }

    /**
     * Update device item with device information
     */
    private fun updateItemDevice(deviceInfo: DeviceEntity) {
        try {
            val itemDevice = DashboardItem.ItemDevice(
                udid = deviceInfo.id,
                userId = "user001",
                name = deviceInfo.name ?: Build.MODEL,
                platform = deviceInfo.platform ?: "Android",
                deviceModel = deviceInfo.model ?: Build.MODEL,
                deviceStatus = if (deviceInfo.isConnected) "在线" else "离线",
                latestRegisteredTime = "2024-01-01 00:00:00"
            )
            
            onItemUpdate(itemDevice)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating device item: ${e.message}")
        }
    }
    

    
    /**
     * Refresh device information manually
     */
    fun refresh() {
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
            if (item is DashboardItem.ItemDevice) {
                // Find existing device info item in the list
                val existingIndex = dashboardItems.indexOfFirst { it is DashboardItem.ItemDevice }
                
                if (existingIndex != -1) {
                    // Update existing item
                    dashboardItems[existingIndex] = item
                } else {
                    // Add new item after WiFi info item
                    val wifiInfoIndex = dashboardItems.indexOfFirst { it is DashboardItem.ItemWiFi }
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