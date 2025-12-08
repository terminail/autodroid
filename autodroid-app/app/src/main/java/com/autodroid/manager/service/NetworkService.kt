// NetworkService.kt
package com.autodroid.manager.service


import android.R
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
import com.autodroid.data.repository.ServerRepository
import com.autodroid.manager.model.Server
import com.autodroid.manager.network.ApiClient
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
 * It handles mDNS service discovery and communication with the Autodroid server via FastAPI.
 * This service now manages its own lifecycle and updates the DiscoveryStatusManager singleton.
 */
class NetworkService : Service() {
    private val binder: IBinder = LocalBinder()
    private var deviceId: String? = null
    private var executorService: ExecutorService? = null
    private var nsdHelper: NsdHelper? = null
    private var mdnsFallbackManager: MdnsFallbackManager? = null
    private var httpClient: OkHttpClient? = null
    private var serverRepository: ServerRepository? = null
    var discoveredServer: Server? = null
        private set

    // mDNS discovery state
    private var isDiscoveryInProgress = false

    inner class LocalBinder : Binder() {
        val service: NetworkService
            get() = this@NetworkService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "NetworkService created")
        Log.d(TAG, "Starting mDNS discovery process...")

        // Initialize ServerRepository
        serverRepository = ServerRepository.getInstance(applicationContext as android.app.Application)

        // DiscoveryStatusManager is already initialized in MyApplication
        // Do NOT re-initialize here to avoid state loss

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

        // Initialize and start mDNS discovery
        initNetworkDiscovery()
        
        // Update service status
        DiscoveryStatusManager.isServiceRunning.value = true
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        
        // Clean up resources
        try {
            executorService?.shutdownNow()
            mdnsFallbackManager?.stopDiscovery()
            mdnsFallbackManager = null
            httpClient?.dispatcher?.executorService?.shutdown()
            httpClient?.connectionPool?.evictAll()
            
            // Update DiscoveryStatusManager
            DiscoveryStatusManager.stopNetworkService()
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up resources", e)
        }
        
        Log.d(TAG, "NetworkService destroyed")
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
            .setContentTitle("Autodroid Manager")
            .setContentText("Connected via Network")
            .setSmallIcon(R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        return builder.build()
    }

    private fun initNetworkDiscovery() {
        Log.d(TAG, "Initializing network discovery...")
        
        // Check network connectivity before starting mDNS discovery
        if (!isNetworkConnected()) {
            Log.e(TAG, "Network not connected, cannot start mDNS discovery")
            
            // Update DiscoveryStatusManager about network connectivity failure
            DiscoveryStatusManager.updateDiscoveryStatus(false)
            DiscoveryStatusManager.updateNetworkStatus(false)
            
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
        
        // Network is connected, update status
        DiscoveryStatusManager.updateNetworkStatus(true)
        Log.d(TAG, "Network connected - mDNS discovery will be started manually by user")
        
        // Initialize MdnsFallbackManager but do not start discovery automatically
        mdnsFallbackManager = MdnsFallbackManager(this)
        Log.d(TAG, "mDNS fallback manager initialized (manual mode)")
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
    
    /**
     * Check if the device is connected to WiFi (required for mDNS)
     */
    private fun isWifiConnected(): Boolean {
        return try {
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val network = connectivityManager.activeNetwork
            
            if (network != null) {
                val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
                networkCapabilities != null && networkCapabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking WiFi connectivity: ${e.message}")
            false
        }
    }
    
    private fun startDiscovery() {
        // Start discovery immediately without delay
        executorService!!.submit(Runnable {
            try {
                isDiscoveryInProgress = true
                // Update discovery status to notify UI
                DiscoveryStatusManager.updateDiscoveryStatus(true)
                mdnsFallbackManager?.startDiscovery(
                    discoveryCallback = { serverInfo ->
                        // Handle service found
                        Log.d(TAG, "Service found: ${serverInfo.serviceName}")
                        // Ensure non-null values for Server constructor
                        val serviceName = serverInfo.serviceName ?: "Unknown Service"
                        val hostname = serverInfo.hostname ?: "unknown"
                        val apiEndpoint = serverInfo.apiEndpoint ?: "http://unknown:8000"
                        discoveredServer = Server(
                            serviceName = serviceName,
                            name = serviceName,
                            hostname = hostname,
                            platform = "Autodroid Server",
                            apiEndpoint = apiEndpoint,
                            discoveryMethod = "mDNS"
                        )
                        
                        // Save to repository using coroutine scope
                        CoroutineScope(Dispatchers.IO).launch {
                            serverRepository?.addDiscoveredServer(discoveredServer!!)
                        }
                        
                        // Perform health check
                        performHealthCheck(discoveredServer!!)
                        
                        // Keep discovery status as true to maintain connection
                        DiscoveryStatusManager.updateDiscoveryStatus(true)
                        // Update server info for UI (this will preserve current connection status)
                        DiscoveryStatusManager.updateServerInfo(discoveredServer)
                        // Mark as connected
                        DiscoveryStatusManager.setServerConnected(true)
                        // Notify UI or other components
                        notifyServiceDiscoveryListeners()
                    },
                    failureCallback = {
                        // Handle discovery failure
                        Log.w(TAG, "mDNS discovery failed")
                        isDiscoveryInProgress = false
                        // Update discovery status to notify UI
                        DiscoveryStatusManager.updateDiscoveryStatus(false)
                        // Mark server as disconnected
                        DiscoveryStatusManager.setServerConnected(false)
                        // Notify failure to UI or other components
                        notifyDiscoveryFailedListeners()
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Discovery error: ${e.message}", e)
                // Update discovery status on error
                DiscoveryStatusManager.updateDiscoveryStatus(false)
            }
        })
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
                // Try to get the first non-loopback IPv4 address from any network interface
                val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
                while (interfaces.hasMoreElements()) {
                    val networkInterface = interfaces.nextElement()
                    val addresses = networkInterface.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val address = addresses.nextElement()
                        if (!address.isLoopbackAddress && address is java.net.Inet4Address) {
                            return address.hostAddress
                        }
                    }
                }
                return "unknown"
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

    /**
     * Stop mDNS discovery
     */
    fun stopMdnsDiscovery() {
        try {
            isDiscoveryInProgress = false
            mdnsFallbackManager?.stopDiscovery()
            Log.d(TAG, "mDNS discovery stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping mDNS discovery", e)
        }
    }

    /**
     * Restart mDNS discovery
     */
    fun restartMdnsDiscovery() {
        Log.d(TAG, "Restarting mDNS discovery")
        try {
            // Stop current discovery
            mdnsFallbackManager?.stopDiscovery()
            
            // Reinitialize and start discovery
            initNetworkDiscovery()
        } catch (e: Exception) {
            Log.e(TAG, "Error restarting mDNS discovery", e)
        }
    }

    /**
     * Perform health check on discovered server
     */
    private fun performHealthCheck(server: Server) {
        executorService!!.submit {
            try {
                // Use coroutine scope for suspend functions
                CoroutineScope(Dispatchers.IO).launch {
                    // Create ApiClient instance for health check
                    val apiClient = ApiClient.getInstance()
                    
                    try {
                        val healthResponse = apiClient.healthCheck()
                        
                        // Update server info in repository using apiEndpoint as server key
                        val apiEndpoint = server.apiEndpoint ?: server.serviceName
                        if (apiEndpoint.isNotEmpty()) {
                            // Update server version and connection status
                            serverRepository?.updateServerInfo(apiEndpoint, healthResponse.version ?: "unknown")
                        }
                        
                        // Update DiscoveryStatusManager
                        DiscoveryStatusManager.updateServerInfo(server)
                        DiscoveryStatusManager.setServerConnected(true)
                        Log.d(TAG, "Server health check successful: ${healthResponse}")
                    } catch (e: Exception) {
                        Log.w(TAG, "Server health check failed: ${e.message}")
                        DiscoveryStatusManager.setServerConnected(false)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during health check: ${e.message}", e)
                DiscoveryStatusManager.setServerConnected(false)
            }
        }
    }

    /**
     * Add server manually (e.g., from QR code or manual input)
     */
    fun addServerManually(server: Server) {
        executorService!!.submit {
            try {
                // Save to repository using coroutine scope
                CoroutineScope(Dispatchers.IO).launch {
                    serverRepository?.addDiscoveredServer(server)
                }
                
                // Perform health check
                performHealthCheck(server)
                
                // Update DiscoveryStatusManager
                DiscoveryStatusManager.updateServerInfo(server)
            } catch (e: Exception) {
                Log.e(TAG, "Error adding server manually: ${e.message}", e)
            }
        }
    }

    /**
     * Notify listeners when a service is discovered
     */
    private fun notifyServiceDiscoveryListeners() {
        // TODO: Implement service discovery notification logic
        // This could involve sending broadcasts, updating LiveData, or calling registered callbacks
        Log.d(TAG, "Service discovery notification triggered")
    }

    /**
     * Notify listeners when discovery fails
     */
    private fun notifyDiscoveryFailedListeners() {
        // Update DiscoveryStatusManager to notify UI about discovery failure
        DiscoveryStatusManager.updateDiscoveryFailed(true)
        Log.d(TAG, "Discovery failure notification triggered - UI updated")
    }

    companion object {
        private const val TAG = "NetworkService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "NetworkServiceChannel"
    }
}