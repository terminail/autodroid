package com.autodroid.sms

import android.app.Application
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.provider.Settings
import androidx.core.content.getSystemService

/**
 * 短信应用主类
 * 负责检查默认短信应用状态
 */
class SmsApplication : Application() {
    
    companion object {
        lateinit var instance: SmsApplication
            private set
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // 检查是否为默认短信应用
        checkDefaultSmsAppStatus()
    }
    
    /**
     * 检查是否为默认短信应用
     */
    private fun checkDefaultSmsAppStatus() {
        if (!isDefaultSmsApp()) {
            // 如果不是默认短信应用，可以提示用户设置
            // 在实际应用中，应该在适当的时候提示用户
        }
    }
    
    /**
     * 请求成为默认短信应用
     */
    fun requestDefaultSmsApp(context: Context) {
        // 尝试设置华为特定的配置
        try {
            Settings.Secure.putString(context.contentResolver, "hsm_default_sms_app", packageName)
        } catch (e: Exception) {
            // 忽略华为特定设置的失败
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ 使用RoleManager
            val roleManager = context.getSystemService<RoleManager>()
            roleManager?.let {
                val intent = it.createRequestRoleIntent(RoleManager.ROLE_SMS)
                context.startActivity(intent)
            }
        } else {
            // Android 4.4-9 使用Intent
            val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
            context.startActivity(intent)
        }
    }
    
    /**
     * 检查当前应用是否为默认短信应用
     */
    fun isDefaultSmsApp(): Boolean {
        // 检查华为特定设置
        val huaweiDefault = try {
            Settings.Secure.getString(this.contentResolver, "hsm_default_sms_app") == packageName
        } catch (e: Exception) {
            false
        }
        
        // 检查Android标准设置
        val androidDefault = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            packageName == Telephony.Sms.getDefaultSmsPackage(this)
        } else {
            true
        }
        
        return huaweiDefault || androidDefault
    }
}