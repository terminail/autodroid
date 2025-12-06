package com.autodroid.manager.ui.dashboard

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import com.autodroid.manager.model.DashboardItem
import com.autodroid.manager.model.Server
import com.autodroid.manager.network.ServerInfoResponse
import com.autodroid.manager.AppViewModel
import com.autodroid.manager.ui.adapters.DashboardAdapter
import com.autodroid.manager.service.DiscoveryStatusManager
import okhttp3.OkHttpClient
import okhttp3.Request
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import android.widget.Toast
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Manages all business logic for the Server Connection dashboard item
 * This class handles server discovery, QR code scanning, and server health checks
 */
class ServerItemManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val viewModel: AppViewModel,
    private val onItemUpdate: (DashboardItem.ServerItem) -> Unit
) {
    private val TAG = "ServerItemManager"
    
    private var currentItem = DashboardItem.ServerItem()
    
    // Activity Result API launchers for QR code scanning
    private lateinit var requestCameraPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var startQRCodeScannerLauncher: ActivityResultLauncher<Intent>
    
    // Server connection state
    private var isServerConnected = false
    private var serverConnectionMethod = "none" // mDNS, QRCode, or none
    
    /**
     * Show detailed toast message with title and description
     */
    private fun showDetailedToast(title: String, description: String = "", duration: Int = Toast.LENGTH_SHORT) {
        // Use a simple toast with formatted text instead of custom layout
        val message = if (description.isNotEmpty()) {
            "$title: $description"
        } else {
            title
        }
        
        Toast.makeText(context, message, duration).show()
    }
    
    /**
     * Show loading indicator in the UI
     */
    private fun showLoadingIndicator(message: String) {
        updateItem(
            status = "$message...",
            serverStatus = "LOADING"
        )
        
        // Also show a brief toast for immediate feedback
        showDetailedToast("正在处理", message, Toast.LENGTH_SHORT)
    }
    
    /**
     * Show error message with detailed information
     */
    private fun showErrorMessage(title: String, error: String? = null) {
        val errorMessage = error ?: "请检查网络连接或服务器状态"
        
        updateItem(
            status = "错误: $title",
            serverStatus = "ERROR"
        )
        
        showDetailedToast("操作失败", "$title: $errorMessage", Toast.LENGTH_LONG)
    }
    
    /**
     * Show success message
     */
    private fun showSuccessMessage(title: String, description: String = "") {
        updateItem(
            status = "成功: $title",
            serverStatus = "SUCCESS"
        )
        
        showDetailedToast("操作成功", if (description.isNotEmpty()) "$title: $description" else title, Toast.LENGTH_SHORT)
    }

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
            status = "Discovering servers via mDNS...",
            serverStatus = "Disconnected",
            apiEndpoint = "-",
            discoveryMethod = "Auto mDNS Discovery",
            isStartMdnsButtonEnabled = false // 仅mDNS按钮在发现过程中禁用
        )
        
        // Start mDNS discovery if not already running
        if (!DiscoveryStatusManager.isQrCodeChosenAsFallback()) {
            DiscoveryStatusManager.startNetworkService()
        }
    }
    
    /**
     * Set up observers for server discovery status
     */
    private fun setupObservers() {
        // Observe server info from AppViewModel
        viewModel.server.observe(lifecycleOwner) { server ->
            server?.let {
                val connected = it.connected
                val discoveryMethod = it.discoveryMethod ?: ""
                val ip = it.ip ?: ""
                val port = it.port ?: 0
                val apiEndpoint = it.api_endpoint ?: "-"
                
                isServerConnected = connected
                serverConnectionMethod = discoveryMethod
                
                // 根据连接状态更新按钮行为
                val isDiscoveryInProgress = viewModel.discoveryStatus.value?.isDiscovering() ?: false
                
                updateItem(
                    status = when {
                        connected -> "Connected via $discoveryMethod"
                        discoveryMethod == "mDNS" -> "mDNS Discovery Successful"
                        discoveryMethod == "QRCode" -> "QR Code Scanned"
                        else -> "Server Found"
                    },
                    serverStatus = if (connected) "CONNECTED" else "DISCOVERED",
                    apiEndpoint = apiEndpoint,
                    discoveryMethod = when {
                        connected -> "Connected"
                        isDiscoveryInProgress -> "Discovery via mDNS..."
                        else -> discoveryMethod
                    },
                    isStartMdnsButtonEnabled = !isDiscoveryInProgress // 仅mDNS按钮在发现过程中禁用
                )
                
                // Check server health if we have an API endpoint
                if (apiEndpoint != "-" && !connected) {
                    checkServerHealth(apiEndpoint) { isHealthy ->
                        // Ensure UI updates run on main thread
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            updateItem(serverStatus = if (isHealthy) "READY" else "HEALTH_CHECK_FAILED")
                            
                            // If server is healthy, automatically connect to it
                            if (isHealthy) {
                                connectToServer(ip, port, apiEndpoint)
                            }
                        }
                    }
                }
            } ?: run {
                // Clear UI when no server info is available
                isServerConnected = false
                serverConnectionMethod = "none"
                
                updateItem(
                    status = "Discovering servers via mDNS...",
                    serverStatus = "DISCONNECTED",
                    apiEndpoint = "-",
                    discoveryMethod = "Auto mDNS Discovery",
                    isStartMdnsButtonEnabled = false // 仅mDNS按钮在发现过程中禁用
                )
            }
        }
        
        // Observe discovery status from AppViewModel
        viewModel.discoveryStatus.observe(lifecycleOwner) { discoveryStatus ->
            discoveryStatus?.let { status ->
                val isDiscovering = status.isDiscovering()
                val retryCount = status.retryCount
                
                // Update UI based on discovery status
                when {
                    isDiscovering -> {
                        updateItem(
                            status = when {
                                retryCount > 0 -> "mDNS Discovery (Retry $retryCount)..."
                                else -> "Discovering servers via mDNS..."
                            },
                            serverStatus = "DISCOVERING",
                            apiEndpoint = "-",
                            discoveryMethod = "Auto mDNS Discovery",
                            isStartMdnsButtonEnabled = false // 仅mDNS按钮在发现过程中禁用
                        )
                    }
                    status.isDiscoveryFailed() -> {
                        updateItem(
                            status = "mDNS Discovery Failed",
                            serverStatus = "FAILED",
                            apiEndpoint = "-",
                            discoveryMethod = "Auto mDNS Discovery Failed",
                            isStartMdnsButtonEnabled = true // mDNS按钮在失败后启用
                        )
                    }
                    else -> {
                        // Other cases are handled by serverInfo observer
                    }
                }
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
        // Show loading indicator
        showLoadingIndicator("正在解析QR码")
        
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
                    status = "QR Code Scanned Successfully",
                    apiEndpoint = apiEndpoint,
                    serverStatus = "DISCOVERED"
                )
                
                // Update DiscoveryStatusManager with server info
                val serverObj = Server(
                    serviceName = "Autodroid Server",
                    name = serverName,
                    ip = ipAddress,
                    port = port,
                    api_endpoint = apiEndpoint,
                    connected = false, // Not connected yet
                    discoveryMethod = "QRCode"
                )
                
                viewModel.setServer(serverObj)
                
                // Check server health
                checkServerHealth(apiEndpoint) { isHealthy ->
                    // Ensure UI updates run on main thread
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        updateItem(
                            serverStatus = if (isHealthy) "READY" else "HEALTH_CHECK_FAILED"
                        )
                        
                        // If server is healthy, connect to it
                        if (isHealthy) {
                            connectToServer(ipAddress, port, apiEndpoint)
                        }
                    }
                }
                
                showSuccessMessage("QR码扫描成功", "服务器信息已成功获取")
            } else {
                showErrorMessage("QR码格式无效", "请确保扫描的是有效的Autodroid服务器QR码")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing QR code result: ${e.message}")
            showErrorMessage("QR码解析失败", e.message)
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
        val discoveryMethod = viewModel.server.value?.discoveryMethod ?: "unknown"
        
        // Ensure UI updates run on main thread
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            updateItem(
                status = "Connected to server via $discoveryMethod",
                serverStatus = "CONNECTED"
            )
        }
        
        // Update DiscoveryStatusManager with server info
        val serverObj = Server(
            serviceName = "Autodroid Server",
            name = "Autodroid Server",
            hostname = null,
            platform = null,
            api_endpoint = apiEndpoint,
            services = emptyMap(),
            capabilities = emptyMap(),
            connected = true,
            ip = serverIp,
            port = serverPort,
            discoveryMethod = discoveryMethod
        )
        
        viewModel.setServer(serverObj)
        
        // Check server health
        checkServerHealth(apiEndpoint) { isHealthy ->
            updateItem(
                serverStatus = if (isHealthy) "READY" else "HEALTH_CHECK_FAILED"
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
        serverStatus: String? = null,
        apiEndpoint: String? = null,
        discoveryMethod: String? = null,
        isStartMdnsButtonEnabled: Boolean? = null,
        serverName: String? = null,
        hostname: String? = null,
        platform: String? = null
    ) {
        currentItem = DashboardItem.ServerItem(
            status = status ?: currentItem.status,
            serverStatus = serverStatus ?: currentItem.serverStatus,
            apiEndpoint = apiEndpoint ?: currentItem.apiEndpoint,
            discoveryMethod = discoveryMethod ?: currentItem.discoveryMethod,
            isStartMdnsButtonEnabled = isStartMdnsButtonEnabled ?: currentItem.isStartMdnsButtonEnabled,
            serverName = serverName ?: currentItem.serverName,
            hostname = hostname ?: currentItem.hostname,
            platform = platform ?: currentItem.platform
        )
        
        Log.d(TAG, "updateItem called: status=$status, serverStatus=$serverStatus, apiEndpoint=$apiEndpoint")
        
        // Ensure UI update runs on main thread
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            try {
                onItemUpdate(currentItem)
                Log.d(TAG, "onItemUpdate callback executed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error in onItemUpdate callback: ${e.message}", e)
            }
        }
    }
    
    /**
     * Get the current server connection item
     */
    fun getCurrentItem(): DashboardItem.ServerItem {
        return currentItem
    }
    
    /**
     * Check if server is currently connected
     */
    fun isServerConnected(): Boolean {
        return isServerConnected
    }
    
    /**
     * Get current server connection method
     */
    fun getServerConnectionMethod(): String {
        return serverConnectionMethod
    }
    
    /**
     * Refresh the server connection information
     */
    fun refresh() {
        // Reset connection state and restart discovery
        isServerConnected = false
        serverConnectionMethod = "none"
        
        // Reset QR code fallback flag to allow mDNS discovery
        DiscoveryStatusManager.resetQrCodeFallback()
        
        // Immediately update UI to show refreshing state
        updateItem(
            status = "Refreshing server connection...",
            serverStatus = "REFRESHING",
            apiEndpoint = "-",
            discoveryMethod = "Auto Refresh",
            isStartMdnsButtonEnabled = false
        )
        
        // Reinitialize the server connection item with current data
        initialize()
        
        // Force immediate UI update with current server state
        forceUpdateWithCurrentState()
    }
    
    /**
     * Force immediate update with current server state from ViewModel
     */
    private fun forceUpdateWithCurrentState() {
        val currentServer = viewModel.server.value
        val currentDiscoveryStatus = viewModel.discoveryStatus.value
        
        if (currentServer != null) {
            // Server is connected or discovered, update UI immediately
            updateItem(
                status = when {
                    currentServer.connected -> "Connected via ${currentServer.discoveryMethod}"
                    currentServer.discoveryMethod == "mDNS" -> "mDNS Discovery Successful"
                    currentServer.discoveryMethod == "QRCode" -> "QR Code Scanned"
                    else -> "Server Found"
                },
                serverStatus = if (currentServer.connected) "CONNECTED" else "DISCOVERED",
                apiEndpoint = currentServer.api_endpoint ?: "-",
                discoveryMethod = currentServer.discoveryMethod ?: "Auto mDNS Discovery",
                isStartMdnsButtonEnabled = currentDiscoveryStatus?.isDiscovering() != true,
                serverName = currentServer.name ?: "Autodroid Server",
                hostname = currentServer.hostname ?: "-",
                platform = currentServer.platform ?: "-"
            )
        } else {
            // No server info available, show discovery status
            val isDiscovering = currentDiscoveryStatus?.isDiscovering() ?: false
            val retryCount = currentDiscoveryStatus?.retryCount ?: 0
            
            updateItem(
                status = when {
                    isDiscovering -> if (retryCount > 0) "mDNS Discovery (Retry $retryCount)..." else "Discovering servers via mDNS..."
                    currentDiscoveryStatus?.isDiscoveryFailed() == true -> "mDNS Discovery Failed"
                    else -> "Discovering servers via mDNS..."
                },
                serverStatus = when {
                    isDiscovering -> "DISCOVERING"
                    currentDiscoveryStatus?.isDiscoveryFailed() == true -> "FAILED"
                    else -> "DISCONNECTED"
                },
                apiEndpoint = "-",
                discoveryMethod = "Auto mDNS Discovery",
                isStartMdnsButtonEnabled = !isDiscovering
            )
        }
    }
    
    /**
     * Handle server connection item update
     */
    fun handleServerConnectionUpdate(item: DashboardItem, dashboardItems: MutableList<DashboardItem>, dashboardAdapter: DashboardAdapter?): Boolean {
        return try {
            if (item is DashboardItem.ServerItem) {
                // Find existing server connection item in the list
                val existingIndex = dashboardItems.indexOfFirst { it is DashboardItem.ServerItem }
                
                if (existingIndex != -1) {
                    // Update existing item
                    dashboardItems[existingIndex] = item
                } else {
                    // Add new item at the beginning
                    dashboardItems.add(0, item)
                }
                
                // Update adapter on main thread - use updateItems to sync both lists
                dashboardAdapter?.let { adapter ->
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        adapter.updateItems(dashboardItems)
                    }
                }
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
    
    /**
     * Handle manual input connection from user
     */
    fun handleManualInputConnection(apiEndpoint: String, serverInfo: com.autodroid.manager.network.ServerInfoResponse) {
        Log.i(TAG, "Handling manual input connection: $apiEndpoint")
        
        // Show loading indicator
        showLoadingIndicator("正在验证服务器连接")
        
        try {
            // Use serverInfo directly instead of parsing API endpoint
            if (serverInfo.isValid()) {
                // Update UI to show manual input connection with server details
                updateItem(
                    status = "Connected via Manual Input",
                    serverStatus = "CONNECTED",
                    apiEndpoint = serverInfo.getApiEndpoint(),
                    discoveryMethod = "Manual Input",
                    isStartMdnsButtonEnabled = true,
                    serverName = serverInfo.name ?: "Autodroid Server",
                    hostname = serverInfo.hostname ?: "-",
                    platform = serverInfo.platform ?: "-"
                )
                
                // Update DiscoveryStatusManager with server info using type-safe data
                // Create Server object instead of Map
                val serverObj = Server(
                    serviceName = serverInfo.name ?: "Autodroid Server (Manual)",
                    name = serverInfo.name ?: "Autodroid Server (Manual)",
                    hostname = serverInfo.hostname,
                    api_endpoint = serverInfo.api_endpoint ?: "",
                    connected = true,
                    discoveryMethod = "Manual Input"
                )
                
                viewModel.setServer(serverObj)
                
                // Check server health
                checkServerHealth(serverInfo.api_endpoint ?: "") { isHealthy ->
                    // Ensure UI updates run on main thread
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        updateItem(
                            serverStatus = if (isHealthy) "READY" else "HEALTH_CHECK_FAILED"
                        )
                    }
                }
                
                // Show success message
                showSuccessMessage("手动连接成功", "服务器信息已成功获取")
            } else {
                showErrorMessage("服务器信息无效", "请检查服务器返回的信息格式是否正确")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling manual input connection: ${e.message}", e)
            showErrorMessage("手动连接失败", e.message)
        }
    }
    
    /**
     * Handle START mDNS button click with retry mechanism
     */
    fun handleStartMdnsDiscovery() {
        // Start mDNS discovery with retry mechanism
        DiscoveryStatusManager.startDiscoveryWithRetry()
        
        // Update UI to show discovery started
        updateItem(
            status = "Starting mDNS Discovery...",
            serverStatus = "DISCOVERING",
            apiEndpoint = "-",
            discoveryMethod = "Manual mDNS Discovery (with Retry)",
            isStartMdnsButtonEnabled = true
        )
        
        // Show detailed feedback to user
        showDetailedToast("开始mDNS发现服务", "正在搜索网络中的Autodroid服务器...", Toast.LENGTH_LONG)
        
        Log.d(TAG, "START mDNS discovery with retry mechanism initiated")
    }
    
    /**
     * Handle manual input button click - show dialog for server address input
     */
    fun handleManualInputClick() {
        // Show manual input dialog
        showManualInputDialog()
    }
    
    /**
     * Show manual input dialog for server address
     */
    private fun showManualInputDialog() {
        // Create dialog layout
        val inputLayout = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        
        // Add API endpoint input field
        val apiEndpointInput = android.widget.EditText(context).apply {
            hint = "输入API端点 (例如: http://192.168.1.59:8004/api)"
            setText("http://192.168.1.59:8004/api")
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(32, 16, 32, 0)
            }
        }
        
        inputLayout.addView(apiEndpointInput)
        
        // Create dialog
        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("手动输入服务器地址")
            .setView(inputLayout)
            .setPositiveButton("连接") { dialog, _ ->
                val apiEndpoint = apiEndpointInput.text.toString().trim()
                if (apiEndpoint.isNotEmpty()) {
                    // Validate and connect to server
                    validateAndConnectToServer(apiEndpoint)
                } else {
                    android.widget.Toast.makeText(context, "请输入API端点", android.widget.Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    
    /**
     * Validate API endpoint and connect to server
     */
    private fun validateAndConnectToServer(apiEndpoint: String) {
        // Show connection progress
        android.widget.Toast.makeText(context, "正在验证服务器连接...", android.widget.Toast.LENGTH_SHORT).show()
        
        // Use coroutine to perform validation in background
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                // Set API endpoint - validation is now handled by ApiClient
                com.autodroid.manager.network.ApiClient.getInstance().setApiEndpoint(apiEndpoint)
                
                // Fetch server info from FastAPI
                val serverInfo = getServerInfo()
                
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (serverInfo != null) {
                        // Successfully fetched server info, update UI and connect to server
                        handleSuccessfulServerConnection(apiEndpoint, serverInfo)
                    } else {
                        android.widget.Toast.makeText(context, "无法连接到服务器", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: IllegalArgumentException) {
                // Handle invalid API endpoint format
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "API端点格式无效: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "连接失败: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    

    
    /**
     * Get server info from FastAPI using the type-safe ApiClient
     */
    private suspend fun getServerInfo(): com.autodroid.manager.network.ServerInfoResponse? {
        return try {
            // Get server info using type-safe ApiClient
            com.autodroid.manager.network.ApiClient.getInstance().getServerInfo()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting server info: ${e.message}", e)
            null
        }
    }
    
    /**
     * Handle successful server connection from manual input
     */
    private fun handleSuccessfulServerConnection(apiEndpoint: String, serverInfo: com.autodroid.manager.network.ServerInfoResponse) {
        // Parse server info and update UI
        try {
            // For now, simply show success message
            android.widget.Toast.makeText(context, "服务器连接成功", android.widget.Toast.LENGTH_SHORT).show()
            
            // Update server connection status
            handleManualInputConnection(apiEndpoint, serverInfo)
            
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "服务器信息解析失败", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Parse API endpoint to extract IP and port
     */
    private fun parseApiEndpoint(apiEndpoint: String): Pair<String?, Int?> {
        return try {
            // Remove http:// or https:// prefix
            val cleanEndpoint = apiEndpoint.replace("^https?://".toRegex(), "")
            
            // Split by colon to get IP and port
            val parts = cleanEndpoint.split(":")
            if (parts.size >= 2) {
                val ip = parts[0]
                val port = parts[1].toInt()
                Pair(ip, port)
            } else {
                Pair(null, null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing API endpoint: $apiEndpoint", e)
            Pair(null, null)
        }
    }
}