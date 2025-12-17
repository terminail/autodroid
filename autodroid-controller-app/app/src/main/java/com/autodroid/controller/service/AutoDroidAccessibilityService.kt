package com.autodroid.controller.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class AutoDroidAccessibilityService : AccessibilityService() {
    private val TAG = "AutoDroidAccessibilityService"
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "无障碍服务已连接")
        
        // 配置无障碍服务
        val info = AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_VIEW_CLICKED or
                         AccessibilityEvent.TYPE_VIEW_FOCUSED or
                         AccessibilityEvent.TYPE_VIEW_SELECTED or
                         AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.notificationTimeout = 100
        info.flags = AccessibilityServiceInfo.DEFAULT
        
        this.serviceInfo = info
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let {
            Log.d(TAG, "无障碍事件: ${event.eventType}, 包名: ${event.packageName}")
            
            // 可以在这里处理无障碍事件，用于增强自动化能力
            when (event.eventType) {
                AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                    Log.d(TAG, "视图点击事件: ${event.text}")
                }
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                    Log.d(TAG, "窗口状态变化: ${event.className}")
                }
            }
        }
    }
    
    override fun onInterrupt() {
        Log.d(TAG, "无障碍服务被中断")
    }
}