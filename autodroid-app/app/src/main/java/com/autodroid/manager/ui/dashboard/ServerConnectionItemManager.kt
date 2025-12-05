package com.autodroid.manager.ui.dashboard

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import com.autodroid.manager.model.DashboardItem
import com.autodroid.manager.service.DiscoveryStatusManager
import com.autodroid.manager.viewmodel.AppViewModel
import com.autodroid.manager.ui.adapters.DashboardAdapter
import okhttp3.OkHttpClient
import okhttp3.Request
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import android.widget.Toast
import java.util.concurrent.TimeUnit

/**
 * Manages all business logic for the Server Connection dashboard item
 * This class handles server discovery, QR code scanning, and server health checks
 */
class ServerConnectionItemManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val viewModel: AppViewModel,
    private val onItemUpdate: (DashboardItem.ServerConnectionItem) -> Unit
) {
    private val TAG = "ServerConnectionItemManager"
    
    private var currentItem = DashboardItem.ServerConnectionItem()
    
    // Activity Result API launchers for QR code scanning
    private lateinit var requestCameraPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var startQRCodeScannerLauncher: ActivityResultLauncher<Intent>
    
    /**
     * Initialize the QR code scanning functionality
     */
    fun initializeQRCodeScanning(
        permissionLauncher: ActivityResultLauncher<String>,
        scannerLauncher: ActivityResultLauncher<Intent>
    ) {
        requestCameraPermissionLauncher = permissionLauncher
        startQRCodeScannerLauncher = scannerLauncher
    }
    
    /**
     * Handle camera permission result
     */
    fun handleCameraPermissionResult(isGranted: Boolean) {
        if (isGranted) {
            // Permission granted, start QR code scanner
            startQRCodeScanner()
        } else {
            // Permission denied
            Toast.makeText(context, "Camera permission is required for QR code scanning", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Handle QR code scan result
     */
    fun handleQrCodeScanResult(result: androidx.activity.result.ActivityResult) {
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            // Get QR code result from the scanner
            val qrResult = result.data?.getStringExtra("QR_RESULT")
            if (qrResult != null) {
                processQrCodeResult(qrResult)
            } else {
                Toast.makeText(context, "Failed to get QR code result", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "QR code scanning cancelled", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Check camera permission and start QR code scanning
     */
    fun checkCameraPermissionAndScanQR() {
        startQRCodeScanning()
    }
    
    /**
     * Initialize the manager and start observing server status
     */
    fun initialize() {
        setupObservers()
        updateItem(
            status = "Discovering servers...",
            serverIp = "Searching...",
            serverPort = "-",
            serverStatus = "Disconnected",
            apiEndpoint = "-",
            showQrButton = true
        )
    }
    
    /**
     * Set up observers for server discovery status
     */
    private fun setupObservers() {
        // Observe server info from DiscoveryStatusManager
        DiscoveryStatusManager.serverInfo.observe(lifecycleOwner) { serverInfo ->
            serverInfo?.let {
                val connected = it["connected"] as? Boolean ?: false
                val discoveryMethod = it["discovery_method"] as? String ?: ""
                val ip = it["ip"] as? String ?: ""
                val port = it["port"] as? Int ?: 0
                val apiEndpoint = it["api_endpoint"] as? String ?: "-"
                
                updateItem(
                    status = if (connected) "Connected via $discoveryMethod" else when (discoveryMethod) {
                        "mDNS" -> "mDNS OK"
                        "QRCode" -> "QRCode OK"
                        else -> discoveryMethod
                    },
                    serverIp = ip,
                    serverPort = port.toString(),
                    serverStatus = "Checking...",
                    apiEndpoint = apiEndpoint,
                    showQrButton = discoveryMethod != "mDNS"
                )
                
                // Check server health if we have an API endpoint
                if (apiEndpoint != "-") {
                    checkServerHealth(apiEndpoint) { isHealthy ->
                        updateItem(serverStatus = if (isHealthy) "READY" else "FAILED")
                    }
                }
            } ?: run {
                // Clear UI when no server info is available
                updateItem(
                    status = "Discovering servers...",
                    serverIp = "Searching...",
                    serverPort = "-",
                    serverStatus = "Checking...",
                    apiEndpoint = "-",
                    showQrButton = true
                )
            }
        }
        
        // Observe discovery status from DiscoveryStatusManager
        DiscoveryStatusManager.discoveryInProgress.observe(lifecycleOwner) { inProgress ->
            if (inProgress == true) {
                updateItem(
                    status = "mDNS Discovering...",
                    showQrButton = false
                )
                Log.d(TAG, "Discovery in progress - QR code button disabled")
            } else {
                updateItem(
                    showQrButton = true
                )
                Log.d(TAG, "Discovery stopped - QR code button enabled")
            }
        }
        
        // Observe discovery retry count from DiscoveryStatusManager
        DiscoveryStatusManager.discoveryRetryCount.observe(lifecycleOwner) { retryCount ->
            val maxRetries = DiscoveryStatusManager.discoveryMaxRetries.value ?: 0
            if (retryCount != null && maxRetries > 0) {
                if (retryCount < maxRetries) {
                    updateItem(
                        status = "mDNS Retry ${retryCount + 1}/$maxRetries",
                        showQrButton = false
                    )
                    Log.d(TAG, "Discovery retry ${retryCount + 1}/$maxRetries - QR code button disabled")
                }
            }
        }
        
        // Observe discovery failure from DiscoveryStatusManager
        DiscoveryStatusManager.discoveryFailed.observe(lifecycleOwner) { failed ->
            if (failed == true) {
                updateItem(
                    status = "mDNS Failed",
                    serverIp = "Discovery failed",
                    serverPort = "-",
                    serverStatus = "FAILED",
                    apiEndpoint = "-",
                    showQrButton = true
                )
                Log.d(TAG, "mDNS failed - QR code button enabled and visible")
                
                // Mark that user is now using QR code as fallback
                DiscoveryStatusManager.setQrCodeChosenAsFallback(true)
                DiscoveryStatusManager.stopNetworkService()
                Log.d(TAG, "Stopped NetworkService after mDNS failure to conserve resources")
            } else {
                // Reset failure state when discovery is restarted
                updateItem(
                    status = "Discovering servers...",
                    serverIp = "Searching...",
                    serverPort = "-",
                    serverStatus = "Checking...",
                    apiEndpoint = "-",
                    showQrButton = true
                )
                Log.d(TAG, "Discovery failure state reset")
            }
        }
    }
    
    /**
     * Start QR code scanning process
     */
    fun startQRCodeScanning() {
        // Check camera permission first
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            startQRCodeScanner()
        } else {
            requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }
    
    /**
     * Start QR code scanner activity
     */
    private fun startQRCodeScanner() {
        // Create intent for QR code scanner
        val intent = Intent(context, com.autodroid.manager.ui.qrcode.QrCodeScannerActivity::class.java)
        startQRCodeScannerLauncher.launch(intent)
    }
    
    /**
     * Process QR code scan result
     */
    fun processQrCodeResult(qrResult: String) {
        try {
            // Parse QR code result as JSON
            val jsonObject = org.json.JSONObject(qrResult)
            
            // Extract server information from JSON
            val serverName = jsonObject.optString("server_name", "Unknown Server")
            val apiEndpoint = jsonObject.optString("api_endpoint", "")
            val ipAddress = jsonObject.optString("ip_address", "")
            val port = jsonObject.optInt("port", 0)
            
            if (apiEndpoint.isNotEmpty() && ipAddress.isNotEmpty() && port > 0) {
                // Mark QR code as chosen as fallback to prevent mDNS restart
                DiscoveryStatusManager.setQrCodeChosenAsFallback(true)

                updateItem(
                    status = "QRCode",
                    serverIp = ipAddress,
                    serverPort = port.toString(),
                    apiEndpoint = apiEndpoint,
                    serverStatus = "CHECKING"
                )
                
                // Update DiscoveryStatusManager with server info
                val serverInfo = mutableMapOf<String?, Any?>()
                serverInfo["name"] = serverName
                serverInfo["ip"] = ipAddress
                serverInfo["port"] = port
                serverInfo["api_endpoint"] = apiEndpoint
                serverInfo["connected"] = false // Not connected yet
                serverInfo["discovery_method"] = "QRCode"
                
                DiscoveryStatusManager.setServerInfo(serverInfo)
                
                // Check server health
                checkServerHealth(apiEndpoint) { isHealthy ->
                    updateItem(
                        serverStatus = if (isHealthy) "READY" else "FAILED"
                    )
                    
                    // If server is healthy, connect to it
                    if (isHealthy) {
                        connectToServer(ipAddress, port, apiEndpoint)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing QR code result: ${e.message}")
        }
    }
    
    /**
     * Parse QR code result to extract IP and port
     */
    private fun parseQRCodeResult(qrResult: String): Pair<String?, Int?> {
        return try {
            // Remove http:// or https:// prefix if present
            val cleanResult = qrResult.replace("^https?://".toRegex(), "")
            
            // Split by colon to get IP and port
            val parts = cleanResult.split(":")
            if (parts.size == 2) {
                val ip = parts[0]
                val port = parts[1].toInt()
                Pair(ip, port)
            } else {
                Pair(null, null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing QR code result: $qrResult", e)
            Pair(null, null)
        }
    }
    
    /**
     * Connect to server using IP and port
     */
    private fun connectToServer(serverIp: String, serverPort: Int, apiEndpoint: String) {
        Log.i(TAG, "Connecting to server: $serverIp:$serverPort")
        
        // Get the discovery method from the server info
        val discoveryMethod = DiscoveryStatusManager.serverInfo.value?.get("discovery_method")?.toString() ?: "unknown"
        
        updateItem(
            status = "Connected to server via $discoveryMethod",
            serverStatus = "CONNECTING"
        )
        
        // Update DiscoveryStatusManager with server info
        val serverInfo = mutableMapOf<String?, Any?>()
        serverInfo["name"] = "Autodroid Server"
        serverInfo["ip"] = serverIp
        serverInfo["port"] = serverPort
        serverInfo["api_endpoint"] = apiEndpoint
        serverInfo["connected"] = true
        serverInfo["discovery_method"] = discoveryMethod
        
        DiscoveryStatusManager.setServerInfo(serverInfo)
        
        // Check server health
        checkServerHealth(apiEndpoint) { isHealthy ->
            updateItem(
                serverStatus = if (isHealthy) "READY" else "FAILED"
            )
        }
    }
    
    /**
     * Check server health by calling the /api/health endpoint
     */
    private fun checkServerHealth(apiEndpoint: String, callback: (Boolean) -> Unit) {
        Thread {
            try {
                val healthUrl = if (apiEndpoint.endsWith("/")) {
                    "${apiEndpoint}health"
                } else {
                    "${apiEndpoint}/health"
                }
                
                Log.d(TAG, "Checking server health at: $healthUrl")
                
                // Use OkHttp for better network handling
                val client = OkHttpClient.Builder()
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .readTimeout(5, TimeUnit.SECONDS)
                    .build()
                
                val request = Request.Builder()
                    .url(healthUrl)
                    .get()
                    .build()
                
                try {
                    val response = client.newCall(request).execute()
                    val isSuccessful = response.isSuccessful
                    val responseCode = response.code
                    Log.d(TAG, "Server health check response: $responseCode, successful: $isSuccessful")
                    callback(isSuccessful)
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking server health with OkHttp: ${e.message}")
                    // Fallback to HttpURLConnection if OkHttp fails
                    try {
                        Log.d(TAG, "Trying fallback to HttpURLConnection")
                        val url = java.net.URL(healthUrl)
                        val connection = url.openConnection() as java.net.HttpURLConnection
                        connection.requestMethod = "GET"
                        connection.connectTimeout = 5000
                        connection.readTimeout = 5000
                        
                        val responseCode = connection.responseCode
                        Log.d(TAG, "Server health check response with HttpURLConnection: $responseCode")
                        callback(responseCode == 200)
                    } catch (e2: Exception) {
                        Log.e(TAG, "Error checking server health with HttpURLConnection: ${e2.message}")
                        callback(false)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking server health: ${e.message}")
                callback(false)
            }
        }.start()
    }
    
    /**
     * Update the current item and notify listeners
     */
    private fun updateItem(
        status: String? = null,
        serverIp: String? = null,
        serverPort: String? = null,
        serverStatus: String? = null,
        apiEndpoint: String? = null,
        showQrButton: Boolean? = null
    ) {
        currentItem = DashboardItem.ServerConnectionItem(
            status = status ?: currentItem.status,
            serverIp = serverIp ?: currentItem.serverIp,
            serverPort = serverPort ?: currentItem.serverPort,
            serverStatus = serverStatus ?: currentItem.serverStatus,
            apiEndpoint = apiEndpoint ?: currentItem.apiEndpoint,
            showQrButton = showQrButton ?: currentItem.showQrButton
        )
        
        onItemUpdate(currentItem)
    }
    
    /**
     * Get the current server connection item
     */
    fun getCurrentItem(): DashboardItem.ServerConnectionItem {
        return currentItem
    }
    
    /**
     * Refresh the server connection information
     */
    fun refresh() {
        // Reinitialize the server connection item with current data
        initialize()
    }
    
    /**
     * Handle list update logic for server connection item
     */
    fun handleListUpdate(item: DashboardItem, dashboardItems: MutableList<DashboardItem>, dashboardAdapter: DashboardAdapter?): Boolean {
        return try {
            if (item is DashboardItem.ServerConnectionItem) {
                // Find existing server connection item in the list
                val existingIndex = dashboardItems.indexOfFirst { it is DashboardItem.ServerConnectionItem }
                
                if (existingIndex != -1) {
                    // Update existing item
                    dashboardItems[existingIndex] = item
                } else {
                    // Add new item at the beginning
                    dashboardItems.add(0, item)
                }
                
                // Update adapter
                dashboardAdapter?.notifyDataSetChanged()
                true
            } else {
                Log.e(TAG, "Invalid item type for server connection: ${item::class.simpleName}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating server connection item in list: ${e.message}", e)
            false
        }
    }
}