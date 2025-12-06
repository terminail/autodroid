package com.autodroid.manager.service

import android.content.Context
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.autodroid.manager.model.Server
import java.io.IOException
import java.net.InetAddress
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceListener

/**
 * Third-party mDNS implementation using JmDNS library
 */
class JmDNSImplementation(
    private val context: Context
) : MdnsImplementation, DefaultLifecycleObserver {

    private val TAG = "JmDNSImplementation"
    private val SERVICE_TYPE = "_autodroid._tcp.local."
    
    private var jmdns: JmDNS? = null
    private var serviceListener: ServiceListener? = null
    private var isDiscovering = false
    
    override fun isAvailable(): Boolean {
        return try {
            // Check if JmDNS class is available
            Class.forName("javax.jmdns.JmDNS")
            // Try to create a temporary instance to verify functionality
            val localHost = InetAddress.getLocalHost()
            val testJmDNS = JmDNS.create(localHost)
            testJmDNS.close()
            true
        } catch (e: ClassNotFoundException) {
            Log.e(TAG, "JmDNS library not available: ${e.message}")
            false
        } catch (e: Exception) {
            Log.e(TAG, "JmDNS not functional: ${e.message}")
            false
        }
    }

    override fun startDiscovery(callback: MdnsImplementation.Callback) {
        if (isDiscovering) {
            Log.d(TAG, "Discovery already in progress")
            return
        }

        isDiscovering = true
        Log.d(TAG, "Starting JmDNS discovery")

        try {
            // Create JmDNS instance
            val localHost = InetAddress.getLocalHost()
            jmdns = JmDNS.create(localHost)
            
            // Create service listener
            serviceListener = object : ServiceListener {
                override fun serviceAdded(event: ServiceEvent) {
                    Log.d(TAG, "Service added: ${event.name}")
                    // Request service info
                    jmdns?.requestServiceInfo(event.type, event.name)
                }
                
                override fun serviceRemoved(event: ServiceEvent) {
                    Log.d(TAG, "Service removed: ${event.name}")
                }
                
                override fun serviceResolved(event: ServiceEvent) {
                    Log.d(TAG, "Service resolved: ${event.name}")
                    val info = event.info
                    
                    if (info != null) {
                        val hostAddresses = info.inet4Addresses
                        if (hostAddresses.isNotEmpty()) {
                            val host = hostAddresses[0].hostAddress
                            val port = info.port
                            
                            Log.d(TAG, "Resolved service: ${info.name} at $host:$port")
                            // Create API endpoint from discovered host and port
                            val apiEndpoint = "http://$host:$port/api"
                            val serverInfo = Server(
                                serviceName = info.name,
                                api_endpoint = apiEndpoint
                            )
                            callback.onServiceFound(serverInfo)
                        }
                    }
                }
            }
            
            // Add service listener
            jmdns?.addServiceListener(SERVICE_TYPE, serviceListener)
            
            // Get existing services
            val services = jmdns?.list(SERVICE_TYPE)
            services?.forEach { service ->
                Log.d(TAG, "Found existing service: ${service.name}")
                val hostAddresses = service.inet4Addresses
                if (hostAddresses.isNotEmpty()) {
                    val host = hostAddresses[0].hostAddress
                    val port = service.port
                    
                    Log.d(TAG, "Resolved existing service: ${service.name} at $host:$port")
                    // Create API endpoint from discovered host and port
                    val apiEndpoint = "http://$host:$port/api"
                    val serverInfo = Server(
                        serviceName = service.name,
                        api_endpoint = apiEndpoint
                    )
                    callback.onServiceFound(serverInfo)
                }
            }
            
            // Discovery is considered started successfully
            Log.d(TAG, "JmDNS discovery started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "JmDNS discovery failed: ${e.message}")
            callback.onDiscoveryFailed()
            isDiscovering = false
        }
    }

    override fun stopDiscovery() {
        if (!isDiscovering) {
            return
        }
        
        Log.d(TAG, "Stopping JmDNS discovery")
        isDiscovering = false
        
        try {
            serviceListener?.let { listener ->
                jmdns?.removeServiceListener(SERVICE_TYPE, listener)
            }
            
            jmdns?.close()
            jmdns = null
            serviceListener = null
        } catch (e: IOException) {
            Log.e(TAG, "Error stopping JmDNS discovery: ${e.message}")
        }
    }

    // Lifecycle management
    override fun onDestroy(owner: LifecycleOwner) {
        stopDiscovery()
    }
}