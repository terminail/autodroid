// WiFiInfoItemManager.kt
package com.autodroid.manager.ui.dashboard

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import com.autodroid.manager.viewmodel.AppViewModel
import com.autodroid.manager.utils.NetworkUtils
import com.autodroid.manager.model.DashboardItem
import com.autodroid.manager.ui.adapters.DashboardAdapter
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Manager class for handling WiFi Information dashboard item functionality
 */
class WiFiInfoItemManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val viewModel: AppViewModel,
    private val onItemUpdate: (DashboardItem) -> Unit
) {
    
    companion object {
        private const val TAG = "WiFiInfoItemManager"
    }
    
    private val connectivityManager: ConnectivityManager by lazy {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
    
    private val wifiManager: WifiManager by lazy {
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }
    
    /**
     * Initialize the WiFiInfoItemManager
     */
    fun initialize() {
        setupWiFiObservers()
        updateWiFiInfo()
    }
    
    /**
     * Set up observers for WiFi information changes
     */
    private fun setupWiFiObservers() {
        // Observe WiFi information changes from ViewModel
        viewModel.wifiInfo.observe(lifecycleOwner) { wifiInfo ->
            wifiInfo?.let {
                updateWiFiItem(it.filter { (k, v) -> k != null && v != null }.mapKeys { it.key!! }.mapValues { it.value!! })
            }
        }
        
        // Observe network connectivity changes
        viewModel.networkInfo.observe(lifecycleOwner) { networkInfo ->
            networkInfo?.let {
                updateWiFiItemWithNetworkInfo(it.filter { (k, v) -> k != null && v != null }.mapKeys { it.key!! }.mapValues { it.value!! })
            }
        }
    }
    
    /**
     * Update WiFi information and refresh the dashboard item
     */
    fun updateWiFiInfo() {
        try {
            val wifiInfo = getCurrentWiFiInfo()
            
            // Update ViewModel with WiFi info
            viewModel.setWifiInfo(wifiInfo.toMutableMap() as MutableMap<String?, Any?>?)
            
            // Create and update dashboard item
            val wifiItem = DashboardItem.WiFiInfoItem(
                ssid = wifiInfo["ssid"] as? String ?: "Unknown",
                bssid = wifiInfo["bssid"] as? String ?: "Unknown",
                signalStrength = wifiInfo["signalStrength"] as? Int ?: 0,
                frequency = wifiInfo["frequency"] as? Int ?: 0,
                ipAddress = wifiInfo["ipAddress"] as? String ?: "Unknown",
                linkSpeed = wifiInfo["linkSpeed"] as? Int ?: 0,
                isConnected = wifiInfo["isConnected"] as? Boolean ?: false
            )
            
            onItemUpdate(wifiItem)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating WiFi info: ${e.message}")
        }
    }
    
    /**
     * Update WiFi item with WiFi information
     */
    private fun updateWiFiItem(wifiInfo: Map<String, Any>) {
        try {
            val wifiItem = DashboardItem.WiFiInfoItem(
                ssid = wifiInfo["ssid"] as? String ?: "Unknown",
                bssid = wifiInfo["bssid"] as? String ?: "Unknown",
                signalStrength = (wifiInfo["signalStrength"] as? Number)?.toInt() ?: 0,
                frequency = (wifiInfo["frequency"] as? Number)?.toInt() ?: 0,
                ipAddress = wifiInfo["ipAddress"] as? String ?: "Unknown",
                linkSpeed = (wifiInfo["linkSpeed"] as? Number)?.toInt() ?: 0,
                isConnected = wifiInfo["isConnected"] as? Boolean ?: false
            )
            
            onItemUpdate(wifiItem)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating WiFi item: ${e.message}")
        }
    }
    
    /**
     * Update WiFi item with network information
     */
    private fun updateWiFiItemWithNetworkInfo(networkInfo: Map<String, Any>) {
        try {
            val currentWifiInfo = viewModel.wifiInfo.value?.toMap() ?: emptyMap()
            
            val wifiItem = DashboardItem.WiFiInfoItem(
                ssid = currentWifiInfo["ssid"] as? String ?: "Unknown",
                bssid = currentWifiInfo["bssid"] as? String ?: "Unknown",
                signalStrength = (currentWifiInfo["signalStrength"] as? Number)?.toInt() ?: 0,
                frequency = (currentWifiInfo["frequency"] as? Number)?.toInt() ?: 0,
                ipAddress = networkInfo["ipAddress"] as? String ?: "Unknown",
                linkSpeed = (currentWifiInfo["linkSpeed"] as? Number)?.toInt() ?: 0,
                isConnected = networkInfo["isWifiConnected"] as? Boolean ?: false
            )
            
            onItemUpdate(wifiItem)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating WiFi item with network info: ${e.message}")
        }
    }
    
    /**
     * Get current WiFi information using NetworkUtils
     */
    private fun getCurrentWiFiInfo(): Map<String, Any> {
        return NetworkUtils.getDetailedWiFiInfo(context)
    }
    
    /**
     * Update WiFi information including name, IP, and status using NetworkUtils
     */
    fun updateWiFiInfoWithNetworkUtils() {
        try {
            val wifiInfo = NetworkUtils.getCurrentWiFiInfo(context)
            
            // Update WiFi information in dashboard item
            updateWiFiItem(wifiInfo.first ?: "Not connected", wifiInfo.second ?: "-", wifiInfo.first != null)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating WiFi info with NetworkUtils: ${e.message}")
        }
    }
    
    /**
     * Update WiFi item with specific values
     */
    fun updateWiFiItem(name: String, ip: String, isConnected: Boolean) {
        try {
            val wifiItem = DashboardItem.WiFiInfoItem(
                ssid = name,
                bssid = "Unknown",
                signalStrength = 0,
                frequency = 0,
                ipAddress = ip,
                linkSpeed = 0,
                isConnected = isConnected
            )
            
            onItemUpdate(wifiItem)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating WiFi item with specific values: ${e.message}")
        }
    }
    
    /**
     * Refresh WiFi information manually
     */
    fun refresh() {
        updateWiFiInfo()
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        // Clean up any observers or resources if needed
    }
    
    /**
     * Handle list update logic for WiFi info item
     */
    fun handleListUpdate(item: DashboardItem, dashboardItems: MutableList<DashboardItem>, dashboardAdapter: DashboardAdapter?): Boolean {
        return try {
            if (item is DashboardItem.WiFiInfoItem) {
                // Find existing WiFi info item in the list
                val existingIndex = dashboardItems.indexOfFirst { it is DashboardItem.WiFiInfoItem }
                
                if (existingIndex != -1) {
                    // Update existing item
                    dashboardItems[existingIndex] = item
                } else {
                    // Add new item after server connection item
                    val serverConnectionIndex = dashboardItems.indexOfFirst { it is DashboardItem.ServerConnectionItem }
                    if (serverConnectionIndex != -1) {
                        dashboardItems.add(serverConnectionIndex + 1, item)
                    } else {
                        // Fallback: add to the beginning
                        dashboardItems.add(0, item)
                    }
                }
                
                // Update adapter
                dashboardAdapter?.notifyDataSetChanged()
                true
            } else {
                Log.e(TAG, "Invalid item type for WiFi info: ${item::class.simpleName}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating WiFi info item in list: ${e.message}", e)
            false
        }
    }
}