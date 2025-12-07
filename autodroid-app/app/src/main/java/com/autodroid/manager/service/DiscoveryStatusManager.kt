package com.autodroid.manager.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.autodroid.manager.model.DiscoveryStatus
import com.autodroid.manager.model.Server


/**
 * Singleton for managing global discovery status that can be observed by any component
 * This eliminates the need for MainActivity to directly manage NetworkService
 */
object DiscoveryStatusManager {
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
    
    // observeCurrentServer method removed - server LiveData now directly maps from repository
    
    /**
     * Start the network service if not already running
     */
    fun startNetworkService() {
        // Always start NetworkService regardless of QR code fallback choice
        // This ensures mDNS discovery can continue even after previous failures
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
            
            Log.d("DiscoveryStatusManager", "NetworkService started (QR code fallback: $qrCodeChosenAsFallback)")
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
     * Update server information based on mDNS discovery
     * This method now only updates discovery status, not server data
     */
    fun updateServerInfo(server: Server?) {
        Handler(Looper.getMainLooper()).post {
            if (server != null) {
                Log.d("DiscoveryStatusManager", "Server discovered via mDNS: ${server.hostname}")
                // Server data management is now handled by AppViewModel
                // The actual server connection should be initiated by UI components through AppViewModel
            } else {
                Log.d("DiscoveryStatusManager", "Server discovery cleared")
            }
        }
    }
    
    /**
     * Set server connection status (for discovery status tracking only)
     */
    fun setServerConnected(connected: Boolean) {
        Handler(Looper.getMainLooper()).post {
            Log.d("DiscoveryStatusManager", "Server connection status updated: connected=$connected")
            // Server connection management is now handled by AppViewModel
            // This method is kept for discovery status tracking purposes
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
     * Reset QR code fallback flag (allow mDNS to start again)
     */
    fun resetQrCodeFallback() {
        qrCodeChosenAsFallback = false
        updateDiscoveryFailed(false)
        Log.d("DiscoveryStatusManager", "Reset QR code fallback flag")
    }

    /**
     * Start discovery (manual mode without automatic retry)
     */
    fun startDiscovery() {
        updateDiscoveryStatus(inProgress = true)
        startNetworkService()
        Log.d("DiscoveryStatusManager", "Discovery started in manual mode")
    }
    
    /**
     * Stop discovery
     */
    fun stopDiscovery() {
        stopNetworkService()
        updateDiscoveryStatus(inProgress = false)
        Log.d("DiscoveryStatusManager", "Discovery stopped")
    }

}