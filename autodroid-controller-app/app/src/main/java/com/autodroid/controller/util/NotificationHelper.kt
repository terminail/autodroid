package com.autodroid.controller.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.autodroid.controller.MainActivity

object NotificationHelper {
    const val SERVICE_CHANNEL_ID = "autodroid_service_channel"
    const val SERVICE_NOTIFICATION_ID = 1001
    
    fun createServiceNotification(context: Context): Notification {
        createNotificationChannel(context)
        
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        return NotificationCompat.Builder(context, SERVICE_CHANNEL_ID)
            .setContentTitle("AutoDroid Controller")
            .setContentText("自动化测试服务运行中")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                SERVICE_CHANNEL_ID,
                "AutoDroid Controller Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "AutoDroid控制器服务运行状态通知"
                setShowBadge(false)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}