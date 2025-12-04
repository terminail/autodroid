package com.autodroid.manager.ui.dashboard

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.net.Inet4Address
import java.net.NetworkInterface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.autodroid.manager.R
import com.autodroid.manager.managers.APKScannerManager
import com.autodroid.manager.managers.DeviceInfoManager
import com.autodroid.manager.managers.WorkflowManager
import com.autodroid.manager.model.DiscoveredServer
import com.autodroid.manager.ui.BaseFragment
import com.autodroid.manager.ui.adapters.BaseItemAdapter
import com.autodroid.manager.service.DiscoveryStatusManager
import com.autodroid.manager.viewmodel.AppViewModel

class DashboardFragment : BaseFragment() {
    private val TAG = "DashboardFragment"
    
    // QR code scanning request code
    private val QR_CODE_SCAN_REQUEST_CODE = 100
    private val CAMERA_PERMISSION_REQUEST_CODE = 101
    
    // Activity Result API launchers
    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, start QR code scanner
            startQRCodeScanner()
        } else {
            // Permission denied
            Toast.makeText(requireContext(), "Camera permission is required for QR code scanning", Toast.LENGTH_SHORT).show()
        }
    }
    
    private val startQRCodeScannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            // Get QR code result from the scanner
            val qrResult = result.data?.getStringExtra("QR_RESULT")
            if (qrResult != null) {
                processQRCodeResult(qrResult)
            } else {
                Toast.makeText(requireContext(), "Failed to get QR code result", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "QR code scanning cancelled", Toast.LENGTH_SHORT).show()
        }
    }
    
    // UI elements for server connection
    private var connectionStatusTextView: TextView? = null
    private var serverIpTextView: TextView? = null
    private var serverPortTextView: TextView? = null
    private var serverStatusTextView: TextView? = null
    private var apiEndpointTextView: TextView? = null
    private var scanQrButton: Button? = null
    
    // UI elements for WiFi information
    private var wifiNameTextView: TextView? = null
    private var wifiIpTextView: TextView? = null
    private var wifiStatusTextView: TextView? = null
    
    // UI elements for device information
    private var deviceUdidTextView: TextView? = null
    private var userIdTextView: TextView? = null
    private var deviceNameTextView: TextView? = null
    private var platformTextView: TextView? = null
    private var deviceModelTextView: TextView? = null
    private var deviceStatusTextView: TextView? = null
    private var connectionTimeTextView: TextView? = null
    private var scanApksButton: Button? = null
    private var serversRecyclerView: RecyclerView? = null

    // Managers
    private var deviceInfoManager: DeviceInfoManager? = null
    private var workflowManager: WorkflowManager? = null
    private var apkScannerManager: APKScannerManager? = null

    private var serversAdapter: BaseItemAdapter? = null
    private val discoveredServers = mutableListOf<DiscoveredServer>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[AppViewModel::class.java]

        // Initialize managers
        deviceInfoManager = DeviceInfoManager(requireContext(), viewModel!!)
        workflowManager = WorkflowManager(requireContext(), viewModel!!)
        apkScannerManager = APKScannerManager(requireContext())

        // NetworkService is now auto-started in MyApplication
    }

    override val layoutId: Int
        get() = R.layout.fragment_dashboard

    override fun initViews(view: View?) {
        // Initialize UI elements for server connection
        connectionStatusTextView = view?.findViewById(R.id.connection_status)
        serverIpTextView = view?.findViewById(R.id.server_ip)
        serverPortTextView = view?.findViewById(R.id.server_port)
        serverStatusTextView = view?.findViewById(R.id.server_status)
        apiEndpointTextView = view?.findViewById(R.id.api_endpoint)
        scanQrButton = view?.findViewById(R.id.scan_qr_button)
        scanQrButton?.setOnClickListener {
            checkCameraPermissionAndScanQR()
        }
        
        // Initialize UI elements for WiFi information
        wifiNameTextView = view?.findViewById(R.id.wifi_name)
        wifiIpTextView = view?.findViewById(R.id.wifi_ip)
        wifiStatusTextView = view?.findViewById(R.id.wifi_status)
        
        // Initialize UI elements for device information
        deviceUdidTextView = view?.findViewById(R.id.device_udid)
        userIdTextView = view?.findViewById(R.id.user_id)
        deviceNameTextView = view?.findViewById(R.id.device_name)
        platformTextView = view?.findViewById(R.id.platform)
        deviceModelTextView = view?.findViewById(R.id.device_model)
        deviceStatusTextView = view?.findViewById(R.id.device_status)
        connectionTimeTextView = view?.findViewById(R.id.connection_time)
        scanApksButton = view?.findViewById(R.id.scan_apks_button)
        serversRecyclerView = view?.findViewById(R.id.dashboard_items_recycler_view)

        // Set up RecyclerView for servers
        serversRecyclerView?.layoutManager = LinearLayoutManager(context)
        serversAdapter = BaseItemAdapter(
            mutableListOf(),
            object : BaseItemAdapter.OnItemClickListener {
                override fun onItemClick(item: MutableMap<String?, Any?>?) {
                    handleServerClick(item)
                }
            },
            R.layout.item_generic
        )
        serversRecyclerView?.adapter = serversAdapter

        // Set up click listeners
        scanApksButton?.setOnClickListener {
            apkScannerManager?.scanInstalledApks()
        }

        // Debug: Check if NetworkService is running
        Log.d("DashboardFragment", "Initializing views - checking NetworkService status")
        Log.d("DashboardFragment", "QR code fallback flag: ${DiscoveryStatusManager.isQrCodeChosenAsFallback()}")

        // Update UI with initial data
        updateUI()
        
        // Start observing discovery status immediately
        setupObservers()
        
        // NetworkService is now auto-started in MyApplication, no need to start it here
        // DashboardFragment only observes the discovery status and updates UI accordingly
    }

    private var discoveryStartTime: Long = 0
    private val discoveryTimer = object : android.os.CountDownTimer(Long.MAX_VALUE, 1000) {
        override fun onTick(millisUntilFinished: Long) {
            val elapsedSeconds = (System.currentTimeMillis() - discoveryStartTime) / 1000
            scanQrButton?.text = "Discovering servers... ${elapsedSeconds}s"
        }
        
        override fun onFinish() {
            // Timer will run indefinitely until canceled
        }
    }
    
    override fun setupObservers() {
        // Observe server info from DiscoveryStatusManager
        DiscoveryStatusManager.serverInfo.observe(viewLifecycleOwner) { serverInfo ->
            serverInfo?.let {
                val connected = it["connected"] as? Boolean ?: false
                val discoveryMethod = it["discovery_method"] as? String ?: ""
                val ip = it["ip"] as? String ?: ""
                val port = it["port"] as? Int ?: 0
                val apiEndpoint = it["api_endpoint"] as? String ?: "-"
                
                // Update UI with server information
                serverIpTextView?.text = ip
                serverPortTextView?.text = port.toString()
                apiEndpointTextView?.text = apiEndpoint
                
                // Update connection status based on discovery method
                if (connected) {
                    connectionStatusTextView?.text = "Connected via $discoveryMethod"
                } else {
                    connectionStatusTextView?.text = when (discoveryMethod) {
                        "mDNS" -> "mDNS OK"
                        "QRCode" -> "QRCode OK"
                        else -> discoveryMethod
                    }
                    
                    // NetworkService is auto-started in MyApplication, no need to restart it here
                    // DashboardFragment only observes the discovery status and updates UI accordingly
                }
                
                // Check server health if we have an API endpoint
                if (apiEndpoint != "-") {
                    checkServerHealth(apiEndpoint) { isHealthy ->
                        requireActivity().runOnUiThread {
                            serverStatusTextView?.text = if (isHealthy) "READY" else "FAILED"
                        }
                    }
                }
                
                // Hide the QR code button when mDNS succeeds
                if (discoveryMethod == "mDNS") {
                    scanQrButton?.visibility = View.GONE
                }
            } ?: run {
                // Clear UI when no server info is available
                serverIpTextView?.text = "Searching..."
                serverPortTextView?.text = "-"
                serverStatusTextView?.text = "Checking..."
                apiEndpointTextView?.text = "-"
                connectionStatusTextView?.text = "Discovering servers..."
                
                // Show QR code button with status when no server is discovered (disabled)
                scanQrButton?.visibility = View.VISIBLE
                scanQrButton?.text = "Discovering servers..."
                scanQrButton?.isEnabled = false
            }
        }
        
        // Observe discovery status from DiscoveryStatusManager
        DiscoveryStatusManager.discoveryInProgress.observe(viewLifecycleOwner) { inProgress ->
            if (inProgress == true) {
                discoveryStartTime = System.currentTimeMillis()
                discoveryTimer.start()
                
                requireActivity().runOnUiThread {
                    connectionStatusTextView?.text = "mDNS Discovering..."
                    // Show QR code button with status info during discovery (disabled)
                    scanQrButton?.visibility = View.VISIBLE
                    scanQrButton?.text = "mDNS Discovering..."
                    scanQrButton?.isEnabled = false
                    Log.d(TAG, "Discovery in progress - QR code button disabled")
                }
            } else {
                discoveryTimer.cancel()
                
                requireActivity().runOnUiThread {
                    scanQrButton?.text = "Scan QR Code"
                    scanQrButton?.isEnabled = true
                    Log.d(TAG, "Discovery stopped - QR code button enabled")
                }
            }
        }
        
        // Observe discovery retry count from DiscoveryStatusManager
        DiscoveryStatusManager.discoveryRetryCount.observe(viewLifecycleOwner) { retryCount ->
            val maxRetries = DiscoveryStatusManager.discoveryMaxRetries.value ?: 0
            if (retryCount != null && maxRetries > 0) {
                if (retryCount < maxRetries) {
                    requireActivity().runOnUiThread {
                        connectionStatusTextView?.text = "mDNS Retry ${retryCount + 1}/$maxRetries"
                        // Show QR code button with retry info during retry (disabled)
                        scanQrButton?.visibility = View.VISIBLE
                        scanQrButton?.text = "mDNS Retry ${retryCount + 1}/$maxRetries"
                        scanQrButton?.isEnabled = false
                        Log.d(TAG, "Discovery retry ${retryCount + 1}/$maxRetries - QR code button disabled")
                    }
                }
            }
        }
        
        // Observe discovery failure from DiscoveryStatusManager
        DiscoveryStatusManager.discoveryFailed.observe(viewLifecycleOwner) { failed ->
            if (failed == true) {
                discoveryTimer.cancel()
                // Calculate actual elapsed time for mDNS failure
                val elapsedSeconds = if (discoveryStartTime > 0) {
                    (System.currentTimeMillis() - discoveryStartTime) / 1000
                } else {
                    56 // Fallback to default time if start time not recorded
                }
                connectionStatusTextView?.text = "mDNS Failed after ${elapsedSeconds}s"
                serverIpTextView?.text = "Discovery failed"
                serverPortTextView?.text = "-"
                serverStatusTextView?.text = "FAILED"
                apiEndpointTextView?.text = "-"
                
                // Ensure UI updates on main thread
                requireActivity().runOnUiThread {
                    // Show QR code button with "Scan QR Code" text when discovery fails (enabled)
                    scanQrButton?.visibility = View.VISIBLE
                    scanQrButton?.text = "Scan QR Code"
                    scanQrButton?.isEnabled = true
                    Log.d(TAG, "mDNS failed after ${elapsedSeconds}s - QR code button enabled and visible")
                }
                
                // Mark that user is now using QR code as fallback
                DiscoveryStatusManager.setQrCodeChosenAsFallback(true)
                
                // Stop NetworkService to avoid unnecessary retries
                DiscoveryStatusManager.stopNetworkService()
                Log.d(TAG, "Stopped NetworkService after mDNS failure to conserve resources")
            } else {
                // Reset failure state when discovery is restarted
                requireActivity().runOnUiThread {
                    connectionStatusTextView?.text = "Discovering servers..."
                    serverIpTextView?.text = "Searching..."
                    serverPortTextView?.text = "-"
                    serverStatusTextView?.text = "Checking..."
                    apiEndpointTextView?.text = "-"
                    Log.d(TAG, "Discovery failure state reset")
                }
            }
        }
    }

    private fun setupServiceDiscovery() {
        // This method is no longer needed as NetworkService handles discovery
        // and DiscoveryStatusManager provides the status updates
    }

    private fun updateServerListUI() {
        val serverItems = discoveredServers.map { server ->
            mutableMapOf<String?, Any?>(
                "title" to server.displayName,
                "subtitle" to "${server.host}:${server.port}",
                "status" to "Available",
                "server" to server
            )
        }.toMutableList()

        serversAdapter?.updateItems(serverItems)
    }

    private fun handleServerClick(item: MutableMap<String?, Any?>?) {
        val server = item?.get("server") as? DiscoveredServer
        if (server != null) {
            // Navigate to ServerDetailFragment with server info
            // TODO: Fix navigation to ServerDetailFragment
            // val bundle = Bundle()
            // bundle.putString("serverIp", server.host)
            // bundle.putInt("serverPort", server.port)
            // findNavController().navigate(R.id.nav_server_detail, bundle)
            Toast.makeText(requireContext(), "Server clicked: ${server.host}:${server.port}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Update WiFi information when fragment is created
        updateWiFiInfo()
    }

    private fun updateUI() {
        // Update device information
        deviceUdidTextView?.text = getDeviceUDID()
        userIdTextView?.text = "user001"
        deviceNameTextView?.text = "KNT-AL10"
        platformTextView?.text = "Android"
        deviceModelTextView?.text = Build.MODEL
        deviceStatusTextView?.text = "在线"
        
        // Set current connection time
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        connectionTimeTextView?.text = dateFormat.format(Date())
        
        updateConnectionStatus("Discovering servers...")
        // Show "Searching..." for server IP from the start
        serverIpTextView?.text = "Searching..."
        serverPortTextView?.text = "-"
        serverStatusTextView?.text = "Disconnected"
        apiEndpointTextView?.text = "-"
        
        // Update WiFi information
        updateWiFiInfo()
    }

    /**
     * Update WiFi information including name, IP, and status
     */
    private fun updateWiFiInfo() {
        val wifiInfo = getCurrentWiFiInfo()
        
        // Update WiFi name
        wifiNameTextView?.text = wifiInfo.first ?: "Not connected"
        
        // Update WiFi IP
        wifiIpTextView?.text = wifiInfo.second ?: "-"
        
        // Update WiFi status
        if (wifiInfo.first == null) {
            wifiStatusTextView?.text = "Not connected to any WiFi"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                wifiStatusTextView?.setTextColor(resources.getColor(android.R.color.holo_orange_dark, null))
            } else {
                @Suppress("DEPRECATION")
                wifiStatusTextView?.setTextColor(resources.getColor(android.R.color.holo_orange_dark))
            }
            updateConnectionStatus("Please connect to WiFi to discover servers")
        } else {
            wifiStatusTextView?.text = "Connected to WiFi"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                wifiStatusTextView?.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
            } else {
                @Suppress("DEPRECATION")
                wifiStatusTextView?.setTextColor(resources.getColor(android.R.color.holo_green_dark))
            }
        }
    }

    /**
     * Get device UDID (unique identifier)
     */
    private fun getDeviceUDID(): String {
        return try {
            // Try to get Android ID as UDID
            Settings.Secure.getString(
                requireContext().contentResolver,
                Settings.Secure.ANDROID_ID
            ) ?: "Unknown-UDID"
        } catch (e: Exception) {
            Log.e(TAG, "Error getting device UDID", e)
            "Unknown-UDID"
        }
    }

    /**
     * Get current WiFi information (name, IP)
     */
    private fun getCurrentWiFiInfo(): Pair<String?, String?> {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For Android 10+ (API 29+)
            val network = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
            
            if (networkCapabilities != null && networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                val linkProperties = connectivityManager.getLinkProperties(network)
                val wifiName = getWifiName()
                val wifiIp = getIpAddressFromLinkProperties(linkProperties)
                return Pair(wifiName, wifiIp)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // For Android 6.0-9 (API 23-28)
            val network = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
            
            if (networkCapabilities != null && networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                val wifiManager = requireContext().applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
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
            val wifiManager = requireContext().applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
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
     * Get WiFi name (SSID) for Android 10+
     */
    private fun getWifiName(): String? {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val network = connectivityManager.activeNetwork
                val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
                
                if (networkCapabilities != null && networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    // For Android 10+, we need to use WifiManager to get SSID
                    val wifiManager = requireContext().applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    @Suppress("DEPRECATION")
                    val wifiInfo = wifiManager.connectionInfo
                    return wifiInfo.ssid?.removeSurrounding("\"")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting WiFi name: ${e.message}")
        }
        return null
    }
    
    /**
     * Get IP address from LinkProperties for Android 10+
     */
    private fun getIpAddressFromLinkProperties(linkProperties: android.net.LinkProperties?): String? {
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
     * Get device's local IP address
     */
    private fun getLocalIpAddress(): String? {
        try {
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
        } catch (e: Exception) {
            Log.e(TAG, "Error getting local IP address: ${e.message}")
        }
        return null
    }

    /**
     * Convert integer IP to string format
     */
    private fun intToIp(ipAddress: Int): String {
        return String.format(
            Locale.getDefault(),
            "%d.%d.%d.%d",
            ipAddress and 0xff,
            ipAddress shr 8 and 0xff,
            ipAddress shr 16 and 0xff,
            ipAddress shr 24 and 0xff
        )
    }

    /**
     * Check if device is on the same local network as the server
     */
    private fun isOnSameNetwork(serverIp: String, deviceIp: String): Boolean {
        try {
            val serverParts = serverIp.split(".")
            val deviceParts = deviceIp.split(".")
            
            // Check if first 3 octets are the same (assuming /24 subnet)
            return serverParts.size == 4 && deviceParts.size == 4 &&
                   serverParts[0] == deviceParts[0] &&
                   serverParts[1] == deviceParts[1] &&
                   serverParts[2] == deviceParts[2]
        } catch (e: Exception) {
            Log.e(TAG, "Error checking network: ${e.message}")
            return false
        }
    }

    /**
     * Check camera permission and start QR code scanning
     */
    private fun checkCameraPermissionAndScanQR() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) 
            != PackageManager.PERMISSION_GRANTED) {
            // Request camera permission using the new launcher
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            // Permission already granted, start QR code scanning
            startQRCodeScanner()
        }
    }
    
    /**
     * Start QR code scanner activity
     */
    private fun startQRCodeScanner() {
        try {
            // Start the QR code scanner activity using the new launcher
            val intent = android.content.Intent(requireContext(), com.autodroid.manager.ui.qrcode.QrCodeScannerActivity::class.java)
            startQRCodeScannerLauncher.launch(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting QR code scanner: ${e.message}")
            Toast.makeText(requireContext(), "Failed to start QR code scanner", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Process QR code scan result
     */
    private fun processQRCodeResult(qrResult: String) {
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

                
                // Update UI with server information
                serverIpTextView?.text = ipAddress
                serverPortTextView?.text = port.toString()
                apiEndpointTextView?.text = apiEndpoint
                connectionStatusTextView?.text = "QRCode"
                
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
                    requireActivity().runOnUiThread {
                        serverStatusTextView?.text = if (isHealthy) "READY" else "FAILED"
                        
                        // If server is healthy, connect to it
                        if (isHealthy) {
                            connectToServer(ipAddress, port, apiEndpoint)
                        }
                    }
                }
                
                Toast.makeText(requireContext(), "QR code scanned successfully", Toast.LENGTH_SHORT).show()
                return
            }
            
            Toast.makeText(requireContext(), "Invalid QR code format", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error processing QR code result: ${e.message}")
            Toast.makeText(requireContext(), "Failed to process QR code", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Connect to server using IP and port
     */
    private fun connectToServer(serverIp: String, serverPort: Int, apiEndpoint: String) {
        // This would implement the actual connection logic
        // For now, we'll just update the UI
        Log.i(TAG, "Connecting to server: $serverIp:$serverPort")
        
        // Get the discovery method from the server info
        val discoveryMethod = DiscoveryStatusManager.serverInfo.value?.get("discovery_method")?.toString() ?: "unknown"
        
        // Update connection status with the discovery method
        connectionStatusTextView?.text = "Connected to server via $discoveryMethod"
        
        // In a real implementation, you would:
        // 1. Create a network connection to the server
        // 2. Authenticate if needed
        // 3. Update the server info in the DiscoveryStatusManager
        // 4. Navigate to the server detail screen
        
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
            requireActivity().runOnUiThread {
                serverStatusTextView?.text = if (isHealthy) "READY" else "FAILED"
            }
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
                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                
                val request = okhttp3.Request.Builder()
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
    
    private fun updateConnectionStatus(status: String) {
        connectionStatusTextView?.text = status
    }

    override fun onDestroy() {
        super.onDestroy()
        // NetworkService now handles its own lifecycle
        // No need to clean up nsdHelper or search progress handler
    }
}