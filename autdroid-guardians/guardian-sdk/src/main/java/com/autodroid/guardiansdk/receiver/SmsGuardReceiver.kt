package com.autodroid.guardiansdk.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.telephony.SmsMessage
import android.util.Log

/**
 * 短信守卫接收器
 * 监听特定关键词短信并触发报警
 */
class SmsGuardReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "SmsGuardReceiver"
        
        // 监控的短信关键词
        private val MONITOR_KEYWORDS = listOf(
            "紧急报警",
            "紧急模式",
            "guardian",
            "报警"
        )
    }
    
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent?.action != "android.provider.Telephony.SMS_RECEIVED") {
            return
        }
        
        val bundle = intent.extras ?: return
        
        try {
            val pdus = bundle.get("pdus") as Array<*>?
            if (pdus != null) {
                for (pdu in pdus) {
                    val smsMessage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        SmsMessage.createFromPdu(pdu as ByteArray, bundle.getString("format"))
                    } else {
                        @Suppress("DEPRECATION")
                        SmsMessage.createFromPdu(pdu as ByteArray)
                    }
                    val messageBody = smsMessage.messageBody
                    val sender = smsMessage.originatingAddress
                    
                    Log.d(TAG, "收到短信 - 发件人: $sender, 内容: $messageBody")
                    
                    // 检查是否包含监控关键词
                    val containsKeyword = MONITOR_KEYWORDS.any { keyword ->
                        messageBody.contains(keyword, ignoreCase = true)
                    }
                    
                    if (containsKeyword) {
                        Log.d(TAG, "检测到关键词短信，触发报警逻辑")
                        
                        // 触发紧急报警
                        triggerEmergencyAlarm(context, sender, messageBody)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "处理短信时出错: ${e.message}")
        }
    }
    
    /**
     * 触发紧急报警
     */
    private fun triggerEmergencyAlarm(context: Context, sender: String?, message: String) {
        // 启动紧急服务
        val intent = Intent(context, com.autodroid.guardiansdk.service.EmergencyService::class.java)
        intent.putExtra("sender", sender)
        intent.putExtra("message", message)
        context.startService(intent)
        
        Log.d(TAG, "紧急报警已触发")
    }
}
