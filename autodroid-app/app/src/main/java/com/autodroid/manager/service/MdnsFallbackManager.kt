package com.autodroid.manager.service

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
// import com.autodroid.manager.model.DiscoveredServer
import com.autodroid.manager.model.Server
import java.util.*
import java.util.concurrent.Executor

/**
 * A fallback mDNS implementation that tries multiple approaches to service discovery
 * before falling back to QR code scanning.
 * 
 * Implementation priority order:
 * 1. LegacyNsdImplementation - Uses older NsdManager APIs for better compatibility
 * 2. JmDNSImplementation - Uses third-party JmDNS library
 * 3. WeUPnPImplementation - Uses UPnP protocol as alternative discovery method
 * 4. StandardNsdImplementation - Uses standard NsdManager APIs as last resort
 */
class MdnsFallbackManager(private val context: Context) {
    private val TAG = "MdnsFallbackManager"
    private val SERVICE_TYPE = "_autodroid._tcp.local."
    
    // List of mDNS implementations to try
    private val implementations = mutableListOf<MdnsImplementation>()
    private var currentImplementationIndex = 0
    private var isDiscoveryInProgress = false
    private var discoveryCallback: ((Server) -> Unit)? = null
    private var failureCallback: (() -> Unit)? = null
    interface ServiceDiscoveryCallback {
        fun onServiceFound(serviceName: String?, host: String?, port: Int)
        fun onServiceLost(serviceName: String?)
        fun onDiscoveryStarted()
        fun onDiscoveryFailed()
        fun onAllImplementationsFailed()
    }
    
    init {
        // Initialize implementations in priority order
        val legacyNsd = LegacyNsdImplementation(context)
        val jmDNS = JmDNSImplementation(context)
        val weUPnP = WeUPnPImplementation(context)
        val standardNsd = StandardNsdImplementation(context)
        
        // Add all implementations initially
        implementations.add(legacyNsd)
        implementations.add(jmDNS)
        implementations.add(weUPnP)
        implementations.add(standardNsd)
        
        Log.d(TAG, "Initialized ${implementations.size} mDNS implementations for manual start mode")
        
        // Log implementation availability
        implementations.forEach { impl ->
            Log.d(TAG, "${impl.javaClass.simpleName}: available for manual start")
        }
    }
    
    /**
     * Start service discovery with fallback mechanism
     */
    fun startDiscovery(discoveryCallback: (Server) -> Unit, failureCallback: () -> Unit) {
        if (isDiscoveryInProgress) {
            Log.w(TAG, "Discovery already in progress with ${implementations.getOrNull(currentImplementationIndex)?.javaClass?.simpleName ?: "unknown"} implementation")
            return
        }
        
        this.discoveryCallback = discoveryCallback
        this.failureCallback = failureCallback
        isDiscoveryInProgress = true
        currentImplementationIndex = 0
        
        Log.d(TAG, "Starting mDNS discovery with ${implementations.size} implementations available")
        
        // Try the first implementation
        tryNextImplementation()
    }
    
    /**
     * Try the next mDNS implementation
     */
    private fun tryNextImplementation() {
        // Use all implementations (no weight filtering)
        val availableImplementations = implementations
        
        if (currentImplementationIndex >= availableImplementations.size) {
            // All implementations failed
            Log.e(TAG, "All mDNS implementations failed")
            isDiscoveryInProgress = false
            failureCallback?.invoke()
            return
        }
        
        val implementation = availableImplementations[currentImplementationIndex]
        val implementationName = implementation.javaClass.simpleName
        
        Log.d(TAG, "Trying mDNS implementation: $implementationName (${currentImplementationIndex + 1}/${availableImplementations.size})")
        
        // Add a delay before trying the next implementation to avoid rapid switching
        if (currentImplementationIndex > 0) {
            Log.d(TAG, "Waiting 1 second before trying next implementation...")
            try {
                Thread.sleep(1000)
            } catch (e: InterruptedException) {
                Log.e(TAG, "Delay interrupted: ${e.message}")
            }
        }
        
        var hasStarted = false
        var hasTimedOut = false
        
        // Create a callback for the current implementation
        val implementationCallback = object : MdnsImplementation.Callback {
            override fun onServiceFound(serverInfo: Server) {
                Log.d(TAG, "Service found using $implementationName: ${serverInfo.hostname}")
                discoveryCallback?.invoke(serverInfo)
            }
            
            override fun onDiscoveryFailed() {
                Log.w(TAG, "Implementation $implementationName failed, trying next")
                hasTimedOut = true // Prevent timeout from triggering
                
                currentImplementationIndex++
                tryNextImplementation()
            }
        }
        
        // Start discovery with the implementation
        implementation.startDiscovery(implementationCallback)
        
        // Set a timeout for this implementation
        Handler(Looper.getMainLooper()).postDelayed({
            if (hasStarted && !hasTimedOut) {
                Log.w(TAG, "TIMEOUT: Implementation $implementationName found no services after 10 seconds, switching to next implementation")
                hasTimedOut = true
                
                implementation.stopDiscovery()
                currentImplementationIndex++
                tryNextImplementation()
            }
        }, 10000) // 10 second timeout
        
        hasStarted = true
    }
    
    /**
     * Stop service discovery
     */
    fun stopDiscovery() {
        if (!isDiscoveryInProgress) {
            return
        }
        
        Log.d(TAG, "Stopping mDNS discovery")
        
        implementations.forEach { implementation ->
            try {
                implementation.stopDiscovery()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping implementation: ${e.message}")
            }
        }
        
        isDiscoveryInProgress = false
    }
    
    /**
     * Clean up resources
     */
    fun tearDown() {
        stopDiscovery()
    }
}





/**
 * Interface for mDNS implementations
 */
interface MdnsImplementation {
    fun startDiscovery(callback: Callback)
    fun stopDiscovery()
    fun isAvailable(): Boolean
    
    /**
     * Callback interface for mDNS implementations
     */
    interface Callback {
        fun onServiceFound(serverInfo: Server)
        fun onDiscoveryFailed()
    }
}