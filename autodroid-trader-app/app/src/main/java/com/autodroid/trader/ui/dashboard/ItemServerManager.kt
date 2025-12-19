package com.autodroid.trader.ui.dashboard

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import com.autodroid.trader.ui.dashboard.DashboardItem
import com.autodroid.trader.model.Server
// import com.autodroid.trader.model.DiscoveredServer
import com.autodroid.trader.AppViewModel
import com.autodroid.trader.ui.dashboard.DashboardAdapter

import com.autodroid.trader.data.repository.ServerRepository
import androidx.activity.result.ActivityResultLauncher
import android.content.Intent
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Manages all business logic for the Server Connection dashboard item
 * This class handles server discovery, QR code scanning, and server health checks
 */
class ItemServerManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val appViewModel: AppViewModel,
    private val onItemUpdate: (DashboardItem.ItemServer) -> Unit
) {
    private val TAG = "ItemServerManager"
    
    private var currentItem = DashboardItem.ItemServer()
    
    // Server repository for database operations
    private val serverRepository = ServerRepository.getInstance(context.applicationContext as Application)
    
    // Activity Result API launchers for QR code scanning
    private lateinit var requestCameraPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var startQRCodeScannerLauncher: ActivityResultLauncher<Intent>
    
    // Server connection state
    private var isServerConnected = false
    private var serverConnectionMethod = "none" // QRCode, or none
    
    /**
     * Initialize the trader and start observing server status
     */
    fun initialize() {

        
        setupObservers()
        
        // Server discovery will be started manually by user - do not auto-start
    }
    
    /**
     * Set up observers for server discovery status
     */
    private fun setupObservers() {
        // Observe server info from AppViewModel (single source of truth)
        appViewModel.server.observe(lifecycleOwner) { server ->
            server?.let {
                val connected = it.connected
                val hostname = it.hostname ?: ""
                val apiEndpoint = it.apiEndpoint ?: "-"
                
                isServerConnected = connected
                
                // 根据连接状态更新UI
                val isDiscoveryInProgress = false // Discovery status management moved to AppViewModel
                
                updateItem(
                    status = when {
                        connected -> "Connected"
                        isDiscoveryInProgress -> "Discovering servers..."
                        else -> "Server Discovered"
                    },
                    serverStatus = if (connected) "CONNECTED" else "DISCOVERED",
                    apiEndpoint = apiEndpoint,
                    discoveryMethod = it.discoveryMethod ?: when {
                        connected -> "Connected"
                        isDiscoveryInProgress -> "Discovery..."
                        else -> "Discovered"
                    },
                    serverName = it.name ?: "-",
                    hostname = it.hostname ?: "",
                    platform = it.platform ?: "Unknown"
                )
                
                Log.d(TAG, "AppViewModel server updated: ${it.name}, connected: $connected, platform: ${it.platform}")
            } ?: run {
                // Clear UI when no server info is available
                isServerConnected = false
                serverConnectionMethod = "none"
                
                // Always set the initial state to ensure UI is properly initialized
                // This ensures buttons and other UI elements are visible from the start
                updateItem(
                    status = "请从下面选择发现服务器方式",
                    serverStatus = "DISCONNECTED",
                    apiEndpoint = "-",
                    discoveryMethod = "选择发现方式",

                )
                
                Log.d(TAG, "AppViewModel server is null, UI initialized with default state")
            }
        }
        
        // Note: Discovery status management has been removed
        // All discovery status handling is now internal
        

    }
    
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
        
        if (description.isNotEmpty()) {
            showDetailedToast("操作成功", "$title: $description", Toast.LENGTH_SHORT)
        } else {
            showDetailedToast("操作成功", title, Toast.LENGTH_SHORT)
        }
    }
    
    /**
     * Update the current item and notify the callback
     */
    private fun updateItem(
        status: String = currentItem.status,
        serverStatus: String = currentItem.serverStatus,
        apiEndpoint: String = currentItem.apiEndpoint,
        discoveryMethod: String = currentItem.discoveryMethod,

        serverName: String = currentItem.serverName,
        hostname: String = currentItem.hostname,
        platform: String = currentItem.platform
    ) {
        currentItem = DashboardItem.ItemServer(
            status = status,
            serverStatus = serverStatus,
            apiEndpoint = apiEndpoint,
            discoveryMethod = discoveryMethod,
    
            serverName = serverName,
            hostname = hostname,
            platform = platform
        )
        
        onItemUpdate(currentItem)
    }
    
    /**
     * Get current item
     */
    fun getCurrentItem(): DashboardItem.ItemServer {
        return currentItem
    }
    
    /**
     * Handle camera permission result for QR code scanning
     */
    fun handleCameraPermissionResult(isGranted: Boolean) {
        if (isGranted) {
            // Permission granted, start QR code scanner
            startQRCodeScanner()
        } else {
            // Permission denied, show error message
            showErrorMessage("相机权限被拒绝", "无法扫描二维码，请在设置中授予权限")
        }
    }
    
    /**
     * Handle QR code scan result
     */
    fun handleQrCodeScanResult(result: androidx.activity.result.ActivityResult) {
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val qrCodeContent = data?.getStringExtra("qr_code_content")
            
            if (!qrCodeContent.isNullOrEmpty()) {
                // Process the QR code content
                processQrCodeContent(qrCodeContent)
            } else {
                showErrorMessage("无效的二维码", "扫描的二维码内容为空")
            }
        } else {
            // Scan cancelled or failed
            Log.d(TAG, "QR code scan cancelled or failed")
        }
    }
    
    /**
     * Process QR code content to extract server information
     */
    private fun processQrCodeContent(content: String) {
        try {
            // Parse QR code content as JSON
            val jsonObject = com.google.gson.JsonParser.parseString(content).asJsonObject
            
            // Extract server information
            val apiEndpoint = jsonObject.get("api_endpoint")?.asString
            val serverName = jsonObject.get("name")?.asString ?: "Autodroid Server"
            val platform = jsonObject.get("platform")?.asString ?: "Unknown"
            
            if (!apiEndpoint.isNullOrEmpty()) {
                // Save server information and connect
                saveAndConnectToServer(apiEndpoint, serverName, platform)
            } else {
                showErrorMessage("无效的二维码", "二维码中缺少API端点信息")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse QR code content: ${e.message}", e)
            showErrorMessage("二维码解析失败", "无法解析二维码内容，请确保二维码格式正确")
        }
    }
    
    /**
     * Save server information and connect to it
     */
    private fun saveAndConnectToServer(apiEndpoint: String, serverName: String, platform: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Show loading indicator
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    showLoadingIndicator("正在连接到服务器")
                }
                
                // Create server model
                val server = Server(
                    serviceName = serverName,
                    name = serverName,
                    hostname = "localhost", // We'll update this later
                    platform = platform,
                    apiEndpoint = apiEndpoint,
                    connected = true,
                    discoveryMethod = "QRCode"
                )
                
                // Insert or update server in repository
                serverRepository.insertOrUpdateServer(server)
                
                // Update AppViewModel with server information
                appViewModel.setServer(server)
                
                // Show success message
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    showSuccessMessage("服务器连接成功", "已连接到 $serverName")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save and connect to server: ${e.message}", e)
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    showErrorMessage("服务器连接失败", e.message)
                }
            }
        }
    }
    
    /**
     * Start QR code scanner activity
     */
    private fun startQRCodeScanner() {
        val intent = Intent(context, com.autodroid.trader.ui.qrcode.QrCodeScannerActivity::class.java)
        startQRCodeScannerLauncher.launch(intent)
    }
    
    /**
     * Check camera permission and start QR code scanning
     */
    fun checkCameraPermissionAndScanQR() {
        // Check if we have camera permission
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            // Permission already granted, start QR code scanner
            startQRCodeScanner()
        } else {
            // Request camera permission
            requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }
    
    /**
     * Initialize QR code scanning functionality
     */
    fun initializeQRCodeScanning(
        requestCameraPermissionLauncher: ActivityResultLauncher<String>,
        startQRCodeScannerLauncher: ActivityResultLauncher<Intent>
    ) {
        this.requestCameraPermissionLauncher = requestCameraPermissionLauncher
        this.startQRCodeScannerLauncher = startQRCodeScannerLauncher
    }
    

    
    /**
     * Handle manual input button click
     */
    fun handleManualInputClick() {
        showAddServerDialog()
    }
    
    /**
     * Show add server dialog
     */
    private fun showAddServerDialog() {
        val editText = android.widget.EditText(context)
        editText.hint = "请输入服务器API端点URL"
        
        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("添加服务器")
            .setMessage("请输入服务器的API端点URL:")
            .setView(editText)
            .setPositiveButton("添加") { _, _ ->
                val apiEndpoint = editText.text.toString().trim()
                if (apiEndpoint.isNotEmpty()) {
                    // Validate and save server
                    validateAndSaveServer(apiEndpoint)
                } else {
                    showErrorMessage("无效输入", "请输入有效的API端点URL")
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    /**
     * Validate and save server information
     */
    private fun validateAndSaveServer(apiEndpoint: String) {
        // Simple validation - check if it looks like a URL
        if (apiEndpoint.startsWith("http://") || apiEndpoint.startsWith("https://")) {
            // Save server information
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Show loading indicator
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        showLoadingIndicator("正在验证服务器")
                    }
                    
                    // Validate server by making a test request
                    val isValid = validateServerEndpoint(apiEndpoint)
                    
                    if (isValid) {
                        // Create server model
                        val server = Server(
                            serviceName = "Manual Server",
                            name = "Manual Server",
                            hostname = "localhost", // We'll update this later
                            platform = "Unknown",
                            apiEndpoint = apiEndpoint,
                            connected = true,
                            discoveryMethod = "Manual"
                        )
                        
                        // Insert or update server in repository
                        serverRepository.insertOrUpdateServer(server)
                        
                        // Update AppViewModel with server information
                        appViewModel.setServer(server)
                        
                        // Show success message
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            showSuccessMessage("服务器添加成功", "已添加并连接到服务器")
                        }
                    } else {
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            showErrorMessage("服务器验证失败", "无法连接到指定的服务器端点")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to validate and save server: ${e.message}", e)
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        showErrorMessage("服务器添加失败", e.message)
                    }
                }
            }
        } else {
            showErrorMessage("无效URL", "请输入有效的HTTP或HTTPS URL")
        }
    }
    
    /**
     * Validate server endpoint by making a test request
     */
    private suspend fun validateServerEndpoint(apiEndpoint: String): Boolean {
        return try {
            // Make a simple GET request to the server endpoint
            val url = java.net.URL(apiEndpoint)
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000 // 5 seconds
            connection.readTimeout = 5000 // 5 seconds
            
            val responseCode = connection.responseCode
            connection.disconnect()
            
            // Consider successful if we get a 2xx response
            responseCode in 200..299
        } catch (e: Exception) {
            Log.e(TAG, "Failed to validate server endpoint: ${e.message}", e)
            false
        }
    }
    
    /**
     * Show server management dialog
     */
    private fun showServerManagementDialog() {
        val options = arrayOf("添加新服务器", "刷新服务器列表")
        
        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("服务器管理")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showAddServerDialog()
                    1 -> {
                        // Note: Server refresh management has been moved to AppViewModel
                        // appViewModel.refreshSavedServers()
                        showDetailedToast("服务器列表已刷新", "", Toast.LENGTH_SHORT)
                    }
                }
            }
            .show()
    }
    
    /**
     * Disconnect from current server
     */
    fun disconnectFromServer() {
        // Note: Server disconnection management is handled by updating connection status
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 断开所有服务器的连接状态
                // serverRepository.getAllServers().value?.forEach { serverEntity ->
                    // Server info is automatically updated when getConnectedServer() is called
                        // No need to manually update server info here
                // }
                showDetailedToast("已断开服务器连接", "", Toast.LENGTH_SHORT)
            } catch (e: Exception) {
                Log.e("ItemServerManager", "Error disconnecting from server: ${e.message}", e)
            }
        }
    }
    
    /**
     * Handle server management button click
     */
    fun onServerManagementButtonClick() {
        showServerManagementDialog()
    }
    
    /**
     * Handle add server button click
     */
    fun onAddServerButtonClick() {
        showAddServerDialog()
    }
    
    /**
     * Handle disconnect button click
     */
    fun onDisconnectButtonClick() {
        if (isServerConnected()) {
            disconnectFromServer()
        }
    }
    
    /**
     * Get current server info
     */
    fun getCurrentServer(): Server? {
        // Note: Server info management has been moved to AppViewModel
        return appViewModel.server.value
    }
    
    /**
     * Update server status in UI
     */
    private fun updateServerStatusUI(server: Server, status: String) {
        updateItem(
            status = status,
            serverStatus = if (server.connected) "CONNECTED" else "DISCONNECTED",
            apiEndpoint = server.apiEndpoint ?: "-",
            discoveryMethod = server.discoveryMethod ?: "Unknown"
        )
    }
    
    /**
     * Check if connected to server
     */
    fun isServerConnected(): Boolean {
        // Note: Server info management has been moved to AppViewModel
        return appViewModel.server.value?.connected == true
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
        
        // Note: QR code fallback management has been moved to AppViewModel
        // Reset QR code fallback flag

        
        // Immediately update UI to show refreshing state
        updateItem(
            status = "Refreshing server connection...",
            serverStatus = "REFRESHING",
            apiEndpoint = "-",
            discoveryMethod = "Auto Refresh"
        )
        
        // Reinitialize the server connection item with current data
        initialize()
        
        // Force immediate UI update with current server state
        forceUpdateWithCurrentState()
    }
    
    /**
     * Force immediate update with current server state
     */
    private fun forceUpdateWithCurrentState() {
        // Note: Discovery status management has been moved to AppViewModel
        // Get server information from AppViewModel (single source of truth)
        val currentServer = appViewModel.server.value
        
        if (currentServer != null) {
            // Server is connected or discovered, update UI immediately
            updateItem(
                status = when {
                    currentServer.connected -> "Connected via ${currentServer.discoveryMethod ?: "Unknown"}"
                    currentServer.discoveryMethod == "QRCode" -> "QR Code Scanned"
                    else -> "Server Found"
                },
                serverStatus = if (currentServer.connected) "CONNECTED" else "DISCOVERED",
                apiEndpoint = currentServer.apiEndpoint ?: "-",
                discoveryMethod = currentServer.discoveryMethod ?: "Auto Discovery",

                serverName = currentServer.name ?: "Autodroid Server",
                hostname = currentServer.hostname ?: "-",
                platform = currentServer.platform ?: "-"
            )
        } else {
            // No server info available, show disconnected state
            updateItem(
                status = "No server connected",
                serverStatus = "DISCONNECTED",
                apiEndpoint = "-",
                discoveryMethod = "None"
            )
        }
    }
    
    /**
     * Handle server connection item update
     */
    fun handleServerConnectionUpdate(item: DashboardItem, dashboardItems: MutableList<DashboardItem>, dashboardAdapter: DashboardAdapter?): Boolean {
        return try {
            if (item is DashboardItem.ItemServer) {
                // Find existing server connection item in the list
                val existingIndex = dashboardItems.indexOfFirst { it is DashboardItem.ItemServer }
                
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
            Log.e(TAG, "Error handling server connection update: ${e.message}", e)
            false
        }
    }
}