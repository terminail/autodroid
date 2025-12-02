package com.autodroid.manager.ui.dashboard

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Locale
import com.autodroid.manager.R
import com.autodroid.manager.managers.APKScannerManager
import com.autodroid.manager.managers.DeviceInfoManager
import com.autodroid.manager.managers.WorkflowManager
import com.autodroid.manager.model.DiscoveredServer
import com.autodroid.manager.ui.BaseFragment
import com.autodroid.manager.ui.adapters.BaseItemAdapter
import com.autodroid.manager.service.NsdHelper
import com.autodroid.manager.viewmodel.AppViewModel

class DashboardFragment : BaseFragment() {
    private val TAG = "DashboardFragment"
    // UI Components
    private var connectionStatusTextView: TextView? = null
    private var deviceInfoTextView: TextView? = null
    private var serverIpTextView: TextView? = null
    private var serverPortTextView: TextView? = null
    private var serverStatusTextView: TextView? = null
    private var wifiNameTextView: TextView? = null
    private var wifiIpTextView: TextView? = null
    private var appLocalIpTextView: TextView? = null
    private var wifiStatusTextView: TextView? = null
    private var scanApksButton: Button? = null
    private var serversRecyclerView: RecyclerView? = null

    // Managers
    private var deviceInfoManager: DeviceInfoManager? = null
    private var workflowManager: WorkflowManager? = null
    private var apkScannerManager: APKScannerManager? = null
    private var nsdHelper: NsdHelper? = null

    private var serversAdapter: BaseItemAdapter? = null
    private val discoveredServers = mutableListOf<DiscoveredServer>()
    private var discoveryRetryCount = 0
    private val maxDiscoveryRetries = 5
    private val initialRetryDelayMs = 2000L
    private var searchStartTime: Long = 0
    private var searchProgressHandler: Handler? = null
    private var searchProgressRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[AppViewModel::class.java]

        // Initialize managers
        deviceInfoManager = DeviceInfoManager(requireContext(), viewModel!!)
        workflowManager = WorkflowManager(requireContext(), viewModel!!)
        apkScannerManager = APKScannerManager(requireContext())

        // Initialize mDNS discovery
        setupServiceDiscovery()
    }

    override val layoutId: Int
        get() = R.layout.fragment_dashboard

    override fun initViews(view: View?) {
        connectionStatusTextView = view?.findViewById(R.id.connection_status)
        deviceInfoTextView = view?.findViewById(R.id.device_info)
        serverIpTextView = view?.findViewById(R.id.server_ip)
        serverPortTextView = view?.findViewById(R.id.server_port)
        serverStatusTextView = view?.findViewById(R.id.server_status)
        wifiNameTextView = view?.findViewById(R.id.wifi_name)
        wifiIpTextView = view?.findViewById(R.id.wifi_ip)
        appLocalIpTextView = view?.findViewById(R.id.app_local_ip)
        wifiStatusTextView = view?.findViewById(R.id.wifi_status)
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

        // Update UI with initial data
        updateUI()
    }

    override fun setupObservers() {
        viewModel?.let { vm ->
            // Observe the unified serverInfo object to ensure all properties update together
            vm.serverInfo.observe(viewLifecycleOwner) { serverInfo ->
                if (serverInfo != null) {
                    // Update all UI elements from the unified serverInfo object
                    val ip = serverInfo["ip"]?.toString() ?: "Unknown"
                    val port = serverInfo["port"]?.toString() ?: "-"
                    val connected = serverInfo["connected"] as? Boolean ?: false
                    val name = serverInfo["name"]?.toString() ?: "Unknown Server"
                    
                    serverIpTextView?.text = ip
                    serverPortTextView?.text = port
                    serverStatusTextView?.text = if (connected) "Connected" else "Disconnected"
                    
                    // Update connection status based on server info
                    connectionStatusTextView?.text = "Connection Status: ${if (connected) "Connected to $name" else "Discovered $name"}"
                } else {
                    // Clear UI when no server info is available
                    serverIpTextView?.text = "Searching..."
                    serverPortTextView?.text = "-"
                    serverStatusTextView?.text = "Disconnected"
                    connectionStatusTextView?.text = "Discovering servers..."
                }
            }
        }
    }

    private fun setupServiceDiscovery() {
        // Initialize search progress handler and runnable
        searchProgressHandler = Handler(Looper.getMainLooper())
        searchProgressRunnable = object : Runnable {
            override fun run() {
                val elapsedSeconds = ((System.currentTimeMillis() - searchStartTime) / 1000).toInt()
                serverIpTextView?.text = "Searching ${elapsedSeconds}s..."
                searchProgressHandler?.postDelayed(this, 1000)
            }
        }
        
        nsdHelper = NsdHelper(requireContext(), object : NsdHelper.ServiceDiscoveryCallback {
            override fun onServiceFound(serviceName: String?, host: String?, port: Int) {
                // Check if fragment is attached to activity before accessing it
                if (!isAdded) {
                    return
                }
                
                requireActivity().runOnUiThread { 
                    // Stop search progress updates
                    searchProgressHandler?.removeCallbacks(searchProgressRunnable!!)
                    
                    // Add to discovered servers list (handle null values)
                    val safeServiceName = serviceName ?: "Unknown Service"
                    val safeHost = host ?: "Unknown Host"
                    val server = DiscoveredServer(safeServiceName, safeHost, port)
                    if (!discoveredServers.any { it.host == safeHost && it.port == port }) {
                        discoveredServers.add(server)
                        updateServerListUI()
                        
                        // Update the viewModel with the unified serverInfo object
                        // This ensures all server properties update together atomically
                        val serverInfo = mutableMapOf<String?, Any?>()
                        serverInfo["name"] = safeServiceName
                        serverInfo["ip"] = safeHost
                        serverInfo["port"] = port
                        serverInfo["connected"] = false // Not connected yet, just discovered
                        
                        viewModel?.setServerInfo(serverInfo)
                        
                        // Update UI directly for immediate feedback
                    serverIpTextView?.text = safeHost
                    serverPortTextView?.text = port.toString()
                    connectionStatusTextView?.text = "Server discovered"
                    }
                }
            }

            override fun onServiceLost(serviceName: String?) {
                // Check if fragment is attached to activity before accessing it
                if (!isAdded) {
                    return
                }
                
                requireActivity().runOnUiThread {
                    // Remove from list (handle null service name)
                    val safeServiceName = serviceName ?: return@runOnUiThread
                    discoveredServers.removeIf { it.serviceName == safeServiceName }
                    updateServerListUI()
                }
            }

            override fun onDiscoveryStarted() {
                // Check if fragment is attached to activity before accessing it
                if (!isAdded) {
                    return
                }
                
                requireActivity().runOnUiThread {
                    connectionStatusTextView?.text = "Discovering servers..."
                    // Start search timer
                    searchStartTime = System.currentTimeMillis()
                    serverIpTextView?.text = "Searching 0s..."
                    serverStatusTextView?.text = "Disconnected"
                    serverPortTextView?.text = "-"
                    // Start updating search progress every second
                    searchProgressHandler?.post(searchProgressRunnable!!)
                }
            }

            override fun onDiscoveryFailed() {
                // Check if fragment is attached to activity before accessing it
                if (!isAdded) {
                    return
                }
                
                requireActivity().runOnUiThread {
                    // Stop search progress updates
                    searchProgressHandler?.removeCallbacks(searchProgressRunnable!!)
                    
                    connectionStatusTextView?.text = "Discovery failed"
                    serverIpTextView?.text = "Discovery failed"
                    serverPortTextView?.text = "-"
                    serverStatusTextView?.text = "Disconnected"
                }
                
                // Implement exponential backoff retry
                if (discoveryRetryCount < maxDiscoveryRetries) {
                    discoveryRetryCount++
                    val delayMs = initialRetryDelayMs * Math.pow(2.0, (discoveryRetryCount - 1).toDouble()).toLong()
                    
                    // Check again if fragment is still attached before scheduling retry
                    if (!isAdded) {
                        return
                    }
                    
                    requireActivity().runOnUiThread {
                        connectionStatusTextView?.text = "Retrying discovery in ${delayMs/1000}s..."
                    }
                    
                    // Schedule retry
                    Handler(Looper.getMainLooper()).postDelayed({
                        // Check if fragment is still attached before executing retry
                        if (!isAdded) {
                            return@postDelayed
                        }
                        
                        requireActivity().runOnUiThread {
                            connectionStatusTextView?.text = "Retrying discovery..."
                        }
                        // Restart discovery
                        nsdHelper?.tearDown()
                        nsdHelper?.initialize()
                        nsdHelper?.discoverServices()
                    }, delayMs)
                }
            }
        })

        nsdHelper?.initialize()
        nsdHelper?.discoverServices()
    }

    private fun updateServerListUI() {
        val serverItems = discoveredServers.map { server ->
            mutableMapOf(
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
        deviceInfoTextView?.text = deviceInfoManager?.deviceInfo
        updateConnectionStatus("Discovering servers...")
        // Show "Searching..." for server IP from the start
        serverIpTextView?.text = "Searching..."
        serverPortTextView?.text = "-"
        serverStatusTextView?.text = "Disconnected"
        // Update WiFi information
        updateWiFiInfo()
    }

    /**
     * Update WiFi information including name, IP, and status
     */
    private fun updateWiFiInfo() {
        val wifiInfo = getCurrentWiFiInfo()
        val localIp = getLocalIpAddress()
        
        // Update WiFi name
        wifiNameTextView?.text = wifiInfo.first ?: "Not connected"
        
        // Update WiFi IP
        wifiIpTextView?.text = wifiInfo.second ?: "-"
        
        // Update app local IP
        appLocalIpTextView?.text = localIp ?: "-"
        
        // Update WiFi status
        if (wifiInfo.first == null) {
            wifiStatusTextView?.text = "Not connected to any WiFi"
            wifiStatusTextView?.setTextColor(resources.getColor(android.R.color.holo_orange_dark))
            updateConnectionStatus("Please connect to WiFi to discover servers")
        } else {
            wifiStatusTextView?.text = "Connected to WiFi"
            wifiStatusTextView?.setTextColor(resources.getColor(android.R.color.holo_green_dark))
        }
    }

    /**
     * Get current WiFi information (name, IP)
     */
    private fun getCurrentWiFiInfo(): Pair<String?, String?> {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
            
            if (networkCapabilities != null && networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                // Connected to WiFi
                val wifiManager = requireContext().applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val wifiInfo = wifiManager.connectionInfo
                val wifiName = wifiInfo.ssid?.removeSurrounding("\"")
                val wifiIp = intToIp(wifiInfo.ipAddress)
                return Pair(wifiName, wifiIp)
            }
        } else {
            // For older Android versions
            val wifiManager = requireContext().applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo = wifiManager.connectionInfo
            if (wifiInfo.networkId != -1) {
                val wifiName = wifiInfo.ssid?.removeSurrounding("\"")
                val wifiIp = intToIp(wifiInfo.ipAddress)
                return Pair(wifiName, wifiIp)
            }
        }
        
        return Pair(null, null)
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

    private fun updateConnectionStatus(status: String) {
        connectionStatusTextView?.text = "Connection Status: $status"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroy() {
        super.onDestroy()
        nsdHelper?.tearDown()
        
        // Clean up search progress handler to avoid memory leaks
        searchProgressHandler?.removeCallbacks(searchProgressRunnable!!)
        searchProgressHandler = null
        searchProgressRunnable = null
    }
}