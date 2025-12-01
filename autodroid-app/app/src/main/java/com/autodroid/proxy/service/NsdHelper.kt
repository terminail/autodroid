// NsdHelper.java
package com.autodroid.proxy.service

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdManager.DiscoveryListener
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.autodroid.proxy.model.DiscoveredServer

class NsdHelper(context: Context, callback: ServiceDiscoveryCallback?) {
    private val context: Context?
    private val nsdManager: NsdManager?
    private var discoveryListener: DiscoveryListener? = null
    private var resolveListener: NsdManager.ResolveListener? = null
    private val callback: ServiceDiscoveryCallback?
    val discoveredServers: MutableList<DiscoveredServer?> = ArrayList<DiscoveredServer?>()

    interface ServiceDiscoveryCallback {
        fun onServiceFound(serviceName: String?, host: String?, port: Int)
        fun onServiceLost(serviceName: String?)
        fun onDiscoveryStarted()
        fun onDiscoveryFailed()
    }

    init {
        this.context = context
        this.callback = callback
        this.nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager?
    }

    fun initialize() {
        initializeResolveListener()
        initializeDiscoveryListener()
    }

    private fun initializeDiscoveryListener() {
        discoveryListener = object : DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode)
                nsdManager!!.stopServiceDiscovery(this)
                if (callback != null) callback.onDiscoveryFailed()
            }

            override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
                Log.e(TAG, "Stop discovery failed: Error code:" + errorCode)
                nsdManager!!.stopServiceDiscovery(this)
            }

            override fun onDiscoveryStarted(serviceType: String?) {
                Log.d(TAG, "Service discovery started")
                if (callback != null) callback.onDiscoveryStarted()
            }

            override fun onDiscoveryStopped(serviceType: String?) {
                Log.d(TAG, "Service discovery stopped")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service found: " + serviceInfo.getServiceName())


                // Check if service type is correct
                if (serviceInfo.getServiceType() != SERVICE_TYPE) {
                    Log.d(TAG, "Unknown Service Type: " + serviceInfo.getServiceType())
                    return
                }


                // Resolve service to get detailed information
                nsdManager!!.resolveService(serviceInfo, resolveListener)
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service lost: " + serviceInfo.getServiceName())
                if (callback != null) {
                    callback.onServiceLost(serviceInfo.getServiceName())
                }
            }
        }
    }

    private fun initializeResolveListener() {
        resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                Log.e(TAG, "Resolve failed: " + errorCode)
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Resolve Succeeded: " + serviceInfo)

                val serviceName = serviceInfo.getServiceName()
                val host = serviceInfo.getHost().getHostAddress()
                val port = serviceInfo.getPort()

                Log.d(TAG, "Resolved service: " + serviceName + " at " + host + ":" + port)

                if (callback != null) {
                    callback.onServiceFound(serviceName, host, port)
                }
            }
        }
    }

    fun discoverServices() {
        nsdManager!!.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    fun stopDiscovery() {
        if (nsdManager != null && discoveryListener != null) {
            nsdManager.stopServiceDiscovery(discoveryListener)
        }
    }

    fun tearDown() {
        stopDiscovery()
    }

    companion object {
        private const val TAG = "NsdHelper"
        private const val SERVICE_TYPE = "_autodroid._tcp."
    }
}