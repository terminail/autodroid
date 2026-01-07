package com.autodroid.trader.aas.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.autodroid.trader.aas.database.AppConfigDao
import com.autodroid.trader.aas.database.ElementFeatureDao
import com.autodroid.trader.aas.database.UIEvent
import com.autodroid.trader.aas.database.UIEventDao
import com.autodroid.trader.aas.database.UIRecorderDatabase
import com.autodroid.trader.aas.rule.RecordingRule
import com.autodroid.trader.aas.rule.RuleCondition
import com.autodroid.trader.aas.rule.RuleEngine
import com.autodroid.trader.aas.ui.FloatingControlManager
import com.google.gson.Gson
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
    
    // 规则引擎
    private val ruleEngine = RuleEngine()

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Accessibility Service Connected")



        // 配置无障碍服务
        val info = AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_VIEW_CLICKED or
                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or
                AccessibilityEvent.TYPE_VIEW_FOCUSED or
                AccessibilityEvent.TYPE_VIEW_SELECTED or
                AccessibilityEvent.TYPE_VIEW_LONG_CLICKED or
                AccessibilityEvent.TYPE_VIEW_SCROLLED or
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED

        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.notificationTimeout = 100
        info.flags = AccessibilityServiceInfo.DEFAULT

        this.serviceInfo = info

        isServiceConnected = true

        // 初始化数据库
        database = UIRecorderDatabase.Companion.getInstance(this)
        uiEventDao = database.uiEventDao()
        elementFeatureDao = database.elementFeatureDao()
        appConfigDao = database.appConfigDao()

        // 初始化事件处理器
        eventProcessor.init(database)

        // 加载配置
        loadAppConfigs()

        // 显示浮动控制窗口
        floatingControlManager.showFloatingControl()

        // 发送服务启动广播
        sendServiceStatusBroadcast(true)



    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!isServiceConnected) return

        val packageName = event.packageName?.toString() ?: return

        // 检查是否是目标应用
        if (!shouldRecordPackage(packageName)) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                handleClickEvent(event)
            }
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                handleTextChangedEvent(event)
            }
            AccessibilityEvent.TYPE_VIEW_SELECTED -> {
                handleSelectionEvent(event)
            }
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                handleScrollEvent(event)
            }
            AccessibilityEvent.TYPE_VIEW_LONG_CLICKED -> {
                handleLongClickEvent(event)
            }
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
                handleFocusEvent(event)
            }
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                handleWindowChangeEvent(event)
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                handleContentChangeEvent(event)
            }
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility Service Interrupted")
    }

    private fun shouldRecordPackage(packageName: String): Boolean {
        // Hardcode target packages for simplicity
        // Add the target package for testing
        return packageName == "com.tdx.androidCCZQ" || targetPackages.contains(packageName)
    }
    
    private fun shouldRecordEvent(uiEvent: UIEvent): Boolean {
        // Create default rules for sensitive content filtering
        val defaultRules = listOf(
            RecordingRule(
                condition = RuleCondition.CONTAINS_SENSITIVE_TEXT
            ),
            RecordingRule(
                condition = RuleCondition.PACKAGE_INCLUDE,
                packages = targetPackages.toList()
            )
        )
        
        return ruleEngine.shouldRecordEvent(uiEvent, defaultRules)
    }

    private fun loadAppConfigs() {
        // Hardcode target packages for simplicity
        targetPackages.add("com.tdx.androidCCZQ")  // Add the main target app
        // Add other apps as needed
        Log.d(TAG, "Loaded hardcoded target packages: $targetPackages")
    }

    private fun handleClickEvent(event: AccessibilityEvent) {
        val source = event.source ?: return

        executor.execute {
            try {
                val packageName = event.packageName?.toString() ?: "unknown"
                val elementId = source.viewIdResourceName ?: source.text?.toString() ?: "unknown"
                
                // 检查最近是否有相同类型的事件（避免重复记录）
                val recentTime = System.currentTimeMillis() - 1000 // 1秒内的重复事件视为重复
                val existingEvent = runBlocking { 
                    uiEventDao.getRecentEventByElementAndType(
                        packageName, 
                        elementId, 
                        "CLICK", 
                        recentTime
                    ) 
                }
                
                if (existingEvent != null) {
                    // 更新现有事件的时间戳
                    val updatedEvent = existingEvent.copy(eventTime = System.currentTimeMillis())
                    runBlocking { uiEventDao.update(updatedEvent) }
                } else {
                    // 插入新事件
                    val uiEvent = eventProcessor.processClickEvent(
                        source,
                        packageName,
                        event.className?.toString()
                    )

                    uiEvent?.let { processedEvent ->
                        // 检查事件是否应该被记录（根据规则）
                        if (shouldRecordEvent(processedEvent)) {
                            // 保存到数据库
                            val eventId = runBlocking { uiEventDao.insert(processedEvent) }.toInt()

                            // 如果需要截图
                            if (shouldTakeScreenshot(processedEvent.packageName)) {
                                val screenshotPath = screenshotHelper.takeScreenshot(
                                    processedEvent.packageName,
                                    eventId
                                )

                                if (screenshotPath != null) {
                                    // 更新事件记录，添加截图路径
                                    processedEvent.copy(screenshotPath = screenshotPath).let { updatedEvent ->
                                        runBlocking { uiEventDao.update(updatedEvent) }
                                    }
                                }
                            }

                            // 发送广播通知新事件
                            sendNewEventBroadcast(processedEvent)
                        } else {
                            Log.d(TAG, "Sensitive event filtered: ${processedEvent.eventType} in ${processedEvent.packageName}")
                        }
                    }
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
                val elementId = source.viewIdResourceName ?: source.text?.toString() ?: "unknown"
                
                // 检查最近是否有相同类型的事件（避免重复记录）
                val recentTime = System.currentTimeMillis() - 1000 // 1秒内的重复事件视为重复
                val existingEvent = runBlocking { 
                    uiEventDao.getRecentEventByElementAndType(
                        packageName, 
                        elementId, 
                        "INPUT", 
                        recentTime
                    ) 
                }
                
                if (existingEvent != null) {
                    // 更新现有事件的时间戳
                    val updatedEvent = existingEvent.copy(eventTime = System.currentTimeMillis())
                    runBlocking { uiEventDao.update(updatedEvent) }
                } else {
                    // 插入新事件
                    val uiEvent = eventProcessor.processInputEvent(
                        source,
                        packageName,
                        event.className?.toString(),
                        event.beforeText,
                        event.text?.firstOrNull()
                    )

                    uiEvent?.let { processedEvent ->
                        // 检查事件是否应该被记录（根据规则）
                        if (shouldRecordEvent(processedEvent)) {
                            runBlocking { uiEventDao.insert(processedEvent) }
                            sendNewEventBroadcast(processedEvent)
                        } else {
                            Log.d(TAG, "Sensitive event filtered: ${processedEvent.eventType} in ${processedEvent.packageName}")
                        }
                    }
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
                val uiEvent = eventProcessor.processSelectionEvent(
                    source,
                    event.packageName?.toString() ?: "",
                    event.className?.toString()
                )

                uiEvent?.let { processedEvent ->
                    // 检查事件是否应该被记录（根据规则）
                    if (shouldRecordEvent(processedEvent)) {
                        runBlocking { uiEventDao.insert(processedEvent) }
                        sendNewEventBroadcast(processedEvent)
                    } else {
                        Log.d(TAG, "Sensitive event filtered: ${processedEvent.eventType} in ${processedEvent.packageName}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to handle selection event", e)
            } finally {
                source.recycle()
            }
        }
    }

    private fun handleScrollEvent(event: AccessibilityEvent) {
        val config = runBlocking { appConfigDao.getConfig(event.packageName?.toString() ?: "") }
        if (config == null || !config.recordScrolls) return

        val source = event.source ?: return

        executor.execute {
            try {
                val elementInfo = elementAnalyzer.extractElementInfo(source)

                val uiEvent = UIEvent(
                    packageName = event.packageName?.toString() ?: "",
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

                // 检查事件是否应该被记录（根据规则）
                if (shouldRecordEvent(uiEvent)) {
                    runBlocking { uiEventDao.insert(uiEvent) }
                } else {
                    Log.d(TAG, "Sensitive event filtered: ${uiEvent.eventType} in ${uiEvent.packageName}")
                }
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

        return Gson().toJson(extra)
    }

    private fun handleLongClickEvent(event: AccessibilityEvent) {
        val source = event.source ?: return

        executor.execute {
            try {
                val elementInfo = elementAnalyzer.extractElementInfo(source)

                val uiEvent = UIEvent(
                    packageName = event.packageName?.toString() ?: "",
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

                // 检查事件是否应该被记录（根据规则）
                if (shouldRecordEvent(uiEvent)) {
                    runBlocking { uiEventDao.insert(uiEvent) }
                    sendNewEventBroadcast(uiEvent)
                } else {
                    Log.d(TAG, "Sensitive event filtered: ${uiEvent.eventType} in ${uiEvent.packageName}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to handle long click event", e)
            } finally {
                source.recycle()
            }
        }
    }

    private fun handleFocusEvent(event: AccessibilityEvent) {
        val source = event.source ?: return

        executor.execute {
            try {
                val elementInfo = elementAnalyzer.extractElementInfo(source)

                val uiEvent = UIEvent(
                    packageName = event.packageName?.toString() ?: "",
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

                // 检查事件是否应该被记录（根据规则）
                if (shouldRecordEvent(uiEvent)) {
                    runBlocking { uiEventDao.insert(uiEvent) }
                } else {
                    Log.d(TAG, "Sensitive event filtered: ${uiEvent.eventType} in ${uiEvent.packageName}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to handle focus event", e)
            } finally {
                source.recycle()
            }
        }
    }

    private fun handleWindowChangeEvent(event: AccessibilityEvent) {
        executor.execute {
            try {
                val uiEvent = UIEvent(
                    packageName = event.packageName?.toString() ?: "",
                    activityName = event.className?.toString(),
                    eventType = EVENT_PAGE_CHANGE,
                    elementId = null,
                    elementType = null,
                    elementText = event.text?.joinToString(", "),
                    elementHint = null,
                    elementContentDesc = null,
                    elementClass = null,
                    elementBounds = null,
                    inputValue = null,
                    selectedValue = null,
                    parentHierarchy = null,
                    siblingInfo = null,
                    extraData = null,
                    screenshotPath = null
                )

                // 检查事件是否应该被记录（根据规则）
                if (shouldRecordEvent(uiEvent)) {
                    runBlocking { uiEventDao.insert(uiEvent) }
                    Log.d(TAG, "Window changed: ${event.packageName} -> ${event.className}")
                } else {
                    Log.d(TAG, "Sensitive event filtered: ${uiEvent.eventType} in ${uiEvent.packageName}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to handle window change event", e)
            }
        }
    }

    private fun handleContentChangeEvent(event: AccessibilityEvent) {
        // 内容变化事件可能很频繁，选择性记录
        if (event.contentChangeTypes and AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE == 0) {
            return
        }

        Log.d(TAG, "Content changed in ${event.packageName}")
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