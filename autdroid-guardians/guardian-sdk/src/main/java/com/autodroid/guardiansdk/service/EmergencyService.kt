package com.autodroid.guardiansdk.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.autodroid.guardiansdk.manager.AudioRecordingManager

/**
 * 紧急模式服务
 * 处理紧急报警逻辑和后台监听
 */
class EmergencyService : Service() {
    
    companion object {
        private const val TAG = "EmergencyService"
        const val ACTION_TRIGGER_EMERGENCY = "TRIGGER_EMERGENCY"
        const val EXTRA_TRIGGER_TYPE = "TRIGGER_TYPE"
        
        fun triggerEmergency(context: Context, triggerType: String) {
            val intent = Intent(context, EmergencyService::class.java).apply {
                action = ACTION_TRIGGER_EMERGENCY
                putExtra(EXTRA_TRIGGER_TYPE, triggerType)
            }
            context.startService(intent)
        }
    }
    
    private lateinit var audioRecordingManager: AudioRecordingManager
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "紧急模式服务已创建")
        
        // 初始化音频录音管理器
        audioRecordingManager = AudioRecordingManager(this)
        
        // 初始化紧急模式监听
        initializeEmergencyMode()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "紧急模式服务已启动")
        
        // 处理紧急触发
        intent?.action?.let { action ->
            when (action) {
                ACTION_TRIGGER_EMERGENCY -> {
                    val triggerType = intent.getStringExtra(EXTRA_TRIGGER_TYPE) ?: "UNKNOWN"
                    handleEmergencyTrigger(triggerType)
                }
            }
        }
        
        // 启动前台服务（如果需要）
        startForegroundServiceIfNeeded()
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "紧急模式服务已销毁")
        
        // 清理资源
        cleanupEmergencyMode()
    }
    
    /**
     * 初始化紧急模式
     */
    private fun initializeEmergencyMode() {
        // 初始化物理按键监听
        // 初始化短信监听
        // 初始化位置监听
        Log.d(TAG, "紧急模式初始化完成")
    }
    
    /**
     * 启动前台服务
     */
    private fun startForegroundServiceIfNeeded() {
        // 如果需要前台服务，创建通知并启动
        // val notification = createEmergencyNotification()
        // startForeground(1, notification)
    }
    
    /**
     * 清理紧急模式
     */
    private fun cleanupEmergencyMode() {
        // 停止所有监听器
        // 释放资源
        Log.d(TAG, "紧急模式清理完成")
    }
    
    /**
     * 处理紧急触发
     */
    private fun handleEmergencyTrigger(triggerType: String) {
        Log.i(TAG, "处理紧急触发，类型: $triggerType")
        
        // 启动隐秘录音
        audioRecordingManager.startStealthRecording(triggerType)
        
        // 发送报警短信（原有的报警逻辑）
        sendEmergencyAlert(triggerType)
        
        // 记录报警日志
        logEmergencyEvent(triggerType)
    }
    
    /**
     * 发送报警短信
     */
    private fun sendEmergencyAlert(triggerType: String) {
        // 原有的报警短信发送逻辑
        Log.i(TAG, "发送报警短信，触发类型: $triggerType")
        
        // 这里可以调用原有的短信发送功能
        // EmergencyAlertManager.sendAlert(triggerType)
    }
    
    /**
     * 记录报警事件
     */
    private fun logEmergencyEvent(triggerType: String) {
        // 记录到数据库或日志文件
        Log.i(TAG, "报警事件记录: $triggerType")
        
        // 这里可以调用原有的日志记录功能
        // DatabaseManager.logEmergencyEvent(triggerType)
    }
}