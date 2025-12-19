// ItemWiFiManager.kt
package com.autodroid.trader.ui.dashboard

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import com.autodroid.trader.AppViewModel
import com.autodroid.trader.utils.NetworkUtils
import com.autodroid.trader.ui.dashboard.DashboardItem
import com.autodroid.trader.model.Wifi
import com.autodroid.trader.model.Network
import com.autodroid.trader.ui.dashboard.DashboardAdapter

/**
 * Manager class for handling WiFi Information dashboard item functionality
 */
class ItemWiFiManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val viewModel: AppViewModel,
    private val onItemUpdate: (DashboardItem) -> Unit
) {
    
    companion object {
        private const val TAG = "ItemWiFiManager"
    }
    
    private val connectivityManager: ConnectivityManager by lazy {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
    
    private val wifiManager: WifiManager by lazy {
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }
    
    /**
     * Initialize the ItemWiFiManager
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
        viewModel.wifi.observe(lifecycleOwner) { wifi ->
            if (wifi != null) {
                updateWiFiItem(wifi)
            }
        }
        
        // Observe network connectivity changes
        viewModel.network.observe(lifecycleOwner) { network ->
            network?.let {
                updateWiFiItemWithNetwork(network)
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
            viewModel.setWifi(wifiInfoObj)
            
            // Create and update dashboard item using the WifiInfo object
            val wifiItemWiFi = DashboardItem.ItemWiFi(
                ssid = wifiInfoObj.ssid ?: "Unknown",
                bssid = wifiInfoObj.bssid ?: "Unknown",
                signalStrength = wifiInfoObj.signalStrength ?: 0,
                frequency = wifiInfoObj.frequency ?: 0,
                ipAddress = wifiInfoObj.ipAddress ?: "Unknown",
                linkSpeed = wifiInfoObj.linkSpeed ?: 0,
                isConnected = wifiInfoObj.isConnected
            )
            
            onItemUpdate(wifiItemWiFi)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating WiFi info: ${e.message}")
        }
    }
    
    /**
     * Update WiFi item with WiFi information
     */
    private fun updateWiFiItem(wifiInfo: Wifi) {
        try {
            val wifiItemWiFi = DashboardItem.ItemWiFi(
                ssid = wifiInfo.ssid ?: "Unknown",
                bssid = wifiInfo.bssid ?: "Unknown",
                signalStrength = wifiInfo.signalStrength ?: 0,
                frequency = wifiInfo.frequency ?: 0,
                ipAddress = wifiInfo.ipAddress ?: "Unknown",
                linkSpeed = wifiInfo.linkSpeed ?: 0,
                isConnected = wifiInfo.isConnected
            )
            
            onItemUpdate(wifiItemWiFi)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating WiFi item: ${e.message}")
        }
    }
    
    /**
     * Update WiFi item with network information
     */
    private fun updateWiFiItemWithNetwork(network: Network) {
        try {
            val currentWifi = viewModel.wifi.value ?: Wifi.empty()
            
            val wifiItemWiFi = DashboardItem.ItemWiFi(
                ssid = currentWifi.ssid ?: "Unknown",
                bssid = currentWifi.bssid ?: "Unknown",
                signalStrength = currentWifi.signalStrength ?: 0,
                frequency = currentWifi.frequency ?: 0,
                ipAddress = network.ipAddress ?: "Unknown",
                linkSpeed = currentWifi.linkSpeed ?: 0,
                isConnected = network.isConnected && network.connectionType == Network.ConnectionType.WIFI
            )
            
            onItemUpdate(wifiItemWiFi)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating WiFi item with network info: ${e.message}")
        }
    }
    
    /**
     * Get current WiFi information using NetworkUtils
     */
    private fun getCurrentWiFiInfo(): com.autodroid.trader.model.Wifi {
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
            val wifiItemWiFi = DashboardItem.ItemWiFi(
                ssid = name,
                bssid = "Unknown",
                signalStrength = 0,
                frequency = 0,
                ipAddress = ip,
                linkSpeed = 0,
                isConnected = isConnected
            )
            
            onItemUpdate(wifiItemWiFi)
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
            if (item is DashboardItem.ItemWiFi) {
                // Find existing WiFi info item in the list
                val existingIndex = dashboardItems.indexOfFirst { it is DashboardItem.ItemWiFi }
                
                if (existingIndex != -1) {
                    // Update existing item
                    dashboardItems[existingIndex] = item
                } else {
                    // Add new item after server connection item
                    val serverConnectionIndex = dashboardItems.indexOfFirst { it is DashboardItem.ItemServer }
                    if (serverConnectionIndex != -1) {
                        dashboardItems.add(serverConnectionIndex + 1, item)
                    } else {
                        // Fallback: add to the beginning
                        dashboardItems.add(0, item)
                    }
                }
                
                // Update adapter - use updateItems to sync both lists
                dashboardAdapter?.updateItems(dashboardItems)
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