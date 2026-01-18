package com.autodroid.guardiansdk.sms.receiver

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Telephony
import android.util.Log
import com.autodroid.guardiansdk.sms.service.SmsSenderService

class SmsStatusReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received intent: ${intent.action}")
        
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
        
        Log.d(TAG, "SMS sent status: address=$address, resultCode=$resultCode")
        
        val newType = when (resultCode) {
            android.app.Activity.RESULT_OK -> {
                Log.d(TAG, "SMS sent successfully")
                Telephony.Sms.MESSAGE_TYPE_SENT
            }
            else -> {
                Log.e(TAG, "SMS failed to send, resultCode: $resultCode")
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
        
        Log.d(TAG, "SMS delivered status: address=$address, resultCode=$resultCode")
        
        if (resultCode == android.app.Activity.RESULT_OK) {
            Log.d(TAG, "SMS delivered successfully")
        }
    }
    
    private fun updateSmsStatus(context: Context, messageId: Long, newType: Int) {
        if (messageId == -1L) return
        
        Log.d(TAG, "Updating SMS status: messageId=$messageId, newType=$newType")
        
        val values = ContentValues().apply {
            put(Telephony.Sms.TYPE, newType)
        }
        
        try {
            val uri = Uri.withAppendedPath(Telephony.Sms.CONTENT_URI, messageId.toString())
            val rowsUpdated = context.contentResolver.update(uri, values, null, null)
            Log.d(TAG, "SMS status updated, rows affected: $rowsUpdated")
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied when updating SMS status", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update SMS status", e)
        }
    }
    
    companion object {
        private const val TAG = "SmsStatusReceiver"
    }
}
