package com.autodroid.sms.service

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.telephony.SmsManager

/**
 * 短信响应服务
 * 用于响应"通过消息回复"功能
 */
class SmsResponseService : Service() {
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "android.intent.action.RESPOND_VIA_MESSAGE" -> {
                handleRespondViaMessage(intent)
            }
        }
        
        stopSelf(startId)
        return START_NOT_STICKY
    }
    
    /**
     * 处理"通过消息回复"请求
     */
    private fun handleRespondViaMessage(intent: Intent) {
        val uri = intent.data
        if (uri != null) {
            val address = getRecipientFromUri(uri)
            val message = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
            
            if (address.isNotEmpty() && message.isNotEmpty()) {
                // 发送回复消息
                SmsManager.getDefault().sendTextMessage(address, null, message, null, null)
            }
        }
    }
    
    /**
     * 从URI中提取收件人地址
     */
    private fun getRecipientFromUri(uri: Uri): String {
        return when (uri.scheme) {
            "sms", "smsto" -> uri.schemeSpecificPart ?: ""
            "mms", "mmsto" -> uri.schemeSpecificPart ?: ""
            else -> ""
        }
    }
}