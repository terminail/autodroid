package com.autodroid.guardiansdk.manager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.autodroid.guardiansdk.R

class TestModeManager private constructor(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "guardian_test_mode"
        private const val NOTIFICATION_ID = 1001

        @Volatile
        private var instance: TestModeManager? = null

        fun getInstance(context: Context): TestModeManager {
            return instance ?: synchronized(this) {
                instance ?: TestModeManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private var isTestMode = false

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Guardian 测试模式",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "测试模式通知"
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun setTestMode(enabled: Boolean) {
        isTestMode = enabled
    }

    fun isTestMode(): Boolean = isTestMode

    fun showTestAlarmNotification(message: String, location: String) {
        if (!isTestMode) return

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("测试模式 - 模拟报警")
            .setContentText("报警信息：$message，位置：$location")
            .setSmallIcon(R.drawable.guardian_ic_setting_default)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun showTestGuardianQueryNotification(guardianName: String, queryMessage: String) {
        if (!isTestMode) return

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("测试模式 - 模拟监护人查询")
            .setContentText("监护人${guardianName}查询：$queryMessage")
            .setSmallIcon(R.drawable.guardian_ic_setting_default)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
