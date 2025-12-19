// BroadcastReceiverManager.kt
package com.autodroid.trader.managers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.core.content.ContextCompat
import com.autodroid.trader.AppViewModel
import com.google.gson.Gson

class BroadcastReceiverManager(
    private val context: Context,
    private val viewModel: AppViewModel?,
    private val tradePlanManager: TradePlanManager?
) {
    private val gson: Gson

    private var serverInfoReceiver: BroadcastReceiver? = null
    private var tradePlansReceiver: BroadcastReceiver? = null
    
    private val executionReceiver: BroadcastReceiver? = null
    private val matchedTradePlansReceiver: BroadcastReceiver? = null

    interface BroadcastListener {
        fun onServerInfoReceived(serverInfoJson: String?)
        fun onTradePlansReceived(tradePlansJson: String?)
        
        fun onExecutionReceived(executionJson: String?)
        fun onMatchedTradePlansReceived(matchedTradePlansJson: String?)
    }

    private var listener: BroadcastListener? = null

    init {
        this.gson = Gson()
    }

    fun setListener(listener: BroadcastListener?) {
        this.listener = listener
    }

    fun registerReceivers() {
        serverInfoReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                val serverInfoJson = intent.getStringExtra("server_info")
                if (listener != null && serverInfoJson != null) {
                    listener!!.onServerInfoReceived(serverInfoJson)
                }
            }
        }

        tradePlansReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                val tradePlansJson = intent.getStringExtra("tradeplans")
                if (listener != null && tradePlansJson != null) {
                    listener!!.onTradePlansReceived(tradePlansJson)
                }
            }
        }

        // Register other receivers similarly...
        ContextCompat.registerReceiver(
            context,
            serverInfoReceiver,
            IntentFilter("com.autodroid.trader.SERVER_INFO_UPDATE"),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        ContextCompat.registerReceiver(
            context,
            tradePlansReceiver,
            IntentFilter("com.autodroid.trader.TRADEPLANS_UPDATE"),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        // Register other intent filters...
    }

    fun unregisterReceivers() {
        if (serverInfoReceiver != null) {
            context.unregisterReceiver(serverInfoReceiver)
        }
        if (tradePlansReceiver != null) {
            context.unregisterReceiver(tradePlansReceiver)
        }
        // Unregister other receivers...
    }

    fun handleServerInfo(serverInfoJson: String?) {
        try {
            // Handle server info parsing and update ViewModel
            // This can be moved from MainActivity
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle server info: " + e.message)
        }
    }

    companion object {
        private const val TAG = "BroadcastReceiverManager"
    }
}