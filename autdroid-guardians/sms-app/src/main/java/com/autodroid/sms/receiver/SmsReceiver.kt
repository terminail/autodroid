package com.autodroid.sms.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsMessage
import com.autodroid.sms.SmsApplication

/**
 * 短信接收器
 * 接收和处理收到的短信
 * 现在使用系统短信数据库，短信会自动保存，这里主要用于通知UI更新
 */
class SmsReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (!SmsApplication.instance.isDefaultSmsApp()) {
            // 如果不是默认短信应用，不处理短信
            return
        }
        
        when (intent.action) {
            Telephony.Sms.Intents.SMS_DELIVER_ACTION -> {
                handleSmsReceived(context, intent)
            }
            Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION -> {
                handleMmsReceived(context, intent)
            }
        }
    }
    
    /**
     * 处理收到的短信
     */
    private fun handleSmsReceived(context: Context, intent: Intent) {
        // 系统会自动保存短信到系统数据库
        // 这里可以发送广播通知UI更新
        val updateIntent = Intent("com.autodroid.sms.action.SMS_RECEIVED")
        context.sendBroadcast(updateIntent)
    }
    
    /**
     * 处理收到的彩信
     */
    private fun handleMmsReceived(context: Context, intent: Intent) {
        // 系统会自动保存彩信到系统数据库
        // 这里可以发送广播通知UI更新
        val updateIntent = Intent("com.autodroid.sms.action.MMS_RECEIVED")
        context.sendBroadcast(updateIntent)
    }
    
    /**
     * 兼容旧版本Android获取短信
     */
    private fun getMessagesFromIntentLegacy(intent: Intent): Array<SmsMessage>? {
        val pdus = intent.getSerializableExtra("pdus") as? Array<ByteArray>
        return pdus?.map { pdu ->
            SmsMessage.createFromPdu(pdu)
        }?.toTypedArray()
    }
}