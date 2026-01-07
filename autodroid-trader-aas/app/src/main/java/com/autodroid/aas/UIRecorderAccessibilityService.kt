package com.autodroid.aas

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.autodroid.aas.database.*
import com.autodroid.aas.service.*
import com.autodroid.aas.service.UIEventProcessor
import com.autodroid.aas.ui.FloatingControlManager
import kotlinx.coroutines.*
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executors

class UIRecorderAccessibilityService : AccessibilityService() {
    
    companion object {
        const val TAG = "UIRecorderService"
        var isServiceConnected = false
        
        // 事件类型常量
        const val EVENT_CLICK = "CLICK"
        const val EVENT_INPUT = "INPUT"
        const val EVENT_SELECT = "SELECT"
        const val EVENT_SCROLL = "SCROLL"
        const val EVENT_LONG_CLICK = "LONG_CLICK"
        const val EVENT_FOCUS = "FOCUS"
        const val EVENT_PAGE_CHANGE = "PAGE_CHANGE"
    }
    
    private lateinit var database: UIRecorderDatabase
    private lateinit var uiEventDao: UIEventDao
    private lateinit var elementFeatureDao: ElementFeatureDao
    private lateinit var appConfigDao: AppConfigDao
    
    private val eventProcessor = UIEventProcessor(this)
    private val screenshotHelper = ScreenshotHelper(this)
    private val elementAnalyzer = ElementAnalyzer()
    private val floatingControlManager = FloatingControlManager(this)
    
    // 线程池用于异步处理
    private val executor = Executors.newSingleThreadExecutor()
    
    // 目标应用包名
    private val targetPackages = mutableSetOf<String>()
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Accessibility Service Connected")
        isServiceConnected = true
        
        // 初始化数据库
        database = UIRecorderDatabase.getInstance(this)
        uiEventDao = database.uiEventDao()
        elementFeatureDao = database.elementFeatureDao()
        appConfigDao = database.appConfigDao()
        
        // 初始化事件处理器
        eventProcessor.init(database)
        
        // 添加默认监控应用
        addDefaultMonitoredApps()
        
        // 加载配置
        loadAppConfigs()
        
        // 显示浮动控制窗口
        floatingControlManager.showFloatingControl()
        
        // 发送服务启动广播
        sendServiceStatusBroadcast(true)
        
        // 添加调试日志
        Log.d(TAG, "Service initialization completed")
        Log.d(TAG, "Target packages: $targetPackages")
        Log.d(TAG, "Service is now monitoring all applications")
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!isServiceConnected) {
            Log.d(TAG, "Service not connected, ignoring event")
            return
        }
        
        val packageName = event.packageName?.toString() ?: "unknown"
        Log.d(TAG, "Received accessibility event from package: $packageName, event type: ${event.eventType}")
        
        // 检查是否是目标应用
        if (!shouldRecordPackage(packageName)) {
            Log.d(TAG, "Package $packageName is not in target packages, ignoring event")
            return
        }
        
        Log.d(TAG, "Processing event for package: $packageName")
        
        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                Log.d(TAG, "Processing click event")
                handleClickEvent(event)
            }
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                Log.d(TAG, "Processing text changed event")
                handleTextChangedEvent(event)
            }
            AccessibilityEvent.TYPE_VIEW_SELECTED -> {
                Log.d(TAG, "Processing selection event")
                handleSelectionEvent(event)
            }
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                Log.d(TAG, "Processing scroll event")
                handleScrollEvent(event)
            }
            AccessibilityEvent.TYPE_VIEW_LONG_CLICKED -> {
                Log.d(TAG, "Processing long click event")
                handleLongClickEvent(event)
            }
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
                Log.d(TAG, "Processing focus event")
                handleFocusEvent(event)
            }
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                Log.d(TAG, "Processing window state changed event")
                handleWindowChangeEvent(event)
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                Log.d(TAG, "Processing window content changed event")
                handleContentChangeEvent(event)
            }
        }
    }
    
    override fun onInterrupt() {
        Log.d(TAG, "Accessibility Service Interrupted")
    }
    
    private fun shouldRecordPackage(packageName: String): Boolean {
        Log.d(TAG, "Checking if package should be recorded: $packageName, target packages: $targetPackages")
        // 如果目标包名列表为空，则记录所有应用
        if (targetPackages.isEmpty()) {
            Log.d(TAG, "Target packages is empty, recording all packages")
            return true
        }
        val shouldRecord = targetPackages.contains(packageName)
        Log.d(TAG, "Should record $packageName: $shouldRecord")
        return shouldRecord
    }
    
    private fun addDefaultMonitoredApps() {
        executor.execute {
            try {
                // 检查是否已经添加了默认应用
                val existingConfig = runBlocking { appConfigDao.getConfig("com.tdx.androidCCZQ") }
                Log.d(TAG, "Existing config for com.tdx.androidCCZQ: $existingConfig")
                if (existingConfig == null) {
                    // 添加默认监控的应用
                    val defaultAppConfig = AppConfig(
                        packageName = "com.tdx.androidCCZQ",
                        appName = "同花顺",
                        recordingEnabled = true,
                        recordClicks = true,
                        recordInputs = true,
                        recordSelections = true,
                        recordScrolls = true,
                        takeScreenshots = false,
                        screenshotQuality = 70
                    )
                    runBlocking { appConfigDao.insert(defaultAppConfig) }
                    Log.d(TAG, "Added default monitored app: com.tdx.androidCCZQ")
                } else {
                    Log.d(TAG, "Default monitored app already exists: com.tdx.androidCCZQ")
                }
                
                // 为了监控所有应用，我们不将任何包名添加到targetPackages
                // 这样targetPackages将保持为空，从而监控所有应用
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add default monitored app", e)
            }
        }
    }
    
    private fun loadAppConfigs() {
        executor.execute {
            try {
                val configs = runBlocking { appConfigDao.getAllConfigs() }
                Log.d(TAG, "Loaded ${configs.size} app configs from database")
                
                // 为了监控所有应用，我们不加载配置到targetPackages
                // 这样targetPackages将保持为空，从而监控所有应用
                Log.d(TAG, "Monitoring all applications (target packages kept empty)")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load app configs", e)
            }
        }
    }
    
    private fun handleClickEvent(event: AccessibilityEvent) {
        val source = event.source ?: return
        
        executor.execute {
            try {
                val packageName = event.packageName?.toString() ?: "unknown"
                val uiEvent = eventProcessor.processClickEvent(
                    source,
                    packageName,
                    event.className?.toString()
                )
                
                uiEvent?.let { event ->
                    // 保存到数据库
                    val eventId = runBlocking { uiEventDao.insert(event) }.toInt()
                    
                    // 如果需要截图
                    if (shouldTakeScreenshot(event.packageName)) {
                        val screenshotPath = screenshotHelper.takeScreenshot(
                            event.packageName,
                            eventId
                        )
                        
                        if (screenshotPath != null) {
                            // 更新事件记录，添加截图路径
                            event.copy(screenshotPath = screenshotPath).let { updatedEvent ->
                                runBlocking { uiEventDao.update(updatedEvent) }
                            }
                        }
                    }
                    
                    // 发送广播通知新事件
                    sendNewEventBroadcast(event)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to handle click event", e)
            } finally {
                source.recycle()
            }
        }
    }
    
    private fun handleTextChangedEvent(event: AccessibilityEvent) {
        val source = event.source ?: return
        
        executor.execute {
            try {
                val packageName = event.packageName?.toString() ?: "unknown"
                val uiEvent = eventProcessor.processInputEvent(
                    source,
                    packageName,
                    event.className?.toString(),
                    event.beforeText,
                    event.text?.firstOrNull()
                )
                
                uiEvent?.let { event ->
                    runBlocking { uiEventDao.insert(event) }
                    sendNewEventBroadcast(event)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to handle text changed event", e)
            } finally {
                source.recycle()
            }
        }
    }
    
    private fun handleSelectionEvent(event: AccessibilityEvent) {
        val source = event.source ?: return
        
        executor.execute {
            try {
                val packageName = event.packageName?.toString() ?: "unknown"
                val uiEvent = eventProcessor.processSelectionEvent(
                    source,
                    packageName,
                    event.className?.toString()
                )
                
                uiEvent?.let { event ->
                    runBlocking { uiEventDao.insert(event) }
                    sendNewEventBroadcast(event)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to handle selection event", e)
            } finally {
                source.recycle()
            }
        }
    }
    
    private fun handleScrollEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: "unknown"
        val config = runBlocking { appConfigDao.getConfig(packageName) }
        if (config == null || !config.recordScrolls) return
        
        val source = event.source ?: return
        
        executor.execute {
            try {
                val elementInfo = elementAnalyzer.extractElementInfo(source)
                
                // 使用事件处理器来确保控件特征被记录
                val uiEvent = UIEvent(
                    packageName = packageName,
                    activityName = event.className?.toString(),
                    eventType = EVENT_SCROLL,
                    elementId = elementInfo.id,
                    elementType = elementInfo.type,
                    elementText = elementInfo.text,
                    elementHint = elementInfo.hint,
                    elementContentDesc = elementInfo.contentDesc,
                    elementClass = source.className?.toString(),
                    elementBounds = elementAnalyzer.getBoundsString(source),
                    inputValue = null,
                    selectedValue = null,
                    parentHierarchy = null,
                    siblingInfo = null,
                    extraData = getScrollExtraData(event),
                    screenshotPath = null
                )
                
                // 手动更新元素特征
                eventProcessor.updateElementFeatureFromService(source, packageName, event.className?.toString(), null)
                
                runBlocking { uiEventDao.insert(uiEvent) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to handle scroll event", e)
            } finally {
                source.recycle()
            }
        }
    }
    
    private fun getScrollExtraData(event: AccessibilityEvent): String {
        val extra = mutableMapOf<String, String>()
        
        extra["scrollX"] = event.scrollX.toString()
        extra["scrollY"] = event.scrollY.toString()
        extra["itemCount"] = event.itemCount.toString()
        extra["currentItemIndex"] = event.currentItemIndex.toString()
        extra["fromIndex"] = event.fromIndex.toString()
        extra["toIndex"] = event.toIndex.toString()
        
        return com.google.gson.Gson().toJson(extra)
    }
    
    private fun handleWindowChangeEvent(event: AccessibilityEvent) {
        val source = event.source ?: return
        
        executor.execute {
            try {
                val packageName = event.packageName?.toString() ?: "unknown"
                val elementInfo = elementAnalyzer.extractElementInfo(source)
                
                val uiEvent = UIEvent(
                    packageName = packageName,
                    activityName = event.className?.toString(),
                    eventType = EVENT_PAGE_CHANGE,
                    elementId = elementInfo.id,
                    elementType = elementInfo.type,
                    elementText = elementInfo.text,
                    elementHint = elementInfo.hint,
                    elementContentDesc = elementInfo.contentDesc,
                    elementClass = source.className?.toString(),
                    elementBounds = elementAnalyzer.getBoundsString(source),
                    inputValue = null,
                    selectedValue = null,
                    parentHierarchy = elementAnalyzer.extractParentHierarchy(source),
                    siblingInfo = elementAnalyzer.extractSiblingInfo(source),
                    extraData = null,
                    screenshotPath = null
                )
                
                // 更新元素特征
                eventProcessor.updateElementFeatureFromService(source, packageName, event.className?.toString(), null)
                
                runBlocking { uiEventDao.insert(uiEvent) }
                sendNewEventBroadcast(uiEvent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to handle window change event", e)
            } finally {
                source.recycle()
            }
        }
    }
    
    private fun handleContentChangeEvent(event: AccessibilityEvent) {
        val source = event.source ?: return
        
        executor.execute {
            try {
                val packageName = event.packageName?.toString() ?: "unknown"
                val elementInfo = elementAnalyzer.extractElementInfo(source)
                
                val uiEvent = UIEvent(
                    packageName = packageName,
                    activityName = event.className?.toString(),
                    eventType = EVENT_PAGE_CHANGE, // Using page change for content changes too
                    elementId = elementInfo.id,
                    elementType = elementInfo.type,
                    elementText = elementInfo.text,
                    elementHint = elementInfo.hint,
                    elementContentDesc = elementInfo.contentDesc,
                    elementClass = source.className?.toString(),
                    elementBounds = elementAnalyzer.getBoundsString(source),
                    inputValue = null,
                    selectedValue = null,
                    parentHierarchy = elementAnalyzer.extractParentHierarchy(source),
                    siblingInfo = elementAnalyzer.extractSiblingInfo(source),
                    extraData = null,
                    screenshotPath = null
                )
                
                // 更新元素特征
                eventProcessor.updateElementFeatureFromService(source, packageName, event.className?.toString(), null)
                
                runBlocking { uiEventDao.insert(uiEvent) }
                sendNewEventBroadcast(uiEvent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to handle content change event", e)
            } finally {
                source.recycle()
            }
        }
    }
    
    private fun handleFocusEvent(event: AccessibilityEvent) {
        val source = event.source ?: return
        
        executor.execute {
            try {
                val packageName = event.packageName?.toString() ?: "unknown"
                val elementInfo = elementAnalyzer.extractElementInfo(source)
                
                val uiEvent = UIEvent(
                    packageName = packageName,
                    activityName = event.className?.toString(),
                    eventType = EVENT_FOCUS,
                    elementId = elementInfo.id,
                    elementType = elementInfo.type,
                    elementText = elementInfo.text,
                    elementHint = elementInfo.hint,
                    elementContentDesc = elementInfo.contentDesc,
                    elementClass = source.className?.toString(),
                    elementBounds = elementAnalyzer.getBoundsString(source),
                    inputValue = null,
                    selectedValue = null,
                    parentHierarchy = elementAnalyzer.extractParentHierarchy(source),
                    siblingInfo = elementAnalyzer.extractSiblingInfo(source),
                    extraData = null,
                    screenshotPath = null
                )
                
                // 更新元素特征
                eventProcessor.updateElementFeatureFromService(source, packageName, event.className?.toString(), null)
                
                runBlocking { uiEventDao.insert(uiEvent) }
                sendNewEventBroadcast(uiEvent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to handle focus event", e)
            } finally {
                source.recycle()
            }
        }
    }
    
    private fun handleLongClickEvent(event: AccessibilityEvent) {
        val source = event.source ?: return
        
        executor.execute {
            try {
                val packageName = event.packageName?.toString() ?: "unknown"
                val elementInfo = elementAnalyzer.extractElementInfo(source)
                
                val uiEvent = UIEvent(
                    packageName = packageName,
                    activityName = event.className?.toString(),
                    eventType = EVENT_LONG_CLICK,
                    elementId = elementInfo.id,
                    elementType = elementInfo.type,
                    elementText = elementInfo.text,
                    elementHint = elementInfo.hint,
                    elementContentDesc = elementInfo.contentDesc,
                    elementClass = source.className?.toString(),
                    elementBounds = elementAnalyzer.getBoundsString(source),
                    inputValue = null,
                    selectedValue = null,
                    parentHierarchy = elementAnalyzer.extractParentHierarchy(source),
                    siblingInfo = elementAnalyzer.extractSiblingInfo(source),
                    extraData = null,
                    screenshotPath = null
                )
                
                // 更新元素特征
                eventProcessor.updateElementFeatureFromService(source, packageName, event.className?.toString(), null)
                
                runBlocking { uiEventDao.insert(uiEvent) }
                sendNewEventBroadcast(uiEvent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to handle long click event", e)
            } finally {
                source.recycle()
            }
        }
    }
    
    private fun shouldTakeScreenshot(packageName: CharSequence?): Boolean {
        return try {
            val config = runBlocking { appConfigDao.getConfig(packageName?.toString() ?: "") }
            config?.takeScreenshots == true
        } catch (e: Exception) {
            false
        }
    }
    
    private fun sendServiceStatusBroadcast(isRunning: Boolean) {
        val intent = Intent("com.autodroid.aas.SERVICE_STATUS")
        intent.putExtra("is_running", isRunning)
        sendBroadcast(intent)
    }
    
    private fun sendNewEventBroadcast(event: UIEvent) {
        val intent = Intent("com.autodroid.aas.NEW_EVENT")
        intent.putExtra("event_id", event.id)
        intent.putExtra("package_name", event.packageName)
        intent.putExtra("event_type", event.eventType)
        sendBroadcast(intent)
    }
    
    private fun sendErrorBroadcast(errorMessage: String) {
        val intent = Intent("com.autodroid.aas.ERROR")
        intent.putExtra("error_message", errorMessage)
        sendBroadcast(intent)
    }
    
    private fun registerBroadcastReceiver() {
        val filter = IntentFilter().apply {
            addAction("com.autodroid.aas.TOGGLE_RECORDING")
        }
        
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    "com.autodroid.aas.TOGGLE_RECORDING" -> {
                        // Toggle recording state
                        toggleRecording()
                    }
                }
            }
        }
        
        registerReceiver(receiver, filter)
    }
    
    private fun toggleRecording() {
        // Toggle recording state - for now just log
        Log.d(TAG, "Recording toggled")
    }
}