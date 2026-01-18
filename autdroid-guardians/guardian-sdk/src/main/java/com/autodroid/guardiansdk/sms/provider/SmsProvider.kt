package com.autodroid.guardiansdk.sms.provider

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log
import com.autodroid.guardiansdk.sms.model.SmsMessage
import com.autodroid.guardiansdk.sms.model.Conversation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

class SmsProvider(private val context: Context) {
    
    private val contentResolver: ContentResolver = context.contentResolver
    
    companion object {
        private const val TAG = "SmsProvider"
        private val SMS_URI: Uri = Telephony.Sms.CONTENT_URI
        private val SMS_INBOX_URI: Uri = Telephony.Sms.Inbox.CONTENT_URI
        private val SMS_SENT_URI: Uri = Telephony.Sms.Sent.CONTENT_URI
        private val SMS_CONVERSATIONS_URI: Uri = Telephony.Sms.Conversations.CONTENT_URI
        private val MMS_URI: Uri = Telephony.Mms.CONTENT_URI
        private val MMS_INBOX_URI: Uri = Telephony.Mms.Inbox.CONTENT_URI
        private val MMS_SENT_URI: Uri = Telephony.Mms.Sent.CONTENT_URI
    }
    
    suspend fun getAllConversations(): List<Conversation> = withContext(Dispatchers.IO) {
        val conversations = mutableListOf<Conversation>()
        
        try {
            val cursor: Cursor? = contentResolver.query(
                SMS_URI,
                null,
                null,
                null,
                Telephony.Sms.DEFAULT_SORT_ORDER
            )
            
            cursor?.use { c ->
                val idIndex = c.getColumnIndex(Telephony.Sms._ID)
                val threadIdIndex = c.getColumnIndex(Telephony.Sms.THREAD_ID)
                val addressIndex = c.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIndex = c.getColumnIndex(Telephony.Sms.BODY)
                val dateIndex = c.getColumnIndex(Telephony.Sms.DATE)
                val typeIndex = c.getColumnIndex(Telephony.Sms.TYPE)
                
                val threadMap = mutableMapOf<Long, Conversation>()
                
                if (c.count > 0) {
                    while (c.moveToNext()) {
                        val threadId = if (threadIdIndex >= 0) c.getLong(threadIdIndex) else 0
                        val address = if (addressIndex >= 0) c.getString(addressIndex) else ""
                        val body = if (bodyIndex >= 0) c.getString(bodyIndex) else ""
                        val date = if (dateIndex >= 0) Date(c.getLong(dateIndex)) else Date()
                        val type = if (typeIndex >= 0) c.getInt(typeIndex) else 0
                        
                        val conversationKey = if (threadId != 0L) threadId else address.hashCode().toLong()
                        
                        if (!threadMap.containsKey(conversationKey)) {
                            try {
                                val conversation = Conversation(
                                    threadId = conversationKey,
                                    address = address ?: "",
                                    snippet = body ?: "",
                                    date = date,
                                    messageCount = 1
                                )
                                threadMap[conversationKey] = conversation
                            } catch (e: Exception) {
                                Log.e(TAG, "Error creating conversation: threadId=$threadId, address=$address, body=$body", e)
                            }
                        } else {
                            val existing = threadMap[conversationKey]
                            if (existing != null) {
                                try {
                                    existing.messageCount++
                                    existing.snippet = body
                                    existing.date = date
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error updating conversation", e)
                                }
                            }
                        }
                    }
                }
                
                conversations.addAll(threadMap.values.sortedByDescending { it.date })
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取会话列表失败", e)
        }
        
        conversations
    }
    
    suspend fun getMessagesByThread(threadId: Long): List<SmsMessage> = withContext(Dispatchers.IO) {
        val messages = mutableListOf<SmsMessage>()
        
        val selection = "${Telephony.Sms.THREAD_ID} = ?"
        val selectionArgs = arrayOf(threadId.toString())
        
        val cursor: Cursor? = contentResolver.query(
            SMS_URI,
            null,
            selection,
            selectionArgs,
            Telephony.Sms.DEFAULT_SORT_ORDER
        )
        
        cursor?.use { c ->
            val idIndex = c.getColumnIndex(Telephony.Sms._ID)
            val addressIndex = c.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyIndex = c.getColumnIndex(Telephony.Sms.BODY)
            val dateIndex = c.getColumnIndex(Telephony.Sms.DATE)
            val typeIndex = c.getColumnIndex(Telephony.Sms.TYPE)
            val readIndex = c.getColumnIndex(Telephony.Sms.READ)
            
            while (c.moveToNext()) {
                val message = SmsMessage(
                    id = c.getLong(idIndex),
                    threadId = threadId,
                    address = c.getString(addressIndex) ?: "",
                    body = c.getString(bodyIndex) ?: "",
                    date = Date(c.getLong(dateIndex)),
                    type = c.getInt(typeIndex),
                    read = c.getInt(readIndex) == 1
                )
                messages.add(message)
            }
        }
        
        messages
    }
    
    suspend fun getMessagesByAddress(address: String): List<SmsMessage> = withContext(Dispatchers.IO) {
        val messages = mutableListOf<SmsMessage>()
        
        val selection = "${Telephony.Sms.ADDRESS} = ?"
        val selectionArgs = arrayOf(address)
        
        val cursor: Cursor? = contentResolver.query(
            SMS_URI,
            null,
            selection,
            selectionArgs,
            Telephony.Sms.DEFAULT_SORT_ORDER
        )
        
        cursor?.use { c ->
            val idIndex = c.getColumnIndex(Telephony.Sms._ID)
            val addressIndex = c.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyIndex = c.getColumnIndex(Telephony.Sms.BODY)
            val dateIndex = c.getColumnIndex(Telephony.Sms.DATE)
            val typeIndex = c.getColumnIndex(Telephony.Sms.TYPE)
            val readIndex = c.getColumnIndex(Telephony.Sms.READ)
            val threadIdIndex = c.getColumnIndex(Telephony.Sms.THREAD_ID)
            
            while (c.moveToNext()) {
                val message = SmsMessage(
                    id = c.getLong(idIndex),
                    threadId = if (threadIdIndex >= 0) c.getLong(threadIdIndex) else 0,
                    address = c.getString(addressIndex) ?: "",
                    body = c.getString(bodyIndex) ?: "",
                    date = Date(c.getLong(dateIndex)),
                    type = c.getInt(typeIndex),
                    read = c.getInt(readIndex) == 1
                )
                messages.add(message)
            }
        }
        
        messages
    }
    
    suspend fun sendSms(address: String, body: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(address, null, body, null, null)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    suspend fun markMessageAsRead(messageId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val values = ContentValues().apply {
                put(Telephony.Sms.READ, 1)
            }
            
            val uri = Uri.withAppendedPath(SMS_URI, messageId.toString())
            val rowsUpdated = contentResolver.update(uri, values, null, null)
            
            rowsUpdated > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    suspend fun markThreadAsRead(threadId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val values = ContentValues().apply {
                put(Telephony.Sms.READ, 1)
            }
            
            val selection = "${Telephony.Sms.THREAD_ID} = ? AND ${Telephony.Sms.READ} = 0"
            val selectionArgs = arrayOf(threadId.toString())
            
            val rowsUpdated = contentResolver.update(SMS_URI, values, selection, selectionArgs)
            
            rowsUpdated > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    suspend fun deleteMessage(messageId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.withAppendedPath(SMS_URI, messageId.toString())
            val rowsDeleted = contentResolver.delete(uri, null, null)
            
            rowsDeleted > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    suspend fun deleteThread(threadId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val selection = "${Telephony.Sms.THREAD_ID} = ?"
            val selectionArgs = arrayOf(threadId.toString())
            
            val rowsDeleted = contentResolver.delete(SMS_URI, selection, selectionArgs)
            
            rowsDeleted > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
