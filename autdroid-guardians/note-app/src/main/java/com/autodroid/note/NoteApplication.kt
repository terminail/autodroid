package com.autodroid.note

import android.app.Application
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.provider.Settings
import androidx.core.content.edit
import com.autodroid.guardiansdk.GuardianSdk

class NoteApplication : Application() {
    
    companion object {
        private const val PREFS_NAME = "guardian_settings"
        private const val KEY_ACCESSIBILITY_PROMT_COUNT = "accessibility_prompt_count"
        private const val MAX_PROMPT_COUNT = 3
    }
    
    private lateinit var prefs: SharedPreferences
    
    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        
        // 先初始化 Guardian SDK
        GuardianSdk.initialize(this)
        
        // 应用启动时自动检查并恢复无障碍服务
        checkAndRestoreAccessibilityService()
    }
    
    private fun checkAndRestoreAccessibilityService() {
        // 检查无障碍服务状态
        if (!GuardianSdk.getInstance().isAccessibilityServiceEnabled()) {
            val promptCount = prefs.getInt(KEY_ACCESSIBILITY_PROMT_COUNT, 0)
            
            // 限制提示次数，避免过于频繁
            if (promptCount < MAX_PROMPT_COUNT) {
                prefs.edit {
                    putInt(KEY_ACCESSIBILITY_PROMT_COUNT, promptCount + 1)
                }
                
                // 自动引导用户重新开启
                showAutoAccessibilityPrompt()
            } else {
                android.util.Log.d("NoteApplication", "已达到最大提示次数，不再提示")
            }
        } else {
            // 服务正常，重置提示计数
            prefs.edit {
                putInt(KEY_ACCESSIBILITY_PROMT_COUNT, 0)
            }
            android.util.Log.d("NoteApplication", "无障碍服务已启用")
        }
    }
    
    private fun showAutoAccessibilityPrompt() {
        // 这里可以添加自动提示逻辑
        // 例如：显示通知、启动引导Activity等
        android.util.Log.d("NoteApplication", "检测到无障碍服务未开启，需要引导用户")
        
        // 记录日志，便于调试
        android.util.Log.i("NoteApplication", "应用启动时发现无障碍服务被关闭，请引导用户重新开启")
    }
    
    /**
     * 重置提示计数，当用户手动开启服务后调用
     */
    fun resetAccessibilityPromptCount() {
        prefs.edit {
            putInt(KEY_ACCESSIBILITY_PROMT_COUNT, 0)
        }
    }
    
    /**
     * 获取当前提示次数
     */
    fun getAccessibilityPromptCount(): Int {
        return prefs.getInt(KEY_ACCESSIBILITY_PROMT_COUNT, 0)
    }
}