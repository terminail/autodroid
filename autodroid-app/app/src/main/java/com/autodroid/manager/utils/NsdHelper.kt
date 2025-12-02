package com.autodroid.manager.utils

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log

class NsdHelper(private val context: Context, private val callback: ServiceDiscoveryCallback) {
    private val TAG = "NsdHelper"
    private val SERVICE_TYPE = "_autodroid._tcp.local."
    
    private lateinit var nsdManager: NsdManager
    private lateinit var discoveryListener: NsdManager.DiscoveryListener
    private lateinit var resolveListener: NsdManager.ResolveListener
    
    interface ServiceDiscoveryCallback {
        fun onServiceFound(serviceName: String, host: String, port: Int)
        fun onServiceLost(serviceName: String)
        fun onDiscoveryStarted()
        fun onDiscoveryFailed()
    }
    
    fun initialize() {
        nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
        initializeResolveListener()
        initializeDiscoveryListener()
    }
    
    private fun initializeDiscoveryListener() {
        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Discovery failed: Error code:$errorCode, ServiceType:$serviceType")
                Log.e(TAG, "Error meaning: ${getErrorName(errorCode)}")
                // Don't try to stop discovery that never started - this causes IllegalArgumentException
                callback.onDiscoveryFailed()
            }
            
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Stop discovery failed: Error code:$errorCode")
                nsdManager.stopServiceDiscovery(this)
            }
            
            override fun onDiscoveryStarted(serviceType: String) {
                Log.d(TAG, "Service discovery started")
                callback.onDiscoveryStarted()
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
                nsdManager.resolveService(serviceInfo, resolveListener)
            }
            
            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service lost: ${serviceInfo.getServiceName()}")
                callback.onServiceLost(serviceInfo.getServiceName())
            }
        }
    }
    
    private fun initializeResolveListener() {
        resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Resolve failed: $errorCode")
            }
            
            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Resolve Succeeded: $serviceInfo")
                
                val serviceName = serviceInfo.getServiceName()
                val host = serviceInfo.getHost().getHostAddress()
                val port = serviceInfo.getPort()
                
                Log.d(TAG, "Resolved service: $serviceName at $host:$port")
                
                host?.let {
                    callback.onServiceFound(serviceName, it, port)
                }
            }
        }
    }
    
    fun discoverServices() {
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }
    
    fun stopDiscovery() {
        nsdManager.stopServiceDiscovery(discoveryListener)
    }
    
    fun tearDown() {
        try {
            stopDiscovery()
        } catch (e: IllegalArgumentException) {
            // Ignore exception if discovery is not active
            Log.d(TAG, "Discovery already stopped or never started")
        }
    }
    
    /**
     * Get human-readable error name for NsdManager error codes
     */
    private fun getErrorName(errorCode: Int): String {
        return when (errorCode) {
            NsdManager.FAILURE_ALREADY_ACTIVE -> "FAILURE_ALREADY_ACTIVE"
            NsdManager.FAILURE_INTERNAL_ERROR -> "FAILURE_INTERNAL_ERROR"
            NsdManager.FAILURE_MAX_LIMIT -> "FAILURE_MAX_LIMIT"
            else -> "UNKNOWN_ERROR_$errorCode"
        }
    }
}