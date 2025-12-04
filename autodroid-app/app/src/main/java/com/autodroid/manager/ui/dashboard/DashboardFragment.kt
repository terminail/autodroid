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
// TextView import removed - UI updates are now handled through data model
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
import com.autodroid.manager.managers.WorkflowManager
import com.autodroid.manager.managers.APKScannerManager
import com.autodroid.manager.model.DiscoveredServer
import com.autodroid.manager.ui.BaseFragment
import com.autodroid.manager.ui.adapters.BaseItemAdapter
import com.autodroid.manager.service.DiscoveryStatusManager
import com.autodroid.manager.viewmodel.AppViewModel
import com.autodroid.manager.ui.adapters.DashboardAdapter
import com.autodroid.manager.model.DashboardItem
import com.autodroid.manager.utils.NetworkUtils

class DashboardFragment : BaseFragment() {
    private val TAG = "DashboardFragment"
    
    // QR code scanning request code
    private val QR_CODE_SCAN_REQUEST_CODE = 100
    private val CAMERA_PERMISSION_REQUEST_CODE = 101
    
    // Activity Result API launchers
    private lateinit var requestCameraPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>
    private lateinit var startQRCodeScannerLauncher: androidx.activity.result.ActivityResultLauncher<android.content.Intent>
    
    // RecyclerView for hybrid dashboard items
    private var dashboardRecyclerView: RecyclerView? = null
    private var dashboardAdapter: DashboardAdapter? = null
    
    // Managers
    private var workflowManager: WorkflowManager? = null
    private var apkScannerManager: APKScannerManager? = null

    private val dashboardItems = mutableListOf<DashboardItem>()
    
    // Item managers for modular architecture
    private lateinit var serverConnectionItemManager: ServerConnectionItemManager
    private lateinit var deviceInfoItemManager: DeviceInfoItemManager
    private lateinit var wifiInfoItemManager: WiFiInfoItemManager
    private lateinit var apkInfoItemManager: ApkInfoItemManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[AppViewModel::class.java]

        // Initialize managers
        workflowManager = WorkflowManager(requireContext(), viewModel)
        apkScannerManager = APKScannerManager(requireContext())

        // Initialize Activity Result API launchers
        requestCameraPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            serverConnectionItemManager.handleCameraPermissionResult(isGranted)
        }
        
        startQRCodeScannerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            serverConnectionItemManager.handleQrCodeScanResult(result)
        }

        // NetworkService is now auto-started in MyApplication
    }

    override val layoutId: Int
        get() = R.layout.fragment_dashboard

    override fun initViews(view: View?) {
        // Initialize RecyclerView for hybrid dashboard items
        dashboardRecyclerView = view?.findViewById(R.id.dashboard_recycler_view)
        
        // Set up RecyclerView with LinearLayoutManager
        dashboardRecyclerView?.layoutManager = LinearLayoutManager(context)
        
        // Initialize DashboardAdapter
        dashboardAdapter = DashboardAdapter()
        dashboardAdapter?.setOnItemClickListener(object : DashboardAdapter.OnItemClickListener {
            override fun onScanQrCodeClick() {
                serverConnectionItemManager.checkCameraPermissionAndScanQR()
            }
            
            override fun onScanApksClick() {
                apkInfoItemManager.handleScanApksClick()
            }
        })
        
        dashboardRecyclerView?.adapter = dashboardAdapter
        
        // Initialize ServerConnectionItemManager
         serverConnectionItemManager = ServerConnectionItemManager(
             context = requireContext(),
             lifecycleOwner = viewLifecycleOwner,
             viewModel = viewModel,
             onItemUpdate = { updatedItem ->
                 // Update the dashboard item when the server connection item changes
                 val serverConnectionIndex = dashboardItems.indexOfFirst { it is DashboardItem.ServerConnectionItem }
                 if (serverConnectionIndex != -1) {
                     dashboardItems[serverConnectionIndex] = updatedItem
                     dashboardAdapter?.updateItems(dashboardItems)
                 }
             }
         )
         
         // Initialize DeviceInfoItemManager
         deviceInfoItemManager = DeviceInfoItemManager(
             context = requireContext(),
             lifecycleOwner = viewLifecycleOwner,
             viewModel = viewModel,
             onItemUpdate = { updatedItem ->
                 // Update the dashboard item when the device info item changes
                 val deviceInfoIndex = dashboardItems.indexOfFirst { it is DashboardItem.DeviceInfoItem }
                 if (deviceInfoIndex != -1) {
                     dashboardItems[deviceInfoIndex] = updatedItem
                     dashboardAdapter?.updateItems(dashboardItems)
                 }
             }
         )
         
         // Initialize WiFiInfoItemManager
         wifiInfoItemManager = WiFiInfoItemManager(
             context = requireContext(),
             lifecycleOwner = viewLifecycleOwner,
             viewModel = viewModel,
             onItemUpdate = { updatedItem ->
                 // Update the dashboard item when the WiFi info item changes
                 val wifiInfoIndex = dashboardItems.indexOfFirst { it is DashboardItem.WiFiInfoItem }
                 if (wifiInfoIndex != -1) {
                     dashboardItems[wifiInfoIndex] = updatedItem
                     dashboardAdapter?.updateItems(dashboardItems)
                 }
             }
         )
         
         // Initialize ApkInfoItemManager
         apkInfoItemManager = ApkInfoItemManager(
             context = requireContext(),
             lifecycleOwner = viewLifecycleOwner,
             viewModel = viewModel,
             onItemUpdate = { updatedItem ->
                 // Update the dashboard item when the APK info item changes
                 val apkInfoIndex = dashboardItems.indexOfFirst { it is DashboardItem.ApkInfoItem }
                 if (apkInfoIndex != -1) {
                     dashboardItems[apkInfoIndex] = updatedItem
                     dashboardAdapter?.updateItems(dashboardItems)
                 }
             },
             onScanApksClick = {
                 // Handle APK scan click
                 apkScannerManager?.scanInstalledApks()
             }
         )
         
         // Initialize QR code scanning functionality
         serverConnectionItemManager.initializeQRCodeScanning(requestCameraPermissionLauncher, startQRCodeScannerLauncher)
         
         // Initialize all item managers
         serverConnectionItemManager.initialize()
         deviceInfoItemManager.initialize()
         wifiInfoItemManager.initialize()
         apkInfoItemManager.initialize()

        // Debug: Check if NetworkService is running
        Log.d("DashboardFragment", "Initializing views - checking NetworkService status")
        Log.d("DashboardFragment", "QR code fallback flag: ${DiscoveryStatusManager.isQrCodeChosenAsFallback()}")

        // Update UI with initial data
        updateUI()
        
        // NetworkService is now auto-started in MyApplication, no need to start it here
        // DashboardFragment only observes the discovery status and updates UI accordingly
    }






    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // UI initialization is already handled in initViews and updateUI
        // No need for additional refresh calls here
    }

    private fun updateUI() {
        // Clear existing items
        dashboardItems.clear()
        
        // Add initial items - these will be updated by the respective item managers
        dashboardItems.add(DashboardItem.ServerConnectionItem(
            status = "Discovering servers...",
            serverIp = "Searching...",
            serverPort = "-",
            serverStatus = "Disconnected",
            apiEndpoint = "-",
            showQrButton = true
        ))
        
        dashboardItems.add(DashboardItem.WiFiInfoItem(
            ssid = "Unknown",
            bssid = "Unknown",
            signalStrength = 0,
            frequency = 0,
            ipAddress = "Unknown",
            linkSpeed = 0,
            isConnected = false
        ))
        
        dashboardItems.add(DashboardItem.DeviceInfoItem(
            udid = "Unknown",
            userId = "user001",
            name = "Unknown",
            platform = "Android",
            deviceModel = "Unknown",
            deviceStatus = "在线",
            connectionTime = "Never"
        ))
        
        dashboardItems.add(DashboardItem.ApkInfoItem(
            packageName = "",
            version = ""
        ))
        
        // Update adapter with new items
        dashboardAdapter?.updateItems(dashboardItems)
        
        // Trigger initial updates from item managers
        serverConnectionItemManager.refresh()
        wifiInfoItemManager.refresh()
        deviceInfoItemManager.refresh()
        apkInfoItemManager.refresh()
    }

    // updateWiFiInfo method removed - WiFi information updates are handled by WiFiInfoItemManager

    // getDeviceUDID method removed - use DeviceUtils.getDeviceUDID() instead

    // getCurrentWiFiInfo method removed - use NetworkUtils.getCurrentWiFiInfo() instead
    
    // getWifiName method removed - use NetworkUtils.getWifiName() instead
    
    // getIpAddressFromLinkProperties method removed - use NetworkUtils.getIpAddressFromLinkProperties() instead

    // getLocalIpAddress method removed - use NetworkUtils.getLocalIpAddress() instead

    // intToIp method removed - use NetworkUtils.intToIp() instead



    // Server connection related methods removed - handled by ServerConnectionItemManager
    
    // updateConnectionStatus method removed - UI updates are now handled through data model

    override fun onDestroy() {
        super.onDestroy()
        // NetworkService now handles its own lifecycle
        // No need to clean up nsdHelper or search progress handler
    }
}