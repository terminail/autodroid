// BroadcastReceiverManager.kt
package com.autodroid.manager.managers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.autodroid.manager.AppViewModel
import com.google.gson.Gson

class BroadcastReceiverManager(
    private val context: Context,
    private val viewModel: AppViewModel?,
    private val workScriptManager: WorkScriptManager?
) {
    private val gson: Gson

    private var serverInfoReceiver: BroadcastReceiver? = null
    private var workScriptsReceiver: BroadcastReceiver? = null
    private val reportsReceiver: BroadcastReceiver? = null
    private val executionReceiver: BroadcastReceiver? = null
    private val matchedWorkScriptsReceiver: BroadcastReceiver? = null

    interface BroadcastListener {
        fun onServerInfoReceived(serverInfoJson: String?)
        fun onWorkScriptsReceived(workScriptsJson: String?)
        fun onReportsReceived(reportsJson: String?)
        fun onExecutionReceived(executionJson: String?)
        fun onMatchedWorkScriptsReceived(matchedWorkScriptsJson: String?)
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

        workScriptsReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                val workScriptsJson = intent.getStringExtra("workscripts")
                if (listener != null && workScriptsJson != null) {
                    listener!!.onWorkScriptsReceived(workScriptsJson)
                }
            }
        }

        // Register other receivers similarly...
        context.registerReceiver(
            serverInfoReceiver,
            IntentFilter("com.autodroid.manager.SERVER_INFO_UPDATE")
        )
        context.registerReceiver(
            workScriptsReceiver,
            IntentFilter("com.autodroid.manager.WORKSCRIPTS_UPDATE")
        )
        // Register other intent filters...
    }

    fun unregisterReceivers() {
        if (serverInfoReceiver != null) {
            context.unregisterReceiver(serverInfoReceiver)
        }
        if (workScriptsReceiver != null) {
            context.unregisterReceiver(workScriptsReceiver)
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