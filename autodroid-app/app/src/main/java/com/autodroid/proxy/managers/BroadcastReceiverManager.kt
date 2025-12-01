// BroadcastReceiverManager.java
package com.autodroid.proxy.managers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.autodroid.proxy.viewmodel.AppViewModel
import com.google.gson.Gson

class BroadcastReceiverManager(
    private val context: Context,
    private val viewModel: AppViewModel?,
    private val workflowManager: WorkflowManager?
) {
    private val gson: Gson

    private var serverInfoReceiver: BroadcastReceiver? = null
    private var workflowsReceiver: BroadcastReceiver? = null
    private val reportsReceiver: BroadcastReceiver? = null
    private val executionReceiver: BroadcastReceiver? = null
    private val matchedWorkflowsReceiver: BroadcastReceiver? = null

    interface BroadcastListener {
        fun onServerInfoReceived(serverInfoJson: String?)
        fun onWorkflowsReceived(workflowsJson: String?)
        fun onReportsReceived(reportsJson: String?)
        fun onExecutionReceived(executionJson: String?)
        fun onMatchedWorkflowsReceived(matchedWorkflowsJson: String?)
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

        workflowsReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                val workflowsJson = intent.getStringExtra("workflows")
                if (listener != null && workflowsJson != null) {
                    listener!!.onWorkflowsReceived(workflowsJson)
                }
            }
        }

        // Register other receivers similarly...
        context.registerReceiver(
            serverInfoReceiver,
            IntentFilter("com.autodroid.proxy.SERVER_INFO_UPDATE")
        )
        context.registerReceiver(
            workflowsReceiver,
            IntentFilter("com.autodroid.proxy.WORKFLOWS_UPDATE")
        )
        // Register other intent filters...
    }

    fun unregisterReceivers() {
        if (serverInfoReceiver != null) {
            context.unregisterReceiver(serverInfoReceiver)
        }
        if (workflowsReceiver != null) {
            context.unregisterReceiver(workflowsReceiver)
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