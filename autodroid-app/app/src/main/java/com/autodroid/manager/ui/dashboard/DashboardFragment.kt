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
import com.autodroid.manager.apk.ApkScannerManager
// import com.autodroid.manager.model.DiscoveredServer
import com.autodroid.manager.model.Server
import com.autodroid.manager.ui.BaseFragment
import com.autodroid.manager.ui.adapters.BaseItemAdapter
import com.autodroid.manager.service.DiscoveryStatusManager
import com.autodroid.manager.AppViewModel
import com.autodroid.manager.ui.adapters.DashboardAdapter
import com.autodroid.manager.model.DashboardItem
import com.autodroid.manager.utils.NetworkUtils
import com.autodroid.manager.utils.DeviceUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    private var apkScannerManager: ApkScannerManager? = null

    private val dashboardItems = mutableListOf<DashboardItem>()
    
    // Item managers for modular architecture
    private lateinit var serverItemManager: ServerItemManager
    private lateinit var deviceInfoItemManager: DeviceItemManager
    private lateinit var wifiItemManager: WiFiItemManager
    private lateinit var apkScannerItemManager: ApkScannerItemManager
    private lateinit var apkItemManager: ApkItemManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize managers
        workflowManager = WorkflowManager(requireContext(), appViewModel)
        apkScannerManager = ApkScannerManager(requireContext())

        // Initialize Activity Result API launchers
        requestCameraPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            serverItemManager.handleCameraPermissionResult(isGranted)
        }
        
        startQRCodeScannerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            serverItemManager.handleQrCodeScanResult(result)
        }

        // NetworkService is now auto-started in MyApplication
    }

    override fun getLayoutId(): Int = R.layout.fragment_dashboard

    override fun initViews(view: View) {
        // Initialize RecyclerView for hybrid dashboard items
        dashboardRecyclerView = view.findViewById(R.id.dashboard_recycler_view)
        
        // Set up RecyclerView with LinearLayoutManager
        dashboardRecyclerView?.layoutManager = LinearLayoutManager(context)
        
        // Initialize item managers FIRST
        serverItemManager = ServerItemManager(
            requireContext(),
            viewLifecycleOwner,
            appViewModel,
            ::onServerConnectionItemUpdate
        )
        
        deviceInfoItemManager = DeviceItemManager(
            requireContext(),
            viewLifecycleOwner,
            appViewModel,
            ::onDeviceInfoItemUpdate
        )
        
        wifiItemManager = WiFiItemManager(
            requireContext(),
            viewLifecycleOwner,
            appViewModel,
            ::onWiFiInfoItemUpdate
        )
        
        apkScannerItemManager = ApkScannerItemManager(
            requireContext(),
            viewLifecycleOwner,
            appViewModel,
            ::onApkScannerItemUpdate
        )
        
        apkItemManager = ApkItemManager(
            requireContext(),
            viewLifecycleOwner,
            appViewModel,
            ::onApkInfoItemUpdate
        )
        
        // Initialize DashboardAdapter
        dashboardAdapter = DashboardAdapter()
        dashboardAdapter?.setOnItemClickListener(object : DashboardAdapter.OnItemClickListener {
            override fun onStartMdnsClick() {
                serverItemManager.handleStartMdnsDiscovery()
            }
            
            override fun onScanQrCodeClick() {
                serverItemManager.checkCameraPermissionAndScanQR()
            }
            
            override fun onManualInputClick() {
                // 委托给ServerItemManager处理手动输入
                serverItemManager.handleManualInputClick()
            }
            

            
            override fun onScanApksClick() {
                android.util.Log.d("DashboardFragment", "onScanApksClick被调用")
                apkScannerItemManager?.handleScanApksClick()
            }
            
            override fun onApkItemClick(apkInfo: com.autodroid.manager.model.Apk) {
                // Navigate to detail fragment with APK information
                navigateToApkDetail(apkInfo)
            }
        })
        
        dashboardRecyclerView?.adapter = dashboardAdapter
         
         // Initialize QR code scanning functionality
         serverItemManager.initializeQRCodeScanning(requestCameraPermissionLauncher, startQRCodeScannerLauncher)
         
         // Start item managers
        serverItemManager.initialize()
        deviceInfoItemManager.initialize()
        wifiItemManager.initialize()
        apkScannerItemManager.initialize()
        apkItemManager.initialize()

        // Debug: Check if NetworkService is running
        Log.d("DashboardFragment", "Initializing views - checking NetworkService status")
        // Note: QR code fallback management has been moved to AppViewModel
        // Log.d("DashboardFragment", "QR code fallback flag: ${DiscoveryStatusManager.isQrCodeChosenAsFallback()}") - REMOVED in architecture refactoring

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

    override fun setupObservers() {
        // 设置观察者，监听AppViewModel中的数据变化
        // 由于DashboardFragment使用ItemManager架构，主要的观察逻辑已经在各个ItemManager中实现
        // 这里可以添加全局性的观察逻辑，如果需要的话
    }

    /**
     * Simplified callback for server connection item updates
     */
    private fun onServerConnectionItemUpdate(item: DashboardItem.ServerItem) {
        Log.d("DashboardFragment", "onServerConnectionItemUpdate called: status=${item.status}, serverStatus=${item.serverStatus}")
        
        try {
            val result = serverItemManager.handleServerConnectionUpdate(item, dashboardItems, dashboardAdapter)
            Log.d("DashboardFragment", "handleServerConnectionUpdate result: $result")
        } catch (e: Exception) {
            Log.e("DashboardFragment", "Error in handleServerConnectionUpdate: ${e.message}", e)
        }
    }
    
    /**
     * Simplified callback for device info item updates
     */
    private fun onDeviceInfoItemUpdate(item: DashboardItem) {
        deviceInfoItemManager.handleListUpdate(item, dashboardItems, dashboardAdapter)
    }
    
    /**
     * Simplified callback for WiFi info item updates
     */
    private fun onWiFiInfoItemUpdate(item: DashboardItem) {
        wifiItemManager.handleListUpdate(item, dashboardItems, dashboardAdapter)
    }
    
    /**
     * Simplified callback for APK scanner item updates
     */
    private fun onApkScannerItemUpdate(item: DashboardItem) {
        apkScannerItemManager.handleListUpdate(item, dashboardItems, dashboardAdapter)
    }

    private fun updateUI() {
        // Clear existing items
        dashboardItems.clear()
        
        // Add initial items in the correct order:
        // 1. Connection Status - Use ServerItemManager to get the current server item
        dashboardItems.add(serverItemManager.getCurrentItem())
        
        // 3. Wifi Information
        dashboardItems.add(DashboardItem.WiFiItem(
            ssid = "Unknown",
            bssid = "Unknown",
            signalStrength = 0,
            frequency = 0,
            ipAddress = "Unknown",
            linkSpeed = 0,
            isConnected = false
        ))
        
        // 4. Device Information
        dashboardItems.add(DashboardItem.DeviceItem(
            udid = "Unknown",
            userId = "user001",
            name = "Unknown",
            platform = "Android",
            deviceModel = "Unknown",
            deviceStatus = "在线",
            connectionTime = "Never"
        ))
        
        // 5. APK scanner
        dashboardItems.add(DashboardItem.ApkScannerItem(
            scanStatus = "SCAN INSTALLED APKS"
        ))
        
        // 6. Add empty APK info item as placeholder (will be updated when APKs are scanned)
        dashboardItems.add(DashboardItem.ApkItem(
            apkInfo = com.autodroid.manager.model.Apk(
                packageName = "",
                appName = "",
                version = "",
                versionCode = 0,
                installedTime = 0L,
                isSystem = false,
                iconPath = ""
            )
        ))
        
        // 7. Static Dashboard Items (add any additional static items here if needed)
        
        // Update adapter with new items
        dashboardAdapter?.updateItems(dashboardItems)
        
        // Trigger initial updates from item managers
        // These will update the dashboardItems list with current data
        // Start with server connection to ensure it's immediately visible
        serverItemManager.refresh()
        wifiItemManager.refresh()
        deviceInfoItemManager.refresh()
        apkScannerItemManager.refresh()
        
        // Force immediate UI update to show current server state
        forceServerItemUpdate()
    }
    
    /**
     * Force immediate update of server connection item with current state
     */
    private fun forceServerItemUpdate() {
        // Simply trigger a refresh of the server item manager
        // The ServerItemManager will handle the state update internally
        serverItemManager.refresh()
    }

    // updateWiFiInfo method removed - WiFi information updates are handled by WiFiItemManager

    // getDeviceUDID method removed - use DeviceUtils.getDeviceUDID() instead

    // getCurrentWiFiInfo method removed - use NetworkUtils.getCurrentWiFiInfo() instead
    
    // getWifiName method removed - use NetworkUtils.getWifiName() instead
    
    // getIpAddressFromLinkProperties method removed - use NetworkUtils.getIpAddressFromLinkProperties() instead

    // getLocalIpAddress method removed - use NetworkUtils.getLocalIpAddress() instead

    // intToIp method removed - use NetworkUtils.intToIp() instead



    // Server connection related methods removed - handled by ServerConnectionItemManager
    
    // updateConnectionStatus method removed - UI updates are now handled through data model

    /**
     * Simplified callback for APK information item updates
     */
    private fun onApkInfoItemUpdate(item: DashboardItem) {
        apkItemManager.handleListUpdate(item, dashboardItems, dashboardAdapter)
    }
    
    private fun navigateToApkDetail(apkInfo: com.autodroid.manager.model.Apk) {
        try {
            // Create bundle with APK info arguments
            val bundle = Bundle().apply {
                putString("appName", apkInfo.appName)
            putString("packageName", apkInfo.packageName)
            putString("version", apkInfo.version)
            putInt("versionCode", apkInfo.versionCode)
            putLong("installTime", apkInfo.installedTime)
            putBoolean("isSystem", apkInfo.isSystem)
            putString("iconPath", apkInfo.iconPath)
            }
            
            // Navigate to APK detail fragment
            findNavController().navigate(R.id.action_nav_dashboard_to_apkDetailFragment, bundle)
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to APK detail: ${e.message}", e)
            // Fallback: show error message
            android.widget.Toast.makeText(requireContext(), "无法打开APK详情页面", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 处理手动输入按钮点击事件 - 委托给ServerItemManager
     */
    private fun onManualInputClick() {
        serverItemManager.handleManualInputClick()
    }

    /**
     * Refresh the UI - called from MainActivity after login success
     */
    fun refreshUI() {
        Log.d(TAG, "DashboardFragment.refreshUI() called")
        
        // Force update all item managers to refresh their data
        serverItemManager.refresh()
        wifiItemManager.refresh()
        deviceInfoItemManager.refresh()
        apkScannerItemManager.refresh()
        apkItemManager.refresh()
        
        // Update navigation state if needed
        updateUI()
    }

    override fun onDestroy() {
        super.onDestroy()
        // NetworkService now handles its own lifecycle
        // No need to clean up nsdHelper or search progress handler
    }
}