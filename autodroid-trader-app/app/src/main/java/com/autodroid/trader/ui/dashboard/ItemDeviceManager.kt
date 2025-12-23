// ItemDeviceManager.kt
package com.autodroid.trader.ui.dashboard

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import com.autodroid.trader.AppViewModel
import com.autodroid.trader.data.dao.DeviceEntity
import com.autodroid.trader.network.AppInfo
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

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
    
    private val gson = Gson()
    
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
            device?.let { de: DeviceEntity ->
                updateItemDevice(de)
            }
        }

    }

    /**
     * Update device item with device information
     */
    private fun updateItemDevice(deviceEntity: DeviceEntity) {
        Log.d(TAG, "updateItemDevice: 开始更新设备项，序列号=${deviceEntity.serialNo}, 名称=${deviceEntity.name}")
            try {
                // 格式化更新时间
                val updatedAt = if ((deviceEntity.updatedAt ?: 0) > 0) {
                    java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                        .format(java.util.Date(deviceEntity.updatedAt ?: 0))
                } else {
                    "未知"
                }
                
                // 解析已安装应用
                val appNames = mutableListOf<String>()
                deviceEntity.apps?.let { appsJson ->
                    try {
                        // 使用AppInfo类解析应用列表
                        val appInfoType = object : TypeToken<List<AppInfo>>() {}.type
                        val appInfos: List<AppInfo> = gson.fromJson(appsJson, appInfoType)
                        
                        // 提取应用名称
                        appInfos.forEach { appInfo ->
                            appInfo.appName.let { name ->
                                if (name.isNotEmpty()) {
                                    appNames.add(name)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing apps: ${e.message}")
                    }
                }
                
                val itemDevice = DashboardItem.ItemDevice(
                    serialNo = deviceEntity.serialNo,
                    userId = "user001",
                    name = deviceEntity.name ?: Build.MODEL,
                    platform = deviceEntity.platform ?: "Android",
                    deviceModel = deviceEntity.model ?: Build.MODEL,
                    deviceStatus = if (deviceEntity.isOnline) "在线" else "离线",
                    latestRegisteredTime = "2024-01-01 00:00:00",
                    updatedAt = updatedAt,
                    usbDebugEnabled = deviceEntity.usbDebugEnabled,
                    wifiDebugEnabled = deviceEntity.wifiDebugEnabled,
                    debugCheckStatus = deviceEntity.checkStatus,
                    debugCheckMessage = deviceEntity.checkMessage,
                    apps = appNames
                )
                
                Log.d(TAG, "updateItemDevice: 设备项创建完成，准备调用onItemUpdate")
                onItemUpdate(itemDevice)
                Log.d(TAG, "updateItemDevice: onItemUpdate调用完成")
                
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