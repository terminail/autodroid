package com.autodroid.guardiansdk.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.autodroid.guardiansdk.ui.GuardianActivity

class GuardianAccessibilityService : AccessibilityService() {

    private var openDoorPassword = "123456"
    private var shouldCloseAfterOpen = false
    private var isMainActivityOpen = false
    private val handler = Handler(Looper.getMainLooper())

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        if (event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            
            val packageName = event.packageName?.toString()
            if (packageName == "com.android.mms" || (packageName != null && packageName.contains("sms", ignoreCase = true))) {
                checkSmsContent(event)
            }
        }
    }

    private fun checkSmsContent(event: AccessibilityEvent) {
        val rootNode = rootInActiveWindow ?: return
        
        val smsNodes = findSmsNodes(rootNode)
        
        for (node in smsNodes) {
            val text = node.text?.toString()
            if (text != null && text.contains(openDoorPassword)) {
                handleOpenDoorPassword()
                break
            }
        }
    }

    private fun findSmsNodes(node: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val nodes = mutableListOf<AccessibilityNodeInfo>()
        
        if (node.text?.isNotEmpty() == true) {
            nodes.add(node)
        }
        
        for (i in 0 until node.childCount) {
            nodes.addAll(findSmsNodes(node.getChild(i)))
        }
        
        return nodes
    }

    private fun handleOpenDoorPassword() {
        if (isMainActivityOpen) {
            return
        }

        val intent = Intent(this, GuardianActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("FROM_ACCESSIBILITY", true)
        }
        startActivity(intent)
        
        isMainActivityOpen = true
        
        if (shouldCloseAfterOpen) {
            handler.postDelayed({
                closeMainActivity()
            }, 5000)
        }
    }

    private fun closeMainActivity() {
        performGlobalAction(GLOBAL_ACTION_BACK)
        isMainActivityOpen = false
    }

    override fun onInterrupt() {
        // 服务被中断时尝试重启
        restartService()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 服务被销毁时尝试重启
        scheduleRestart()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or
                        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = 100
        }
        serviceInfo = info
        
        // 服务连接成功，记录日志
        android.util.Log.d("GuardianAccessibilityService", "无障碍服务已连接")
    }
    
    private fun restartService() {
        android.util.Log.d("GuardianAccessibilityService", "无障碍服务被中断，尝试重启")
        // 通过Intent尝试重启服务
        val intent = Intent(this, GuardianAccessibilityService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
    
    private fun scheduleRestart() {
        android.util.Log.d("GuardianAccessibilityService", "无障碍服务被销毁，安排重启")
        handler.postDelayed({
            restartService()
        }, 3000) // 3秒后重启
    }

    fun setOpenDoorPassword(password: String) {
        openDoorPassword = password
    }

    fun setShouldCloseAfterOpen(close: Boolean) {
        shouldCloseAfterOpen = close
    }

    fun getOpenDoorPassword(): String = openDoorPassword

    fun isShouldCloseAfterOpen(): Boolean = shouldCloseAfterOpen
}
