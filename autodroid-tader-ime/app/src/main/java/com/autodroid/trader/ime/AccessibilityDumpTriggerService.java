package com.autodroid.trader.ime;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class AccessibilityDumpTriggerService extends Service {
    private static final String TAG = "AccessibilityDumpTrigger";
    private static final int NOTIFICATION_ID = 1001;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getStringExtra("action");
        if ("dump".equals(action)) {
            Log.d(TAG, "Triggering accessibility dump");
            // 发送一个无障碍事件来触发服务
            // 我们将使用 AccessibilityService 的全局动作来触发
            // 但在这里我们只是启动服务，让系统触发相关的无障碍事件
        }

        // 启动前台服务以避免被系统杀死
        startForeground(NOTIFICATION_ID, createNotification());

        // 立即停止服务
        stopSelf();

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "ACCESSIBILITY_DUMP_CHANNEL",
                    "Accessibility Dump Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, "ACCESSIBILITY_DUMP_CHANNEL")
                .setContentTitle("Accessibility Dump")
                .setContentText("Triggering accessibility dump...")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build();
    }
}