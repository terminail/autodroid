package com.autodroid.sms.receiver

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Telephony
import android.util.Log
import com.autodroid.sms.service.SmsSenderService

/**
 * 短信发送状态接收器
 * 处理短信发送成功/失败状态，并更新系统短信数据库
 */
class SmsStatusReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("SmsStatusReceiver", "Received intent: ${intent.action}")
        
        when (intent.action) {
            SmsSenderService.ACTION_SMS_SENT -> {
                handleSmsSent(context, intent)
            }
            SmsSenderService.ACTION_SMS_DELIVERED -> {
                handleSmsDelivered(context, intent)
            }
        }
    }
    
    private fun handleSmsSent(context: Context, intent: Intent) {
        val address = intent.getStringExtra(SmsSenderService.EXTRA_ADDRESS) ?: return
        val body = intent.getStringExtra(SmsSenderService.EXTRA_BODY) ?: return
        val messageId = intent.getLongExtra(SmsSenderService.EXTRA_MESSAGE_ID, -1L)
        val resultCode = resultCode
        
        Log.d("SmsStatusReceiver", "SMS sent status: address=$address, resultCode=$resultCode")
        
        // 根据发送结果更新短信状态
        val newType = when (resultCode) {
            android.app.Activity.RESULT_OK -> {
                Log.d("SmsStatusReceiver", "SMS sent successfully")
                Telephony.Sms.MESSAGE_TYPE_SENT
            }
            else -> {
                Log.e("SmsStatusReceiver", "SMS failed to send, resultCode: $resultCode")
                Telephony.Sms.MESSAGE_TYPE_FAILED
            }
        }
        
        updateSmsStatus(context, messageId, newType)
    }
    
    private fun handleSmsDelivered(context: Context, intent: Intent) {
        val address = intent.getStringExtra(SmsSenderService.EXTRA_ADDRESS) ?: return
        val body = intent.getStringExtra(SmsSenderService.EXTRA_BODY) ?: return
        val messageId = intent.getLongExtra(SmsSenderService.EXTRA_MESSAGE_ID, -1L)
        val resultCode = resultCode
        
        Log.d("SmsStatusReceiver", "SMS delivered status: address=$address, resultCode=$resultCode")
        
        if (resultCode == android.app.Activity.RESULT_OK) {
            Log.d("SmsStatusReceiver", "SMS delivered successfully")
            // 可以在这里处理送达状态，但通常不需要更新系统数据库
            // 因为系统会自动处理送达状态
        }
    }
    
    private fun updateSmsStatus(context: Context, messageId: Long, newType: Int) {
        if (messageId == -1L) return
        
        Log.d("SmsStatusReceiver", "Updating SMS status: messageId=$messageId, newType=$newType")
        
        val values = ContentValues().apply {
            put(Telephony.Sms.TYPE, newType)
        }
        
        try {
            val uri = Uri.withAppendedPath(Telephony.Sms.CONTENT_URI, messageId.toString())
            val rowsUpdated = context.contentResolver.update(uri, values, null, null)
            Log.d("SmsStatusReceiver", "SMS status updated, rows affected: $rowsUpdated")
        } catch (e: SecurityException) {
            Log.e("SmsStatusReceiver", "Permission denied when updating SMS status", e)
        } catch (e: Exception) {
            Log.e("SmsStatusReceiver", "Failed to update SMS status", e)
        }
    }
}