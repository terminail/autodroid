// WiFiInfoItemManager.kt
package com.autodroid.manager.ui.dashboard

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import com.autodroid.manager.AppViewModel
import com.autodroid.manager.utils.NetworkUtils
import com.autodroid.manager.model.DashboardItem
import com.autodroid.manager.model.WifiInfo
import com.autodroid.manager.model.NetworkInfo
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
                updateWiFiItem(wifiInfo)
            }
        }
        
        // Observe network connectivity changes
        viewModel.networkInfo.observe(lifecycleOwner) { networkInfo ->
            networkInfo?.let {
                updateWiFiItemWithNetworkInfo(networkInfo)
            }
        }
    }
    
    /**
     * Update WiFi information and refresh the dashboard item
     */
    fun updateWiFiInfo() {
        try {
            val wifiInfoObj = getCurrentWiFiInfo()
            
            // Update ViewModel with WiFi info
            viewModel.setWifiInfo(wifiInfoObj)
            
            // Create and update dashboard item using the WifiInfo object
            val wifiItem = DashboardItem.WiFiInfoItem(
                ssid = wifiInfoObj.ssid ?: "Unknown",
                bssid = wifiInfoObj.bssid ?: "Unknown",
                signalStrength = wifiInfoObj.signalStrength ?: 0,
                frequency = wifiInfoObj.frequency ?: 0,
                ipAddress = wifiInfoObj.ipAddress ?: "Unknown",
                linkSpeed = wifiInfoObj.linkSpeed ?: 0,
                isConnected = wifiInfoObj.isConnected
            )
            
            onItemUpdate(wifiItem)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating WiFi info: ${e.message}")
        }
    }
    
    /**
     * Update WiFi item with WiFi information
     */
    private fun updateWiFiItem(wifiInfo: WifiInfo) {
        try {
            val wifiItem = DashboardItem.WiFiInfoItem(
                ssid = wifiInfo.ssid ?: "Unknown",
                bssid = wifiInfo.bssid ?: "Unknown",
                signalStrength = wifiInfo.signalStrength ?: 0,
                frequency = wifiInfo.frequency ?: 0,
                ipAddress = wifiInfo.ipAddress ?: "Unknown",
                linkSpeed = wifiInfo.linkSpeed ?: 0,
                isConnected = wifiInfo.isConnected
            )
            
            onItemUpdate(wifiItem)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating WiFi item: ${e.message}")
        }
    }
    
    /**
     * Update WiFi item with network information
     */
    private fun updateWiFiItemWithNetworkInfo(networkInfo: NetworkInfo) {
        try {
            val currentWifiInfo = viewModel.wifiInfo.value ?: WifiInfo.empty()
            
            val wifiItem = DashboardItem.WiFiInfoItem(
                ssid = currentWifiInfo.ssid ?: "Unknown",
                bssid = currentWifiInfo.bssid ?: "Unknown",
                signalStrength = currentWifiInfo.signalStrength ?: 0,
                frequency = currentWifiInfo.frequency ?: 0,
                ipAddress = networkInfo.ipAddress ?: "Unknown",
                linkSpeed = currentWifiInfo.linkSpeed ?: 0,
                isConnected = networkInfo.isConnected && networkInfo.connectionType == NetworkInfo.ConnectionType.WIFI
            )
            
            onItemUpdate(wifiItem)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating WiFi item with network info: ${e.message}")
        }
    }
    
    /**
     * Get current WiFi information using NetworkUtils
     */
    private fun getCurrentWiFiInfo(): WifiInfo {
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