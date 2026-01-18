package com.autodroid.guardiansdk.sms.receiver

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsManager
import com.autodroid.guardiansdk.sms.service.SmsSenderService

class SmsSentReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        val address = intent.getStringExtra(SmsSenderService.EXTRA_ADDRESS) ?: return
        val body = intent.getStringExtra(SmsSenderService.EXTRA_BODY) ?: return
        
        when (resultCode) {
            android.app.Activity.RESULT_OK -> {
                updateSmsStatus(context, address, body, Telephony.Sms.STATUS_COMPLETE)
            }
            SmsManager.RESULT_ERROR_GENERIC_FAILURE -> {
                updateSmsStatus(context, address, body, Telephony.Sms.STATUS_FAILED)
            }
            SmsManager.RESULT_ERROR_NO_SERVICE -> {
                updateSmsStatus(context, address, body, Telephony.Sms.STATUS_FAILED)
            }
            SmsManager.RESULT_ERROR_NULL_PDU -> {
                updateSmsStatus(context, address, body, Telephony.Sms.STATUS_FAILED)
            }
            SmsManager.RESULT_ERROR_RADIO_OFF -> {
                updateSmsStatus(context, address, body, Telephony.Sms.STATUS_FAILED)
            }
        }
    }
    
    private fun updateSmsStatus(context: Context, address: String, body: String, status: Int) {
        try {
            val selection = "${Telephony.Sms.ADDRESS} = ? AND ${Telephony.Sms.BODY} = ? AND ${Telephony.Sms.TYPE} = ?"
            val selectionArgs = arrayOf(address, body, Telephony.Sms.MESSAGE_TYPE_OUTBOX.toString())
            
            val values = ContentValues().apply {
                put(Telephony.Sms.STATUS, status)
                if (status == Telephony.Sms.STATUS_COMPLETE) {
                    put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT)
                }
            }
            
            val rowsUpdated = context.contentResolver.update(
                Telephony.Sms.CONTENT_URI,
                values,
                selection,
                selectionArgs
            )
            
            if (rowsUpdated == 0 && status == Telephony.Sms.STATUS_COMPLETE) {
                saveSmsToDatabase(context, address, body, Telephony.Sms.MESSAGE_TYPE_SENT, Telephony.Sms.STATUS_COMPLETE)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun saveSmsToDatabase(context: Context, address: String, body: String, type: Int, status: Int) {
        try {
            val values = ContentValues().apply {
                put(Telephony.Sms.ADDRESS, address)
                put(Telephony.Sms.BODY, body)
                put(Telephony.Sms.DATE, System.currentTimeMillis())
                put(Telephony.Sms.READ, 0)
                put(Telephony.Sms.SEEN, 0)
                put(Telephony.Sms.TYPE, type)
                put(Telephony.Sms.STATUS, status)
            }
            
            context.contentResolver.insert(Telephony.Sms.CONTENT_URI, values)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
