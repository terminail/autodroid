package com.autodroid.trader.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import com.autodroid.trader.model.Wifi
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Utility class for network-related operations
 */
object NetworkUtils {
    private const val TAG = "NetworkUtils"
    
    /**
     * Check if device is on the same local network as the server
     * by comparing the first 3 octets of the IP addresses (assuming /24 subnet)
     * 
     * @param serverIp The server IP address
     * @param deviceIp The device IP address
     * @return true if both IPs are on the same /24 subnet, false otherwise
     */
    fun isOnSameNetwork(serverIp: String, deviceIp: String): Boolean {
        return try {
            val serverParts = serverIp.split(".")
            val deviceParts = deviceIp.split(".")
            
            // Check if first 3 octets are the same (assuming /24 subnet)
            serverParts.size == 4 && deviceParts.size == 4 &&
                   serverParts[0] == deviceParts[0] &&
                   serverParts[1] == deviceParts[1] &&
                   serverParts[2] == deviceParts[2]
        } catch (e: Exception) {
            Log.e(TAG, "Error checking network: ${e.message}")
            false
        }
    }
    
    /**
     * Convert integer IP to string format
     * 
     * @param ipAddress The IP address as integer
     * @return The IP address in string format (e.g., "192.168.1.1")
     */
    fun intToIp(ipAddress: Int): String {
        return String.format(
            java.util.Locale.getDefault(),
            "%d.%d.%d.%d",
            ipAddress and 0xff,
            ipAddress shr 8 and 0xff,
            ipAddress shr 16 and 0xff,
            ipAddress shr 24 and 0xff
        )
    }
    
    /**
     * Extract the subnet from an IP address (first 3 octets)
     * 
     * @param ipAddress The IP address
     * @return The subnet as string (e.g., "192.168.1") or null if invalid IP
     */
    fun getSubnet(ipAddress: String): String? {
        return try {
            val parts = ipAddress.split(".")
            if (parts.size == 4) {
                "${parts[0]}.${parts[1]}.${parts[2]}"
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting subnet: ${e.message}")
            null
        }
    }
    
    /**
     * Get device's local IP address
     * 
     * @return The local IPv4 address as a string, or null if not found
     */
    fun getLocalIpAddress(): String? {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        return address.hostAddress
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting local IP address: ${e.message}")
            null
        }
    }
    

    
    /**
     * Get IP address from LinkProperties for Android 10+
     * 
     * @param linkProperties The LinkProperties object containing network information
     * @return The IPv4 address as a string, or null if not found
     */
    fun getIpAddressFromLinkProperties(linkProperties: LinkProperties?): String? {
        if (linkProperties == null) return null
        
        for (linkAddress in linkProperties.linkAddresses) {
            val address = linkAddress.address
            if (!address.isLoopbackAddress && address is Inet4Address) {
                return address.hostAddress
            }
        }
        return null
    }
    
    /**
     * Get WiFi name (SSID) for Android 10+
     * 
     * @param context The application context
     * @return The WiFi SSID as a string, or null if not connected to WiFi
     */
    fun getWifiName(context: Context): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val network = connectivityManager.activeNetwork
                val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
                
                if (networkCapabilities != null && networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    // For Android 10+, we need to use WifiManager to get SSID
                    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    @Suppress("DEPRECATION")
                    val wifiInfo = wifiManager.connectionInfo
                    return wifiInfo.ssid?.removeSurrounding("\"")
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting WiFi name: ${e.message}")
            null
        }
    }
    
    /**
     * Get current WiFi information (name, IP)
     * 
     * @param context The application context
     * @return A pair containing WiFi name and IP address, both nullable
     */
    fun getCurrentWiFiInfo(context: Context): Pair<String?, String?> {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For Android 10+ (API 29+)
            val network = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
            
            if (networkCapabilities != null && networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                val linkProperties = connectivityManager.getLinkProperties(network)
                val wifiName = getWifiName(context)
                val wifiIp = getIpAddressFromLinkProperties(linkProperties)
                return Pair(wifiName, wifiIp)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // For Android 6.0-9 (API 23-28)
            val network = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
            
            if (networkCapabilities != null && networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                @Suppress("DEPRECATION")
                val wifiInfo = wifiManager.connectionInfo
                val wifiName = wifiInfo.ssid?.removeSurrounding("\"")
                val wifiIp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // For Android 10+, use getIpAddress from LinkProperties
                    getIpAddressFromLinkProperties(connectivityManager.getLinkProperties(connectivityManager.activeNetwork))
                } else {
                    // For older Android versions, use the deprecated ipAddress
                    @Suppress("DEPRECATION")
                    intToIp(wifiInfo.ipAddress)
                }
                return Pair(wifiName, wifiIp)
            }
        } else {
            // For older Android versions
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            val wifiInfo = wifiManager.connectionInfo
            if (wifiInfo.networkId != -1) {
                val wifiName = wifiInfo.ssid?.removeSurrounding("\"")
                val wifiIp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // For Android 10+, use getIpAddress from LinkProperties
                    getIpAddressFromLinkProperties(connectivityManager.getLinkProperties(connectivityManager.activeNetwork))
                } else {
                    // For older Android versions, use the deprecated ipAddress
                    @Suppress("DEPRECATION")
                    intToIp(wifiInfo.ipAddress)
                }
                return Pair(wifiName, wifiIp)
            }
        }
        
        return Pair(null, null)
    }
    
    /**
     * Get detailed WiFi information including SSID, BSSID, signal strength, frequency, IP address, link speed, and connection status
     * 
     * @param context The application context
     * @return A Wifi object containing detailed WiFi information
     */
    fun getDetailedWiFiInfo(context: Context): Wifi {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo = wifiManager.connectionInfo
            val isWifiConnected = isWifiConnected(context)
            
            if (isWifiConnected && wifiInfo != null) {
                Wifi(
                    ssid = wifiInfo.ssid?.removeSurrounding("\"") ?: "Unknown",
                    bssid = wifiInfo.bssid ?: "Unknown",
                    signalStrength = WifiManager.calculateSignalLevel(wifiInfo.rssi, 5),
                    frequency = wifiInfo.frequency,
                    ipAddress = getWiFiIpAddress() ?: "Unknown",
                    linkSpeed = wifiInfo.linkSpeed,
                    isConnected = true
                )
            } else {
                Wifi(
                    ssid = "Not Connected",
                    bssid = "Unknown",
                    signalStrength = 0,
                    frequency = 0,
                    ipAddress = "Unknown",
                    linkSpeed = 0,
                    isConnected = false
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting detailed WiFi info: ${e.message}")
            Wifi.empty()
        }
    }
    
    /**
     * Check if WiFi is connected
     * 
     * @param context The application context
     * @return true if WiFi is connected, false otherwise
     */
    fun isWifiConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: false
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.type == ConnectivityManager.TYPE_WIFI && networkInfo.isConnected
        }
    }
    
    /**
     * Get WiFi IP address by scanning network interfaces for WiFi-related interfaces
     * 
     * @return The WiFi IP address as a string, or null if not found
     */
    fun getWiFiIpAddress(): String? {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (networkInterface.name.contains("wlan") || networkInterface.name.contains("ap")) {
                    val addresses = networkInterface.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val address = addresses.nextElement()
                        if (!address.isLoopbackAddress && address is Inet4Address) {
                            return address.hostAddress
                        }
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }
}