package com.autodroid.guardiansdk.sms.service

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

class SmsSenderService : Service() {
    
    private val smsManager by lazy {
        SmsManager.getDefault()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SEND_SMS -> {
                val address = intent.getStringExtra(EXTRA_ADDRESS) ?: return START_NOT_STICKY
                val body = intent.getStringExtra(EXTRA_BODY) ?: return START_NOT_STICKY
                sendSms(address, body, startId)
            }
        }
        
        return START_NOT_STICKY
    }
    
    private fun sendSms(address: String, body: String, startId: Int) {
        Log.d(TAG, "sendSms called: address=$address, body=$body")
        CoroutineScope(Dispatchers.IO).launch {
            var messageId: Long = -1
            
            try {
                Log.d(TAG, "Getting or creating thread ID for address: $address")
                val threadId = getOrCreateThreadId(address)
                Log.d(TAG, "Thread ID: $threadId")
                
                Log.d(TAG, "Saving SMS to database as draft")
                messageId = saveSmsToDatabase(address, body, Telephony.Sms.MESSAGE_TYPE_DRAFT, threadId)
                Log.d(TAG, "Message ID: $messageId")
                
                val sentIntent = createSentIntent(address, body, messageId)
                val deliveredIntent = createDeliveredIntent(address, body, messageId)
                
                Log.d(TAG, "Sending SMS via SmsManager")
                smsManager.sendTextMessage(
                    address,
                    null,
                    body,
                    sentIntent,
                    deliveredIntent
                )
                
                Log.d(TAG, "SMS sent successfully, updating status to SENT")
                updateSmsStatus(messageId, Telephony.Sms.MESSAGE_TYPE_SENT)
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send SMS", e)
                
                if (messageId == -1L) {
                    Log.d(TAG, "Message not saved yet, saving as FAILED")
                    val threadId = getOrCreateThreadId(address)
                    saveSmsToDatabase(address, body, Telephony.Sms.MESSAGE_TYPE_FAILED, threadId)
                } else {
                    Log.d(TAG, "Message already saved, updating status to FAILED")
                    updateSmsStatus(messageId, Telephony.Sms.MESSAGE_TYPE_FAILED)
                }
                
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
    
    private fun saveSmsToDatabase(address: String, body: String, type: Int, threadId: Long): Long {
        val values = ContentValues().apply {
            put(Telephony.Sms.ADDRESS, address)
            put(Telephony.Sms.BODY, body)
            put(Telephony.Sms.DATE, System.currentTimeMillis())
            put(Telephony.Sms.READ, 1)
            put(Telephony.Sms.TYPE, type)
            if (threadId > 0) {
                put(Telephony.Sms.THREAD_ID, threadId)
            }
        }
        
        try {
            Log.d(TAG, "Inserting SMS into database: address=$address, body=$body, type=$type, threadId=$threadId")
            Log.d(TAG, "Checking if app is default SMS app...")
            val isDefault = Telephony.Sms.getDefaultSmsPackage(this) == packageName
            Log.d(TAG, "Is default SMS app: $isDefault")
            
            val uri = contentResolver.insert(Telephony.Sms.CONTENT_URI, values)
            val messageId = uri?.lastPathSegment?.toLongOrNull() ?: -1L
            Log.d(TAG, "Insert result: uri=$uri, messageId=$messageId")
            
            if (messageId <= 0) {
                Log.e(TAG, "Failed to insert SMS - app may not be default SMS app")
            }
            
            return messageId
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save SMS to database", e)
            return -1L
        }
    }
    
    private fun getOrCreateThreadId(address: String): Long {
        try {
            val uri = Telephony.Sms.CONTENT_URI
            val projection = arrayOf(Telephony.Sms.THREAD_ID)
            val selection = "${Telephony.Sms.ADDRESS} = ?"
            val selectionArgs = arrayOf(address)
            val sortOrder = "${Telephony.Sms.DATE} DESC LIMIT 1"
            
            Log.d(TAG, "Querying for existing thread ID from SMS: address=$address")
            
            contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                Log.d(TAG, "Query result count: ${cursor.count}")
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(Telephony.Sms.THREAD_ID)
                    if (index >= 0) {
                        val threadId = cursor.getLong(index)
                        if (threadId > 0) {
                            Log.d(TAG, "Found existing thread ID: $threadId")
                            return threadId
                        }
                    }
                }
            }
            
            Log.d(TAG, "No valid thread ID found, using address hash")
            val threadId = address.hashCode().toLong()
            Log.d(TAG, "Using thread ID from hash: $threadId")
            return threadId
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get or create thread ID", e)
            return address.hashCode().toLong()
        }
    }
    
    private fun updateSmsStatus(messageId: Long, newType: Int) {
        if (messageId == -1L) return
        
        val values = ContentValues().apply {
            put(Telephony.Sms.TYPE, newType)
        }
        
        try {
            val uri = Uri.withAppendedPath(Telephony.Sms.CONTENT_URI, messageId.toString())
            contentResolver.update(uri, values, null, null)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update SMS status", e)
        }
    }
    
    companion object {
        private const val TAG = "SmsSenderService"
        const val ACTION_SEND_SMS = "com.autodroid.guardiansdk.sms.action.SEND_SMS"
        const val ACTION_SMS_SENT = "com.autodroid.guardiansdk.sms.action.SMS_SENT"
        const val ACTION_SMS_DELIVERED = "com.autodroid.guardiansdk.sms.action.SMS_DELIVERED"
        const val EXTRA_ADDRESS = "extra_address"
        const val EXTRA_BODY = "extra_body"
        const val EXTRA_MESSAGE_ID = "extra_message_id"
        const val EXTRA_SEND_RESULT = "extra_send_result"
        
        const val RESULT_SUCCESS = 0
        const val RESULT_ERROR = 1
        
        fun createSendSmsIntent(context: Context, address: String, body: String): Intent {
            return Intent(context, SmsSenderService::class.java).apply {
                action = ACTION_SEND_SMS
                putExtra(EXTRA_ADDRESS, address)
                putExtra(EXTRA_BODY, body)
            }
        }
    }
}
