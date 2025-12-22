package com.autodroid.trader.ui.dashboard

import android.os.Bundle
import android.util.Log
import android.view.View
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
import android.view.MotionEvent
import android.view.ViewConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.widget.Toast
import androidx.annotation.RequiresPermission
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
    private lateinit var itemDeviceManager: ItemDeviceManager
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
            itemServerManager.processCameraPermissionResult(isGranted)
        }
        
        startQRCodeScannerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            itemServerManager.processQrCodeScanResult(result)
        }

        // NetworkService is now auto-started in MyApplication
    }

    override fun getLayoutId(): Int = R.layout.fragment_dashboard

    override fun initViews(view: View) {
        Log.d(TAG, "initViews: 开始初始化视图")
        // Initialize RecyclerView for hybrid dashboard items
        dashboardRecyclerView = view.findViewById(R.id.dashboard_recycler_view)
        
        // Find AppBarLayout
        appBarLayout = view.findViewById(R.id.app_bar_layout)
        
        // Initialize touch slop for pull-down detection
        touchSlop = ViewConfiguration.get(requireContext()).scaledTouchSlop
        
        // Set up RecyclerView with LinearLayoutManager
        dashboardRecyclerView?.layoutManager = LinearLayoutManager(context)
        
        // Set up touch listener for pull-down detection
        dashboardRecyclerView?.setOnTouchListener { v, event ->
            val result = handleTouchEvent(event)
            // For accessibility, perform click if this was a simple tap
            if (event.action == MotionEvent.ACTION_UP && !isPullingDown) {
                v.performClick()
            }
            result
        }
        
        // Initialize item managers FIRST
        Log.d(TAG, "开始初始化 itemServerManager")
        itemServerManager = ItemServerManager(
            requireContext(),
            viewLifecycleOwner,
            appViewModel,
            ::onItemServerUpdate
        )
        Log.d(TAG, "itemServerManager 初始化完成")
        
        itemDeviceManager = ItemDeviceManager(
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
            
            override fun onAutoScanClick() {
                itemServerManager.handleAutoScanClick()
            }
            
            override fun onManualInputClick() {
                // 委托给ServerItemManager处理手动输入
                itemServerManager.handleManualSetButtonClick()
            }
            
            @RequiresPermission("android.permission.READ_PHONE_STATE")
            override fun onRegisterDeviceClick() {
                handleRegisterDeviceClick()
            }
            
            @RequiresPermission("android.permission.READ_PHONE_STATE")
            override fun onCheckDeviceClick() {
                handleCheckDeviceClick()
            }
        })
        
        dashboardRecyclerView?.adapter = dashboardAdapter
         
         // Initialize QR code scanning functionality
         itemServerManager.initializeQRCodeScanning(requestCameraPermissionLauncher, startQRCodeScannerLauncher)
         
         // Start item managers
         Log.d(TAG, "调用 itemServerManager.initialize()")
         itemServerManager.initialize()
         Log.d(TAG, "itemServerManager.initialize() 调用完成")
        itemDeviceManager.initialize()
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
    private fun onItemServerUpdate(item: DashboardItem.ItemServer) {
        Log.d("DashboardFragment", "onItemServerUpdate called: status=${item.status}, serverStatus=${item.serverStatus}")
        
        try {
            val result = itemServerManager.handleItemServerUpdate(item, dashboardItems, dashboardAdapter)
            Log.d("DashboardFragment", "handleServerConnectionUpdate result: $result")
        } catch (e: Exception) {
            Log.e("DashboardFragment", "Error in handleServerConnectionUpdate: ${e.message}", e)
        }
    }
    
    /**
     * Simplified callback for device info item updates
     */
    private fun onDeviceInfoItemUpdate(item: DashboardItem) {
        itemDeviceManager.handleListUpdate(item, dashboardItems, dashboardAdapter)
    }
    
    /**
     * Simplified callback for WiFi info item updates
     */
    private fun onWiFiInfoItemUpdate(item: DashboardItem) {
        wifiItemManager.handleListUpdate(item, dashboardItems, dashboardAdapter)
    }
    
    /**
     * Handle device registration button click
     */
    @androidx.annotation.RequiresPermission("android.permission.READ_PHONE_STATE")
    private fun handleRegisterDeviceClick() {
        CoroutineScope(Dispatchers.IO).launch  {
            try {
                // AppViewModel会在应用启动时初始化，直接使用DeviceManager注册设备到服务器
                appViewModel.deviceManager.registerLocalDeviceWithServer()
            } catch (e: Exception) {
                Log.e(TAG, "Error during device registration: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "设备注册出错: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    /**
     * Handle  check button click
     */
    @androidx.annotation.RequiresPermission("android.permission.READ_PHONE_STATE")
    private fun handleCheckDeviceClick() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 请服务器检查设备调试权限、安装app等情况
               appViewModel.deviceManager.checkLocalDeviceWithServer()
            } catch (e: Exception) {
                Log.e(TAG, "Error during device check: ${e.message}", e)
            }
        }
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
            serialNo = "Unknown",
            userId = "user001",
            name = "Unknown",
            platform = "Android",
            deviceModel = "Unknown",
            deviceStatus = "在线",
            latestRegisteredTime = "Never",
            updatedAt = "Never"
        ))
        
        // 5. Port Range Settings
        // 从SharedPreferences读取端口范围
        val prefs = requireContext().getSharedPreferences("server_scan_settings", 0)
        val portStart = prefs.getInt("port_start", 8000)
        val portEnd = prefs.getInt("port_end", 8080)
        
        dashboardItems.add(DashboardItem.ItemPortRange(
            portStart = portStart,
            portEnd = portEnd
        ))
        
        // Update adapter with new items
        dashboardAdapter?.updateItems(dashboardItems)
        
        // Trigger initial updates from item managers
        // These will update the dashboardItems list with current data
        // Start with server connection to ensure it's immediately visible
        itemServerManager.refresh()
        wifiItemManager.refresh()
        itemDeviceManager.refresh()
        
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

    /**
     * 处理手动输入按钮点击事件 - 委托给ServerItemManager
     */
    private fun onManualInputClick() {
        itemServerManager.handleManualSetButtonClick()
    }

    /**
     * Refresh the UI - called from MainActivity after login success
     */
    fun refreshUI() {
        Log.d(TAG, "DashboardFragment.refreshUI() called")
        
        // Force update all item managers to refresh their data
        itemServerManager.refresh()
        wifiItemManager.refresh()
        itemDeviceManager.refresh()
        
        // Update navigation state if needed
        updateUI()
    }

    override fun onDestroy() {
        super.onDestroy()
        // NetworkService now handles its own lifecycle

    }
}