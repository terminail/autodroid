package com.autodroid.trader.ui.floatwindow

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log

object FloatWindowManager {
    
    private const val TAG = "FloatWindowManager"
    
    /**
     * 检查悬浮窗权限
     */
    fun checkFloatWindowPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true // 6.0以下默认有权限
        }
    }
    
    /**
     * 申请悬浮窗权限
     */
    fun requestFloatWindowPermission(context: Context) {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to request float window permission: ${e.message}")
        }
    }
    
    /**
     * 显示悬浮窗
     */
    fun showFloatWindow(context: Context) {
        if (checkFloatWindowPermission(context)) {
            val intent = Intent(context, FloatWindowService::class.java)
            intent.action = FloatWindowService.ACTION_SHOW
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } else {
            requestFloatWindowPermission(context)
        }
    }
    
    /**
     * 隐藏悬浮窗
     */
    fun hideFloatWindow(context: Context) {
        val intent = Intent(context, FloatWindowService::class.java)
        intent.action = FloatWindowService.ACTION_HIDE
        context.stopService(intent)
    }
    
    /**
     * 切换悬浮窗显示状态
     */
    fun toggleFloatWindow(context: Context) {
        if (checkFloatWindowPermission(context)) {
            // 这里可以根据服务是否运行来切换
            // 简单实现：直接显示
            showFloatWindow(context)
        } else {
            requestFloatWindowPermission(context)
        }
    }
}