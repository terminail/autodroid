package com.autodroid.manager.ui.dashboard

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import com.autodroid.manager.model.DashboardItem
import com.autodroid.manager.model.Server
// import com.autodroid.manager.model.DiscoveredServer
import com.autodroid.manager.AppViewModel
import com.autodroid.manager.ui.adapters.DashboardAdapter
import com.autodroid.manager.service.DiscoveryStatusManager
import com.autodroid.data.repository.ServerRepository
import okhttp3.OkHttpClient
import okhttp3.Request
import androidx.activity.result.ActivityResultLauncher
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
    private val appViewModel: AppViewModel,
    private val onItemUpdate: (DashboardItem.ServerItem) -> Unit
) {
    private val TAG = "ServerItemManager"
    
    private var currentItem = DashboardItem.ServerItem()
    
    // Server repository for database operations
    private val serverRepository = ServerRepository.getInstance(context.applicationContext as android.app.Application)
    
    // Activity Result API launchers for QR code scanning
    private lateinit var requestCameraPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var startQRCodeScannerLauncher: ActivityResultLauncher<Intent>
    
    // Server connection state
    private var isServerConnected = false
    private var serverConnectionMethod = "none" // mDNS, QRCode, or none
    
    /**
     * Initialize the manager and start observing server status
     */
    fun initialize() {

        
        setupObservers()
        
        // mDNS discovery will be started manually by user - do not auto-start
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
                        isDiscoveryInProgress -> "Discovering servers via mDNS..."
                        else -> "Server Discovered"
                    },
                    serverStatus = if (connected) "CONNECTED" else "DISCOVERED",
                    apiEndpoint = apiEndpoint,
                    discoveryMethod = it.discoveryMethod ?: when {
                        connected -> "Connected"
                        isDiscoveryInProgress -> "Discovery via mDNS..."
                        else -> "Discovered"
                    },
                    serverName = it.name,
                    hostname = it.hostname ?: "",
                    platform = it.platform ?: "Unknown",
                    isStartMdnsButtonEnabled = !isDiscoveryInProgress
                )
                
                Log.d(TAG, "AppViewModel server updated: ${it.name}, connected: $connected, platform: ${it.platform}")
            } ?: run {
                // Clear UI when no server info is available
                isServerConnected = false
                serverConnectionMethod = "none"
                
                // 在手动模式下，应用启动时完全不设置任何状态
                // 只有在用户交互或实际状态变化时才更新UI
                // 避免在启动时自动显示任何状态信息
                if (currentItem.status == "Discovering servers..." || currentItem.status == "请从下面选择发现服务器方式") {
                    // 保持空白状态，不自动设置任何文本
                    return@run
                }
                
                // 只有在实际有状态变化时才更新
                updateItem(
                    status = "请从下面选择发现服务器方式",
                    serverStatus = "DISCONNECTED",
                    apiEndpoint = "-",
                    discoveryMethod = "选择发现方式",
                    isStartMdnsButtonEnabled = true // mDNS按钮启用
                )
                
                Log.d(TAG, "AppViewModel server is null, UI cleared")
            }
        }
        
        // Note: Discovery status management is handled by DiscoveryStatusManager
        // Observe discovery status from DiscoveryStatusManager
        DiscoveryStatusManager.discoveryStatus.observe(lifecycleOwner) { discoveryStatus ->
            discoveryStatus?.let { status ->
                val isDiscovering = status.inProgress
                
                // 在应用启动时，如果当前状态是初始状态，则不自动显示mDNS状态
                if (currentItem.status == "Discovering servers..." || currentItem.status == "请从下面选择发现服务器方式") {
                    // 保持空白状态，不自动设置任何mDNS相关文本
                    return@let
                }
                
                // Update UI based on discovery status
                when {
                    isDiscovering -> {
                        updateItem(
                            status = "Discovering servers via mDNS...",
                            serverStatus = "DISCOVERING",
                            apiEndpoint = "-",
                            discoveryMethod = "Auto mDNS Discovery",
                            isStartMdnsButtonEnabled = false // 仅mDNS按钮在发现过程中禁用
                        )
                    }
                    status.failed -> {
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
            val apiEndpoint = jsonObject.optString("apiEndpoint", "")
            val ipAddress = jsonObject.optString("ipAddress", "")
            val port = jsonObject.optInt("port", 0)
            
            if (apiEndpoint.isNotEmpty() && ipAddress.isNotEmpty() && port > 0) {
                // Note: QR code fallback management has been moved to AppViewModel
                // Mark QR code as chosen as fallback to prevent mDNS restart
                // DiscoveryStatusManager.setQrCodeChosenAsFallback(true) - REMOVED in architecture refactoring

                updateItem(
                    status = "QR Code Scanned Successfully",
                    apiEndpoint = apiEndpoint,
                    serverStatus = "DISCOVERED"
                )
                
                // Update DiscoveryStatusManager with server info
                val serverObj = Server(
                    serviceName = "Autodroid Server",
                    name = serverName,
                    hostname = ipAddress,
                    apiEndpoint = apiEndpoint,
                    connected = false, // Not connected yet
                    discoveryMethod = "QRCode"
                )
                
                // Server data management is now handled by AppViewModel
                // The actual server connection should be initiated by UI components through AppViewModel
                
                // Set server status to READY for now (health check removed)
                updateItem(
                    serverStatus = "READY"
                )
                
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
     * Get the current server connection item
     */
    fun getCurrentItem(): DashboardItem.ServerItem {
        return currentItem
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
     * Show add server dialog
     */
    fun showAddServerDialog() {
        val editText = android.widget.EditText(context)
        editText.hint = "服务器地址 (IP:Port)"
        
        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("添加服务器")
            .setView(editText)
            .setPositiveButton("添加") { _, _ ->
                val input = editText.text.toString().trim()
                if (input.isNotEmpty()) {
                    addServerManually(input)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    /**
     * Add a server manually
     */
    private fun addServerManually(apiEndpoint: String) {
        // Show progress toast
        showDetailedToast("正在连接服务器...", "", Toast.LENGTH_SHORT)
        
        // Use coroutine to perform server connection in background
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                // Set API endpoint and get server info
                com.autodroid.manager.network.ApiClient.getInstance().setApiEndpoint(apiEndpoint)
                
                // Fetch server information from FastAPI
                val serverInfo = com.autodroid.manager.network.ApiClient.getInstance().getServerInfo()
                
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (serverInfo != null) {
                        // Extract host from api_endpoint
                        val host = try {
                            val endpoint = serverInfo.apiEndpoint ?: apiEndpoint
                            val url = java.net.URL(endpoint)
                            url.host
                        } catch (e: Exception) {
                            // Fallback parsing if URL parsing fails
                            val hostRegex = "//([^:/]+)".toRegex()
                            val hostMatch = hostRegex.find(apiEndpoint)
                            hostMatch?.groupValues?.get(1) ?: "localhost"
                        }
                        
                        // Create a Server with all the information we already have
                        val serverEndpoint = serverInfo.apiEndpoint ?: apiEndpoint
                        val server = Server(
                            serviceName = serverInfo.name ?: "Manual Server",
                            name = serverInfo.name ?: "Manual Server",
                            hostname = host,
                            platform = serverInfo.platform,
                            apiEndpoint = serverEndpoint,
                            discoveryMethod = "manual"
                        )
                        
                        // Add server using repository
                        try {
                            serverRepository.insertOrUpdateServer(server)
                            // UI will be automatically updated by the observer in setupObservers()
                            showDetailedToast("服务器连接成功", "", Toast.LENGTH_SHORT)
                        } catch (e: Exception) {
                            showDetailedToast("添加服务器失败: ${e.message}", "", Toast.LENGTH_SHORT)
                        }
                    } else {
                        showDetailedToast("无法获取服务器信息", "", Toast.LENGTH_SHORT)
                    }
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    showDetailedToast("服务器连接失败: ${e.message}", "", Toast.LENGTH_SHORT)
                }
            }
        }
    }
    
    /**
     * Show server management dialog
     */
    fun showServerManagementDialog() {
        val options = arrayOf("添加服务器", "刷新服务器列表")
        
        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("服务器管理")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showAddServerDialog()
                    1 -> {
                        // Note: Server refresh management has been moved to AppViewModel
                        appViewModel.refreshSavedServers()
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
                serverRepository.getAllServers().value?.forEach { serverEntity ->
                    // Server info is automatically updated when getConnectedServer() is called
                        // No need to manually update server info here
                }
                showDetailedToast("已断开服务器连接", "", Toast.LENGTH_SHORT)
            } catch (e: Exception) {
                Log.e("ServerItemManager", "Error disconnecting from server: ${e.message}", e)
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
            discoveryMethod = server.discoveryMethod ?: "Unknown",
            isStartMdnsButtonEnabled = !server.connected
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
        // Reset QR code fallback flag to allow mDNS discovery
        // DiscoveryStatusManager.resetQrCodeFallback() - REMOVED in architecture refactoring
        
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
     * Force immediate update with current server state from DiscoveryStatusManager
     */
    private fun forceUpdateWithCurrentState() {
        // Note: Discovery status management has been moved to AppViewModel
        // Get server information from DiscoveryStatusManager (single source of truth)
        val currentDiscoveryStatus = DiscoveryStatusManager.discoveryStatus.value
        val currentServer = appViewModel.server.value
        
        if (currentServer != null) {
            // Server is connected or discovered, update UI immediately
            updateItem(
                status = when {
                    currentServer.connected -> "Connected via ${currentServer.discoveryMethod ?: "Unknown"}"
                    currentServer.discoveryMethod == "mDNS" -> "mDNS Discovery Successful"
                    currentServer.discoveryMethod == "QRCode" -> "QR Code Scanned"
                    else -> "Server Found"
                },
                serverStatus = if (currentServer.connected) "CONNECTED" else "DISCOVERED",
                apiEndpoint = currentServer.apiEndpoint ?: "-",
                discoveryMethod = currentServer.discoveryMethod ?: "Auto mDNS Discovery",
                isStartMdnsButtonEnabled = currentDiscoveryStatus?.inProgress != true,
                serverName = currentServer.name ?: "Autodroid Server",
                hostname = currentServer.hostname ?: "-",
                platform = currentServer.platform ?: "-"
            )
        } else {
            // No server info available, show discovery status
            val isDiscovering = currentDiscoveryStatus?.inProgress ?: false
            
            updateItem(
                status = when {
                    isDiscovering -> "Discovering servers via mDNS..."
                    currentDiscoveryStatus?.failed == true -> "mDNS Discovery Failed"
                    else -> "Discovering servers via mDNS..."
                },
                serverStatus = when {
                    isDiscovering -> "DISCOVERING"
                    currentDiscoveryStatus?.failed == true -> "FAILED"
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
     * Handle START mDNS button click (manual mode without retry)
     */
    fun handleStartMdnsDiscovery() {
        // Note: Discovery management is handled directly by DiscoveryStatusManager
        // Start mDNS discovery (manual mode without retry)
        DiscoveryStatusManager.startDiscovery()
        
        // Update UI to show discovery started
        updateItem(
            status = "Starting mDNS Discovery...",
            serverStatus = "DISCOVERING",
            apiEndpoint = "-",
            discoveryMethod = "Manual mDNS Discovery",
            isStartMdnsButtonEnabled = true
        )
        
        // Show detailed feedback to user
        showDetailedToast("开始mDNS发现服务", "正在搜索网络中的Autodroid服务器...", Toast.LENGTH_LONG)
        
        Log.d(TAG, "START mDNS discovery initiated (manual mode)")
    }
    
    /**
     * Handle manual input button click - show dialog for server address input
     */
    fun handleManualInputClick() {
        // Show manual input dialog

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
            hint = "输入API Endpoint (例如: http://192.168.1.59:8004/api)"
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
                    // Add server manually
                    addServerManually(apiEndpoint)
                } else {
                    android.widget.Toast.makeText(context, "请输入服务器地址", android.widget.Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }


}
    
