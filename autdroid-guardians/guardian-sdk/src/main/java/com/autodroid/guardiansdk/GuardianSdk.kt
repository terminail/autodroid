package com.autodroid.guardiansdk

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.autodroid.guardiansdk.data.database.GuardianDatabase
import com.autodroid.guardiansdk.data.repository.SettingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 报警功能SDK主入口
 * 提供隐秘报警功能的集成接口和界面组件
 */
class GuardianSdk private constructor(private val context: Context) {
    
    companion object {
        @Volatile
        private var instance: GuardianSdk? = null
        
        /**
         * 初始化SDK
         */
        fun initialize(context: Context): GuardianSdk {
            return instance ?: synchronized(this) {
                instance ?: GuardianSdk(context.applicationContext).also { 
                    instance = it
                    // 初始化数据库和默认设置
                    it.initializeDatabase()
                }
            }
        }
        
        /**
         * 获取SDK实例
         */
        fun getInstance(): GuardianSdk {
            return instance ?: throw IllegalStateException("GuardianSdk not initialized. Call initialize() first.")
        }
    }
    
    private lateinit var settingRepository: SettingRepository
    
    /**
     * 初始化数据库
     */
    private fun initializeDatabase() {
        val database = GuardianDatabase.getDatabase(context)
        settingRepository = SettingRepository(database.settingDao())
        
        // 初始化默认设置（异步执行）
        CoroutineScope(Dispatchers.IO).launch {
            settingRepository.initializeDefaultSettings()
        }
    }
    
    /**
     * 启动设置界面
     */
    fun startSettingActivity() {
        val intent = Intent(context, com.autodroid.guardiansdk.ui.SettingActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
    
    /**
     * 启动浮动窗口服务
     */
    fun startFloatingWindowService() {
        try {
            val intent = Intent(context, com.autodroid.guardiansdk.service.FloatingWindowService::class.java)
            context.startService(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 停止浮动窗口服务
     */
    fun stopFloatingWindowService() {
        try {
            val intent = Intent(context, com.autodroid.guardiansdk.service.FloatingWindowService::class.java)
            context.stopService(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 启动报警服务
     */
    fun startAlarmService() {
        // 启动后台服务
        // 初始化物理键监听
        // 启动短信监听
    }
    
    /**
     * 停止报警服务
     */
    fun stopAlarmService() {
        // 停止所有服务
    }
    
    /**
     * 触发紧急报警
     */
    fun triggerEmergencyAlarm(alarmType: AlarmType) {
        // 根据类型触发不同的报警
    }
    
    /**
     * 获取隐秘设置界面
     */
    fun getEmergencyContactsFragment(): androidx.fragment.app.Fragment {
        return com.autodroid.guardiansdk.ui.settings.SettingFragment.newInstance()
    }
    
    /**
     * 检查无障碍服务是否已启用
     */
    fun isAccessibilityServiceEnabled(): Boolean {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        
        for (service in enabledServices) {
            if (service.id.contains(context.packageName)) {
                return true
            }
        }
        return false
    }
    
    /**
     * 打开无障碍服务设置页面
     */
    fun openAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

/**
 * 报警类型枚举
 */
enum class AlarmType {
    URGENT_RESCUE,    // 紧急求救
    FOLLOWED,         // 被跟踪
    NEED_HELP,        // 需要帮助
    MEDICAL_EMERGENCY // 医疗紧急
}