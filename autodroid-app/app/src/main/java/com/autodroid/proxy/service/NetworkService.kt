// NetworkService.java
package com.autodroid.proxy.service

import android.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.autodroid.proxy.model.DiscoveredServer
import com.autodroid.proxy.service.NsdHelper.ServiceDiscoveryCallback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.InetAddress
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * NetworkService is a foreground service that manages network connections and communication.
 * It handles mDNS service discovery and communication with the Autodroid server via FastAPI.
 */
class NetworkService : Service() {
    private val binder: IBinder = LocalBinder()
    private var deviceId: String? = null
    private var executorService: ExecutorService? = null
    private var nsdHelper: NsdHelper? = null
    private var httpClient: OkHttpClient? = null
    var discoveredServer: DiscoveredServer? = null
        private set

    // Callback for server discovery
    private var serverDiscoveryCallback: ServerDiscoveryCallback? = null

    interface ServerDiscoveryCallback {
        fun onServerDiscovered(server: DiscoveredServer?)
        fun onServerLost(server: DiscoveredServer?)
    }

    inner class LocalBinder : Binder() {
        val service: NetworkService
            get() = this@NetworkService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "NetworkService created")


        // Create notification channel for foreground service
        createNotificationChannel()


        // Start service in foreground
        startForeground(NOTIFICATION_ID, createNotification())


        // Initialize device ID
        deviceId = Build.SERIAL


        // Initialize executor service
        executorService = Executors.newSingleThreadExecutor()


        // Initialize HTTP client
        httpClient = OkHttpClient()


        // Initialize and start mDNS discovery
        initNetworkDiscovery()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "NetworkService destroyed")


        // Stop mDNS discovery
        if (nsdHelper != null) {
            nsdHelper!!.tearDown()
        }


        // Shutdown executor service
        if (executorService != null) {
            executorService!!.shutdownNow()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "NetworkService onStartCommand called")

        if (intent != null && intent.getAction() != null) {
            when (intent.getAction()) {
                "MATCH_WORKFLOWS" -> {
                    // App-based APK scanning: Receive APK info from app
                    val apkInfoListJson = intent.getStringExtra("apk_info_list")
                    if (apkInfoListJson != null) {
                        matchWorkflowsForApks(apkInfoListJson)
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
            .setContentTitle("Autodroid Proxy")
            .setContentText("Connected via Network")
            .setSmallIcon(R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        return builder.build()
    }

    private fun initNetworkDiscovery() {
        nsdHelper = NsdHelper(this, object : ServiceDiscoveryCallback {
            override fun onServiceFound(serviceName: String?, host: String?, port: Int) {
                Log.d(TAG, "Service found: " + serviceName + " at " + host + ":" + port)
                discoveredServer = DiscoveredServer(serviceName, host, port)


                // Notify callback if set
                if (serverDiscoveryCallback != null) {
                    serverDiscoveryCallback!!.onServerDiscovered(discoveredServer)
                }


                // Publish device information to the discovered server
                publishDeviceInfo(host, port)
            }

            override fun onServiceLost(serviceName: String?) {
                Log.d(TAG, "Service lost: " + serviceName)
                if (serverDiscoveryCallback != null && discoveredServer != null &&
                    discoveredServer!!.name == serviceName
                ) {
                    serverDiscoveryCallback!!.onServerLost(discoveredServer)
                    discoveredServer = null
                }
            }

            override fun onDiscoveryStarted() {
                Log.d(TAG, "mDNS discovery started")
            }

            override fun onDiscoveryFailed() {
                Log.e(TAG, "mDNS discovery failed")
            }
        })

        nsdHelper!!.initialize()
        nsdHelper!!.discoverServices()
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
            val url = String.format("http://%s:%d/api/devices/register", serverHost, serverPort)

            val body = deviceInfoJson
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
            try {
                val inetAddress = InetAddress.getLocalHost()
                return inetAddress.getHostAddress()
            } catch (e: Exception) {
                Log.e(
                    TAG,
                    "Failed to get local IP address: " + e.message
                )
                return "unknown"
            }
        }

    private fun matchWorkflowsForApks(apkInfoListJson: String?) {
        // Simplified: Log that we're matching workflows
        Log.d(TAG, "Matching workflows for APKs (simulated): " + apkInfoListJson)
    }

    fun setServerDiscoveryCallback(callback: ServerDiscoveryCallback?) {
        this.serverDiscoveryCallback = callback
    }

    val discoveredServers: MutableList<DiscoveredServer?>?
        get() = if (nsdHelper != null) nsdHelper!!.discoveredServers else null

    companion object {
        private const val TAG = "NetworkService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "NetworkServiceChannel"
    }
}