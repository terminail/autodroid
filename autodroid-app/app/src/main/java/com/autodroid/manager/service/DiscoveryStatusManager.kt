package com.autodroid.manager.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.autodroid.manager.model.DiscoveredServer
import com.autodroid.manager.model.DiscoveryStatus

/**
 * Singleton for managing global discovery status that can be observed by any component
 * This eliminates the need for MainActivity to directly manage NetworkService
 */
object DiscoveryStatusManager {
    // Server information
    val serverInfo = MutableLiveData<MutableMap<String?, Any?>?>()
    
    // Discovery status information (encapsulated)
    val discoveryStatus = MutableLiveData<DiscoveryStatus>()
    
    // Network service status
    val isServiceRunning = MutableLiveData<Boolean>()
    
    // Network connectivity status
    val networkConnected = MutableLiveData<Boolean>()
    
    // Flag to track if user has chosen QR code as fallback
    private var qrCodeChosenAsFallback = false
    
    private var applicationContext: Context? = null
    
    /**
     * Initialize the singleton with application context
     */
    fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }
    
    /**
     * Start the network service if not already running
     */
    fun startNetworkService() {
        // Don't start if user has chosen QR code as fallback
        if (qrCodeChosenAsFallback) {
            Log.d("DiscoveryStatusManager", "Not starting NetworkService - QR code chosen as fallback")
            return
        }
        
        if (isServiceRunning.value != true) {
            val context = applicationContext ?: return
            val intent = android.content.Intent(context, NetworkService::class.java)
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            
            Handler(Looper.getMainLooper()).post {
                isServiceRunning.value = true
            }
        }
    }
    
    /**
     * Stop the network service
     */
    fun stopNetworkService() {
        val context = applicationContext ?: return
        val intent = android.content.Intent(context, NetworkService::class.java)
        context.stopService(intent)
        
        Handler(Looper.getMainLooper()).post {
            isServiceRunning.value = false
        }
    }
    
    /**
     * Update server information
     */
    fun updateServerInfo(server: DiscoveredServer?) {
        Handler(Looper.getMainLooper()).post {
            if (server != null) {
                val info = mutableMapOf<String?, Any?>()
                info["name"] = server.serviceName
                info["ip"] = server.host
                info["port"] = server.port
                info["connected"] = true
                info["discovery_method"] = "mDNS"
                info["api_endpoint"] = "http://${server.host}:${server.port}/api"
                serverInfo.value = info
                Log.d("DiscoveryStatusManager", "Server info updated via mDNS: ${server.host}:${server.port}")
            } else {
                // Mark as disconnected
                val currentInfo = serverInfo.value?.toMutableMap() ?: mutableMapOf()
                currentInfo["connected"] = false
                serverInfo.value = currentInfo
                Log.d("DiscoveryStatusManager", "Server disconnected")
            }
        }
    }
    
    /**
     * Set server information directly
     */
    fun setServerInfo(info: MutableMap<String?, Any?>?) {
        Handler(Looper.getMainLooper()).post {
            serverInfo.value = info
        }
    }
    
    /**
     * Update discovery status (encapsulated version)
     */
    fun updateDiscoveryStatus(status: DiscoveryStatus) {
        Handler(Looper.getMainLooper()).post {
            discoveryStatus.value = status
        }
    }

    /**
     * Update discovery status (simplified version with only inProgress flag)
     */
    fun updateDiscoveryStatus(inProgress: Boolean) {
        Handler(Looper.getMainLooper()).post {
            val currentStatus = discoveryStatus.value ?: DiscoveryStatus.initial()
            discoveryStatus.value = currentStatus.copy(inProgress = inProgress)
        }
    }

    /**
     * Update discovery status with retry information
     */
    fun updateDiscoveryStatus(inProgress: Boolean, retryCount: Int, maxRetries: Int) {
        Handler(Looper.getMainLooper()).post {
            discoveryStatus.value = DiscoveryStatus(inProgress, retryCount, maxRetries, false)
        }
    }
    
    /**
     * Update discovery failure status
     */
    fun updateDiscoveryFailed(failed: Boolean) {
        Handler(Looper.getMainLooper()).post {
            val currentStatus = discoveryStatus.value ?: DiscoveryStatus.initial()
            discoveryStatus.value = currentStatus.copy(failed = failed)
        }
    }
    
    /**
     * Update network connectivity status
     */
    fun updateNetworkStatus(connected: Boolean) {
        Handler(Looper.getMainLooper()).post {
            networkConnected.value = connected
        }
    }
    
    /**
     * Mark QR code as chosen as fallback
     */
    fun setQrCodeChosenAsFallback(chosen: Boolean) {
        Handler(Looper.getMainLooper()).post {
            qrCodeChosenAsFallback = chosen
            Log.d("DiscoveryStatusManager", "QR code chosen as fallback: $chosen")
        }
    }
    
    /**
     * Check if QR code has been chosen as fallback
     */
    fun isQrCodeChosenAsFallback(): Boolean {
        return qrCodeChosenAsFallback
    }
    
    /**
     * Reset QR code fallback flag (allow mDNS to start again)
     */
    fun resetQrCodeFallback() {
        qrCodeChosenAsFallback = false
        updateDiscoveryFailed(false)
        Log.d("DiscoveryStatusManager", "Reset QR code fallback flag")
    }
    
    /**
     * Restart discovery process
     */
    fun restartDiscovery() {
        val context = applicationContext ?: return
        val intent = android.content.Intent(context, NetworkService::class.java)
        context.stopService(intent)
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}