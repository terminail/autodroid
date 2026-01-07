package com.autodroid.aas.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class UIEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "com.autodroid.aas.NEW_EVENT" -> {
                val packageName = intent.getStringExtra("package_name")
                val eventType = intent.getStringExtra("event_type")
                Toast.makeText(context, "New event: $eventType in $packageName", Toast.LENGTH_SHORT).show()
            }
            "com.autodroid.aas.SERVICE_STATUS" -> {
                val isRunning = intent.getBooleanExtra("is_running", false)
                val status = if (isRunning) "started" else "stopped"
                Toast.makeText(context, "Service $status", Toast.LENGTH_SHORT).show()
            }
        }
    }
}