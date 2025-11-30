package com.autodroid.proxy.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ZenohService is a foreground service that manages Zenoh connections and communication.
 */
public class ZenohService extends Service {

    private static final String TAG = "ZenohService";
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "ZenohServiceChannel";
    
    // Zenoh configuration constants
    private static final String ZENOH_LISTEN_KEY = "autodroid/device/{device_id}/commands";
    private static final String ZENOH_SERVER_INFO_KEY = "autodroid/server/info";
    private static final String ZENOH_WORKFLOWS_KEY = "autodroid/workflows";
    private static final String ZENOH_REPORTS_KEY = "autodroid/reports";
    private static final String ZENOH_EXECUTION_KEY = "autodroid/execution";
    private static final String ZENOH_PUBLISH_KEY = "autodroid/device/{device_id}/info";
    
    private String deviceId;
    private ExecutorService executorService;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "ZenohService created");
        
        // Create notification channel for foreground service
        createNotificationChannel();
        
        // Start service in foreground
        startForeground(NOTIFICATION_ID, createNotification());
        
        // Initialize device ID
        deviceId = android.os.Build.SERIAL;
        
        // Initialize executor service for Zenoh operations
        executorService = Executors.newSingleThreadExecutor();
        
        // Initialize and connect Zenoh (simplified version)
        initZenoh();
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "ZenohService destroyed");
        
        // Shutdown executor service
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "ZenohService onStartCommand called");
        
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case "SCAN_APKS":
                    // Legacy server-based APK scan (deprecated)
                    scanApks();
                    break;
                case "MATCH_WORKFLOWS":
                    // App-based APK scanning: Receive APK info from app
                    String apkInfoListJson = intent.getStringExtra("apk_info_list");
                    if (apkInfoListJson != null) {
                        matchWorkflowsForApks(apkInfoListJson);
                    }
                    break;
            }
        }
        
        return START_STICKY;
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Zenoh Service",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Maintains Zenoh connection to Autodroid server");
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
    
    private Notification createNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Autodroid Proxy")
                .setContentText("Connected via Zenoh")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        
        return builder.build();
    }
    
    private void initZenoh() {
        executorService.submit(() -> {
            try {
                Log.d(TAG, "Zenoh initialization started (simplified version)");
                
                // Simplified: Just log that we're connected
                Log.d(TAG, "Zenoh connection established (simulated)");
                
                // Publish device information
                publishDeviceInfo();
                
            } catch (Exception e) {
                Log.e(TAG, "Zenoh initialization error: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    private void publishDeviceInfo() {
        executorService.submit(() -> {
            try {
                // Collect device information
                String deviceName = android.os.Build.MODEL;
                String androidVersion = android.os.Build.VERSION.RELEASE;
                String localIp = getLocalIpAddress();
                
                // Create JSON message
                String deviceInfoJson = String.format(
                        "{\"type\":\"device_info\",\"data\":{\"device_name\":\"%s\",\"android_version\":\"%s\",\"device_id\":\"%s\",\"local_ip\":\"%s\"}}",
                        deviceName, androidVersion, deviceId, localIp
                );
                
                // Log device info instead of publishing via Zenoh
                Log.d(TAG, "Publishing device info (simulated): " + deviceInfoJson);
                
            } catch (Exception e) {
                Log.e(TAG, "Error publishing device info: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    private String getLocalIpAddress() {
        try {
            java.net.InetAddress inetAddress = java.net.InetAddress.getLocalHost();
            return inetAddress.getHostAddress();
        } catch (Exception e) {
            Log.e(TAG, "Failed to get local IP address: " + e.getMessage());
            return "unknown";
        }
    }
    
    private void scanApks() {
        // Simplified: Log that we're scanning APKs
        Log.d(TAG, "Scanning APKs (simulated)");
    }
    
    private void matchWorkflowsForApks(String apkInfoListJson) {
        // Simplified: Log that we're matching workflows
        Log.d(TAG, "Matching workflows for APKs (simulated): " + apkInfoListJson);
    }
    
    private void handleMessage(String key, String message) {
        // Simplified: Log incoming messages
        Log.d(TAG, "Handling message on key '" + key + "': " + message);
    }
    
    private void handleServerInfo(String message) {
        // Simplified: Log server info
        Log.d(TAG, "Handling server info: " + message);
    }
    
    private void handleWorkflowsInfo(String message) {
        // Simplified: Log workflows info
        Log.d(TAG, "Handling workflows info: " + message);
    }
    
    private void handleReportsInfo(String message) {
        // Simplified: Log reports info
        Log.d(TAG, "Handling reports info: " + message);
    }
    
    private void handleExecutionInfo(String message) {
        // Simplified: Log execution info
        Log.d(TAG, "Handling execution info: " + message);
    }
}