package com.autodroid.controller

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.autodroid.controller.adapter.ControlAdapter
import com.autodroid.controller.databinding.ActivityMainBinding
import com.autodroid.controller.model.AppiumStatus
import com.autodroid.controller.model.ControlItem
import com.autodroid.controller.service.AutoDroidControllerService
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: ControlAdapter
    private var controlItems: MutableList<ControlItem> = mutableListOf()
    
    private val appiumStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                AutoDroidControllerService.ACTION_APPIUM_STATUS_UPDATE -> {
                    val statusName = intent.getStringExtra(AutoDroidControllerService.EXTRA_APPIUM_STATUS)
                    val status = AppiumStatus.valueOf(statusName ?: AppiumStatus.STOPPED.name)
                    updateAppiumStatusInUI("appium_control", status)
                }
                AutoDroidControllerService.ACTION_SERVICE_STATUS_UPDATE -> {
                    val statusName = intent.getStringExtra(AutoDroidControllerService.EXTRA_SERVICE_STATUS)
                    val status = com.autodroid.controller.model.ServiceStatus.valueOf(statusName ?: com.autodroid.controller.model.ServiceStatus.STOPPED.name)
                    val lastCheckTime = intent.getStringExtra(AutoDroidControllerService.EXTRA_LAST_CHECK_TIME)
                    updateServiceStatusInUI("service_control", status, lastCheckTime)
                }
            }
        }
    }
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            startAutomationService()
        } else {
            Toast.makeText(this, "需要授予权限才能运行自动化服务", Toast.LENGTH_LONG).show()
        }
    }
    

    
    private fun setupUI() {
        controlItems = mutableListOf(
            ControlItem.HeaderItem("Appium UIA2 Server 控制器"),
            ControlItem.ServiceControlItem(
                id = "service_control",
                title = "自动化服务控制",
                description = "启动和停止自动化服务",
                startAction = "start_service",
                stopAction = "stop_service",
                status = com.autodroid.controller.model.ServiceStatus.STOPPED
            ),
            ControlItem.AppiumControlItem(
                id = "appium_control",
                title = "Appium UIA2 Server 控制",
                description = "启动和停止 Appium 服务器",
                startAction = "start_appium",
                stopAction = "stop_appium",
                status = AppiumStatus.STOPPED,
                lastCheckTime = null
            ),
            ControlItem.TestControlItem(
                id = "test_control",
                title = "测试任务",
                description = "执行测试任务",
                action = "test_task"
            ),
            ControlItem.SettingsItem(
                id = "settings",
                title = "设置",
                description = "应用设置和权限管理",
                action = "settings"
            )
        )
        
        adapter = ControlAdapter(
            onServiceStartClick = { serviceControl ->
                when (serviceControl.id) {
                    "service_control" -> {
                        // 启动自动化服务
                        handleServiceStart(serviceControl)
                    }
                }
            },
            onServiceStopClick = { serviceControl ->
                when (serviceControl.id) {
                    "service_control" -> {
                        // 停止自动化服务
                        handleServiceStop(serviceControl)
                    }
                }
            },
            onServiceCheckStatusClick = { serviceControl ->
                when (serviceControl.id) {
                    "service_control" -> {
                        // 检查服务状态
                        handleServiceCheckStatus(serviceControl)
                    }
                }
            },
            onTestControlClick = { testControl ->
                when (testControl.id) {
                    "test_control" -> executeTestTask()
                }
            },
            onTestMingYongBaoClick = { testControl ->
                when (testControl.id) {
                    "test_control" -> testMingYongBao()
                }
            },
            onTestGetPageXmlClick = { testControl ->
                when (testControl.id) {
                    "test_control" -> testGetPageXml()
                }
            },
            onSettingsClick = { settings ->
                when (settings.id) {
                    "settings" -> openAccessibilitySettings()
                }
            },
            onCheckAppiumStatusClick = { appiumControl ->
                when (appiumControl.id) {
                    "appium_control" -> {
                        // 只检查Appium服务器状态，不进行控制操作
                        checkAppiumServerStatus()
                    }
                }
            },
            onAppiumAppInfoClick = { appiumControl ->
                openAppiumAppInfo()
            },
            onShareToWechatClick = { appiumControl ->
                shareToWechat()
            }
        )
        
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        adapter.setData(controlItems)
    }
    
    private fun checkAndRequestPermissions() {
        val requiredPermissions = mutableListOf<String>()
        
        // 网络权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            requiredPermissions.add(Manifest.permission.INTERNET)
        }
        
        // Android 13+ 通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requiredPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        if (requiredPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(requiredPermissions.toTypedArray())
        } else {
            // 检查前台服务权限
            if (checkAndHandlePermissions()) {
                startAutomationService()
            } else {
                // 注意：checkAndHandlePermissions() 已经会显示错误消息并打开设置页面
                // 这里不需要额外的操作
            }
        }
    }
    
    private fun checkForegroundServicePermission(): Boolean {
        return try {
            // 对于 Android 14+ (API 34+)，FOREGROUND_SERVICE 是一个普通权限，会在安装时自动授予
            // 但仍需要检查通知权限以确保前台服务可以正常工作
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Android 14+ 检查通知权限
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else
                // Android 8.0+ (API 26+): FOREGROUND_SERVICE 是普通权限，在安装时自动授予
                // 但在 Android 8.0-12 上，我们只需要确保应用有前台服务权限即可
                // 实际上，在 Android 8.0-12 上，FOREGROUND_SERVICE 权限总是被授予的
                true
        } catch (e: Exception) {
            Log.e("MainActivity", "Error checking foreground service permission", e)
            false
        }
    }
    
    /**
     * 检查并处理权限，如果权限不足则跳转到设置页面
     * @return true表示权限充足，false表示权限不足
     */
    private fun checkAndHandlePermissions(): Boolean {
        // 对于 Android 14+ (API 34+)，FOREGROUND_SERVICE 是一个普通权限，会在安装时自动授予
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+ 只需检查通知权限
            val postNotificationsGranted = ContextCompat.checkSelfPermission(
                this, 
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            
            if (!postNotificationsGranted) {
                Toast.makeText(this, "缺少通知权限，请在设置中启用", Toast.LENGTH_LONG).show()
                openAppSettings()
                return false
            }
            return true
        } else {
            // 对于 Android 8.0-13，前台服务权限是自动授予的
            // 我们只需要确保应用有前台服务权限即可
            if (checkForegroundServicePermission()) {
                return true
            } else {
                // 在 Android 8.0-13 上，如果前台服务权限检查失败，可能是其他问题
                // 显示更具体的错误消息
                val errorMsg =
                    "前台服务权限检查失败，请确保应用已正确安装并具有前台服务权限"
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
                openAppSettings()
                return false
            }
        }
    }
    

    
    private fun openAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, getString(R.string.toast_cannot_open_accessibility_settings), Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, getString(R.string.toast_cannot_open_app_settings), Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun startAppiumServer() {
        // 先检查权限
        if (!checkAndHandlePermissions()) {
            return
        }
        
        // 立即更新UI状态为"启动中"
        updateAppiumStatusInUI("appium_control", AppiumStatus.STARTING)
        
        try {
            val intent = Intent(this, AutoDroidControllerService::class.java).apply {
                action = AutoDroidControllerService.ACTION_START_APPIUM_SERVER
            }

            startForegroundService(intent)

            Toast.makeText(this, "正在启动Appium UIA2 Server...", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            val errorMsg = when {
                e is SecurityException -> {
                    // 根据Android版本提供更准确的错误消息
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        "请检查通知权限是否已启用"
                    } else {
                        "缺少前台服务权限，请检查权限设置"
                    }
                }
                e.message?.contains("ForegroundService") == true -> "前台服务启动失败，请检查通知权限"
                else -> "启动Appium服务器失败: ${e.message}"
            }
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
            Log.e("MainActivity", "Failed to start Appium server", e)
            
            // 启动失败时更新状态为"已停止"
            updateAppiumStatusInUI("appium_control", AppiumStatus.STOPPED)
        }
    }
    
    private fun stopAppiumServer() {
        // 先检查权限
        if (!checkAndHandlePermissions()) {
            return
        }
        
        try {
            val intent = Intent(this, AutoDroidControllerService::class.java).apply {
                action = AutoDroidControllerService.ACTION_STOP_APPIUM_SERVER
            }

            startForegroundService(intent)

            Toast.makeText(this, "正在停止Appium UIA2 Server...", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            val errorMsg = when {
                e is SecurityException -> {
                    // 根据Android版本提供更准确的错误消息
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        "请检查通知权限是否已启用"
                    } else {
                        "缺少前台服务权限，请检查权限设置"
                    }
                }
                e.message?.contains("ForegroundService") == true -> "前台服务启动失败，请检查通知权限"
                else -> "停止Appium服务器失败: ${e.message}"
            }
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
            Log.e("MainActivity", "Failed to stop Appium server", e)
        }
    }
    
    private fun checkAppiumServerStatus() {
        try {
            // 更新UI状态为检查中
            updateAppiumStatusInUI("appium_control", AppiumStatus.STARTING)
            
            // 检查Appium服务器进程是否在运行
            val isRunning = checkAppiumProcessRunning()
            
            if (isRunning) {
                // 如果进程在运行，检查HTTP连接
                checkAppiumHttpStatus()
            } else {
                // 更新UI状态为已停止
                updateAppiumStatusInUI("appium_control", AppiumStatus.STOPPED)
                Toast.makeText(this, "Appium UIA2 Server 未运行", Toast.LENGTH_SHORT).show()
            }
            
        } catch (e: Exception) {
            // 更新UI状态为未知
            updateAppiumStatusInUI("appium_control", AppiumStatus.STOPPED)
            Toast.makeText(this, "检查服务器状态失败: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e("MainActivity", "Failed to check Appium server status", e)
        }
    }
    
    private fun checkAppiumProcessRunning(): Boolean {
        try {
            // 直接通过PackageManager检查Appium包是否存在
            packageManager.getPackageInfo("io.appium.uiautomator2.server", 0)
            // 包存在，认为服务可能可用
            return true
        } catch (_: PackageManager.NameNotFoundException) {
            // 包不存在
            return false
        } catch (e: Exception) {
            Log.e("MainActivity", "Error checking Appium process", e)
            return false
        }
    }
    
    private fun checkAppiumHttpStatus() {
        Thread {
            try {
                val client = OkHttpClient.Builder()
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .readTimeout(5, TimeUnit.SECONDS)
                    .build()
                
                val request = Request.Builder()
                    .url("http://127.0.0.1:6790/status")
                    .build()
                
                val response = client.newCall(request).execute()
                
                runOnUiThread {
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        updateAppiumStatusInUI("appium_control", AppiumStatus.RUNNING)
                        Toast.makeText(this, "Appium UIA2 Server 正常运行", Toast.LENGTH_LONG).show()
                        Log.d("MainActivity", "Appium server status: $responseBody")
                    } else {
                        updateAppiumStatusInUI("appium_control", AppiumStatus.STOPPED)
                        Toast.makeText(this, "Appium UIA2 Server 无响应", Toast.LENGTH_LONG).show()
                    }
                }
                
            } catch (e: Exception) {
                runOnUiThread {
                    updateAppiumStatusInUI("appium_control", AppiumStatus.STOPPED)
                    Toast.makeText(this, "无法连接到Appium UIA2 Server", Toast.LENGTH_LONG).show()
                    Log.e("MainActivity", "Error connecting to Appium server", e)
                }
            }
        }.start()
    }
    
    private fun executeTestTask() {
        // 先检查权限
        if (!checkAndHandlePermissions()) {
            return
        }
        
        val testTaskJson = """
        {
            "taskId": "test_${System.currentTimeMillis()}",
            "deviceId": "test_device_${System.currentTimeMillis()}",
            "actions": [
                {
                    "action": "initSession",
                    "params": {
                        "capabilities": {
                            "platformName": "Android",
                            "appium:automationName": "UiAutomator2",
                            "appium:udid": "TDCDU17905004388",
                            "appium:appPackage": "com.tdx.androidCCZQ",
                            "appium:noReset": false,
                            "appium:autoGrantPermissions": true,
                            "appium:skipServerInstallation": true,
                            "appium:remoteAppsCacheLimit": 0,
                            "appium:dontStopAppOnReset": true
                        }
                    }
                },
                {
                    "action": "takeScreenshot"
                },
                {
                    "action": "closeSession"
                }
            ]
        }
        """.trimIndent()
        
        val intent = Intent(this, AutoDroidControllerService::class.java).apply {
            action = AutoDroidControllerService.ACTION_EXECUTE_TASK
            putExtra(AutoDroidControllerService.EXTRA_TASK_JSON, testTaskJson)
        }

        try {
            startForegroundService(intent)
            Toast.makeText(this, getString(R.string.toast_test_task_sent), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            val errorMsg = when {
                e is SecurityException -> {
                    // 根据Android版本提供更准确的错误消息
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        "请检查通知权限是否已启用"
                    } else {
                        "缺少前台服务权限，请检查权限设置"
                    }
                }
                e.message?.contains("ForegroundService") == true -> "前台服务启动失败，请检查通知权限"
                else -> "发送测试任务失败: ${e.message}"
            }
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
            Log.e("MainActivity", "Failed to send test task", e)
        }
    }
    
    private fun handleServiceStart(serviceControl: ControlItem.ServiceControlItem) {
        // 启动服务前检查权限
        if (checkAndHandlePermissions()) {
            // 立即更新UI状态为"启动中"
            updateServiceStatusInUI(serviceControl.id, com.autodroid.controller.model.ServiceStatus.STARTING)
            startAutomationService()
        } else {
            // 权限检查失败，更新状态为已停止
            updateServiceStatusInUI(serviceControl.id, com.autodroid.controller.model.ServiceStatus.STOPPED)
        }
    }
    
    private fun handleServiceStop(serviceControl: ControlItem.ServiceControlItem) {
        // 停止服务
        updateServiceStatusInUI(serviceControl.id, com.autodroid.controller.model.ServiceStatus.STOPPING)
        stopAutomationService()
    }
    
    private fun handleServiceCheckStatus(serviceControl: ControlItem.ServiceControlItem) {
        // 检查服务状态
        checkServiceStatus()
    }
    
    private fun startAutomationService() {
        try {
            val intent = Intent(this, AutoDroidControllerService::class.java).apply {
                action = AutoDroidControllerService.ACTION_START
            }

            startForegroundService(intent)

            Toast.makeText(this, "自动化服务正在启动...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            val errorMsg = when {
                e is SecurityException -> {
                    // 根据Android版本提供更准确的错误消息
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        "请检查通知权限是否已启用"
                    } else {
                        "缺少前台服务权限，请检查权限设置"
                    }
                }
                e.message?.contains("ForegroundService") == true -> "前台服务启动失败，请检查通知权限"
                else -> "启动自动化服务失败: ${e.message}"
            }
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
            Log.e("MainActivity", "Failed to start automation service", e)
        }
    }
    
    private fun stopAutomationService() {
        try {
            val intent = Intent(this, AutoDroidControllerService::class.java).apply {
                action = AutoDroidControllerService.ACTION_STOP
            }
            
            stopService(intent)
            
            Toast.makeText(this, "自动化服务正在停止...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "停止自动化服务失败: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e("MainActivity", "Failed to stop automation service", e)
        }
    }
    
    private fun checkServiceStatus() {
        val isServiceRunning = isServiceRunning(AutoDroidControllerService::class.java)
        
        // 生成当前时间戳
        val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        
        if (isServiceRunning) {
            Toast.makeText(this, "自动化服务正在运行", Toast.LENGTH_SHORT).show()
            updateServiceStatusInUI("service_control", com.autodroid.controller.model.ServiceStatus.RUNNING, currentTime)
        } else {
            Toast.makeText(this, "自动化服务已停止", Toast.LENGTH_SHORT).show()
            updateServiceStatusInUI("service_control", com.autodroid.controller.model.ServiceStatus.STOPPED, currentTime)
        }
    }
    
    private fun updateServiceStatusInUI(serviceId: String, status: com.autodroid.controller.model.ServiceStatus, lastCheckTime: String? = null) {
        val serviceIndex = controlItems.indexOfFirst { 
            it is ControlItem.ServiceControlItem && it.id == serviceId 
        }
        
        if (serviceIndex != -1) {
            val currentItem = controlItems[serviceIndex] as ControlItem.ServiceControlItem
            controlItems[serviceIndex] = currentItem.copy(
                status = status,
                lastCheckTime = lastCheckTime ?: currentItem.lastCheckTime
            )
            adapter.setData(controlItems)
        }
    }
    
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        
        // 在onCreate中注册广播接收器，确保应用生命周期内都能接收状态更新
        val filter = IntentFilter().apply {
            addAction(AutoDroidControllerService.ACTION_APPIUM_STATUS_UPDATE)
            addAction(AutoDroidControllerService.ACTION_SERVICE_STATUS_UPDATE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+ (API 34) 需要指定导出标志
            registerReceiver(appiumStatusReceiver, filter, RECEIVER_EXPORTED)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13 (API 33) 使用兼容性方法
            registerReceiver(appiumStatusReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(appiumStatusReceiver, filter)
        }
        
        checkAndRequestPermissions()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // 在onDestroy中注销广播接收器
        try {
            unregisterReceiver(appiumStatusReceiver)
        } catch (e: Exception) {
            // 忽略注销异常，可能接收器未注册
            Log.w("MainActivity", "Error unregistering receiver: ${e.message}")
        }
    }
    
    override fun onResume() {
        super.onResume()
        updateServiceStatus()
        
        // 检查当前服务器状态
        checkCurrentAppiumStatus()
    }
    
    private fun updateServiceStatus() {
        // 检查服务是否正在运行
        val isServiceRunning = isServiceRunning(AutoDroidControllerService::class.java)
        
        // 生成当前时间戳
        val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        
        // 根据服务运行状态更新UI
        if (isServiceRunning) {
            updateServiceStatusInUI("service_control", com.autodroid.controller.model.ServiceStatus.RUNNING, currentTime)
        } else {
            updateServiceStatusInUI("service_control", com.autodroid.controller.model.ServiceStatus.STOPPED, currentTime)
        }
    }
    
    private fun updateAppiumStatusInUI(appiumId: String, status: AppiumStatus) {
        Log.d("MainActivity", "Updating Appium status in UI: $status")
        
        // 更新控制项中的状态和最后检查时间
        val appiumControlIndex = controlItems.indexOfFirst { 
            it is ControlItem.AppiumControlItem && it.id == appiumId 
        }
        
        if (appiumControlIndex != -1) {
            val currentItem = controlItems[appiumControlIndex] as ControlItem.AppiumControlItem
            val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            controlItems[appiumControlIndex] = currentItem.copy(
                status = status,
                lastCheckTime = currentTime
            )
            adapter.setData(controlItems)
            
            // 显示状态更新提示
            when (status) {
                AppiumStatus.RUNNING -> Toast.makeText(this, "Appium服务器已启动", Toast.LENGTH_SHORT).show()
                AppiumStatus.STOPPED -> {
                    Toast.makeText(this, "Appium服务器已停止", Toast.LENGTH_SHORT).show()
                    // 如果服务器启动失败，显示用户指导
                    showAppiumStartupGuide()
                }
                AppiumStatus.STARTING -> Toast.makeText(this, "Appium服务器正在启动...", Toast.LENGTH_SHORT).show()
                AppiumStatus.STOPPING -> Toast.makeText(this, "Appium服务器正在停止...", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showAppiumStartupGuide() {
        val guideMessage = """
            Appium UIA2服务器启动失败！
            
            Appium 7.06版本的Settings应用没有"Start Server"按钮。
            请使用以下ADB命令启动服务器：
            
            ⚡ ADB命令启动（唯一方法）
            1. 确保设备已连接电脑并启用USB调试
            2. 执行以下ADB命令：
               adb shell am start-foreground-service -n io.appium.uiautomator2.server/.Service
            3. 等待服务器启动（约5-10秒）
            4. 返回此应用查看状态
            
            💡 如果ADB命令失败：
            - 检查USB调试是否已授权
            - 尝试重启ADB服务：adb kill-server && adb start-server
            - 确保设备已正确连接
            
            注意：Appium 7.06版本中，UIA2 Server需要通过ADB命令启动
        """.trimIndent()
        
        AlertDialog.Builder(this)
            .setTitle("Appium服务器启动指导")
            .setMessage(guideMessage)
            .setPositiveButton("复制ADB命令") { _, _ ->
                copyAdbCommandToClipboard()
            }
            .setNeutralButton("打开Appium Settings") { _, _ ->
                openAppiumSettings()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun openAppiumAppInfo() {
        try {
            // 创建Intent来打开应用信息界面
            val intent = Intent()
            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            intent.data = Uri.parse("package:io.appium.uiautomator2.server")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            
            startActivity(intent)
        } catch (e: Exception) {
            // 如果无法打开应用信息界面，显示错误提示
            Toast.makeText(this, "无法打开Appium应用信息界面", Toast.LENGTH_SHORT).show()
            Log.e("MainActivity", "Failed to open Appium app info", e)
        }
    }
    
    private fun openAppiumSettings() {
        try {
            val intent = packageManager.getLaunchIntentForPackage("io.appium.settings")
            if (intent != null) {
                startActivity(intent)
            } else {
                Toast.makeText(this, "未找到Appium Settings应用", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开Appium Settings: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun copyAdbCommandToClipboard() {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("ADB启动命令", "adb shell am start-foreground-service -n io.appium.uiautomator2.server/.Service")
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "ADB命令已复制到剪贴板", Toast.LENGTH_LONG).show()
    }
    
    private fun checkCurrentAppiumStatus() {
        // 在后台线程中检查当前服务器状态
        Thread {
            try {
                val client = OkHttpClient.Builder()
                    .connectTimeout(3, TimeUnit.SECONDS)
                    .readTimeout(3, TimeUnit.SECONDS)
                    .build()
                
                val request = Request.Builder()
                    .url("http://127.0.0.1:6790/status")
                    .build()
                
                val response = client.newCall(request).execute()
                
                runOnUiThread {
                    if (response.isSuccessful) {
                        updateAppiumStatusInUI("appium_control", AppiumStatus.RUNNING)
                    } else {
                        updateAppiumStatusInUI("appium_control", AppiumStatus.STOPPED)
                    }
                }

            } catch (_: Exception) {
                runOnUiThread {
                    updateAppiumStatusInUI("appium_control", AppiumStatus.STOPPED)
                }
            }
        }.start()
    }
    
    @SuppressLint("ObsoleteSdkInt")
    @Suppress("DEPRECATION")
    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
        
        // 使用更现代的方法检查服务运行状态
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Android 8.0+ 使用 getRunningServiceProcesses()
            val runningAppProcesses = manager.runningAppProcesses
            return runningAppProcesses?.any { 
                it.processName == serviceClass.`package`?.name 
            } ?: false
        } else {
            // 对于旧版本Android，使用getRunningServices()
            val runningServices = manager.getRunningServices(Integer.MAX_VALUE)
            return runningServices.any { it.service.className == serviceClass.name }
        }
    }
    
    private fun shareToWechat() {
        try {
            // 创建分享内容 - Appium UIA2 Server启动和停止命令
            val shareText = """
🚀 Appium UIA2 Server 启动/停止命令

📱 启动服务器：
adb shell am instrument -w io.appium.uiautomator2.server.test/androidx.test.runner.AndroidJUnitRunner

🛑 停止服务器：
adb shell am force-stop io.appium.uiautomator2.server

💡 说明：
• 使用 am instrument 命令通过测试APK启动主服务器
• io.appium.uiautomator2.server.test 是测试应用
• 主服务器包名：io.appium.uiautomator2.server
• 默认端口：6790

🔧 验证服务器状态：
adb shell ps | grep appium

#Appium #UIAutomator2 #Android自动化
""".trimIndent()
            
            // 创建分享Intent，使用系统分享选择器
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
                // 不指定特定包名，让用户选择分享应用
            }
            
            // 使用系统分享选择器
            startActivity(Intent.createChooser(shareIntent, "分享Appium命令到..."))
            Toast.makeText(this, "请选择分享应用", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            Toast.makeText(this, "分享失败: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e("MainActivity", "Failed to share to WeChat", e)
        }
    }
    
    private fun testMingYongBao() {
        // 先检查权限
        if (!checkAndHandlePermissions()) {
            return
        }
        
        val testTaskJson = """
        {
            "taskId": "mingyongbao_${System.currentTimeMillis()}",
            "deviceId": "test_device_${System.currentTimeMillis()}",
            "actions": [
                {
                    "action": "initSession",
                    "params": {
                        "capabilities": {
                            "platformName": "Android",
                            "appium:automationName": "UiAutomator2",
                            "appium:udid": "TDCDU17905004388",
                            "appium:appPackage": "com.tdx.androidCCZQ",
                            "appium:appActivity": "com.tdx.Android.TdxAndroidActivity",
                            "appium:noReset": false,
                            "appium:autoGrantPermissions": true,
                            "appium:skipServerInstallation": true,
                            "appium:remoteAppsCacheLimit": 0,
                            "appium:dontStopAppOnReset": true
                        }
                    }
                },
                {
                    "action": "takeScreenshot"
                },
                {
                    "action": "closeSession"
                }
            ]
        }
        """.trimIndent()
        
        try {
            val intent = Intent(this, AutoDroidControllerService::class.java).apply {
                action = AutoDroidControllerService.ACTION_EXECUTE_TASK
                putExtra(AutoDroidControllerService.EXTRA_TASK_JSON, testTaskJson)
            }
            
            startForegroundService(intent)
            Toast.makeText(this, "正在启动明佣宝测试任务...", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            val errorMsg = when {
                e is SecurityException -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        "请检查通知权限是否已启用"
                    } else {
                        "缺少前台服务权限，请检查权限设置"
                    }
                }
                e.message?.contains("ForegroundService") == true -> "前台服务启动失败，请检查通知权限"
                else -> "启动明佣宝测试任务失败: ${e.message}"
            }
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
            Log.e("MainActivity", "Failed to start MingYongBao test task", e)
        }
    }
    
    private fun testGetPageXml() {
        // 先检查权限
        if (!checkAndHandlePermissions()) {
            return
        }
        
        val testTaskJson = """
        {
            "taskId": "getpagexml_${System.currentTimeMillis()}",
            "deviceId": "test_device_${System.currentTimeMillis()}",
            "actions": [
                {
                    "action": "initSession",
                    "params": {
                        "capabilities": {
                            "platformName": "Android",
                            "appium:automationName": "UiAutomator2",
                            "appium:udid": "TDCDU17905004388",
                            "appium:appPackage": "com.tdx.androidCCZQ",
                            "appium:appActivity": "com.tdx.Android.TdxAndroidActivity",
                            "appium:noReset": false,
                            "appium:autoGrantPermissions": true,
                            "appium:skipServerInstallation": true,
                            "appium:remoteAppsCacheLimit": 0,
                            "appium:dontStopAppOnReset": true
                        }
                    }
                },
                {
                    "action": "getPageSource"
                },
                {
                    "action": "closeSession"
                }
            ]
        }
        """.trimIndent()
        
        try {
            val intent = Intent(this, AutoDroidControllerService::class.java).apply {
                action = AutoDroidControllerService.ACTION_EXECUTE_TASK
                putExtra(AutoDroidControllerService.EXTRA_TASK_JSON, testTaskJson)
            }
            
            startForegroundService(intent)
            Toast.makeText(this, "正在获取明佣宝页面XML...", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            val errorMsg = when {
                e is SecurityException -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        "请检查通知权限是否已启用"
                    } else {
                        "缺少前台服务权限，请检查权限设置"
                    }
                }
                e.message?.contains("ForegroundService") == true -> "前台服务启动失败，请检查通知权限"
                else -> "获取页面XML测试任务失败: ${e.message}"
            }
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
            Log.e("MainActivity", "Failed to start get page XML test task", e)
        }
    }
}