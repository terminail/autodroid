package com.autodroid.guardiansdk

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.autodroid.guardiansdk.data.database.GuardianDatabase
import com.autodroid.guardiansdk.data.repository.SettingRepository
import com.autodroid.guardiansdk.sms.repository.SmsRepository
import com.autodroid.guardiansdk.util.GuardianSdkValidator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 报警功能SDK主入口
 * 提供隐秘报警功能和短信守卫功能的集成接口和界面组件
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
                    // 验证SDK集成
                    val validationResult = GuardianSdkValidator.validate(context)
                    if (!validationResult.isValid) {
                        android.util.Log.e("GuardianSdk", "SDK集成验证失败:")
                        validationResult.issues.forEach { issue ->
                            android.util.Log.e("GuardianSdk", "  - $issue")
                        }
                    }
                    validationResult.warnings.forEach { warning ->
                        android.util.Log.w("GuardianSdk", "  - $warning")
                    }
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
    private lateinit var smsRepository: SmsRepository
    
    /**
     * 初始化数据库
     */
    private fun initializeDatabase() {
        val database = GuardianDatabase.getDatabase(context)
        settingRepository = SettingRepository(database.settingDao())
        smsRepository = SmsRepository(context)
        
        // 初始化默认设置（异步执行）
        CoroutineScope(Dispatchers.IO).launch {
            settingRepository.initializeDefaultSettings()
        }
    }
    
    /**
     * 启动设置界面
     */
    fun startSettingActivity() {
        val intent = Intent(context, com.autodroid.guardiansdk.ui.GuardianActivity::class.java)
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
     * 获取设置界面
     */
    fun getSettingFragment(): androidx.fragment.app.Fragment {
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
    
    /**
     * 检查并引导开启无障碍服务
     * 如果服务未启用，显示对话框引导用户开启
     * @param activity 当前Activity，用于显示对话框
     * @return true-服务已启用，false-需要用户开启
     */
    fun checkAndRequestAccessibilitySettings(activity: Activity): Boolean {
        if (isAccessibilityServiceEnabled()) {
            return true
        }
        
        AlertDialog.Builder(activity)
            .setTitle("需要开启无障碍服务")
            .setMessage("请开启无障碍服务以使用短信守卫功能")
            .setPositiveButton("去设置") { _, _ ->
                openAccessibilitySettings()
            }
            .setNegativeButton("取消", null)
            .setOnDismissListener {
                // 用户关闭对话框后，应用可以再次调用 checkAndRequestPermissions()
            }
            .show()
        
        return false
    }
    
    /**
     * 检查并请求短信权限
     * 如果权限未授予，显示对话框引导用户授权
     * @param activity 当前Activity，用于显示对话框和请求权限
     * @param requestCode 请求码，用于onPermissionsResult回调
     * @return true-权限已授予，false-需要用户授权
     */
    fun checkAndRequestSmsPermissions(activity: Activity, requestCode: Int = 1001): Boolean {
        val permissions = arrayOf(
            android.Manifest.permission.SEND_SMS,
            android.Manifest.permission.RECEIVE_SMS,
            android.Manifest.permission.READ_SMS
        )
        
        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (missingPermissions.isEmpty()) {
            return true
        }
        
        AlertDialog.Builder(activity)
            .setTitle("需要短信权限")
            .setMessage("请授予短信权限以使用报警和短信守卫功能")
            .setPositiveButton("去授权") { _, _ ->
                ActivityCompat.requestPermissions(activity, missingPermissions.toTypedArray(), requestCode)
            }
            .setNegativeButton("取消", null)
            .show()
        
        return false
    }
    
    /**
     * 检查并请求位置权限
     * 如果权限未授予，显示对话框引导用户授权
     * @param activity 当前Activity，用于显示对话框和请求权限
     * @param requestCode 请求码，用于onPermissionsResult回调
     * @return true-权限已授予，false-需要用户授权
     */
    fun checkAndRequestLocationPermissions(activity: Activity, requestCode: Int = 1002): Boolean {
        val permissions = arrayOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
        
        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (missingPermissions.isEmpty()) {
            return true
        }
        
        AlertDialog.Builder(activity)
            .setTitle("需要位置权限")
            .setMessage("请授予位置权限以获取报警时的位置信息")
            .setPositiveButton("去授权") { _, _ ->
                ActivityCompat.requestPermissions(activity, missingPermissions.toTypedArray(), requestCode)
            }
            .setNegativeButton("取消", null)
            .show()
        
        return false
    }
    
    /**
     * 检查并请求悬浮窗权限
     * 如果权限未授予，显示对话框引导用户授权
     * @param activity 当前Activity，用于显示对话框
     * @return true-权限已授予，false-需要用户授权
     */
    fun checkAndRequestOverlayPermission(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(context)) {
            return true
        }
        
        AlertDialog.Builder(activity)
            .setTitle("需要悬浮窗权限")
            .setMessage("请授予悬浮窗权限以使用浮动窗口报警功能")
            .setPositiveButton("去设置") { _, _ ->
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                activity.startActivity(intent)
            }
            .setNegativeButton("取消", null)
            .show()
        
        return false
    }
    
    /**
     * 检查并请求录音权限
     * 如果权限未授予，显示对话框引导用户授权
     * @param activity 当前Activity，用于显示对话框和请求权限
     * @param requestCode 请求码，用于onPermissionsResult回调
     * @return true-权限已授予，false-需要用户授权
     */
    fun checkAndRequestRecordAudioPermission(activity: Activity, requestCode: Int = 1003): Boolean {
        val permission = android.Manifest.permission.RECORD_AUDIO
        
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            return true
        }
        
        AlertDialog.Builder(activity)
            .setTitle("需要录音权限")
            .setMessage("请授予录音权限以使用报警录音功能")
            .setPositiveButton("去授权") { _, _ ->
                ActivityCompat.requestPermissions(activity, arrayOf(permission), requestCode)
            }
            .setNegativeButton("取消", null)
            .show()
        
        return false
    }
    
    /**
     * 检查并请求所有必要权限（智能版本）
     * 循环检查所有权限，引导用户逐个授权，直到所有权限都授予
     * @param activity 当前Activity，用于显示对话框和请求权限
     * @return true-所有权限已授予，false-有权限需要用户授权
     */
    fun checkAndRequestPermissions(activity: Activity): Boolean {
        if (checkAllPermissions()) {
            return true
        }
        
        checkNextPermission(activity)
        return false
    }
    
    /**
     * 检查下一个缺失的权限并引导用户授权
     * @param activity 当前Activity
     */
    private fun checkNextPermission(activity: Activity) {
        if (!isAccessibilityServiceEnabled()) {
            checkAndRequestAccessibilitySettings(activity)
        } else if (!checkSmsPermissions()) {
            checkAndRequestSmsPermissions(activity)
        } else if (!checkLocationPermissions()) {
            checkAndRequestLocationPermissions(activity)
        } else if (!checkOverlayPermission()) {
            checkAndRequestOverlayPermission(activity)
        } else if (!checkRecordAudioPermission()) {
            checkAndRequestRecordAudioPermission(activity)
        }
    }
    
    /**
     * 检查所有权限是否已授予（不显示对话框）
     * @return true-所有权限已授予，false-有权限未授予
     */
    private fun checkAllPermissions(): Boolean {
        return isAccessibilityServiceEnabled() &&
                checkSmsPermissions() &&
                checkLocationPermissions() &&
                checkOverlayPermission() &&
                checkRecordAudioPermission()
    }
    
    /**
     * 检查短信权限是否已授予（不显示对话框）
     * @return true-权限已授予，false-权限未授予
     */
    private fun checkSmsPermissions(): Boolean {
        val permissions = arrayOf(
            android.Manifest.permission.SEND_SMS,
            android.Manifest.permission.RECEIVE_SMS,
            android.Manifest.permission.READ_SMS
        )
        
        return permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * 检查位置权限是否已授予（不显示对话框）
     * @return true-权限已授予，false-权限未授予
     */
    private fun checkLocationPermissions(): Boolean {
        val permissions = arrayOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
        
        return permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * 检查悬浮窗权限是否已授予（不显示对话框）
     * @return true-权限已授予，false-权限未授予
     */
    private fun checkOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }
    
    /**
     * 检查录音权限是否已授予（不显示对话框）
     * @return true-权限已授予，false-权限未授予
     */
    private fun checkRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * 获取短信仓库
     */
    fun getSmsRepository(): SmsRepository {
        return smsRepository
    }
    
    /**
     * 发送短信
     */
    suspend fun sendSms(address: String, body: String): Boolean {
        return smsRepository.sendSms(address, body)
    }
    
    /**
     * 获取所有会话
     */
    fun getAllConversations() = smsRepository.getAllConversations()
    
    /**
     * 获取指定会话的消息
     */
    fun getMessagesByThread(threadId: Long) = smsRepository.getMessagesByThread(threadId)
    
    /**
     * 根据地址获取消息
     */
    fun getMessagesByAddress(address: String) = smsRepository.getMessagesByAddress(address)
    
    /**
     * 验证SDK集成
     */
    fun validateIntegration(): com.autodroid.guardiansdk.util.ValidationResult {
        return GuardianSdkValidator.validate(context)
    }
    
    /**
     * 检查缺失的权限
     */
    fun checkMissingPermissions(): List<String> {
        return GuardianSdkValidator.checkPermissions(context)
    }
    
    /**
     * 检查悬浮窗权限是否授予
     */
    fun isOverlayPermissionGranted(): Boolean {
        return GuardianSdkValidator.isOverlayPermissionGranted(context)
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