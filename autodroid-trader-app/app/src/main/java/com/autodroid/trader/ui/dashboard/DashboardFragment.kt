package com.autodroid.trader.ui.dashboard

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
// TextView import removed - UI updates are now handled through data model
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.trader.R
import com.autodroid.trader.managers.TradePlanManager
// import com.autodroid.trader.apk.ApkScannerManager
// import com.autodroid.trader.model.DiscoveredServer
import com.autodroid.trader.ui.BaseFragment
import com.autodroid.trader.ui.dashboard.DashboardAdapter
import com.autodroid.trader.ui.dashboard.DashboardItem
import android.view.MotionEvent
import android.view.ViewConfiguration
import com.google.android.material.appbar.AppBarLayout

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
    private var tradePlanManager: TradePlanManager? = null
    // private var apkScannerManager: ApkScannerManager? = null

    private val dashboardItems = mutableListOf<DashboardItem>()
    
    // Pull-down detection for fragment header
    private var appBarLayout: AppBarLayout? = null
    private var touchStartY = 0f
    private var isPullingDown = false
    private var touchSlop = 0
    
    // Item managers for modular architecture
    private lateinit var itemServerManager: ItemServerManager
    private lateinit var deviceInfoItemManager: ItemDeviceManager
    private lateinit var wifiItemManager: ItemWiFiManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize managers
        tradePlanManager = TradePlanManager(requireContext(), appViewModel)
        // apkScannerManager = ApkScannerManager(requireContext())

        // Initialize Activity Result API launchers
        requestCameraPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            itemServerManager.handleCameraPermissionResult(isGranted)
        }
        
        startQRCodeScannerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            itemServerManager.handleQrCodeScanResult(result)
        }

        // NetworkService is now auto-started in MyApplication
    }

    override fun getLayoutId(): Int = R.layout.fragment_dashboard

    override fun initViews(view: View) {
        // Initialize RecyclerView for hybrid dashboard items
        dashboardRecyclerView = view.findViewById(R.id.dashboard_recycler_view)
        
        // Find AppBarLayout
        appBarLayout = view.findViewById(R.id.app_bar_layout)
        
        // Initialize touch slop for pull-down detection
        touchSlop = ViewConfiguration.get(requireContext()).scaledTouchSlop
        
        // Set up RecyclerView with LinearLayoutManager
        dashboardRecyclerView?.layoutManager = LinearLayoutManager(context)
        
        // Set up touch listener for pull-down detection
        dashboardRecyclerView?.setOnTouchListener { _, event ->
            handleTouchEvent(event)
        }
        
        // Initialize item managers FIRST
        itemServerManager = ItemServerManager(
            requireContext(),
            viewLifecycleOwner,
            appViewModel,
            ::onServerConnectionItemUpdate
        )
        
        deviceInfoItemManager = ItemDeviceManager(
            requireContext(),
            viewLifecycleOwner,
            appViewModel,
            ::onDeviceInfoItemUpdate
        )
        
        wifiItemManager = ItemWiFiManager(
            requireContext(),
            viewLifecycleOwner,
            appViewModel,
            ::onWiFiInfoItemUpdate
        )
        
        // Initialize DashboardAdapter
        dashboardAdapter = DashboardAdapter()
        dashboardAdapter?.setOnItemClickListener(object : DashboardAdapter.OnItemClickListener {
            override fun onScanQrCodeClick() {
                itemServerManager.checkCameraPermissionAndScanQR()
            }
            
            override fun onManualInputClick() {
                // 委托给ServerItemManager处理手动输入
                itemServerManager.handleManualInputClick()
            }
        })
        
        dashboardRecyclerView?.adapter = dashboardAdapter
         
         // Initialize QR code scanning functionality
         itemServerManager.initializeQRCodeScanning(requestCameraPermissionLauncher, startQRCodeScannerLauncher)
         
         // Start item managers
        itemServerManager.initialize()
        deviceInfoItemManager.initialize()
        wifiItemManager.initialize()

        // Debug: Check if NetworkService is running
        Log.d("DashboardFragment", "Initializing views - checking NetworkService status")
        // Note: QR code fallback management has been moved to AppViewModel


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
    private fun onServerConnectionItemUpdate(item: DashboardItem.ItemServer) {
        Log.d("DashboardFragment", "onServerConnectionItemUpdate called: status=${item.status}, serverStatus=${item.serverStatus}")
        
        try {
            val result = itemServerManager.handleServerConnectionUpdate(item, dashboardItems, dashboardAdapter)
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
    
    private fun updateUI() {
        // Clear existing items
        dashboardItems.clear()
        
        // Add initial items in the correct order:
        // 1. Connection Status - Use ItemServerManager to get the current server item
        dashboardItems.add(itemServerManager.getCurrentItem())
        
        // 3. Wifi Information
        dashboardItems.add(DashboardItem.ItemWiFi(
            ssid = "Unknown",
            bssid = "Unknown",
            signalStrength = 0,
            frequency = 0,
            ipAddress = "Unknown",
            linkSpeed = 0,
            isConnected = false
        ))
        
        // 4. Device Information
        dashboardItems.add(DashboardItem.ItemDevice(
            udid = "Unknown",
            userId = "user001",
            name = "Unknown",
            platform = "Android",
            deviceModel = "Unknown",
            deviceStatus = "在线",
            connectionTime = "Never"
        ))
        
        // Update adapter with new items
        dashboardAdapter?.updateItems(dashboardItems)
        
        // Trigger initial updates from item managers
        // These will update the dashboardItems list with current data
        // Start with server connection to ensure it's immediately visible
        itemServerManager.refresh()
        wifiItemManager.refresh()
        deviceInfoItemManager.refresh()
        
        // Force immediate UI update to show current server state
        forceServerItemUpdate()
    }
    
    /**
     * Force immediate update of server connection item with current state
     */
    private fun forceServerItemUpdate() {
        // Simply trigger a refresh of the server item trader
        // The ItemServerManager will handle the state update internally
        itemServerManager.refresh()
    }
    
    /**
     * Handle touch events for pull-down detection
     */
    private fun handleTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchStartY = event.y
                isPullingDown = false
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaY = event.y - touchStartY
                
                // Check if pulling down at the top of the list
                if (deltaY > touchSlop && !isPullingDown) {
                    val layoutManager = dashboardRecyclerView?.layoutManager as? LinearLayoutManager
                    if (layoutManager?.findFirstVisibleItemPosition() == 0) {
                        // At the top of the list and pulling down
                        isPullingDown = true
                        appBarLayout?.visibility = VISIBLE
                        return true
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isPullingDown = false
            }
        }
        return false
    }

    // updateWiFiInfo method removed - WiFi information updates are handled by ItemWiFiManager

    // getDeviceUDID method removed - use DeviceUtils.getDeviceUDID() instead

    // getCurrentWiFiInfo method removed - use NetworkUtils.getCurrentWiFiInfo() instead
    
    // getWifiName method removed - use NetworkUtils.getWifiName() instead
    
    // getIpAddressFromLinkProperties method removed - use NetworkUtils.getIpAddressFromLinkProperties() instead

    // getLocalIpAddress method removed - use NetworkUtils.getLocalIpAddress() instead

    // intToIp method removed - use NetworkUtils.intToIp() instead

    // Server connection related methods removed - handled by ServerConnectionItemManager
    
    // updateConnectionStatus method removed - UI updates are now handled through data model
    
    /**
     * 处理手动输入按钮点击事件 - 委托给ServerItemManager
     */
    private fun onManualInputClick() {
        itemServerManager.handleManualInputClick()
    }

    /**
     * Refresh the UI - called from MainActivity after login success
     */
    fun refreshUI() {
        Log.d(TAG, "DashboardFragment.refreshUI() called")
        
        // Force update all item managers to refresh their data
        itemServerManager.refresh()
        wifiItemManager.refresh()
        deviceInfoItemManager.refresh()
        
        // Update navigation state if needed
        updateUI()
    }

    override fun onDestroy() {
        super.onDestroy()
        // NetworkService now handles its own lifecycle

    }
}