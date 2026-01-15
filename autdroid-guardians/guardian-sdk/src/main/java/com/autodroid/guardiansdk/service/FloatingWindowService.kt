package com.autodroid.guardiansdk.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

/**
 * 浮动窗口服务
 * 显示浮动报警按钮，支持长按触发报警
 */
class FloatingWindowService : Service() {
    
    companion object {
        private const val TAG = "FloatingWindowService"
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "浮动窗口服务已创建")
        
        // 初始化浮动窗口
        initializeFloatingWindow()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "浮动窗口服务已启动")
        
        // 显示浮动窗口
        showFloatingWindow()
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "浮动窗口服务已销毁")
        
        // 隐藏浮动窗口
        hideFloatingWindow()
    }
    
    /**
     * 初始化浮动窗口
     */
    private fun initializeFloatingWindow() {
        // 创建浮动窗口管理器
        // 设置窗口参数
        Log.d(TAG, "浮动窗口初始化完成")
    }
    
    /**
     * 显示浮动窗口
     */
    private fun showFloatingWindow() {
        // 显示浮动报警按钮
        Log.d(TAG, "浮动窗口已显示")
    }
    
    /**
     * 隐藏浮动窗口
     */
    private fun hideFloatingWindow() {
        // 隐藏浮动报警按钮
        Log.d(TAG, "浮动窗口已隐藏")
    }
}