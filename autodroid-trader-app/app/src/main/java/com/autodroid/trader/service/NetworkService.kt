// NetworkService.kt
package com.autodroid.trader.service


import android.R
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.autodroid.trader.data.repository.ServerRepository
import com.autodroid.trader.data.dao.ServerEntity
import com.autodroid.trader.utils.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * NetworkService is a foreground service that manages network connections and communication.
 * It handles network communication with the Autodroid server via FastAPI.
 */
class NetworkService : Service() {
    private val binder: IBinder = LocalBinder()
    private var deviceId: String? = null
    private var executorService: ExecutorService? = null


    private var httpClient: OkHttpClient? = null
    private var serverRepository: ServerRepository? = null
    var discoveredServer: ServerEntity? = null
        private set



    inner class LocalBinder : Binder() {
        val service: NetworkService
            get() = this@NetworkService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "NetworkService created")
        Log.d(TAG, "Starting network service...")

        // Initialize ServerRepository
        serverRepository = ServerRepository.getInstance(applicationContext as Application)



        // Create notification channel for foreground service
        createNotificationChannel()

        // Start service in foreground
        startForeground(NOTIFICATION_ID, createNotification())

        // Initialize device ID
        // Use a combination of device properties to create a unique identifier
        // This avoids permission issues with Build.getSerial()
        deviceId = "${Build.MANUFACTURER}_${Build.MODEL}_${Build.VERSION.RELEASE}_${Build.ID}"

        // Initialize executor service
        executorService = Executors.newSingleThreadExecutor()

        // Initialize HTTP client
        httpClient = OkHttpClient()

        // Initialize network communication
        initNetworkCommunication()
        

    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        
        // Clean up resources
        try {
            executorService?.shutdownNow()

            httpClient?.dispatcher?.executorService?.shutdown()
            httpClient?.connectionPool?.evictAll()
            

        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up resources", e)
        }
        
        Log.d(TAG, "NetworkService destroyed")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "NetworkService onStartCommand called")

        if (intent != null && intent.getAction() != null) {
            when (intent.getAction()) {
                "MATCH_WORKSCRIPTS" -> {
                    // App-based APK scanning: Receive APK info from app
                    val apkInfoListJson = intent.getStringExtra("apk_info_list")
                    if (apkInfoListJson != null) {
                        matchTradePlansForApks(apkInfoListJson)
                    }
                }
            }
        }

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Network Service",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.setDescription("Maintains network connection to Autodroid server")

            val notificationManager =
                getSystemService<NotificationManager?>(NotificationManager::class.java)
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    private fun createNotification(): Notification {
        val builder: NotificationCompat.Builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Autodroid Manager")
            .setContentText("Connected via Network")
            .setSmallIcon(R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        return builder.build()
    }

    private fun initNetworkCommunication() {
        Log.d(TAG, "Initializing network communication...")
        
        // Check network connectivity
        if (!isNetworkConnected()) {
            Log.e(TAG, "Network not connected")
            

            
            // Auto-stop the service after network failure
            executorService!!.submit(Runnable {
                try {
                    Thread.sleep(2000)
                    Log.d(TAG, "Stopping NetworkService due to network connectivity failure")
                    stopSelf()
                } catch (e: InterruptedException) {
                    Log.e(TAG, "Auto-stop interrupted", e)
                }
            })
            return
        }
        

        Log.d(TAG, "Network connected")
    }
    
    /**
     * Check if the device has network connectivity
     */
    private fun isNetworkConnected(): Boolean {
        return try {
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val network = connectivityManager.activeNetwork
            
            if (network != null) {
                val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
                networkCapabilities != null && (
                    networkCapabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
                    networkCapabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    networkCapabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)
                )
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking network connectivity: ${e.message}")
            false
        }
    }
    

    


    private fun publishDeviceInfo(serverHost: String?, serverPort: Int) {
        executorService!!.submit(Runnable {
            try {
                // Collect device information
                val deviceName = Build.MODEL
                val androidVersion = Build.VERSION.RELEASE
                val localIp = this.localIpAddress

                // Create JSON message
                val deviceInfoJson = String.format(
                    "{\"type\":\"device_info\",\"data\":{\"device_name\":\"%s\",\"android_version\":\"%s\",\"device_id\":\"%s\",\"local_ip\":\"%s\"}}",
                    deviceName, androidVersion, deviceId, localIp
                )

                // Send device info to server via FastAPI
                sendDeviceInfoToServer(serverHost, serverPort, deviceInfoJson)
            } catch (e: Exception) {
                Log.e(TAG, "Error publishing device info: " + e.message)
                e.printStackTrace()
            }
        })
    }

    private fun sendDeviceInfoToServer(
        serverHost: String?,
        serverPort: Int,
        deviceInfoJson: String
    ) {
        try {
            // 检查serverHost是否为空
            if (serverHost == null) {
                Log.e(TAG, "Cannot send device info: serverHost is null")
                return
            }
            val url = String.format("http://%s:%d/api/devices", serverHost, serverPort)

            val body = deviceInfoJson!!
                .toRequestBody("application/json".toMediaTypeOrNull())

            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            httpClient!!.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.d(TAG, "Device info sent successfully to server")
                } else {
                    Log.e(TAG, "Failed to send device info to server: " + response.code)
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error sending device info to server: " + e.message)
        }
    }

    private val localIpAddress: String?
        get() {
            return NetworkUtils.getLocalIpAddress() ?: "unknown"
        }

    private fun matchTradePlansForApks(apkInfoListJson: String?) {
        // Simplified: Log that we're matching trade plans
        Log.d(TAG, "Matching trade plans for APKs (simulated): " + apkInfoListJson)
    }









    companion object {
        private const val TAG = "NetworkService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "NetworkServiceChannel"
    }
}