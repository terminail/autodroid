package com.autodroid.guardiansdk.sms.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log

class SmsReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Telephony.Sms.Intents.SMS_DELIVER_ACTION -> {
                handleSmsReceived(context, intent)
            }
            Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION -> {
                handleMmsReceived(context, intent)
            }
        }
    }
    
    private fun handleSmsReceived(context: Context, intent: Intent) {
        try {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            
            for (message in messages) {
                val address = message.originatingAddress
                val body = message.messageBody
                
                Log.d(TAG, "Received SMS from $address: $body")
                
                val sdkIntent = Intent(ACTION_SMS_RECEIVED).apply {
                    putExtra(EXTRA_ADDRESS, address)
                    putExtra(EXTRA_BODY, body)
                    putExtra(EXTRA_TIMESTAMP, message.timestampMillis)
                }
                context.sendBroadcast(sdkIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing SMS", e)
        }
    }
    
    private fun handleMmsReceived(context: Context, intent: Intent) {
        try {
            Log.d(TAG, "Received MMS")
            
            val sdkIntent = Intent(ACTION_MMS_RECEIVED).apply {
                putExtra(EXTRA_MMS_URI, intent.data?.toString())
            }
            context.sendBroadcast(sdkIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error processing MMS", e)
        }
    }
    
    companion object {
        private const val TAG = "SmsReceiver"
        const val ACTION_SMS_RECEIVED = "com.autodroid.guardiansdk.sms.action.SMS_RECEIVED"
        const val ACTION_MMS_RECEIVED = "com.autodroid.guardiansdk.sms.action.MMS_RECEIVED"
        const val EXTRA_ADDRESS = "extra_address"
        const val EXTRA_BODY = "extra_body"
        const val EXTRA_TIMESTAMP = "extra_timestamp"
        const val EXTRA_MMS_URI = "extra_mms_uri"
    }
}
