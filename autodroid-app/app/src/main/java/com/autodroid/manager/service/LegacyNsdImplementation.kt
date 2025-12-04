package com.autodroid.manager.service

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.util.Log
import com.autodroid.manager.model.ServerInfo

/**
 * Legacy NsdManager implementation for older Android versions
 */
class LegacyNsdImplementation(
    private val context: Context
) : MdnsImplementation {
    
    private val TAG = "LegacyNsdImpl"
    private val SERVICE_TYPE = "_autodroid._tcp.local."
    
    private lateinit var nsdManager: NsdManager
    private lateinit var discoveryListener: NsdManager.DiscoveryListener
    private lateinit var resolveListener: NsdManager.ResolveListener
    
    override fun isAvailable(): Boolean {
        return try {
            context.getSystemService(Context.NSD_SERVICE) as NsdManager
            true
        } catch (e: Exception) {
            Log.e(TAG, "NsdManager not available: ${e.message}")
            false
        }
    }
    
    override fun startDiscovery(callback: MdnsImplementation.Callback) {
        nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
        
        // Check if NsdManager is available
        if (nsdManager == null) {
            Log.e(TAG, "NsdManager not available on this device")
            callback.onDiscoveryFailed()
            return
        }
        
        initializeResolveListener(callback)
        initializeDiscoveryListener(callback)
        
        try {
            // Add a small delay before starting discovery to avoid race conditions
            Thread.sleep(200)
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
            Log.d(TAG, "Service discovery initiated successfully")
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception during discovery: ${e.message}")
            callback.onDiscoveryFailed()
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Illegal argument during discovery: ${e.message}")
            callback.onDiscoveryFailed()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start discovery: ${e.message}")
            callback.onDiscoveryFailed()
        }
    }
    
    override fun stopDiscovery() {
        Log.d(TAG, "Stopping LegacyNsdImplementation discovery")
        
        try {
            if (::nsdManager.isInitialized && ::discoveryListener.isInitialized) {
                nsdManager.stopServiceDiscovery(discoveryListener)
                Log.d(TAG, "Discovery stopped successfully")
            }
        } catch (e: IllegalArgumentException) {
            Log.d(TAG, "Discovery already stopped or never started: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping discovery: ${e.message}")
        }
    }
    
    private fun initializeDiscoveryListener(callback: MdnsImplementation.Callback) {
        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Discovery failed: Error code:$errorCode, ServiceType:$serviceType")
                Log.e(TAG, "Error meaning: ${getErrorName(errorCode)}")
                
                // Handle specific error codes
                when (errorCode) {
                    NsdManager.FAILURE_ALREADY_ACTIVE -> {
                        Log.w(TAG, "Discovery already active, stopping and retrying")
                        try {
                            nsdManager.stopServiceDiscovery(this)
                            // Retry after a short delay
                            Thread.sleep(1000)
                            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, this)
                            return // Don't fail, we're retrying
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to retry discovery: ${e.message}")
                        }
                    }
                    NsdManager.FAILURE_INTERNAL_ERROR -> {
                        Log.e(TAG, "Internal error, trying alternative approach")
                        // Try to stop and restart with a different approach
                        try {
                            nsdManager.stopServiceDiscovery(this)
                            Thread.sleep(2000) // Longer delay for internal error
                            // Try discovery with a different protocol or approach
                            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, this)
                            return // Don't fail, we're retrying
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to recover from internal error: ${e.message}")
                        }
                    }
                    NsdManager.FAILURE_MAX_LIMIT -> {
                        Log.e(TAG, "Max limit reached, cannot start discovery")
                    }
                }
                
                callback.onDiscoveryFailed()
            }
            
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Stop discovery failed: Error code:$errorCode")
                try {
                    nsdManager.stopServiceDiscovery(this)
                } catch (e: Exception) {
                    Log.e(TAG, "Error stopping discovery: ${e.message}")
                }
            }
            
            override fun onDiscoveryStarted(serviceType: String) {
                Log.d(TAG, "Service discovery started")
            }
            
            override fun onDiscoveryStopped(serviceType: String) {
                Log.d(TAG, "Service discovery stopped")
            }
            
            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service found: ${serviceInfo.getServiceName()}")
                
                // Check if service type is correct
                if (serviceInfo.getServiceType() != SERVICE_TYPE) {
                    Log.d(TAG, "Unknown Service Type: ${serviceInfo.getServiceType()}")
                    return
                }
                
                // Resolve service to get detailed information
                try {
                    @Suppress("DEPRECATION")
                    nsdManager.resolveService(serviceInfo, resolveListener)
                } catch (e: Exception) {
                    Log.e(TAG, "Error with legacy resolve API: ${e.message}")
                }
            }
            
            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service lost: ${serviceInfo.getServiceName()}")
            }
        }
    }
    
    private fun initializeResolveListener(callback: MdnsImplementation.Callback) {
        resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Resolve failed for ${serviceInfo.serviceName}: $errorCode")
                
                // Handle specific resolution errors
                when (errorCode) {
                    NsdManager.FAILURE_INTERNAL_ERROR -> {
                        Log.w(TAG, "Internal resolution error, retrying resolution")
                        try {
                            // Retry resolution after a short delay
                            Thread.sleep(1000)
                            nsdManager.resolveService(serviceInfo, this)
                            return // Don't fail, we're retrying
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to retry resolution: ${e.message}")
                        }
                    }
                    NsdManager.FAILURE_ALREADY_ACTIVE -> {
                        Log.w(TAG, "Resolution already active, waiting for completion")
                    }
                    NsdManager.FAILURE_MAX_LIMIT -> {
                        Log.e(TAG, "Max resolution limit reached")
                    }
                }
            }
            
            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Resolve Succeeded: $serviceInfo")
                
                val serviceName = serviceInfo.getServiceName()
                val host = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    serviceInfo.host?.hostAddress
                } else {
                    @Suppress("DEPRECATION")
                    serviceInfo.getHost().getHostAddress()
                }
                val port = serviceInfo.getPort()
                
                Log.d(TAG, "Resolved service: $serviceName at $host:$port")
                
                host?.let {
                    val serverInfo = ServerInfo(serviceName, it, port)
                    callback.onServiceFound(serverInfo)
                }
            }
        }
    }
    
    private fun getErrorName(errorCode: Int): String {
        return when (errorCode) {
            NsdManager.FAILURE_ALREADY_ACTIVE -> "FAILURE_ALREADY_ACTIVE"
            NsdManager.FAILURE_INTERNAL_ERROR -> "FAILURE_INTERNAL_ERROR"
            NsdManager.FAILURE_MAX_LIMIT -> "FAILURE_MAX_LIMIT"
            else -> "UNKNOWN_ERROR_$errorCode"
        }
    }
}