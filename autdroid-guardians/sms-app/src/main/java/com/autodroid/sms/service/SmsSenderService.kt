package com.autodroid.sms.service

import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 短信发送服务
 * 使用 Android 标准 SMS API 发送短信，让系统自动处理短信存储
 */
class SmsSenderService : Service() {
    
    private val smsManager by lazy {
        SmsManager.getDefault()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("SmsSenderService", "onStartCommand called, action: ${intent?.action}")
        
        when (intent?.action) {
            ACTION_SEND_SMS -> {
                val address = intent.getStringExtra(EXTRA_ADDRESS) ?: return START_NOT_STICKY
                val body = intent.getStringExtra(EXTRA_BODY) ?: return START_NOT_STICKY
                Log.d("SmsSenderService", "Sending SMS to $address: $body")
                sendSms(address, body, startId)
            }
            ACTION_SEND_MMS -> {
                val address = intent.getStringExtra(EXTRA_ADDRESS) ?: return START_NOT_STICKY
                val subject = intent.getStringExtra(EXTRA_SUBJECT)
                val body = intent.getStringExtra(EXTRA_BODY)
                val uri = intent.getStringExtra(EXTRA_MMS_URI)
                sendMms(address, subject, body, uri, startId)
            }
            else -> {
                Log.w("SmsSenderService", "Unknown action: ${intent?.action}")
            }
        }
        
        return START_NOT_STICKY
    }
    
    private fun sendSms(address: String, body: String, startId: Int) {
        Log.d("SmsSenderService", "sendSms called for $address: $body")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 先保存短信到系统数据库（发送中状态）
                val sentMessageId = saveSmsToDatabase(address, body, Telephony.Sms.MESSAGE_TYPE_SENT)
                
                Log.d("SmsSenderService", "Creating pending intents")
                val sentIntent = createSentIntent(address, body, sentMessageId)
                val deliveredIntent = createDeliveredIntent(address, body, sentMessageId)
                
                Log.d("SmsSenderService", "Calling smsManager.sendTextMessage")
                smsManager.sendTextMessage(
                    address,
                    null,
                    body,
                    sentIntent,
                    deliveredIntent
                )
                Log.d("SmsSenderService", "SMS sent successfully")
                
                // 发送成功，更新短信状态为已发送
                updateSmsStatus(sentMessageId, Telephony.Sms.MESSAGE_TYPE_SENT)
                
            } catch (e: Exception) {
                Log.e("SmsSenderService", "Failed to send SMS", e)
                // 发送失败，保存失败状态的短信到数据库
                saveSmsToDatabase(address, body, Telephony.Sms.MESSAGE_TYPE_FAILED)
                
                // 发送失败广播
                sendBroadcast(Intent(ACTION_SMS_SENT).apply {
                    putExtra(EXTRA_ADDRESS, address)
                    putExtra(EXTRA_BODY, body)
                    putExtra(EXTRA_SEND_RESULT, RESULT_ERROR)
                })
            } finally {
                stopSelf(startId)
            }
        }
    }
    
    private fun createSentIntent(address: String, body: String, messageId: Long): PendingIntent {
        val intent = Intent(ACTION_SMS_SENT).apply {
            putExtra(EXTRA_ADDRESS, address)
            putExtra(EXTRA_BODY, body)
            putExtra(EXTRA_MESSAGE_ID, messageId)
            `package` = packageName
        }
        return PendingIntent.getBroadcast(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    
    private fun createDeliveredIntent(address: String, body: String, messageId: Long): PendingIntent {
        val intent = Intent(ACTION_SMS_DELIVERED).apply {
            putExtra(EXTRA_ADDRESS, address)
            putExtra(EXTRA_BODY, body)
            putExtra(EXTRA_MESSAGE_ID, messageId)
            `package` = packageName
        }
        return PendingIntent.getBroadcast(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    
    private fun saveSmsToDatabase(address: String, body: String, type: Int): Long {
        Log.d("SmsSenderService", "Saving SMS to database: address=$address, body=$body, type=$type")
        
        val values = ContentValues().apply {
            put(Telephony.Sms.ADDRESS, address)
            put(Telephony.Sms.BODY, body)
            put(Telephony.Sms.DATE, System.currentTimeMillis())
            put(Telephony.Sms.READ, 1)
            put(Telephony.Sms.TYPE, type)
        }
        
        try {
            val uri = contentResolver.insert(Telephony.Sms.CONTENT_URI, values)
            val messageId = uri?.lastPathSegment?.toLongOrNull() ?: -1L
            Log.d("SmsSenderService", "SMS saved to database with ID: $messageId")
            return messageId
        } catch (e: SecurityException) {
            Log.e("SmsSenderService", "Permission denied when saving SMS to database", e)
        } catch (e: Exception) {
            Log.e("SmsSenderService", "Failed to save SMS to database", e)
        }
        
        return -1L
    }
    
    private fun updateSmsStatus(messageId: Long, newType: Int) {
        if (messageId == -1L) return
        
        Log.d("SmsSenderService", "Updating SMS status: messageId=$messageId, newType=$newType")
        
        val values = ContentValues().apply {
            put(Telephony.Sms.TYPE, newType)
        }
        
        try {
            val uri = Uri.withAppendedPath(Telephony.Sms.CONTENT_URI, messageId.toString())
            val rowsUpdated = contentResolver.update(uri, values, null, null)
            Log.d("SmsSenderService", "SMS status updated, rows affected: $rowsUpdated")
        } catch (e: SecurityException) {
            Log.e("SmsSenderService", "Permission denied when updating SMS status", e)
        } catch (e: Exception) {
            Log.e("SmsSenderService", "Failed to update SMS status", e)
        }
    }
    
    private fun sendMms(address: String, subject: String?, body: String?, uri: String?, startId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                stopSelf(startId)
            }
        }
    }
    
    companion object {
        const val ACTION_SEND_SMS = "com.autodroid.sms.action.SEND_SMS"
        const val ACTION_SEND_MMS = "com.autodroid.sms.action.SEND_MMS"
        const val ACTION_SMS_SENT = "com.autodroid.sms.action.SMS_SENT"
        const val ACTION_SMS_DELIVERED = "com.autodroid.sms.action.SMS_DELIVERED"
        const val EXTRA_ADDRESS = "extra_address"
        const val EXTRA_BODY = "extra_body"
        const val EXTRA_SUBJECT = "extra_subject"
        const val EXTRA_MMS_URI = "extra_mms_uri"
        const val EXTRA_SEND_RESULT = "extra_send_result"
        const val EXTRA_MESSAGE_ID = "extra_message_id"
        
        const val RESULT_SUCCESS = 0
        const val RESULT_ERROR = 1
        
        fun createSendSmsIntent(context: Context, address: String, body: String): Intent {
            return Intent(context, SmsSenderService::class.java).apply {
                action = ACTION_SEND_SMS
                putExtra(EXTRA_ADDRESS, address)
                putExtra(EXTRA_BODY, body)
            }
        }
        
        fun createSendMmsIntent(context: Context, address: String, subject: String?, body: String?, uri: String?): Intent {
            return Intent(context, SmsSenderService::class.java).apply {
                action = ACTION_SEND_MMS
                putExtra(EXTRA_ADDRESS, address)
                putExtra(EXTRA_SUBJECT, subject)
                putExtra(EXTRA_BODY, body)
                putExtra(EXTRA_MMS_URI, uri)
            }
        }
    }
}