package com.autodroid.guardiansdk.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.autodroid.guardiansdk.GuardianSdk
import com.autodroid.guardiansdk.ui.SettingActivity

/**
 * 报警功能辅助服务
 * 监控特定短信自动启动设置界面
 */
class GuardianAccessibilityService : AccessibilityService() {
    
    companion object {
        private const val TAG = "GuardianAccessibilityService"
        
        // 监控的短信关键词
        private val MONITOR_KEYWORDS = listOf(
            "紧急设置",
            "报警设置", 
            "guardian",
            "紧急模式"
        )
    }
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Guardian Accessibility Service 已连接")
        
        // 配置服务信息
        val serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            packageNames = arrayOf("com.android.mms") // 监控短信应用
        }
        
        this.serviceInfo = serviceInfo
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        
        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                // 监控短信内容变化
                handleSmsContent(event)
            }
        }
    }
    
    override fun onInterrupt() {
        Log.d(TAG, "Guardian Accessibility Service 被中断")
    }
    
    /**
     * 处理短信内容
     */
    private fun handleSmsContent(event: AccessibilityEvent) {
        val text = event.text?.joinToString(" ") ?: ""
        
        // 检查是否包含监控关键词
        val containsKeyword = MONITOR_KEYWORDS.any { keyword ->
            text.contains(keyword, ignoreCase = true)
        }
        
        if (containsKeyword) {
            Log.d(TAG, "检测到关键词短信，启动设置界面: $text")
            
            // 自动启动设置界面
            startSettingActivity()
        }
    }
    
    /**
     * 启动设置界面
     */
    private fun startSettingActivity() {
        try {
            val intent = Intent(this, SettingActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            Log.d(TAG, "设置界面启动成功")
        } catch (e: Exception) {
            Log.e(TAG, "启动设置界面失败: ${e.message}")
        }
    }
}