package com.autodroid.guardiansdk.utils

import android.content.Context
import android.telephony.SmsManager
import android.util.Log

// 为不同Android版本提供兼容的SmsManager获取方式
@Suppress("DEPRECATION")
private fun Context.getCompatSmsManager(): SmsManager {
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        // Android 12+ 使用新的API
        getSystemService(Context.TELEPHONY_SERVICE) as? SmsManager 
            ?: SmsManager.getDefault()
    } else {
        // 旧版本使用getDefault()
        SmsManager.getDefault()
    }
}

/**
 * 短信发送工具类
 * 用于发送监护人相关的短信
 */
object SmsUtils {
    
    private const val TAG = "SmsUtils"
    
    /**
     * 发送监护人确认短信
     * @param context 上下文
     * @param phoneNumber 接收方手机号
     * @param wardName 被监护人姓名
     * @param wardPhone 被监护人手机号
     * @return 是否发送成功
     */
    fun sendGuardianConfirmationSms(
        context: Context,
        phoneNumber: String,
        wardName: String,
        wardPhone: String
    ): Boolean {
        return try {
            val smsManager = context.getCompatSmsManager()
            val message = "你是报警联系人。被监护人：$wardName，手机号：$wardPhone"
            
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            Log.d(TAG, "监护人确认短信已发送给: $phoneNumber")
            true
        } catch (e: Exception) {
            Log.e(TAG, "发送短信失败: ${e.message}")
            false
        }
    }
    
    /**
     * 发送紧急报警短信
     * @param context 上下文
     * @param phoneNumber 接收方手机号
     * @param location 当前位置
     * @param message 报警信息
     * @return 是否发送成功
     */
    fun sendEmergencySms(
        context: Context,
        phoneNumber: String,
        location: String,
        message: String
    ): Boolean {
        return try {
            val smsManager = context.getCompatSmsManager()
            val fullMessage = "紧急报警！位置：$location，信息：$message"
            
            smsManager.sendTextMessage(phoneNumber, null, fullMessage, null, null)
            Log.d(TAG, "紧急报警短信已发送给: $phoneNumber")
            true
        } catch (e: Exception) {
            Log.e(TAG, "发送紧急短信失败: ${e.message}")
            false
        }
    }
    
    /**
     * 批量发送短信
     * @param context 上下文
     * @param phoneNumbers 接收方手机号列表
     * @param message 短信内容
     * @return 成功发送的手机号列表
     */
    fun sendBatchSms(
        context: Context,
        phoneNumbers: List<String>,
        message: String
    ): List<String> {
        val successNumbers = mutableListOf<String>()
        
        phoneNumbers.forEach { phoneNumber ->
            if (sendTextMessage(context, phoneNumber, message)) {
                successNumbers.add(phoneNumber)
            }
        }
        
        return successNumbers
    }
    
    /**
     * 发送普通短信
     * @param context 上下文
     * @param phoneNumber 接收方手机号
     * @param message 短信内容
     * @return 是否发送成功
     */
    private fun sendTextMessage(
        context: Context,
        phoneNumber: String,
        message: String
    ): Boolean {
        return try {
            val smsManager = context.getCompatSmsManager()
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            Log.d(TAG, "短信已发送给: $phoneNumber")
            true
        } catch (e: Exception) {
            Log.e(TAG, "发送短信给 $phoneNumber 失败: ${e.message}")
            false
        }
    }
}